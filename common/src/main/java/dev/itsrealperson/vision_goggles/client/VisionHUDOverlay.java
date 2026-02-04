package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.client.hud.IHudModule;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class VisionHUDOverlay {
    private static final List<IHudModule> MODULES = new ArrayList<>();

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

        for (IHudModule module : MODULES) {
            if (module.shouldRender(mc.player, goggles)) {
                module.render(gui, goggles, partialTicks, width, height);
            }
        }
    }
}
