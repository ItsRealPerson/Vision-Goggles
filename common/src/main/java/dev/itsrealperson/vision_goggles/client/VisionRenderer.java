package dev.itsrealperson.vision_goggles.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.network.ToggleNVGPacket;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import dev.itsrealperson.vision_goggles.util.ModuleType;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;

import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import dev.architectury.event.EventResult;

public class VisionRenderer {
    private static boolean visorActive = false;
    private static boolean grayscaleEnabled = false;
    private static int lastModeId = -1;
    private static VisionMode currentVisionMode = VisionMode.NIGHT_VISION;
    private static float currentBatteryPct = 1.0f;
    private static int shaderRetryTimer = 0;
    private static int damageFlickerTimer = 0;
    private static boolean lastServerActive = false;
    private static boolean lastWasUnderwater = false;
    
    private static Method loadEffectMethod = null;
    private static Field postEffectField = null;
    private static Method shutdownEffectMethod = null;
    private static Field passesField = null;

    private static boolean zoomActive = false;
    private static int sonarPulseTimer = 0;
    private static final int SONAR_PULSE_INTERVAL = 60; // 3 seconds
    private static final int SONAR_PULSE_DURATION = 20; // 1 second visibility

    public static boolean isZoomActive() {
        return zoomActive;
    }

