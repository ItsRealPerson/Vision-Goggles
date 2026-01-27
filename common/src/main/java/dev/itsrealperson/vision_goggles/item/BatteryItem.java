package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.network.BatteryPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BatteryItem extends Item {

    public BatteryItem(Properties properties) {
        super(properties);
    }

    public static boolean isBattery(ItemStack stack) {
        return ModConfig.getBatteryCharge(stack) > 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack batteryStack = player.getItemInHand(hand);

        if (level.isClientSide) {
            if (!PlatformMethods.getEquippedHelmet(player).isEmpty()) {
                NetworkManager.sendToServer(new BatteryPacket());
                float charge = ModConfig.getBatteryCharge(batteryStack);
                int pct = (int)(charge * 100);
                player.displayClientMessage(Component.translatable("message.vision_goggles.recharged", pct), true);
                return InteractionResultHolder.sidedSuccess(batteryStack, true);
            } else {
                player.displayClientMessage(Component.translatable("message.vision_goggles.equip_warning"), true);
            }
        }

        return InteractionResultHolder.pass(batteryStack);
    }
}
