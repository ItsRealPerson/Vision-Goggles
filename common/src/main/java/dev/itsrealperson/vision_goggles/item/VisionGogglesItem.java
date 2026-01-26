package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.network.EquipPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;
import dev.itsrealperson.vision_goggles.util.VisionMode;

import java.util.function.IntSupplier;

public class VisionGogglesItem extends Item {
    private final List<VisionMode> supportedModes;
    private final IntSupplier batteryCapacity;

    public VisionGogglesItem(IntSupplier batteryCapacity, VisionMode... modes) {
        super(new Item.Properties().stacksTo(1));
        this.batteryCapacity = batteryCapacity;
        this.supportedModes = Arrays.asList(modes);
        if (this.supportedModes.isEmpty()) throw new IllegalArgumentException("Must have at least one vision mode");
    }

    public int getBatteryCapacity() {
        return batteryCapacity.getAsInt();
    }

    public VisionMode getVisionMode() {
        return supportedModes.get(0); // Default mode
    }

    public List<VisionMode> getSupportedModes() {
        return supportedModes;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            NetworkManager.INSTANCE.sendToServer(new EquipPacket());
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        return InteractionResultHolder.pass(stack);
    }
}