    public static boolean isSonarActive() {
        return sonarPulseTimer > 0 && sonarPulseTimer < SONAR_PULSE_DURATION;
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(VisionRenderer::onClientTick);
        ClientGuiEvent.RENDER_HUD.register((guiGraphics, partialTicks) -> {
            renderHUD(guiGraphics, partialTicks);
        });

        dev.architectury.event.events.common.EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (entity.level().isClientSide && entity == Minecraft.getInstance().player && visorActive) {
                damageFlickerTimer = 10; // 0.5 seconds of glitch
            }
            return EventResult.pass();
        });
    }

    private static float currentZoom = 1.0f; // 1.0 = no zoom, 0.33 = max zoom
    private static float zoomActiveAmount = 0.0f; // 0 to 1 for lerp

    public static float getZoomMultiplier() {
        return currentZoom;
    }

    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        boolean hasHelmet = !helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem;
        
        if (hasHelmet) {
            CompoundTag nbt = helmet.getOrCreateTag();
            boolean isServerActive = nbt.getBoolean(ModConstants.TAG_ACTIVE);
            
            VisionGogglesItem goggles = (VisionGogglesItem) helmet.getItem();
            VisionMode itemMode = goggles.getVisionMode();
            int nbtModeId = nbt.contains(ModConstants.TAG_MODE) ? nbt.getInt(ModConstants.TAG_MODE) : itemMode.getId();
            
            if (nbtModeId != -1) {
                currentVisionMode = VisionMode.byId(nbtModeId);
            } else {
                currentVisionMode = null;
            }
            int currentModeId = nbtModeId;

            float maxBattery = (float) goggles.getBatteryCapacity();
            boolean hasZoom = false;
            boolean hasSonar = false;

            if (goggles instanceof ModularGogglesItem modular) {
                maxBattery = (float) modular.getBatteryCapacity(helmet);
                List<ModuleType> utils = modular.getUtilityModules(helmet);
                hasZoom = utils.contains(ModuleType.ZOOM);
                hasSonar = utils.contains(ModuleType.SONAR);
            }

            float currentBattery = nbt.contains(ModConstants.TAG_BATTERY) ? nbt.getFloat(ModConstants.TAG_BATTERY) : maxBattery;
            currentBatteryPct = currentBattery / maxBattery;
            visorActive = isServerActive;

            if (isServerActive != lastServerActive || (isServerActive && currentModeId != lastModeId)) {
                if (isServerActive) {
                    if (currentVisionMode != null) {
                        grayscaleEnabled = true;
                        forceLoadShader(mc, currentVisionMode);
                    } else {
                        grayscaleEnabled = false;
                        shutdownEffect(mc);
                    }
                    lastModeId = currentModeId;
                    if (isServerActive != lastServerActive) mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_ON.get(), 1.0F));
                } else {
                    grayscaleEnabled = false;
                    lastModeId = -1;
                    zoomActive = false;
                    shutdownEffect(mc);
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_OFF.get(), 1.0F));
                }
                lastServerActive = isServerActive;
            }

            // Sonar Pulse Timer
            if (isServerActive && hasSonar) {
                if (sonarPulseTimer > 0) sonarPulseTimer--;
                else sonarPulseTimer = SONAR_PULSE_INTERVAL;
            } else {
                sonarPulseTimer = 0;
            }

            // Smooth Zoom Logic
            if (isServerActive && hasZoom && ModKeyMappings.zoomKey.isDown()) {
                zoomActive = true;
                zoomActiveAmount = Math.min(1.0f, zoomActiveAmount + 0.15f);
            } else {
                zoomActive = false;
                zoomActiveAmount = Math.max(0.0f, zoomActiveAmount - 0.15f);
            }
            currentZoom = 1.0f - (zoomActiveAmount * 0.67f); // Lerp from 1.0 to 0.33

            // Key Controls
            if (ModKeyMappings.toggleGrayscaleKey.consumeClick()) {
                NetworkManager.INSTANCE.sendToServer(new ToggleNVGPacket(false));
            }
            if (ModKeyMappings.switchModeKey.consumeClick()) {
                NetworkManager.INSTANCE.sendToServer(new ToggleNVGPacket(true));
            }

            if (damageFlickerTimer > 0) damageFlickerTimer--;

        } else {
            visorActive = false;
            sonarPulseTimer = 0;
            if (mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                mc.player.removeEffect(MobEffects.NIGHT_VISION);
            }

            if (grayscaleEnabled) {
                grayscaleEnabled = false;
                shutdownEffect(mc);
                lastServerActive = false;
                lastModeId = -1;
                zoomActive = false;
                zoomActiveAmount = 0.0f;
                currentZoom = 1.0f;
            }
        }

        if (grayscaleEnabled) {
            boolean isUnderwater = mc.player.isUnderWater();
            if (currentVisionMode == VisionMode.HYDRO && isUnderwater != lastWasUnderwater) {
                 forceLoadShader(mc, currentVisionMode);
            }
            lastWasUnderwater = isUnderwater;

            if (getPostChain(mc) == null && shaderRetryTimer <= 0) {
                forceLoadShader(mc, currentVisionMode);
                shaderRetryTimer = 3;
            } else if (shaderRetryTimer > 0) {
                shaderRetryTimer--;
            }
            updateShaderUniforms(mc);
        }
    }

    private static PostChain getPostChain(Minecraft mc) {
        try {
            if (postEffectField == null) {
                for (Field f : net.minecraft.client.renderer.GameRenderer.class.getDeclaredFields()) {
                    if (f.getType() == PostChain.class) {
                        f.setAccessible(true);
                        postEffectField = f;
                        break;
                    }
                }
            }
            return postEffectField != null ? (PostChain) postEffectField.get(mc.gameRenderer) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void shutdownEffect(Minecraft mc) {
        try {
            PostChain chain = getPostChain(mc);
            if (chain != null) {
                chain.close();
            }
            if (postEffectField == null) getPostChain(mc);
            if (postEffectField != null) {
                postEffectField.set(mc.gameRenderer, null);
            }
        } catch (Exception e) {
            try {
                if (shutdownEffectMethod == null) {
                    for (Method m : net.minecraft.client.renderer.GameRenderer.class.getDeclaredMethods()) {
                        if (m.getReturnType() == void.class && m.getParameterCount() == 0 && 
                           (m.getName().equals("shutdownEffect") || m.getName().equals("m_109149_") || m.getName().equals("method_3182"))) {
                            m.setAccessible(true);
                            shutdownEffectMethod = m;
                            break;
                        }
                    }
                }
                if (shutdownEffectMethod != null) shutdownEffectMethod.invoke(mc.gameRenderer);
            } catch (Exception ignored) {}
        }
    }

    private static void forceLoadShader(Minecraft mc, VisionMode mode) {
        ResourceLocation loc = mode.getShaderLocation();
        if (mode == VisionMode.HYDRO && !mc.player.isUnderWater()) {
            loc = new ResourceLocation(Vision_goggles.MOD_ID, "shaders/post/hydro_dry.json");
        }
        try {
            if (loadEffectMethod == null) {
                for (Method m : net.minecraft.client.renderer.GameRenderer.class.getDeclaredMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == ResourceLocation.class) {
                        m.setAccessible(true);
                        loadEffectMethod = m;
                        break;
                    }
                }
            }
            if (loadEffectMethod != null) {
                loadEffectMethod.invoke(mc.gameRenderer, loc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateShaderUniforms(Minecraft mc) {
        PostChain effect = getPostChain(mc);
        if (effect != null) {
            try {
                if (passesField == null) {
                    for (Field f : PostChain.class.getDeclaredFields()) {
                        if (f.getType() == List.class) {
                            f.setAccessible(true);
                            passesField = f;
                            break;
                        }
                    }
                }
                if (passesField != null) {
                    List<PostPass> passes = (List<PostPass>) passesField.get(effect);
                    float time = (float)mc.level.getGameTime() + mc.getFrameTime();
                    float battery = currentBatteryPct;
                    
                    // Color Theme logic
                    int theme = ModConfig.getNvgColorTheme();
                    float rf = 0.05f, gf = 1.0f, bf = 0.05f; // Default Green
                    if (theme == 1) { rf = 0.8f; gf = 0.9f; bf = 1.0f; } // White Phosphor
                    else if (theme == 2) { rf = 0.0f; gf = 0.8f; bf = 1.0f; } // Digital Cyan

                    for (PostPass pass : passes) {
                        var shader = pass.getEffect();
                        if (shader.getUniform("battery") != null) {
                            float finalBatt = battery;
                            if (damageFlickerTimer > 0 && mc.level.random.nextFloat() > 0.5f) finalBatt = 0.0f; // Glitch!
                            shader.getUniform("battery").set(finalBatt);
                        }
                        if (shader.getUniform("time") != null) shader.getUniform("time").set(time);
                        if (shader.getUniform("colorFilter") != null) shader.getUniform("colorFilter").set(rf, gf, bf);
                        if (shader.getUniform("focus") != null) shader.getUniform("focus").set(zoomActiveAmount);
                    }
                }
            } catch (Exception e) {}
        }
    }

    private static final ResourceLocation BATTERY_GUI = new ResourceLocation(Vision_goggles.MOD_ID, "textures/gui/battery_gui.png");

    public static void renderHUD(GuiGraphics g, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !visorActive) return;
        
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // 1. Render Sonar Pulse
        if (sonarPulseTimer > 0 && sonarPulseTimer < SONAR_PULSE_DURATION) {
            float progress = 1.0f - ((float)sonarPulseTimer / (float)SONAR_PULSE_DURATION);
            renderSonarPulse(g, width, height, progress);
        }

        // 2. Render Battery Texture (Middle-Right)
        renderBatteryGUI(g, width, height);

        // 3. Render Damage Glitch (HUD only)
        if (damageFlickerTimer > 0 && mc.level.random.nextFloat() > 0.3f) {
            g.fill(0, 0, width, height, 0x44FFFFFF); // White flash
        }

        // 4. Render Mode Text
        if (currentVisionMode != null) {
            Component modeText = currentVisionMode.getDisplayName();
            g.drawString(mc.font, modeText, width - mc.font.width(modeText) - 15, height - 20, 0xFFFFFF, true);
        } else {
            Component modeText = Component.translatable("hud.vision_goggles.mode_normal");
            g.drawString(mc.font, modeText, width - mc.font.width(modeText) - 15, height - 20, 0xAAAAAA, true);
        }

        // 5. Render Zoom Indicator
        if (zoomActiveAmount > 0.01f) {
            float zoomLevel = 1.0f / currentZoom;
            String zoomText = String.format("ZOOM: %.1fx", zoomLevel);
            int zoomWidth = mc.font.width(zoomText);
            int color = 0xFFFFAA00; // Same as biometric color
            
            // Draw bracketed text in the center-bottom
            g.drawString(mc.font, "[", (width / 2) - (zoomWidth / 2) - 10, height - 40, color, true);
            g.drawString(mc.font, zoomText, (width / 2) - (zoomWidth / 2), height - 40, 0xFFFFFF, true);
            g.drawString(mc.font, "]", (width / 2) + (zoomWidth / 2) + 5, height - 40, color, true);
            
            // Draw a small slider bar
            int barW = 60;
            int barH = 2;
            int barX = (width / 2) - (barW / 2);
            int barY = height - 28;
            g.fill(barX, barY, barX + barW, barY + barH, 0x80000000);
            g.fill(barX, barY, barX + (int)(barW * zoomActiveAmount), barY + barH, color);
        }

        if (currentVisionMode == VisionMode.BIOMETRIC) {
            renderBiometricInfo(g, mc, width, height, partialTicks);
        }
    }

    private static void renderBatteryGUI(GuiGraphics g, int width, int height) {
        int renderWidth = 32;  // 16 * 2
        int renderHeight = 64; // 32 * 2
        int x = width - renderWidth - 15; // 15 pixels from right edge
        int y = height / 2 - (renderHeight / 2); // Centered vertically
        
        // Determine Color
        float r = 0, g_col = 1, b = 0; // Default Green
        if (currentBatteryPct < 0.20f) {
            r = 1; g_col = 0; // Red
        } else if (currentBatteryPct < 0.50f) {
            r = 1; g_col = 1; // Yellow
        }

        // Render Background Frame (Scaled to 32x64, from 16x32 source area)
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        g.blit(BATTERY_GUI, x, y, renderWidth, renderHeight, 0, 0, 16, 32, 32, 32);

        // Render Bars (5 segments, each scaled to 24x8 from 12x4 source)
        int bars = Math.round(currentBatteryPct * 5);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g_col, b, 1.0f);
        
        for (int i = 0; i < bars; i++) {
            // Draw each bar from bottom to top
            // Each source bar is 4px high, scaled to 8px. Spacing becomes 2px.
            int barY = y + 50 - (i * 10); 
            g.blit(BATTERY_GUI, x + 4, barY, 24, 8, 16, 0, 12, 4, 32, 32);
        }
        
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Render Percentage Text
        Minecraft mc = Minecraft.getInstance();
        String text = (int)(currentBatteryPct * 100) + "%";
        int textWidth = mc.font.width(text);
        int textX = x + (renderWidth / 2) - (textWidth / 2);
        int textY = y + renderHeight + 2;

        int textColor = 0xFF55FF55; // Green
        if (currentBatteryPct < 0.20f) {
            textColor = 0xFFFF5555; // Red
        } else if (currentBatteryPct < 0.50f) {
            textColor = 0xFFFFFF55; // Yellow
        }

        g.drawString(mc.font, text, textX, textY, textColor, true);
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

    private static void renderBiometricInfo(GuiGraphics g, Minecraft mc, int width, int height, float partialTicks) {
        double range = zoomActiveAmount > 0.5f ? 100.0 : 50.0;
        Vec3 eyePos = mc.player.getEyePosition(partialTicks);
        Vec3 viewVec = mc.player.getViewVector(partialTicks);
        Vec3 endVec = eyePos.add(viewVec.x * range, viewVec.y * range, viewVec.z * range);
        AABB box = mc.player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0);
        
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(mc.player, eyePos, endVec, box, (entity) -> !entity.isSpectator() && entity.isPickable(), range * range);

        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            int cx = width / 2;
            int cy = height / 2;
            int color = 0xFFFFAA00;
            
            String name = target.getDisplayName().getString();
            float health = target.getHealth();
            float maxHealth = target.getMaxHealth();
            int dist = (int) mc.player.distanceTo(target);
            
            String info = String.format("%s [DIST: %dm]", name.toUpperCase(), dist);
            String hp = String.format("HP: %.1f / %.1f", health, maxHealth);
            
            g.drawString(mc.font, info, cx + 15, cy - 10, color, true);
            g.drawString(mc.font, hp, cx + 15, cy, (health < maxHealth * 0.3 ? 0xFFFF5555 : color), true);
            
            int barW = 60;
            int barH = 2;
            int barX = cx + 15;
            int barY = cy + 10;
            g.fill(barX, barY, barX + barW, barY + barH, 0x80000000);
            g.fill(barX, barY, barX + (int)(barW * (health / maxHealth)), barY + barH, color);
            
            g.fill(cx - 5, cy - 5, cx - 4, cy + 5, color);
            g.fill(cx + 4, cy - 5, cx + 5, cy + 5, color);
        }
    }
}