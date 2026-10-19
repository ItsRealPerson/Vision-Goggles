package dev.itsrealperson.vision_goggles.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

@OnlyIn(Dist.CLIENT)
public class Vision_gogglesForgeClient {

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) ->
                dev.itsrealperson.vision_goggles.client.ModConfigGui.createConfigScreen(parent)
            )
        );
    }
}
