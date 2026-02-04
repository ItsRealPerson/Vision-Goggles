package dev.itsrealperson.vision_goggles.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Interfaz base para todos los módulos del HUD.
 * Cada funcionalidad visual (Coordenadas, Oxígeno, etc.) implementará esta interfaz.
 */
public interface IHudModule {
    boolean shouldRender(Player player, ItemStack goggles);
    void render(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height);
}
