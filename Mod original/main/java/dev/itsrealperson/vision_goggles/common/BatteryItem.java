package dev.itsrealperson.vision_goggles.common;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import dev.itsrealperson.vision_goggles.client.ClientHooks;
import dev.itsrealperson.vision_goggles.util.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class BatteryItem extends Item {

    public static final TagKey<Item> NVG_BATTERIES = ItemTags.create(new ResourceLocation(VisionGoggles.MODID, "nvg_batteries"));

    public BatteryItem(Properties properties) {
        super(properties);
    }

    public static boolean isBattery(ItemStack stack) {
        return Config.getBatteryCharge(stack) > 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack batteryStack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientHooks.debugBattery(player, batteryStack);
                ClientHooks.tryUseBattery(player, batteryStack);
            });
            return InteractionResultHolder.sidedSuccess(batteryStack, true);
        }

        return InteractionResultHolder.pass(batteryStack);
    }
}