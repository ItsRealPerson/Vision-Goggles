package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(modid = VisionGoggles.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {
    public static final String KEY_CATEGORY = "key.categories.vision_goggles";
    public static KeyMapping toggleGrayscaleKey;
    public static KeyMapping switchModeKey;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        toggleGrayscaleKey = new KeyMapping(
            "key.vision_goggles.toggle",
            InputConstants.Type.KEYSYM,
            71, // G
            KEY_CATEGORY
        );
        switchModeKey = new KeyMapping(
            "key.vision_goggles.switch_mode",
            InputConstants.Type.KEYSYM,
            72, // H
            KEY_CATEGORY
        );
        event.register(toggleGrayscaleKey);
        event.register(switchModeKey);
    }
}