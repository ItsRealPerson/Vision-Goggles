package dev.itsrealperson.vision_goggles.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.event.ModEvents;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.network.ToggleNVGPacket;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class VisionRenderer {
    private static boolean grayscaleEnabled = false;
    private static int lastModeId = -1;
    private static VisionMode currentVisionMode = VisionMode.NIGHT_VISION;
    private static float currentBatteryPct = 1.0f;
    private static int shaderRetryTimer = 0;
    private static boolean lastServerActive = false;
    private static boolean lastWasUnderwater = false;
    
    private static Method loadEffectMethod = null;
    private static Field postEffectField = null;
    private static Method shutdownEffectMethod = null;
    private static Field passesField = null;

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(VisionRenderer::onClientTick);
        ClientGuiEvent.RENDER_HUD.register((guiGraphics, partialTicks) -> {
            renderHUD(guiGraphics, partialTicks);
        });
    }

    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        boolean hasHelmet = !helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem;
        
        if (hasHelmet) {
            CompoundTag nbt = helmet.getOrCreateTag();
            boolean isServerActive = nbt.getBoolean(ModEvents.NBT_ACTIVE);
            
            // Get mode from Item (Static) or NBT (Dynamic)
            VisionMode itemMode = ((VisionGogglesItem) helmet.getItem()).getVisionMode();
            int nbtModeId = nbt.contains(ModEvents.NBT_MODE) ? nbt.getInt(ModEvents.NBT_MODE) : itemMode.getId();
            
            VisionGogglesItem goggles = (VisionGogglesItem) helmet.getItem();
            
            // Respect NBT mode which allows switching
            currentVisionMode = VisionMode.byId(nbtModeId);
            int currentModeId = currentVisionMode.getId();

            float maxBattery = (float) goggles.getBatteryCapacity();
            if (goggles instanceof ModularGogglesItem modular) {
                maxBattery = (float) modular.getBatteryCapacity(helmet);
            }

            float currentBattery = nbt.contains(ModEvents.NBT_BATTERY) ? nbt.getFloat(ModEvents.NBT_BATTERY) : maxBattery;
            currentBatteryPct = currentBattery / maxBattery;

            if (isServerActive != lastServerActive || (isServerActive && currentModeId != lastModeId)) {
                if (isServerActive) {
                    grayscaleEnabled = true;
                    lastModeId = currentModeId;
                    forceLoadShader(mc, currentVisionMode);
                    if (isServerActive != lastServerActive) mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_ON.get(), 1.0F));
                } else {
                    grayscaleEnabled = false;
                    lastModeId = -1;
                    shutdownEffect(mc);
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_OFF.get(), 1.0F));
                }
                lastServerActive = isServerActive;
            }
        } else if (grayscaleEnabled) {
            grayscaleEnabled = false;
            shutdownEffect(mc);
            lastServerActive = false;
            lastModeId = -1;
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

        if (hasHelmet) {
            if (ModKeyMappings.toggleGrayscaleKey.consumeClick()) {
                NetworkManager.INSTANCE.sendToServer(new ToggleNVGPacket(false));
            }
            if (ModKeyMappings.switchModeKey.consumeClick()) {
                NetworkManager.INSTANCE.sendToServer(new ToggleNVGPacket(true));
            }
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
            // First, try to call the official close method if the shader exists
            PostChain chain = getPostChain(mc);
            if (chain != null) {
                chain.close();
            }

            // Force the field to null using reflection as a failsafe
            if (postEffectField == null) getPostChain(mc);
            if (postEffectField != null) {
                postEffectField.set(mc.gameRenderer, null);
            }
            
            System.out.println("[Vision Goggles] Shader forced to NULL.");
        } catch (Exception e) {
            // Fallback: try calling the method if field access failed
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
                    // loadEffect takes 1 ResourceLocation
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == ResourceLocation.class) {
                        m.setAccessible(true);
                        loadEffectMethod = m;
                        break;
                    }
                }
            }
            if (loadEffectMethod != null) {
                loadEffectMethod.invoke(mc.gameRenderer, loc);
                System.out.println("[Vision Goggles] Shader loaded successfully: " + loc);
            } else {
                System.err.println("[Vision Goggles] CRITICAL ERROR: Could not find loadEffect method by parameter type!");
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
                    
                    for (PostPass pass : passes) {
                        var shader = pass.getEffect();
                        if (shader.getUniform("battery") != null) shader.getUniform("battery").set(battery);
                        if (shader.getUniform("time") != null) shader.getUniform("time").set(time);
                    }
                }
            } catch (Exception e) {}
        }
    }

    public static void renderHUD(GuiGraphics g, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !grayscaleEnabled) return;
        
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        
        Component modeText = currentVisionMode.getDisplayName();
        Component batteryText = Component.translatable("hud.vision_goggles.battery", (int)(currentBatteryPct * 100));
        
        int color = currentBatteryPct < 0.2f ? 0xFF0000 : (currentBatteryPct < 0.6f ? 0xFFFF00 : 0x00FF00);
        g.drawString(mc.font, modeText, width - mc.font.width(modeText) - 10, height - 30, 0xFFFFFF, true);
        g.drawString(mc.font, batteryText, width - mc.font.width(batteryText) - 10, height - 20, color, true);

        if (currentVisionMode == VisionMode.BIOMETRIC) {
            renderBiometricInfo(g, mc, width, height, partialTicks);
        }
    }

    private static void renderBiometricInfo(GuiGraphics g, Minecraft mc, int width, int height, float partialTicks) {
        // Custom RayTrace for long range (50 blocks)
        double range = 50.0;
        Vec3 eyePos = mc.player.getEyePosition(partialTicks);
        Vec3 viewVec = mc.player.getViewVector(partialTicks);
        Vec3 endVec = eyePos.add(viewVec.x * range, viewVec.y * range, viewVec.z * range);
        AABB box = mc.player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0);
        
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            mc.player, 
            eyePos, 
            endVec, 
            box, 
            (entity) -> !entity.isSpectator() && entity.isPickable(), 
            range * range
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            int cx = width / 2;
            int cy = height / 2;
            int color = 0xFFFFAA00; // Amber/Gold for Bio
            
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
            
            // Scanner brackets
            g.fill(cx - 5, cy - 5, cx - 4, cy + 5, color); // Left
            g.fill(cx + 4, cy - 5, cx + 5, cy + 5, color); // Right
        }
    }
}