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
            mc.player.displayClientMessage(Component.literal("§b[Zoom]: §f" + (zoomLocked ? "§aFijado (2.5x)" : "§cDesactivado")), true);
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

        while (ModKeyMappings.switchModeKey.consumeClick()) {
            if (hasHelmet) {
                NetworkManager.INSTANCE.sendToServer(new ToggleNVGPacket(true));
            }
        }

        while (ModKeyMappings.switchSonarModeKey.consumeClick()) {
            if (hasSonar) {
                NetworkManager.INSTANCE.sendToServer(new dev.itsrealperson.vision_goggles.network.ToggleSonarPacket());
            }
        }
        
        while (ModKeyMappings.toggleFlashlightKey.consumeClick()) {
            if (dev.itsrealperson.vision_goggles.client.gui.TacticalRadialScreen.hasFlashlightAvailable(mc.player)) {
                NetworkManager.INSTANCE.sendToServer(new dev.itsrealperson.vision_goggles.network.ToggleFlashlightPacket());
            }
        }
        
        while (ModKeyMappings.cycleFlashlightModeKey.consumeClick()) {
            if (dev.itsrealperson.vision_goggles.client.gui.TacticalRadialScreen.hasFlashlightAvailable(mc.player)) {
                NetworkManager.INSTANCE.sendToServer(new dev.itsrealperson.vision_goggles.network.CycleFlashlightModePacket());
            }
        }

        while (ModKeyMappings.rechargeKey.consumeClick()) {
            if (hasHelmet || dev.itsrealperson.vision_goggles.client.gui.TacticalRadialScreen.hasFlashlightAvailable(mc.player)) {
                NetworkManager.INSTANCE.sendToServer(new dev.itsrealperson.vision_goggles.network.BatteryPacket());
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

    public static void updateFlashlightEveryFrame(net.minecraft.client.Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.beginUpdate(camera.rotation());
        net.minecraft.world.phys.Vec3 camPos = camera.getPosition();

        for (Player player : mc.level.players()) {
            boolean flashlightOn = false;
            int modeId = 0;

            ItemStack playerHelmet = PlatformMethods.getEquippedHelmet(player);
            if (playerHelmet.getItem() instanceof ModularGogglesItem modular) {
                List<ModuleType> utils = modular.getUtilityModules(playerHelmet);
                boolean hasFlashlight = utils.contains(ModuleType.FLASHLIGHT);
                float batteryLvl = playerHelmet.getOrCreateTag().getFloat(ModConstants.TAG_BATTERY);
                if (hasFlashlight && playerHelmet.getOrCreateTag().getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE) && batteryLvl > 0) {
                    flashlightOn = true;
                    modeId = playerHelmet.getOrCreateTag().getInt(ModConstants.TAG_FLASHLIGHT_MODE);
                }
            }

            if (!flashlightOn) {
                ItemStack heldFlashlight = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getHeldActiveFlashlight(player);
                if (!heldFlashlight.isEmpty()) {
                    flashlightOn = true;
                    modeId = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getMode(heldFlashlight);
                }
            }

            if (flashlightOn) {
                    net.minecraft.world.phys.Vec3 playerEyePos = player.getEyePosition(partialTick);
                    org.joml.Vector3f pos = new org.joml.Vector3f(
                        (float)(playerEyePos.x - camPos.x),
                        (float)(playerEyePos.y - camPos.y),
                        (float)(playerEyePos.z - camPos.z)
                    );
                    
                    float yaw = player.getViewYRot(partialTick);
                    float pitch = player.getViewXRot(partialTick);
                    org.joml.Quaternionf rot = new org.joml.Quaternionf()
                        .rotationY((float) Math.toRadians(-yaw))
                        .rotateX((float) Math.toRadians(pitch));

                    org.joml.Vector3f dir = rot.transform(new org.joml.Vector3f(0, 0, 1));
                    org.joml.Vector3f up = rot.transform(new org.joml.Vector3f(0, 1, 0));
                    org.joml.Vector3f right = rot.transform(new org.joml.Vector3f(1, 0, 0));
                    
                    org.joml.Vector3f color = new org.joml.Vector3f(1.0f, 0.95f, 0.85f);
                    
                    dev.itsrealperson.vision_goggles.util.FlashlightMode fMode = dev.itsrealperson.vision_goggles.util.FlashlightMode.byId(modeId);
                    
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
                    hitRanges[0] = performRaycast(player, playerEyePos, dirCenter, fMode.range);
                    hitRanges[1] = performRaycast(player, playerEyePos, dirUp, fMode.range);       // 90 deg
                    hitRanges[2] = performRaycast(player, playerEyePos, dirDown, fMode.range);     // 270 deg
                    hitRanges[3] = performRaycast(player, playerEyePos, dirRight, fMode.range);    // 0 deg
                    hitRanges[4] = performRaycast(player, playerEyePos, dirLeft, fMode.range);     // 180 deg
                    hitRanges[5] = performRaycast(player, playerEyePos, dirUpLeft, fMode.range);   // 135 deg
                    hitRanges[6] = performRaycast(player, playerEyePos, dirUpRight, fMode.range);  // 45 deg
                    hitRanges[7] = performRaycast(player, playerEyePos, dirDownLeft, fMode.range); // 225 deg
                    hitRanges[8] = performRaycast(player, playerEyePos, dirDownRight, fMode.range);// 315 deg
                    
                    net.minecraft.client.Minecraft mcInst = net.minecraft.client.Minecraft.getInstance();
                    boolean isLocal = (player == mcInst.player) && mcInst.options.getCameraType().isFirstPerson();
                    boolean isWall = checkIsWall(player, playerEyePos, dirCenter, fMode.range);
                    dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.addFlashlight(pos, dir, up, right, color, fMode, hitRanges, isLocal, isWall);
                }
        }
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
