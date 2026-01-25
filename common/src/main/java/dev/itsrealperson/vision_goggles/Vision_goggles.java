package dev.itsrealperson.vision_goggles;

import dev.itsrealperson.vision_goggles.event.ModEvents;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.registry.ModTabs;
import dev.itsrealperson.vision_goggles.util.ModConfig;

public final class Vision_goggles {
    public static final String MOD_ID = "vision_goggles";

    public static void init() {
        ModConfig.load();
        ModItems.register();
        ModTabs.register();
        ModSounds.register();
        NetworkManager.register();
        ModEvents.init();
    }
}
