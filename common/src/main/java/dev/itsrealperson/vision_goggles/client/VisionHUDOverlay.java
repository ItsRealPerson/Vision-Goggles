package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.client.hud.IHudModule;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class VisionHUDOverlay {
    private static final List<IHudModule> MODULES = new ArrayList<>();
    private static final ResourceLocation CRACKED_LENS_TEXTURE =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/gui/cracked_lens.png");

    public static void registerModule(IHudModule module) {
        MODULES.add(module);
    }

    public static void render(GuiGraphics gui, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack goggles = PlatformMethods.getEquippedHelmet(mc.player);
        if (goggles.isEmpty() || !(goggles.getItem() instanceof VisionGogglesItem)) return;

        // Solo renderizar si las gafas están encendidas (esta lógica se sincronizará con VisionRenderer)
        if (!VisionRenderer.isVisorActive()) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        renderCrackedLensOverlay(gui, goggles, width, height);
        renderWaterShortCircuitOverlay(gui, mc, width, height);

        for (IHudModule module : MODULES) {
            if (module.shouldRender(mc.player, goggles)) {
                module.render(gui, goggles, partialTicks, width, height);
            }
        }

        dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge.onRenderHud(gui, partialTicks);
        dev.itsrealperson.vision_goggles.api.VisionGogglesClientAPI.renderHudOverlayLayers(gui, partialTicks);
    }

    private static void renderCrackedLensOverlay(GuiGraphics gui, ItemStack goggles, int width, int height) {
        if (!goggles.isDamageableItem() || goggles.getMaxDamage() <= 0) return;
        float durabilityRatio = 1.0f - ((float) goggles.getDamageValue() / (float) goggles.getMaxDamage());
        if (durabilityRatio >= 0.30f) return;

        // Subtle progressive opacity: ~0.14 at 30% durability up to ~0.42 at <= 5%
        float severity = Math.max(0.0f, Math.min(1.0f, (0.30f - durabilityRatio) / 0.25f));
        float alpha = 0.14f + severity * 0.28f;

        gui.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        gui.blit(CRACKED_LENS_TEXTURE, 0, 0, 0, 0, width, height, width, height);
        gui.flush();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void renderWaterShortCircuitOverlay(GuiGraphics gui, Minecraft mc, int width, int height) {
        float progress = VisionShaderManager.getUnderwaterShortProgress(mc);
        if (progress <= 0.0f) return;

        long seed = System.nanoTime() / 25_000_000L; // ~40 Hz erratic electronic jitter
        java.util.Random rng = new java.util.Random(seed);

        // Brief dark blackout flashes ramping in frequency & opacity
        if (rng.nextFloat() < (0.18f + progress * 0.52f)) {
            int darkAlpha = Math.min(210, Math.round((0.28f + 0.50f * progress) * 255.0f * (0.5f + 0.5f * rng.nextFloat())));
            gui.fill(0, 0, width, height, (darkAlpha << 24));
        }

        // Erratic horizontal static/short-circuit glitch bars
        int bars = 3 + Math.round(progress * 9.0f);
        for (int i = 0; i < bars; i++) {
            int y = rng.nextInt(Math.max(1, height));
            int h = 1 + rng.nextInt(3 + Math.round(progress * 5.0f));
            int x = rng.nextInt(Math.max(1, width / 3));
            int w = width - x - rng.nextInt(Math.max(1, width / 4));
            int alpha = Math.min(200, Math.round((0.20f + 0.55f * progress) * 255.0f * rng.nextFloat()));
            int rgb = rng.nextBoolean() ? 0x99EEFF : 0x101820;
            gui.fill(x, y, Math.min(width, x + w), Math.min(height, y + h), (alpha << 24) | rgb);
        }
    }
}
