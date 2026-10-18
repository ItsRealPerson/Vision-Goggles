package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.ModuleType;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ToggleFlashlightPacket {
    public ToggleFlashlightPacket() {
    }

    public ToggleFlashlightPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public void handle(java.util.function.Supplier<NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            ServerPlayer player = (ServerPlayer) contextSupplier.get().getPlayer();
            if (player == null) return;

            boolean toggled = false;
            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (!helmet.isEmpty() && helmet.getItem() instanceof ModularGogglesItem modular) {
                List<ModuleType> utils = modular.getUtilityModules(helmet);
                if (utils.contains(ModuleType.FLASHLIGHT)) {
                    float currentBattery = helmet.getOrCreateTag().contains(ModConstants.TAG_BATTERY) ? helmet.getOrCreateTag().getFloat(ModConstants.TAG_BATTERY) : (float) modular.getBatteryCapacity(helmet);
                    if (currentBattery <= 0) {
                        player.level().playSound(null, player.blockPosition(), dev.itsrealperson.vision_goggles.registry.ModSounds.BLIP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 0.5f);
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.vision_goggles.low_battery").withStyle(net.minecraft.ChatFormatting.RED), true);
                        return;
                    }

                    boolean wasActive = helmet.getOrCreateTag().getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE);
                    boolean newActive = !wasActive;
                    int modeId = helmet.getOrCreateTag().getInt(ModConstants.TAG_FLASHLIGHT_MODE);
                    helmet.getOrCreateTag().putBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE, newActive);

                    player.level().playSound(null, player.blockPosition(), dev.itsrealperson.vision_goggles.registry.ModSounds.FLASHLIGHT_SWITCH.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, newActive ? 1.0f : 0.85f);

                    // Sync the new state to all tracking clients (includes mode)
                    dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToPlayers(
                        player.serverLevel().players(),
                        new SyncFlashlightPacket(player.getId(), newActive, modeId)
                    );
                    toggled = true;
                }
            }

            if (!toggled) {
                ItemStack flashlight = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getAnyHeldFlashlight(player);
                if (!flashlight.isEmpty() && flashlight.getItem() instanceof dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem) {
                    dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.toggle(flashlight, player);
                    toggled = true;
                }
            }

            player.containerMenu.broadcastChanges();
        });
    }
}
