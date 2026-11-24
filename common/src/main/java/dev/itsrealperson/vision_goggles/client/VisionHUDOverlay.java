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

        for (IHudModule module : MODULES) {
            if (module.shouldRender(mc.player, goggles)) {
                module.render(gui, goggles, partialTicks, width, height);
            }
        }

        dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge.onRenderHud(gui, partialTicks);
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
}
