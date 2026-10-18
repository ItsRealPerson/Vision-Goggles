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

    public static final KeyMapping toggleSpawnSecurityKey = new KeyMapping(
            "key.vision_goggles.toggle_spawn_security",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_O,
            "key.categories.vision_goggles"
    );

    public static final KeyMapping switchSonarModeKey = new KeyMapping(
            "key.vision_goggles.switch_sonar",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_P,
            "key.categories.vision_goggles"
    );

    public static final KeyMapping toggleFlashlightKey = new KeyMapping(
            "key.vision_goggles.toggle_flashlight",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_L,
            "key.categories.vision_goggles"
    );

    public static final KeyMapping cycleFlashlightModeKey = new KeyMapping(
            "key.vision_goggles.cycle_flashlight_mode",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.vision_goggles"
    );

    public static void init() {
        try {
            KeyMappingRegistry.register(toggleGrayscaleKey);
            KeyMappingRegistry.register(switchModeKey);
            KeyMappingRegistry.register(zoomKey);
            KeyMappingRegistry.register(toggleSpawnSecurityKey);
            KeyMappingRegistry.register(switchSonarModeKey);
            KeyMappingRegistry.register(toggleFlashlightKey);
            KeyMappingRegistry.register(cycleFlashlightModeKey);
        } catch (Exception e) {
            // Log or ignore if already registered
        }
    }
}
