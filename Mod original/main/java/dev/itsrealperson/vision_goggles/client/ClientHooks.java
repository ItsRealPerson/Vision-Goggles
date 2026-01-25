package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import dev.itsrealperson.vision_goggles.common.BatteryItem;
import dev.itsrealperson.vision_goggles.network.BatteryPacket;
import dev.itsrealperson.vision_goggles.network.ModMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class ClientHooks {

    public static void tryUseBattery(Player player, ItemStack stack) {
        if (ClientEvents.isHelmetEquipped()) {
            ModMessages.sendToServer(new BatteryPacket());
            if (!player.getAbilities().instabuild) stack.shrink(1);
            
            float charge = dev.itsrealperson.vision_goggles.util.Config.getBatteryCharge(stack);
            int pct = (int)(charge * 100);
            player.displayClientMessage(Component.translatable("message.vision_goggles.recharged", pct), true);
        } else {
            player.displayClientMessage(Component.translatable("message.vision_goggles.equip_warning"), true);
        }
    }

    public static void debugBattery(Player player, ItemStack stack) {

    }
}
