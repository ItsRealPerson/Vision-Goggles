package dev.itsrealperson.vision_goggles.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;

import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class OxygenModule implements IHudModule {

    private static final ResourceLocation OXYGEN_ICON = new ResourceLocation("vision_goggles", "textures/gui/oxygen_icon.png");

    @Override
    public boolean shouldRender(Player player, ItemStack goggles) {
        if (!ModConfig.shouldShowOxygen()) return false;
        
        if (!(goggles.getItem() instanceof VisionGogglesItem vgi)) return false;
        if (!vgi.hasModule(goggles, ModConstants.MODULE_ENVIRONMENT)) return false;

        return player.isUnderWater() || player.getAirSupply() < player.getMaxAirSupply();
    }

    @Override
    public void render(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        int airTicks = player.getAirSupply();
        float baseSeconds = Math.max(0, airTicks / 20.0f);
        
        // Adjust for Respiration enchantment
        // Respiration adds chance to not consume air, effectively extending time by (level + 1)
        int respirationLevel = EnchantmentHelper.getRespiration(player);
        float realSeconds = baseSeconds * (respirationLevel + 1);
        
        String oxygenText = String.format("%.0fs", realSeconds);
        int color = realSeconds < 5.0f ? 0xFFFF5555 : 0xFF55FFFF;

        // Posicionamiento: alineado a la derecha sobre la barra de burbujas
        int x = width / 2 + 10;
        int y = height - 59; 

        // Renderizar el icono de oxígeno personalizado
        gui.blit(OXYGEN_ICON, x, y, 0, 0, 9, 9, 9, 9);
        
        // Renderizar el texto al lado del icono
        gui.drawString(mc.font, oxygenText, x + 11, y + 1, color, true);
    }
}
