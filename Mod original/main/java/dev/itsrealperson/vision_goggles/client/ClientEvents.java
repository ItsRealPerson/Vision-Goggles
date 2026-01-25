package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import dev.itsrealperson.vision_goggles.common.ModSounds;
import dev.itsrealperson.vision_goggles.common.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.Config;
import dev.itsrealperson.vision_goggles.network.ModMessages;
import dev.itsrealperson.vision_goggles.network.ToggleNVGPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = VisionGoggles.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    private static boolean grayscaleEnabled = false;
    private static int lastMode = -1; 
    private static float currentBatteryPct = 1.0f;
    private static int shaderRetryTimer = 0;
    private static boolean lastServerActive = false;
    private static Field passesField = null;

    private static final String NBT_BATTERY = "nvg_battery";
    private static final String NBT_ACTIVE = "nvg_active";

    public static boolean isGrayscaleActive() { return grayscaleEnabled; }
    public static boolean isHelmetEquipped() { return !getEquippedHelmet().isEmpty(); }

    public static boolean isVisor(Item item) {
        String key = ForgeRegistries.ITEMS.getKey(item).toString();
        return key.contains("goggles_helmet");
    }

    public static ItemStack getEquippedHelmet() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;

        return CuriosApi.getCuriosHelper().findFirstCurio(mc.player, stack -> isVisor(stack.getItem()))
                .map(slot -> slot.stack()).orElse(ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack helmet = getEquippedHelmet();
        boolean hasHelmet = !helmet.isEmpty();
        
        if (hasHelmet) {
            CompoundTag nbt = helmet.getOrCreateTag();
            boolean isServerActive = nbt.getBoolean(NBT_ACTIVE);
            int currentMode = nbt.getInt("vision_mode");

            float maxBattery = (helmet.getItem().toString().contains("thermal")) ? (float)Config.thermalDuration : (float)Config.nvgDuration;
            float currentBattery = nbt.contains(NBT_BATTERY) ? nbt.getFloat(NBT_BATTERY) : maxBattery;
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
                    mc.gameRenderer.shutdownEffect();
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.VISOR_OFF.get(), 1.0F));
                }
                lastServerActive = isServerActive;
            }
        } else if (grayscaleEnabled) {
            grayscaleEnabled = false;
            mc.gameRenderer.shutdownEffect();
            lastServerActive = false;
            lastMode = -1;
        }

        if (grayscaleEnabled) {
            if (mc.gameRenderer.currentEffect() == null && shaderRetryTimer <= 0) {
                forceLoadShader(mc, lastMode);
                shaderRetryTimer = 3;
            } else if (shaderRetryTimer > 0) {
                shaderRetryTimer--;
            }
            updateShaderUniforms(mc);
        }

        if (hasHelmet) {
            if (KeyBindings.toggleGrayscaleKey != null && KeyBindings.toggleGrayscaleKey.consumeClick()) {
                ModMessages.sendToServer(new ToggleNVGPacket(false));
            }
            if (KeyBindings.switchModeKey != null && KeyBindings.switchModeKey.consumeClick()) {
                ModMessages.sendToServer(new ToggleNVGPacket(true));
            }
        }
    }

    private static void forceLoadShader(Minecraft mc, int mode) {
        String path = (mode == 1) ? "shaders/post/thermal.json" : "shaders/post/nvg.json";
        try { mc.gameRenderer.loadEffect(new ResourceLocation(VisionGoggles.MODID, path)); } catch (Exception ignored) {}
    }

    private static void updateShaderUniforms(Minecraft mc) {
        PostChain effect = mc.gameRenderer.currentEffect();
        if (effect != null) {
            try {
                if (passesField == null) {
                    for (Field f : PostChain.class.getDeclaredFields()) {
                        if (f.getType() == List.class) {
                            f.setAccessible(true);
                            List<?> list = (List<?>) f.get(effect);
                            if (!list.isEmpty() && list.get(0) instanceof PostPass) {
                                passesField = f;
                                break;
                            }
                        }
                    }
                }

                if (passesField != null) {
                    List<PostPass> passes = (List<PostPass>) passesField.get(effect);
                    float time = (float)mc.level.getGameTime() + mc.getFrameTime();
                    float battery = currentBatteryPct;
                    
                    for (PostPass pass : passes) {
                        if (pass.getEffect().getUniform("battery") != null) {
                            pass.getEffect().getUniform("battery").set(battery);
                        }
                        if (pass.getEffect().getUniform("time") != null) {
                            pass.getEffect().getUniform("time").set(time);
                        }
                    }
                }
            } catch (Exception e) {}
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !grayscaleEnabled) return;
        
        GuiGraphics g = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        
        net.minecraft.network.chat.Component modeText = (lastMode == 1) ? 
            net.minecraft.network.chat.Component.translatable("hud.vision_goggles.mode_thermal") : 
            net.minecraft.network.chat.Component.translatable("hud.vision_goggles.mode_night");
            
        net.minecraft.network.chat.Component batteryText = net.minecraft.network.chat.Component.translatable("hud.vision_goggles.battery", (int)(currentBatteryPct * 100));
        
        int color = currentBatteryPct < 0.2f ? 0xFF0000 : (currentBatteryPct < 0.6f ? 0xFFFF00 : 0x00FF00);
        g.drawString(mc.font, modeText, width - mc.font.width(modeText) - 10, height - 30, 0xFFFFFF, true);
        g.drawString(mc.font, batteryText, width - mc.font.width(batteryText) - 10, height - 20, color, true);
    }
}