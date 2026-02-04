package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;

public class ModKeyMappings {
    public static final KeyMapping toggleGrayscaleKey = new KeyMapping(
            "key.vision_goggles.toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_N,
            "key.categories.vision_goggles"
    );

    public static final KeyMapping switchModeKey = new KeyMapping(
            "key.vision_goggles.switch",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_M,
            "key.categories.vision_goggles"
    );

    public static final KeyMapping zoomKey = new KeyMapping(
            "key.vision_goggles.zoom",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            "key.categories.vision_goggles"
    );

    public static void init() {
        try {
            KeyMappingRegistry.register(toggleGrayscaleKey);
            KeyMappingRegistry.register(switchModeKey);
            KeyMappingRegistry.register(zoomKey);
        } catch (Exception e) {
            // Log or ignore if already registered
        }
    }
}
