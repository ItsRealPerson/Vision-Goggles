package dev.itsrealperson.vision_goggles.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ElytraModule implements IHudModule {

    @Override
    public boolean shouldRender(Player player, ItemStack goggles) {
        return player.isFallFlying();
    }

    @Override
    public void render(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Calculate speed (m/s = block/tick * 20)
        Vec3 deltaMovement = player.getDeltaMovement();
        double horizontalDistanceSqr = deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z;
        double speedMps = Math.sqrt(horizontalDistanceSqr) * 20.0;
        double verticalSpeedMps = deltaMovement.y * 20.0;

        // Calculate altitude
        int altitude = (int) player.getY();

        String speedText = String.format("VEL: %.1f m/s", speedMps);
        String vSpeedText = String.format("V.VEL: %.1f m/s", verticalSpeedMps);
        String altText = String.format("ALT: %d", altitude);

        int textHeight = mc.font.lineHeight;
        int y = height / 2 - 20;
        
        // Render to the left of the crosshair
        int x = width / 2 - 80;
        
        gui.drawString(mc.font, speedText, x, y, 0xFF00FFFF, true);
        gui.drawString(mc.font, vSpeedText, x, y + textHeight + 2, 0xFF00FFFF, true);
        gui.drawString(mc.font, altText, x, y + (textHeight + 2) * 2, 0xFF00FFFF, true);
    }
}
