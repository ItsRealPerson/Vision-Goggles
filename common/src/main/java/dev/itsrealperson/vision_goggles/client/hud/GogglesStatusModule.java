package dev.itsrealperson.vision_goggles.client.hud;

import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GogglesStatusModule implements IHudModule {
    private static final ResourceLocation BATTERY_TEXTURE = new ResourceLocation("vision_goggles", "textures/gui/battery_gui.png");

    @Override
    public boolean shouldRender(Player player, ItemStack goggles) {
        return true; // Always show battery/mode if goggles are active
    }

    @Override
    public void render(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        CompoundTag nbt = goggles.getTag();
        if (nbt == null) return;

        // 1. Renderizar Modo de Visión Actual (Arriba a la Derecha)
        int modeId = nbt.contains(ModConstants.TAG_MODE) ? nbt.getInt(ModConstants.TAG_MODE) : -1;
        VisionMode mode = VisionMode.byId(modeId);
        
        boolean showMode = mode != null;
        if (showMode && goggles.getItem() instanceof dev.itsrealperson.vision_goggles.item.ModularGogglesItem modular) {
            showMode = modular.getModes(goggles).contains(mode);
        }

        if (showMode) {
            Component modeName = mode.getDisplayName();
            int xMode = width - mc.font.width(modeName) - 5;
            gui.drawString(mc.font, modeName.getVisualOrderText(), xMode, 5, 0xFFAAAAAA, true);
        }

        // 2. Renderizar Batería (Lógica original recuperada)
        float battery = nbt.getFloat(ModConstants.TAG_BATTERY);
        float maxBattery = 6000;
        if (goggles.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem vgi) {
            maxBattery = vgi.getBatteryCapacity(goggles);
        }
        
        float batteryPct = battery / maxBattery;
        int renderWidth = 32;  // 16 * 2
        int renderHeight = 64; // 32 * 2
        int x = width - renderWidth - 15;
        int y = height / 2 - (renderHeight / 2);

        // Determinar Color para las barras
        float r = 0, g_col = 1, b = 0;
        if (batteryPct < 0.20f) { r = 1; g_col = 0; }
        else if (batteryPct < 0.50f) { r = 1; g_col = 1; }

        // Render Background Frame
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        gui.blit(BATTERY_TEXTURE, x, y, renderWidth, renderHeight, 0, 0, 16, 32, 32, 32);

        // Render Bars (5 segmentos)
        int bars = Math.round(batteryPct * 5);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g_col, b, 1.0f);
        for (int i = 0; i < bars; i++) {
            int barY = y + 50 - (i * 10); 
            gui.blit(BATTERY_TEXTURE, x + 4, barY, 24, 8, 16, 0, 12, 4, 32, 32);
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Render Percentage Text (Abajo)
        String batteryText = (int)(batteryPct * 100) + "%";
        int textWidth = mc.font.width(batteryText);
        int textX = x + (renderWidth / 2) - (textWidth / 2);
        int textY = y + renderHeight + 2;

        int textColor = 0xFF55FF55;
        if (batteryPct < 0.20f) textColor = 0xFFFF5555;
        else if (batteryPct < 0.50f) textColor = 0xFFFFFF55;

        gui.drawString(mc.font, batteryText, textX, textY, textColor, true);
    }
}
