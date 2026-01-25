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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class VisionRenderer {
    private static boolean grayscaleEnabled = false;
    private static int lastMode = -1; 
    private static float currentBatteryPct = 1.0f;
    private static int shaderRetryTimer = 0;
    private static boolean lastServerActive = false;
    
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
        boolean hasHelmet = !helmet.isEmpty();
        
        if (hasHelmet) {
            CompoundTag nbt = helmet.getOrCreateTag();
            boolean isServerActive = nbt.getBoolean(ModEvents.NBT_ACTIVE);
            int currentMode = nbt.getInt(ModEvents.NBT_MODE);

            boolean isThermal = helmet.getItem() == ModItems.THERMAL_GOGGLES.get();
            float maxBattery = isThermal ? (float)ModConfig.getThermalDuration() : (float)ModConfig.getNvgDuration();
            float currentBattery = nbt.contains(ModEvents.NBT_BATTERY) ? nbt.getFloat(ModEvents.NBT_BATTERY) : maxBattery;
            currentBatteryPct = currentBattery / maxBattery;

            if (isServerActive != lastServerActive || (isServerActive && currentMode != lastMode)) {
                if (isServerActive) {
                    grayscaleEnabled = true;
                    lastMode = currentMode;
                    forceLoadShader(mc, currentMode);
                    if (isServerActive != lastServerActive) mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_ON.get(), 1.0F));
                } else {
                    grayscaleEnabled = false;
                    lastMode = -1;
                    shutdownEffect(mc);
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_OFF.get(), 1.0F));
                }
                lastServerActive = isServerActive;
            }
        } else if (grayscaleEnabled) {
            grayscaleEnabled = false;
            shutdownEffect(mc);
            lastServerActive = false;
            lastMode = -1;
        }

        if (grayscaleEnabled) {
            if (getPostChain(mc) == null && shaderRetryTimer <= 0) {
                forceLoadShader(mc, lastMode);
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

    private static void forceLoadShader(Minecraft mc, int mode) {
        String path = (mode == 1) ? "shaders/post/thermal.json" : "shaders/post/nvg.json";
        ResourceLocation loc = new ResourceLocation(Vision_goggles.MOD_ID, path);
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
        
        Component modeText = (lastMode == 1) ? Component.translatable("hud.vision_goggles.mode_thermal") : Component.translatable("hud.vision_goggles.mode_night");
        Component batteryText = Component.translatable("hud.vision_goggles.battery", (int)(currentBatteryPct * 100));
        
        int color = currentBatteryPct < 0.2f ? 0xFF0000 : (currentBatteryPct < 0.6f ? 0xFFFF00 : 0x00FF00);
        g.drawString(mc.font, modeText, width - mc.font.width(modeText) - 10, height - 30, 0xFFFFFF, true);
        g.drawString(mc.font, batteryText, width - mc.font.width(batteryText) - 10, height - 20, color, true);
    }
}