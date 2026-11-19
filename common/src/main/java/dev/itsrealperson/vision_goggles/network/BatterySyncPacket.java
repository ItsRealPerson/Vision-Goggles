package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class BatterySyncPacket {
    private final float batteryLevel;
    private final boolean isFlashlight;

    public BatterySyncPacket(float batteryLevel) {
        this(batteryLevel, false);
    }

    public BatterySyncPacket(float batteryLevel, boolean isFlashlight) {
        this.batteryLevel = batteryLevel;
        this.isFlashlight = isFlashlight;
    }

    public BatterySyncPacket(FriendlyByteBuf buf) {
        this.batteryLevel = buf.readFloat();
        this.isFlashlight = buf.readableBytes() > 0 && buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.batteryLevel);
        buf.writeBoolean(this.isFlashlight);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        // Server-authoritative: Battery levels are computed on the server. Reject client packets!
        if (context.getEnv() != EnvType.CLIENT) {
            return;
        }
        context.queue(() -> {
            Player player = context.getPlayer();
            if (player == null) return;

            if (this.isFlashlight) {
                ItemStack flashlight = PlatformMethods.getEquippedFlashlight(player);
                if (flashlight.isEmpty() || !(flashlight.getItem() instanceof TacticalFlashlightItem)) {
                    flashlight = TacticalFlashlightItem.getAnyHeldFlashlight(player);
                }
                if (!flashlight.isEmpty() && flashlight.getItem() instanceof TacticalFlashlightItem) {
                    TacticalFlashlightItem.setBattery(flashlight, batteryLevel);
                    if (batteryLevel <= 0) {
                        flashlight.getOrCreateTag().putBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE, false);
                    }
                }
            } else {
                ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
                if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem) {
                    CompoundTag nbt = helmet.getOrCreateTag();
                    nbt.putFloat(ModConstants.TAG_BATTERY, batteryLevel);
                }
            }
        });
    }
}