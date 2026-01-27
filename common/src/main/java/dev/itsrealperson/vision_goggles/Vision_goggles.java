package dev.itsrealperson.vision_goggles;

import dev.itsrealperson.vision_goggles.event.ModEvents;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.registry.*;
import dev.itsrealperson.vision_goggles.util.ModConfig;

public final class Vision_goggles {
    public static final String MOD_ID = "vision_goggles";

    public static void init() {
        ModConfig.load();
        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModDataComponents.register();
        ModMenus.register();
        ModTabs.register();
        ModSounds.register();
        NetworkManager.register();
        ModEvents.init();
    }
}