package dev.itsrealperson.vision_goggles.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import dev.itsrealperson.vision_goggles.util.ModConfig;

import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.VisionMode;

public class DurabilityModule implements IHudModule {

    private static final ResourceLocation CRACKED_LENS = new ResourceLocation("vision_goggles", "textures/gui/cracked_lens.png");

    @Override
    public boolean shouldRender(Player player, ItemStack goggles) {
        if (!ModConfig.shouldShowDurabilityWarning()) return false;
        return goggles.isDamageableItem() && (float) goggles.getDamageValue() / goggles.getMaxDamage() >= 0.0f;
    }

    @Override
    public void render(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        float damagePct = (float) goggles.getDamageValue() / goggles.getMaxDamage();

        // Determine color based on active mode
        float r = 1.0f, g = 1.0f, b = 1.0f;
        if (goggles.hasTag() && goggles.getTag().getBoolean(ModConstants.TAG_ACTIVE)) {
             int modeId = goggles.getTag().getInt(ModConstants.TAG_MODE);
             VisionMode mode = VisionMode.byId(modeId);
             switch (mode) {
                 case NIGHT_VISION -> { r = 0.2f; g = 1.0f; b = 0.2f; } // Verde
                 case THERMAL -> { r = 0.2f; g = 0.5f; b = 1.0f; }      // Azul (Térmica)
                 case HYDRO -> { 
                     // Solo aplicar tinte si está bajo el agua
                     if (mc.player != null && mc.player.isUnderWater()) {
                        r = 0.0f; g = 1.0f; b = 0.8f; // Cian/Turquesa (Hydro activo)
                     } else {
                        r = 1.0f; g = 1.0f; b = 1.0f; // Blanco (Hydro inactivo/fuera del agua)
                     }
                 }
                 case BIOMETRIC -> { r = 1.0f; g = 0.5f; b = 0.0f; }    // Naranja (Biométrica)
             }
        }

        // 1. Overlay de grietas (si el daño es superior al 70%)
        if (damagePct > 0.7f) {
            gui.setColor(r, g, b, 0.4F); // 40% Opacity with tint
            gui.blit(CRACKED_LENS, 0, 0, 0, 0, width, height, width, height);
            gui.setColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset color
        }
        
        // 2. Efecto de interferencia/glitch visual (si el daño es crítico > 85%)
        if (damagePct > 0.85f && mc.level.random.nextFloat() > 0.7f) {
            // Dibujar un rectángulo semitransparente que parpadea
            // Usamos el mismo color del modo pero muy transparente
            int color = (int)(r*255) << 16 | (int)(g*255) << 8 | (int)(b*255);
            gui.fill(0, 0, width, height, 0x33000000 | color); 
            
            // Simular líneas de interferencia
            for (int i = 0; i < 5; i++) {
                int y = mc.level.random.nextInt(height);
                gui.fill(0, y, width, y + 1, 0x55FFFFFF);
            }
        }
    }
}
