package dev.itsrealperson.vision_goggles.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlatformMethods {
    @ExpectPlatform
    public static boolean equipInSlot(Player player, ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ItemStack getEquippedHelmet(Player player) {
        throw new AssertionError();
    }
}
