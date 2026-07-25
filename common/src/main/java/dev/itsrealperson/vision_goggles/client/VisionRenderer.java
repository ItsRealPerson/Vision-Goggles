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
    private static int damageFlickerTimer = 0;
    private static boolean lastServerActive = false;
    private static boolean lastWasUnderwater = false;

    private static boolean zoomActive = false;
    private static float zoomActiveAmount = 0.0f; // 0 to 1 for lerp
    private static float currentZoom = 1.0f; // 1.0 = no zoom

    private static int sonarPulseTimer = 0;
    private static final int SONAR_PULSE_INTERVAL = 200; // 10 seconds
    private static final int SONAR_PULSE_DURATION = 40; 

    public static boolean isVisorActive() { return visorActive; }
    public static VisionMode getCurrentVisionMode() { return currentVisionMode; }
    public static boolean isZoomActive() { return zoomActive; }
    public static float getZoomMultiplier() { return currentZoom; }
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

        while (ModKeyMappings.toggleGrayscaleKey.consumeClick()) {
            NetworkManager.INSTANCE.sendToServer(new ToggleNVGPacket(false));
        }

        while (ModKeyMappings.switchModeKey.consumeClick()) {
            NetworkManager.INSTANCE.sendToServer(new ToggleNVGPacket(true));
        }

        while (ModKeyMappings.switchSonarModeKey.consumeClick()) {
            NetworkManager.INSTANCE.sendToServer(new dev.itsrealperson.vision_goggles.network.ToggleSonarPacket());
        }
        
        while (ModKeyMappings.toggleFlashlightKey.consumeClick()) {
            NetworkManager.INSTANCE.sendToServer(new dev.itsrealperson.vision_goggles.network.ToggleFlashlightPacket());
        }
        
        while (ModKeyMappings.cycleFlashlightModeKey.consumeClick()) {
            NetworkManager.INSTANCE.sendToServer(new dev.itsrealperson.vision_goggles.network.CycleFlashlightModePacket());
        }

        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        boolean hasHelmet = !helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem;
        
        if (hasHelmet) {
            CompoundTag nbt = helmet.getOrCreateTag();
            boolean isServerActive = nbt.getBoolean(ModConstants.TAG_ACTIVE);
            
            VisionGogglesItem goggles = (VisionGogglesItem) helmet.getItem();
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
            boolean isFlashlightActive = nbt.getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE);
            visorActive = isServerActive || isFlashlightActive;

            // Zoom & Utility Logic
            boolean hasZoom = false;
            boolean hasSonar = false;
            if (goggles instanceof ModularGogglesItem modular) {
                java.util.List<ModuleType> utils = modular.getUtilityModules(helmet);
                hasZoom = utils.contains(ModuleType.ZOOM);
                hasSonar = utils.contains(ModuleType.SONAR);
            }

            // Shader Lifecycle Management
            boolean shaderNeeded = (currentVisionMode != null);

            if (isServerActive != lastServerActive || (isServerActive && nbtModeId != lastModeId)) {
                if (isServerActive && shaderNeeded) {
                    grayscaleEnabled = true;
                    VisionShaderManager.enableShader(mc, currentVisionMode);
                    
                    lastModeId = nbtModeId;
                    if (isServerActive != lastServerActive) mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_ON.get(), 1.0F));
                } else {
                    grayscaleEnabled = false;
                    lastModeId = -1;
                    VisionShaderManager.disableShader(mc);
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_OFF.get(), 1.0F));
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
                if (mc.player.isUnderWater() || mc.player.getAirSupply() < mc.player.getMaxAirSupply()) {
                    int airTicks = mc.player.getAirSupply();
                    float baseSeconds = Math.max(0, airTicks / 20.0f);
                    int resp = net.minecraft.world.item.enchantment.EnchantmentHelper.getRespiration(mc.player);
                    float realSeconds = baseSeconds * (resp + 1);
                    if (realSeconds < 5.0f && mc.level.getGameTime() % 20 == 0) {
                        mc.player.playSound(dev.itsrealperson.vision_goggles.registry.ModSounds.BLIP.get(), 1.0f, 1.0f);
                    }
                }
            }

            if (isServerActive && hasZoom && ModKeyMappings.zoomKey.isDown()) {
                zoomActive = true;
                zoomActiveAmount = Math.min(1.0f, zoomActiveAmount + 0.15f);
            } else {
                zoomActive = false;
                zoomActiveAmount = Math.max(0.0f, zoomActiveAmount - 0.15f);
            }
            currentZoom = 1.0f - (zoomActiveAmount * 0.67f);

            if (damageFlickerTimer > 0) damageFlickerTimer--;

            // Update Shader State & Resilience
            if (grayscaleEnabled) {
                boolean isUnderwater = mc.player.isUnderWater();
                
                // RE-ENABLE logic (Crucial para F5 y Embeddium)
                if (!VisionShaderManager.isShaderActive()) {
                    VisionShaderManager.enableShader(mc, currentVisionMode);
                }

                if (currentVisionMode != null) {
                    if (currentVisionMode == VisionMode.HYDRO && isUnderwater != lastWasUnderwater) {
                        VisionShaderManager.enableShader(mc, currentVisionMode);
                    }
                }
                lastWasUnderwater = isUnderwater;
                
                VisionShaderManager.update(currentVisionMode, currentBatteryPct, damageFlickerTimer > 0);
            }
            
            dev.itsrealperson.vision_goggles.client.audio.GogglesHumSoundInstance.updateHum(mc.player);
            
        } else if (grayscaleEnabled || lastServerActive) {
            cleanup(mc);
        }

        // Flashlight Logic runs unconditionally so we can see other players' flashlights even without a helmet
        dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.beginUpdate(mc.gameRenderer.getMainCamera().rotation());
        net.minecraft.world.phys.Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        for (Player player : mc.level.players()) {
            ItemStack playerHelmet = PlatformMethods.getEquippedHelmet(player);
            if (playerHelmet.getItem() instanceof ModularGogglesItem modular) {
                List<ModuleType> utils = modular.getUtilityModules(playerHelmet);
                
                boolean hasFlashlight = utils.contains(ModuleType.FLASHLIGHT);
                float batteryLvl = playerHelmet.getOrCreateTag().getFloat(ModConstants.TAG_BATTERY);
                boolean flashlightOn = hasFlashlight && playerHelmet.getOrCreateTag().getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE) && batteryLvl > 0;

                if (flashlightOn) {
                    org.joml.Vector3f pos = new org.joml.Vector3f(
                        (float)(player.getX() - camPos.x),
                        (float)(player.getEyeY() - camPos.y),
                        (float)(player.getZ() - camPos.z)
                    );
                    
                    net.minecraft.world.phys.Vec3 look = player.getViewVector(1.0f);
                    org.joml.Vector3f dir = new org.joml.Vector3f((float)look.x, (float)look.y, (float)look.z);
                    org.joml.Vector3f color = new org.joml.Vector3f(1.0f, 0.95f, 0.85f);
                    
                    int modeId = playerHelmet.getOrCreateTag().getInt(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_FLASHLIGHT_MODE);
                    dev.itsrealperson.vision_goggles.util.FlashlightMode fMode = dev.itsrealperson.vision_goggles.util.FlashlightMode.byId(modeId);
                    
                    net.minecraft.world.phys.Vec3 startVec = new net.minecraft.world.phys.Vec3(player.getX(), player.getEyeY(), player.getZ());
                    net.minecraft.world.phys.Vec3 endVec = startVec.add(look.x * fMode.range, look.y * fMode.range, look.z * fMode.range);
                    net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(
                        startVec,
                        endVec,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        player
                    );
                    net.minecraft.world.phys.HitResult hitResult = player.level().clip(context);
                    float finalRange = fMode.range;
                    if (hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                        double hitDist = hitResult.getLocation().distanceTo(startVec);
                        finalRange = (float) Math.min(fMode.range, hitDist + 1.2);
                    }
                    
                    dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.addFlashlight(pos, dir, color, fMode, finalRange);
                }
            }
        }
    }

    private static void cleanup(Minecraft mc) {
        visorActive = false;
        grayscaleEnabled = false;
        lastServerActive = false;
        lastModeId = -1;
        sonarPulseTimer = 0;
        zoomActive = false;
        zoomActiveAmount = 0.0f;
        currentZoom = 1.0f;
        VisionShaderManager.disableShader(mc);
        dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.beginUpdate(null);
        if (mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
        dev.itsrealperson.vision_goggles.client.audio.GogglesHumSoundInstance.updateHum(mc.player);
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
