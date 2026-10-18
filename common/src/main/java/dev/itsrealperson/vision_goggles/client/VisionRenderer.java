package dev.itsrealperson.vision_goggles.client;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.network.ToggleNVGPacket;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.entity.player.Player;

import java.util.List;

public class VisionRenderer {
    private static boolean visorActive = false;
    private static boolean grayscaleEnabled = false;
    private static int lastModeId = -1;
    private static VisionMode currentVisionMode = VisionMode.NIGHT_VISION;
    private static float currentBatteryPct = 1.0f;
    public static float getCurrentBatteryPct() { return currentBatteryPct; }
    private static int damageFlickerTimer = 0;
    private static boolean lastServerActive = false;
    private static boolean lastWasUnderwater = false;

    private static boolean zoomActive = false;
    private static boolean zoomLocked = false;
    public static boolean isZoomLocked() { return zoomLocked; }
    public static void toggleZoomLock(Minecraft mc) {
        zoomLocked = !zoomLocked;
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable(zoomLocked ? "message.vision_goggles.zoom_locked" : "message.vision_goggles.zoom_unlocked"), true);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(zoomLocked ? ModSounds.VISOR_ON.get() : ModSounds.VISOR_OFF.get(), 1.5F));
        }
    }
    private static float targetZoomFactor = 1.0f; // 1.0x to 8.0x
    private static float currentZoomFactor = 1.0f; // Smooth lerped magnification
    private static float currentZoom = 1.0f; // FOV multiplier = 1.0f / currentZoomFactor

    private static long nKeyHoldStart = 0L;
    private static boolean radialMenuOpened = false;

    private static boolean lastFlashlightActive = false;
    private static int lastFlashlightModeId = -1;

    private static int sonarPulseTimer = 0;
    private static final int SONAR_PULSE_INTERVAL = 200; // 10 seconds
    private static final int SONAR_PULSE_DURATION = 40; 

    private static boolean visionModeActive = false;

    public static boolean isVisorActive() { return visorActive; }
    public static boolean isVisionModeActive() { return visionModeActive; }
    public static VisionMode getCurrentVisionMode() { return currentVisionMode; }
    public static boolean isZoomActive() { return zoomActive || currentZoomFactor > 1.05f; }
    public static float getZoomMultiplier() { return currentZoom; }
    public static float getCurrentZoomFactor() { return currentZoomFactor; }
    public static boolean isSonarActive() { return sonarPulseTimer > 0 && sonarPulseTimer < SONAR_PULSE_DURATION; }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(VisionRenderer::onClientTick);
        ClientGuiEvent.RENDER_HUD.register((guiGraphics, partialTicks) -> {
            renderHUD(guiGraphics, partialTicks);
        });

        dev.architectury.event.events.common.PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player.level().isClientSide) {
                Minecraft mc = Minecraft.getInstance();
                cleanup(mc);
            }
        });

        dev.architectury.event.events.common.EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (entity.level().isClientSide && entity == Minecraft.getInstance().player && visorActive) {
                damageFlickerTimer = 10; 
            }
            return EventResult.pass();
        });
    }

    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        if (damageFlickerTimer > 0) {
            damageFlickerTimer--;
        }

        // Do not process in-game hotkeys if a screen is active
        if (mc.screen instanceof dev.itsrealperson.vision_goggles.client.gui.TacticalRadialScreen) {
            nKeyHoldStart = 0L;
            radialMenuOpened = false;
            return;
        }

        // 'N' Key: Quick tap (< 220ms) = Toggle on/off; Long press (> 220ms) = Tactical Radial Menu
        if (ModKeyMappings.toggleGrayscaleKey.isDown()) {
            if (nKeyHoldStart == 0L) {
                nKeyHoldStart = System.currentTimeMillis();
                radialMenuOpened = false;
            } else if (!radialMenuOpened && (System.currentTimeMillis() - nKeyHoldStart > 220L)) {
                if (mc.screen == null && mc.player != null) {
                    radialMenuOpened = true;
                    mc.setScreen(new dev.itsrealperson.vision_goggles.client.gui.TacticalRadialScreen());
                }
            }
        } else {
            if (nKeyHoldStart != 0L) {
                if (!radialMenuOpened) {
                    NetworkManager.INSTANCE.sendToServer(new ToggleNVGPacket(false));
                }
                nKeyHoldStart = 0L;
                radialMenuOpened = false;
            }
        }

        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        boolean hasHelmet = !helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem;
        VisionGogglesItem goggles = hasHelmet ? (VisionGogglesItem) helmet.getItem() : null;
        boolean hasSonar = hasHelmet && goggles.hasModule(helmet, ModConstants.ID_SONAR);
        boolean hasZoom = hasHelmet && goggles.hasModule(helmet, ModConstants.ID_ZOOM);

        
        while (ModKeyMappings.toggleFlashlightKey.consumeClick()) {
            if (dev.itsrealperson.vision_goggles.client.gui.TacticalRadialScreen.hasFlashlightAvailable(mc.player)) {
                NetworkManager.INSTANCE.sendToServer(new dev.itsrealperson.vision_goggles.network.ToggleFlashlightPacket());
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.FLASHLIGHT_SWITCH.get(), 1.0F));
            }
        }
        
        while (ModKeyMappings.cycleFlashlightModeKey.consumeClick()) {
            if (dev.itsrealperson.vision_goggles.client.gui.TacticalRadialScreen.hasFlashlightAvailable(mc.player)) {
                NetworkManager.INSTANCE.sendToServer(new dev.itsrealperson.vision_goggles.network.CycleFlashlightModePacket());
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.FLASHLIGHT_MODE.get(), 1.0F));
            }
        }

        if (hasHelmet) {
            CompoundTag nbt = helmet.getOrCreateTag();
            boolean isServerActive = nbt.getBoolean(ModConstants.TAG_ACTIVE);
            
            int nbtModeId = nbt.contains(ModConstants.TAG_MODE) ? nbt.getInt(ModConstants.TAG_MODE) : -1;
            
            // If mode ID is invalid (-1) or not found, try to get default from goggles, 
            // BUT for Modular Goggles, default might be null/empty if no modules installed.
            if (nbtModeId == -1) {
                if (goggles instanceof ModularGogglesItem modular) {
                     List<VisionMode> modes = modular.getModes(helmet);
                     if (!modes.isEmpty()) {
                         currentVisionMode = modes.get(0);
                         nbtModeId = currentVisionMode.getId();
                     } else {
                         currentVisionMode = null;
                     }
                } else {
                    currentVisionMode = goggles.getVisionMode();
                    nbtModeId = currentVisionMode.getId();
                }
            } else {
                currentVisionMode = VisionMode.byId(nbtModeId);
            }
            
            float maxBattery = (float) goggles.getBatteryCapacity(helmet);
            float currentBattery = nbt.contains(ModConstants.TAG_BATTERY) ? nbt.getFloat(ModConstants.TAG_BATTERY) : maxBattery;
            currentBatteryPct = currentBattery / maxBattery;
            boolean isFlashlightActive = nbt.getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE) && currentBatteryPct > 0.0f;
            visionModeActive = isServerActive && currentVisionMode != null;
            visorActive = isServerActive;

            // Shader Lifecycle Management
            boolean shaderNeeded = (currentVisionMode != null);

            if (isServerActive != lastServerActive || (isServerActive && nbtModeId != lastModeId)) {
                if (isServerActive) {
                    if (shaderNeeded) {
                        grayscaleEnabled = true;
                        VisionShaderManager.enableShader(mc, currentVisionMode);
                    } else {
                        grayscaleEnabled = false;
                        VisionShaderManager.disableShader(mc);
                    }
                    lastModeId = nbtModeId;
                    if (isServerActive != lastServerActive) {
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_ON.get(), 1.0F));
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, 1.25F));
                    }
                } else {
                    grayscaleEnabled = false;
                    lastModeId = -1;
                    zoomLocked = false;
                    VisionShaderManager.disableShader(mc);
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_OFF.get(), 1.0F));
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC, 0.85F));
                }
                // Al cambiar de modo, limpiar el efecto de visión nocturna SOLO si el nuevo modo
                // no lo requiere. THERMAL e HYDRO reciben NV del servidor, no se deben limpiar aquí.
                if (mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                    boolean newModeUsesNV = currentVisionMode == VisionMode.NIGHT_VISION
                                        || currentVisionMode == VisionMode.THERMAL
                                        || currentVisionMode == VisionMode.HYDRO;
                    if (!newModeUsesNV) {
                        mc.player.removeEffect(MobEffects.NIGHT_VISION);
                    }
                }
                lastServerActive = isServerActive;
            }
            int flashlightModeId = nbt.contains(ModConstants.TAG_FLASHLIGHT_MODE) ? nbt.getInt(ModConstants.TAG_FLASHLIGHT_MODE) : 0;
            ItemStack heldFlashlight = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getHeldActiveFlashlight(mc.player);
            if (!heldFlashlight.isEmpty() && !isFlashlightActive) {
                isFlashlightActive = true;
                flashlightModeId = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getMode(heldFlashlight);
            }

            if (isFlashlightActive != lastFlashlightActive || (isFlashlightActive && flashlightModeId != lastFlashlightModeId)) {
                if (isFlashlightActive) {
                    VisionShaderManager.enableFlashlight(mc, flashlightModeId);
                } else {
                    VisionShaderManager.disableFlashlight(mc);
                }
                lastFlashlightActive = isFlashlightActive;
                lastFlashlightModeId = flashlightModeId;
            }

            // Update Grayscale state for tick updates
            grayscaleEnabled = isServerActive && shaderNeeded;

            if (isServerActive && hasSonar) {
                if (sonarPulseTimer > 0) sonarPulseTimer--;
                else sonarPulseTimer = SONAR_PULSE_INTERVAL;
            } else {
                sonarPulseTimer = 0;
            }

            // Battery Alert
            if (isServerActive && currentBatteryPct <= 0.10f && mc.level.getGameTime() % 40 == 0) {
                mc.player.playSound(dev.itsrealperson.vision_goggles.registry.ModSounds.BLIP.get(), 1.0f, 1.0f);
            }

            // Oxygen Alert
            if (isServerActive && goggles.hasModule(helmet, ModConstants.ID_ENVIRONMENT)) {
                int air = mc.player.getAirSupply();
                int maxAir = mc.player.getMaxAirSupply();
                if (air < maxAir * 0.20 && mc.level.getGameTime() % 20 == 0) {
                    mc.player.playSound(dev.itsrealperson.vision_goggles.registry.ModSounds.BLIP.get(), 1.0f, 0.8f);
                }
            }

            if (isServerActive && hasZoom && (ModKeyMappings.zoomKey.isDown() || zoomLocked)) {
                zoomActive = true;
                if (targetZoomFactor < 1.2f) {
                    targetZoomFactor = 2.5f; // Default starting magnification
                }
            } else {
                zoomActive = false;
                targetZoomFactor = 1.0f;
            }

            // Smooth continuous FOV lerp
            currentZoomFactor += (targetZoomFactor - currentZoomFactor) * 0.22f;
            if (Math.abs(currentZoomFactor - 1.0f) < 0.01f && !zoomActive) {
                currentZoomFactor = 1.0f;
            }
            currentZoom = 1.0f / currentZoomFactor;

            // Shader updates (battery failure, color filter, etc.)
            if (isServerActive && shaderNeeded) {
                boolean isUnderwater = mc.player.isUnderWater();

                // Initial enable if shader isn't loaded yet (first equip, F5 switch, etc.)
                // Frame-rate restoration is handled by MixinGameRenderer.vision_goggles$ensureShaderPerFrame
                if (!VisionShaderManager.isShaderActive()) {
                    VisionShaderManager.enableShader(mc, currentVisionMode);
                }

                // Hydro-mode needs a shader swap when entering/leaving water
                if (currentVisionMode == VisionMode.HYDRO && isUnderwater != lastWasUnderwater) {
                    VisionShaderManager.enableShader(mc, currentVisionMode);
                }
                lastWasUnderwater = isUnderwater;

                VisionShaderManager.update(currentVisionMode, currentBatteryPct, damageFlickerTimer > 0);
            }

            try {
                dev.itsrealperson.vision_goggles.client.audio.GogglesHumSoundInstance.updateHum(mc.player);
            } catch (Throwable ignored) {}
        } else {
            // Check handheld tactical flashlight when not wearing goggles
            currentVisionMode = null;
            visionModeActive = false;
            visorActive = false;
            zoomLocked = false;
            zoomActive = false;
            targetZoomFactor = 1.0f;

            ItemStack heldFlashlight = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getHeldActiveFlashlight(mc.player);
            boolean isHandheldActive = !heldFlashlight.isEmpty();
            int handFlashlightModeId = isHandheldActive ? dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getMode(heldFlashlight) : 0;

            if (isHandheldActive != lastFlashlightActive || (isHandheldActive && handFlashlightModeId != lastFlashlightModeId)) {
                if (isHandheldActive) {
                    VisionShaderManager.enableFlashlight(mc, handFlashlightModeId);
                } else {
                    VisionShaderManager.disableFlashlight(mc);
                }
                lastFlashlightActive = isHandheldActive;
                lastFlashlightModeId = handFlashlightModeId;
            }

            if (isHandheldActive) {
                currentBatteryPct = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getBattery(heldFlashlight) / dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.MAX_BATTERY;
            }

            if (grayscaleEnabled || lastServerActive) {
                cleanup(mc);
            }
        }

        dev.itsrealperson.vision_goggles.scripting.LuaModuleManager.onClientTick(mc, currentBatteryPct);
    }

    private static class ActiveFlashlightSource {
        final FlashlightSlot slot;
        final int modeId;

        ActiveFlashlightSource(FlashlightSlot slot, int modeId) {
            this.slot = slot;
            this.modeId = modeId;
        }
    }

    public static void updateFlashlightEveryFrame(net.minecraft.client.Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.beginUpdate(camera.rotation());
        net.minecraft.world.phys.Vec3 camPos = camera.getPosition();

        for (Player player : mc.level.players()) {
            List<ActiveFlashlightSource> sources = new java.util.ArrayList<>();

            // 1. Módulo en Gafas / Casco (Cabeza)
            ItemStack playerHelmet = PlatformMethods.getEquippedHelmet(player);
            if (playerHelmet.getItem() instanceof ModularGogglesItem modular) {
                List<ModuleType> utils = modular.getUtilityModules(playerHelmet);
                boolean hasFlashlight = utils.contains(ModuleType.FLASHLIGHT);
                float batteryLvl = playerHelmet.getOrCreateTag().getFloat(ModConstants.TAG_BATTERY);
                if (hasFlashlight && playerHelmet.getOrCreateTag().getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE) && batteryLvl > 0) {
                    int modeId = playerHelmet.getOrCreateTag().getInt(ModConstants.TAG_FLASHLIGHT_MODE);
                    sources.add(new ActiveFlashlightSource(FlashlightSlot.HEAD_GOGGLES, modeId));
                }
            }

            // 2. Linterna de Torso / Chaleco (Curios, Trinkets, Slot Pechera)
            ItemStack equippedChest = PlatformMethods.getEquippedFlashlight(player);
            if (dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.isActive(equippedChest)) {
                int modeId = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getMode(equippedChest);
                sources.add(new ActiveFlashlightSource(FlashlightSlot.TORSO_CHEST, modeId));
            }

            // 3. Mano Principal (si no es la misma que la del pecho)
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand != equippedChest && dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.isActive(mainHand)) {
                int modeId = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getMode(mainHand);
                sources.add(new ActiveFlashlightSource(FlashlightSlot.MAIN_HAND, modeId));
            }

            // 4. Mano Secundaria
            ItemStack offHand = player.getOffhandItem();
            if (offHand != equippedChest && dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.isActive(offHand)) {
                int modeId = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getMode(offHand);
                sources.add(new ActiveFlashlightSource(FlashlightSlot.OFF_HAND, modeId));
            }

            if (sources.isEmpty()) continue;

            boolean isLocalPlayer = (player == mc.player);
            boolean isFirstPerson = isLocalPlayer && (mc.options.getCameraType() != null && mc.options.getCameraType().isFirstPerson());

            float yaw = player.getViewYRot(partialTick);
            float pitch = player.getViewXRot(partialTick);
            org.joml.Quaternionf headRot = new org.joml.Quaternionf()
                .rotationY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch));

            for (ActiveFlashlightSource src : sources) {
                net.minecraft.world.phys.Vec3 originWorld;
                org.joml.Vector3f dir;
                org.joml.Vector3f up;
                org.joml.Vector3f right;

                if (isFirstPerson) {
                    org.joml.Vector3f camDir = headRot.transform(new org.joml.Vector3f(0, 0, 1));
                    org.joml.Vector3f camUp = headRot.transform(new org.joml.Vector3f(0, 1, 0));
                    org.joml.Vector3f camRight = headRot.transform(new org.joml.Vector3f(1, 0, 0));

                    switch (src.slot) {
                        case HEAD_GOGGLES -> {
                            originWorld = camPos.add(camUp.x * 0.08 + camRight.x * 0.12,
                                                     camUp.y * 0.08 + camRight.y * 0.12,
                                                     camUp.z * 0.08 + camRight.z * 0.12);
                            dir = new org.joml.Vector3f(camDir);
                            up = new org.joml.Vector3f(camUp);
                            right = new org.joml.Vector3f(camRight);
                        }
                        case TORSO_CHEST -> {
                            originWorld = camPos.add(-camUp.x * 0.35 + camRight.x * 0.18 + camDir.x * 0.15,
                                                     -camUp.y * 0.35 + camRight.y * 0.18 + camDir.y * 0.15,
                                                     -camUp.z * 0.35 + camRight.z * 0.18 + camDir.z * 0.15);
                            dir = new org.joml.Vector3f(camDir);
                            up = new org.joml.Vector3f(camUp);
                            right = new org.joml.Vector3f(camRight);
                        }
                        case MAIN_HAND -> {
                            originWorld = camPos.add(-camUp.x * 0.25 + camRight.x * 0.32 + camDir.x * 0.35,
                                                     -camUp.y * 0.25 + camRight.y * 0.32 + camDir.y * 0.35,
                                                     -camUp.z * 0.25 + camRight.z * 0.32 + camDir.z * 0.35);
                            net.minecraft.world.phys.Vec3 target = camPos.add(camDir.x * 30.0, camDir.y * 30.0, camDir.z * 30.0);
                            net.minecraft.world.phys.Vec3 delta = target.subtract(originWorld).normalize();
                            dir = new org.joml.Vector3f((float) delta.x, (float) delta.y, (float) delta.z);
                            up = new org.joml.Vector3f(camUp);
                            right = new org.joml.Vector3f(camRight);
                        }
                        case OFF_HAND -> {
                            originWorld = camPos.add(-camUp.x * 0.25 - camRight.x * 0.32 + camDir.x * 0.35,
                                                     -camUp.y * 0.25 - camRight.y * 0.32 + camDir.y * 0.35,
                                                     -camUp.z * 0.25 - camRight.z * 0.32 + camDir.z * 0.35);
                            net.minecraft.world.phys.Vec3 target = camPos.add(camDir.x * 30.0, camDir.y * 30.0, camDir.z * 30.0);
                            net.minecraft.world.phys.Vec3 delta = target.subtract(originWorld).normalize();
                            dir = new org.joml.Vector3f((float) delta.x, (float) delta.y, (float) delta.z);
                            up = new org.joml.Vector3f(camUp);
                            right = new org.joml.Vector3f(camRight);
                        }
                        default -> {
                            originWorld = player.getEyePosition(partialTick);
                            dir = new org.joml.Vector3f(camDir);
                            up = new org.joml.Vector3f(camUp);
                            right = new org.joml.Vector3f(camRight);
                        }
                    }
                } else {
                    // 3RA PERSONA Y JUGADORES REMOTOS
                    double px = net.minecraft.util.Mth.lerp(partialTick, player.xo, player.getX());
                    double py = net.minecraft.util.Mth.lerp(partialTick, player.yo, player.getY());
                    double pz = net.minecraft.util.Mth.lerp(partialTick, player.zo, player.getZ());
                    float bodyYaw = net.minecraft.util.Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);

                    switch (src.slot) {
                        case HEAD_GOGGLES -> {
                            originWorld = player.getEyePosition(partialTick);
                            dir = headRot.transform(new org.joml.Vector3f(0, 0, 1));
                            up = headRot.transform(new org.joml.Vector3f(0, 1, 0));
                            right = headRot.transform(new org.joml.Vector3f(1, 0, 0));
                        }
                        case TORSO_CHEST -> {
                            double torsoY = py + (player.isCrouching() ? 1.05 : 1.25);
                            float bodyPitch = player.isCrouching() ? 28.6f : 0.0f;
                            org.joml.Quaternionf torsoRot = new org.joml.Quaternionf()
                                .rotationY((float) Math.toRadians(-bodyYaw))
                                .rotateX((float) Math.toRadians(bodyPitch));

                            org.joml.Vector3f localOffset = torsoRot.transform(new org.joml.Vector3f(0.18f, -0.20f, 0.16f));
                            originWorld = new net.minecraft.world.phys.Vec3(px + localOffset.x, torsoY + localOffset.y, pz + localOffset.z);

                            dir = torsoRot.transform(new org.joml.Vector3f(0, 0, 1));
                            up = torsoRot.transform(new org.joml.Vector3f(0, 1, 0));
                            right = torsoRot.transform(new org.joml.Vector3f(1, 0, 0));
                        }
                        case MAIN_HAND -> {
                            boolean isRightArm = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
                            originWorld = computeArmHandWorldPos(player, px, py, pz, bodyYaw, headRot, isRightArm, partialTick);
                            dir = headRot.transform(new org.joml.Vector3f(0, 0, 1));
                            up = headRot.transform(new org.joml.Vector3f(0, 1, 0));
                            right = headRot.transform(new org.joml.Vector3f(1, 0, 0));
                        }
                        case OFF_HAND -> {
                            boolean isRightArm = player.getMainArm() != net.minecraft.world.entity.HumanoidArm.RIGHT;
                            originWorld = computeArmHandWorldPos(player, px, py, pz, bodyYaw, headRot, isRightArm, partialTick);
                            dir = headRot.transform(new org.joml.Vector3f(0, 0, 1));
                            up = headRot.transform(new org.joml.Vector3f(0, 1, 0));
                            right = headRot.transform(new org.joml.Vector3f(1, 0, 0));
                        }
                        default -> {
                            originWorld = player.getEyePosition(partialTick);
                            dir = headRot.transform(new org.joml.Vector3f(0, 0, 1));
                            up = headRot.transform(new org.joml.Vector3f(0, 1, 0));
                            right = headRot.transform(new org.joml.Vector3f(1, 0, 0));
                        }
                    }
                }

                org.joml.Vector3f pos = new org.joml.Vector3f(
                    (float)(originWorld.x - camPos.x),
                    (float)(originWorld.y - camPos.y),
                    (float)(originWorld.z - camPos.z)
                );

                org.joml.Vector3f color = new org.joml.Vector3f(1.0f, 0.95f, 0.85f);
                FlashlightMode fMode = FlashlightMode.byId(src.modeId);

                float offsetAngle = (float) Math.acos(fMode.coneOuter) * 0.85f;
                float diagOffset = offsetAngle * 0.7071f;

                org.joml.Vector3f dirCenter = new org.joml.Vector3f(dir);
                org.joml.Vector3f dirUp = new org.joml.Vector3f(dir).rotateAxis(-offsetAngle, right.x, right.y, right.z);
                org.joml.Vector3f dirDown = new org.joml.Vector3f(dir).rotateAxis(offsetAngle, right.x, right.y, right.z);
                org.joml.Vector3f dirLeft = new org.joml.Vector3f(dir).rotateAxis(-offsetAngle, up.x, up.y, up.z);
                org.joml.Vector3f dirRight = new org.joml.Vector3f(dir).rotateAxis(offsetAngle, up.x, up.y, up.z);

                org.joml.Vector3f dirUpLeft = new org.joml.Vector3f(dir).rotateAxis(-diagOffset, right.x, right.y, right.z).rotateAxis(-diagOffset, up.x, up.y, up.z);
                org.joml.Vector3f dirUpRight = new org.joml.Vector3f(dir).rotateAxis(-diagOffset, right.x, right.y, right.z).rotateAxis(diagOffset, up.x, up.y, up.z);
                org.joml.Vector3f dirDownLeft = new org.joml.Vector3f(dir).rotateAxis(diagOffset, right.x, right.y, right.z).rotateAxis(-diagOffset, up.x, up.y, up.z);
                org.joml.Vector3f dirDownRight = new org.joml.Vector3f(dir).rotateAxis(diagOffset, right.x, right.y, right.z).rotateAxis(diagOffset, up.x, up.y, up.z);

                float[] hitRanges = new float[9];
                hitRanges[0] = performRaycast(player, originWorld, dirCenter, fMode.range);
                hitRanges[1] = performRaycast(player, originWorld, dirUp, fMode.range);
                hitRanges[2] = performRaycast(player, originWorld, dirDown, fMode.range);
                hitRanges[3] = performRaycast(player, originWorld, dirRight, fMode.range);
                hitRanges[4] = performRaycast(player, originWorld, dirLeft, fMode.range);
                hitRanges[5] = performRaycast(player, originWorld, dirUpLeft, fMode.range);
                hitRanges[6] = performRaycast(player, originWorld, dirUpRight, fMode.range);
                hitRanges[7] = performRaycast(player, originWorld, dirDownLeft, fMode.range);
                hitRanges[8] = performRaycast(player, originWorld, dirDownRight, fMode.range);

                boolean isLocal = isLocalPlayer && isFirstPerson;
                boolean isWall = checkIsWall(player, originWorld, dirCenter, fMode.range);
                dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.addFlashlight(pos, dir, up, right, color, fMode, hitRanges, isLocal, isWall);
            }
        }
    }

    private static net.minecraft.world.phys.Vec3 computeArmHandWorldPos(Player player, double px, double py, double pz, float bodyYaw, org.joml.Quaternionf headRot, boolean rightArm, float partialTick) {
        double shoulderY = py + (player.isCrouching() ? 1.05 : 1.25);
        float sideSign = rightArm ? 1.0f : -1.0f;

        org.joml.Quaternionf bodyRot = new org.joml.Quaternionf().rotationY((float) Math.toRadians(-bodyYaw));
        org.joml.Vector3f shoulderOffset = bodyRot.transform(new org.joml.Vector3f(sideSign * 0.35f, 0.0f, 0.0f));
        net.minecraft.world.phys.Vec3 shoulderPos = new net.minecraft.world.phys.Vec3(px + shoulderOffset.x, shoulderY + shoulderOffset.y, pz + shoulderOffset.z);

        org.joml.Vector3f armLook = headRot.transform(new org.joml.Vector3f(0, 0, 1));
        return shoulderPos.add(armLook.x * 0.38, armLook.y * 0.38 - 0.22, armLook.z * 0.38);
    }


    private static boolean checkIsWall(Player player, net.minecraft.world.phys.Vec3 startVec, org.joml.Vector3f dir, float maxRange) {
        net.minecraft.world.phys.Vec3 endVec = startVec.add(dir.x() * maxRange, dir.y() * maxRange, dir.z() * maxRange);
        net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(
            startVec, endVec,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            player
        );
        net.minecraft.world.phys.HitResult hitResult = player.level().clip(context);
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr) {
            net.minecraft.core.Direction face = bhr.getDirection();
            return face == net.minecraft.core.Direction.NORTH || face == net.minecraft.core.Direction.SOUTH || face == net.minecraft.core.Direction.EAST || face == net.minecraft.core.Direction.WEST;
        }
        return false;
    }

    private static float performRaycast(Player player, net.minecraft.world.phys.Vec3 startVec, org.joml.Vector3f dir, float maxRange) {
        net.minecraft.world.phys.Vec3 endVec = startVec.add(dir.x() * maxRange, dir.y() * maxRange, dir.z() * maxRange);
        net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(
            startVec,
            endVec,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            player
        );
        net.minecraft.world.phys.HitResult hitResult = player.level().clip(context);
        if (hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            double hitDist = hitResult.getLocation().distanceTo(startVec);
            return (float) Math.min(maxRange, hitDist + 1.2);
        }
        return maxRange;
    }

    private static void cleanup(Minecraft mc) {
        visorActive = false;
        visionModeActive = false;
        currentVisionMode = null;
        grayscaleEnabled = false;
        lastServerActive = false;
        lastModeId = -1;
        sonarPulseTimer = 0;
        damageFlickerTimer = 0;
        zoomActive = false;
        targetZoomFactor = 1.0f;
        currentZoomFactor = 1.0f;
        currentZoom = 1.0f;
        VisionShaderManager.disableShader(mc);
        dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.beginUpdate(null);
        if (mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
        try {
            dev.itsrealperson.vision_goggles.client.audio.GogglesHumSoundInstance.updateHum(mc.player);
        } catch (Throwable ignored) {}
    }

    public static boolean onMouseScroll(double delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        if (helmet.isEmpty() || !(helmet.getItem() instanceof VisionGogglesItem goggles)) return false;

        boolean hasZoom = false;
        if (goggles instanceof ModularGogglesItem modular) {
            hasZoom = modular.getUtilityModules(helmet).contains(ModuleType.ZOOM);
        }

        boolean isServerActive = helmet.hasTag() && helmet.getTag().getBoolean(ModConstants.TAG_ACTIVE);
        if (isServerActive && hasZoom && ModKeyMappings.zoomKey.isDown()) {
            if (delta > 0) {
                targetZoomFactor = Math.min(8.0f, targetZoomFactor + 0.5f);
            } else if (delta < 0) {
                targetZoomFactor = Math.max(1.2f, targetZoomFactor - 0.5f);
            }
            mc.player.playSound(dev.itsrealperson.vision_goggles.registry.ModSounds.BLIP.get(), 0.5f, 1.4f);
            return true;
        }
        return false;
    }

    public static void renderHUD(GuiGraphics g, float partialTicks) {
        if (!visorActive) return;
        
        // Delegar al nuevo sistema modular
        VisionHUDOverlay.render(g, partialTicks);

        // Renderizar Sonar Pulse (Legacy, pendiente de mover a módulo en v2.1.0)
        if (sonarPulseTimer > 0 && sonarPulseTimer < SONAR_PULSE_DURATION) {
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            float progress = 1.0f - ((float)sonarPulseTimer / (float)SONAR_PULSE_DURATION);
            renderSonarPulse(g, width, height, progress);
        }
    }

    private static void renderSonarPulse(GuiGraphics g, int width, int height, float progress) {
        // Disabled visually as requested by user
    }
}
