package dev.itsrealperson.vision_goggles;

import dev.itsrealperson.vision_goggles.event.ModEvents;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.registry.ModBlockEntities;
import dev.itsrealperson.vision_goggles.registry.ModBlocks;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.registry.ModMenus;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.registry.ModTabs;
import dev.itsrealperson.vision_goggles.util.ModConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Vision_goggles {
    public static final String MOD_ID = "vision_goggles";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        ModConfig.load();
        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModMenus.register();
        ModTabs.register();
        ModSounds.register();
        NetworkManager.register();
        ModEvents.init();
    }
}
