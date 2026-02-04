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

import dev.itsrealperson.vision_goggles.client.flashlight.FlashlightManager;
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
    private static final int SONAR_PULSE_INTERVAL = 60; 
    private static final int SONAR_PULSE_DURATION = 20; 

    public static boolean isVisorActive() { return visorActive; }
    public static boolean isZoomActive() { return zoomActive; }
    public static float getZoomMultiplier() { return currentZoom; }
    public static boolean isSonarActive() { return sonarPulseTimer > 0 && sonarPulseTimer < SONAR_PULSE_DURATION; }

    public static void renderFlashlight(float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        List<? extends net.minecraft.world.entity.player.Player> players = mc.level.players();
        for (Player player : players) {
            boolean isFlashlightActive = false;
            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            
            if (!helmet.isEmpty() && helmet.getItem() instanceof ModularGogglesItem goggles) {
                CompoundTag nbt = helmet.getTag();
                if (nbt != null && nbt.getBoolean(ModConstants.TAG_ACTIVE)) {
                    List<ModuleType> utils = goggles.getUtilityModules(helmet);
                    if (utils.contains(ModuleType.FLASHLIGHT)) {
                        isFlashlightActive = true;
                    }
                }
            }
            
            // Log once per second to avoid spamming
            if (player == mc.player && mc.level.getGameTime() % 20 == 0) {
                dev.itsrealperson.vision_goggles.Vision_goggles.LOGGER.info("Flashlight State for " + player.getName().getString() + ": " + isFlashlightActive + " (Helmet: " + !helmet.isEmpty() + ")");
            }
            
            FlashlightManager.render(player, partialTicks, isFlashlightActive);
        }
    }

    public static void init() {
        ClientTickEvent.CLIENT_PRE.register(instance -> renderFlashlight(instance.getFrameTime()));
        ClientTickEvent.CLIENT_POST.register(VisionRenderer::onClientTick);
        ClientGuiEvent.RENDER_HUD.register((guiGraphics, partialTicks) -> {
            renderHUD(guiGraphics, partialTicks);
        });

        dev.architectury.event.events.common.PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player.level().isClientSide) {
                Minecraft mc = Minecraft.getInstance();
                cleanup(mc);
                // Flashlight cleanup for all players happens here
                mc.level.players().forEach(FlashlightManager::cleanup);
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
            visorActive = isServerActive;

            // Zoom & Utility Logic
            boolean hasZoom = false;
            boolean hasSonar = false;
            boolean hasFlashlight = false;
            if (goggles instanceof ModularGogglesItem modular) {
                java.util.List<ModuleType> utils = modular.getUtilityModules(helmet);
                hasZoom = utils.contains(ModuleType.ZOOM);
                hasSonar = utils.contains(ModuleType.SONAR);
                hasFlashlight = utils.contains(ModuleType.FLASHLIGHT);
            }

            // Shader Lifecycle Management
            boolean isFlashlightActive = hasFlashlight && isServerActive;
            boolean shaderNeeded = (currentVisionMode != null) || isFlashlightActive;

            if (isServerActive != lastServerActive || (isServerActive && nbtModeId != lastModeId)) {
                if (isServerActive && shaderNeeded) {
                    grayscaleEnabled = true;
                    // Llamamos a enableShader incluso si currentVisionMode es null (él manejará la linterna)
                    VisionShaderManager.enableShader(mc, currentVisionMode);
                    
                    lastModeId = nbtModeId;
                    if (isServerActive != lastServerActive) mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_ON.get(), 1.0F));
                } else {
                    grayscaleEnabled = false;
                    lastModeId = -1;
                    VisionShaderManager.disableShader(mc);
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_OFF.get(), 1.0F));
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
                
                VisionShaderManager.setFlashlightActive(isFlashlightActive);
                VisionShaderManager.update(currentVisionMode, currentBatteryPct, damageFlickerTimer > 0);
            }

        } else if (grayscaleEnabled || lastServerActive) {
            cleanup(mc);
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
        if (mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
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
        int cx = width / 2;
        int cy = height / 2;
        int maxRadius = Math.max(width, height) / 2;
        int currentRadius = (int) (progress * maxRadius);
        int alpha = (int) ((1.0f - progress) * 128);
        int color = (alpha << 24) | 0x00AAFF;
        g.fill(cx - currentRadius, cy - currentRadius, cx + currentRadius, cy - currentRadius + 1, color);
        g.fill(cx - currentRadius, cy + currentRadius - 1, cx + currentRadius, cy + currentRadius, color);
        g.fill(cx - currentRadius, cy - currentRadius, cx - currentRadius + 1, cy + currentRadius, color);
        g.fill(cx + currentRadius - 1, cy - currentRadius, cx + currentRadius, cy + currentRadius, color);
    }
}
