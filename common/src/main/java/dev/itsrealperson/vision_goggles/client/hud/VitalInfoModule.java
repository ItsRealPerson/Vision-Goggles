package dev.itsrealperson.vision_goggles.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;

public class VitalInfoModule implements IHudModule {
    private static final ResourceLocation COORD_ICON = new ResourceLocation("vision_goggles", "textures/gui/coord_icon.png");
    private static final ResourceLocation SAT_ICON = new ResourceLocation("vision_goggles", "textures/gui/saturation_icon.png");

    @Override
    public boolean shouldRender(Player player, ItemStack goggles) {
        // La alerta de durabilidad es siempre visible si está activada en config
        if (ModConfig.shouldShowDurabilityWarning()) {
             if (goggles.isDamageableItem() && (float) goggles.getDamageValue() / goggles.getMaxDamage() > 0.85f) return true;
        }

        if (!(goggles.getItem() instanceof VisionGogglesItem vgi)) return false;
        if (!vgi.hasModule(goggles, ModConstants.ID_VITAL_INFO)) return false;

        return ModConfig.shouldShowCoordinates() || ModConfig.shouldShowSaturation();
    }

    @Override
    public void render(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !(goggles.getItem() instanceof VisionGogglesItem vgi)) return;

        boolean hasVitalModule = vgi.hasModule(goggles, ModConstants.ID_VITAL_INFO);

        // 1. Renderizar Coordenadas
        if (hasVitalModule && ModConfig.shouldShowCoordinates()) {
            renderCoordinates(gui, mc, player);
        }

        // 2. Renderizar Saturación
        if (hasVitalModule && ModConfig.shouldShowSaturation()) {
            renderSaturation(gui, mc, player, width, height);
        }

        // 3. Alerta de Durabilidad
        if (ModConfig.shouldShowDurabilityWarning()) {
            renderDurabilityWarning(gui, mc, goggles, width, height);
        }
    }

    private void renderCoordinates(GuiGraphics gui, Minecraft mc, Player player) {
        BlockPos pos = player.blockPosition();
        String coords = String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
        
        // Conversión Nether/Overworld
        String dimText = "";
        if (player.level().dimension().location().getPath().equals("the_nether")) {
            dimText = String.format("OW: %d, %d, %d", pos.getX() * 8, pos.getY(), pos.getZ() * 8);
        } else if (player.level().dimension().location().getPath().equals("overworld")) {
            dimText = String.format("N: %d, %d, %d", pos.getX() / 8, pos.getY(), pos.getZ() / 8);
        }

        gui.blit(COORD_ICON, 10, 10, 0, 0, 9, 9, 9, 9);
        gui.drawString(mc.font, coords, 22, 11, 0xFFFFFF, true);
        
        if (!dimText.isEmpty()) {
            gui.drawString(mc.font, dimText, 22, 21, 0xAAAAAA, true);
        }
    }

    private void renderSaturation(GuiGraphics gui, Minecraft mc, Player player, int width, int height) {
        float saturation = player.getFoodData().getSaturationLevel();
        // Mostrar "Sat: X.X" para que sea claro
        String satText = String.format("Sat: %.1f", saturation);
        int color = saturation > 0 ? 0xFFFFAA00 : 0xFFCCCCCC; // Gris si es 0, Dorado si hay saturación
        
        // Posicionar sobre la barra de hambre (lado derecho)
        int x = width / 2 + 10;
        int y = height - 49; // Justo encima de los muslitos de comida

        gui.blit(SAT_ICON, x, y, 0, 0, 9, 9, 9, 9);
        gui.drawString(mc.font, satText, x + 11, y + 1, color, true);
    }

    private void renderDurabilityWarning(GuiGraphics gui, Minecraft mc, ItemStack goggles, int width, int height) {
        if (goggles.isDamageableItem()) {
            float damagePct = (float) goggles.getDamageValue() / goggles.getMaxDamage();
            if (damagePct > 0.85f) { // Alerta al superar el 85% de daño
                String warn = "!!! GOGGLES DAMAGE CRITICAL !!!";
                int x = (width / 2) - (mc.font.width(warn) / 2);
                gui.drawString(mc.font, warn, x, 40, 0xFFFF0000, true);
            }
        }
    }
}
