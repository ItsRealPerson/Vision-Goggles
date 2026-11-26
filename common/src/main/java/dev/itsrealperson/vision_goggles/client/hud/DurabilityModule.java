package dev.itsrealperson.vision_goggles.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DurabilityModule implements IHudModule {

    private static final ResourceLocation CRACKED_LENS = new ResourceLocation("vision_goggles", "textures/gui/cracked_lens.png");

    public static float getDamagePercent(ItemStack goggles) {
        if (!goggles.isDamageableItem() || goggles.getMaxDamage() <= 0) return 0.0f;
        return (float) goggles.getDamageValue() / goggles.getMaxDamage();
    }

    @Override
    public boolean shouldRender(Player player, ItemStack goggles) {
        if (!ModConfig.shouldShowDurabilityWarning()) return false;
        return getDamagePercent(goggles) > 0.70f;
    }

    @Override
    public void render(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        float damagePct = getDamagePercent(goggles);
        if (damagePct <= 0.70f) return;

        // Determine subtle tint based on active mode
        float r = 1.0f, g = 1.0f, b = 1.0f;
        if (goggles.hasTag() && goggles.getTag().getBoolean(ModConstants.TAG_ACTIVE)) {
            VisionMode mode = VisionGogglesItem.getActiveVisionMode(goggles);
            if (mode != null) {
                switch (mode) {
                    case NIGHT_VISION -> { r = 0.45f; g = 1.0f; b = 0.55f; }
                    case THERMAL -> { r = 0.55f; g = 0.75f; b = 1.0f; }
                    case HYDRO -> {
                        if (mc.player != null && mc.player.isUnderWater()) {
                            r = 0.35f; g = 1.0f; b = 0.90f;
                        }
                    }
                    case BIOMETRIC -> { r = 1.0f; g = 0.72f; b = 0.30f; }
                }
            }
        }

        // Progressive opacity from ~0.14 at 70% damage (30% durability left) up to ~0.42 at >= 95% damage
        float severity = Math.max(0.0f, Math.min(1.0f, (damagePct - 0.70f) / 0.25f));
        float alpha = 0.14f + severity * 0.28f;

        gui.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, alpha);
        gui.blit(CRACKED_LENS, 0, 0, 0, 0, width, height, width, height);
        gui.flush();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        // Critical damage optical scanline glitch (> 85% damage)
        if (damagePct > 0.85f && mc.level != null && mc.level.random.nextFloat() > 0.74f) {
            for (int i = 0; i < 4; i++) {
                int y = mc.level.random.nextInt(Math.max(1, height));
                gui.fill(0, y, width, y + 1, 0x44FFFFFF);
            }
        }
    }
}
