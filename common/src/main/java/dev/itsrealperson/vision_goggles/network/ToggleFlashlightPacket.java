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

            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (!helmet.isEmpty() && helmet.getItem() instanceof ModularGogglesItem modular) {
                List<ModuleType> utils = modular.getUtilityModules(helmet);
                if (utils.contains(ModuleType.FLASHLIGHT)) {
                    boolean isActive = helmet.getOrCreateTag().getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE);
                    int modeId = helmet.getOrCreateTag().getInt(ModConstants.TAG_FLASHLIGHT_MODE);
                    helmet.getOrCreateTag().putBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE, !isActive);
                    player.level().playSound(null, player.blockPosition(), dev.itsrealperson.vision_goggles.registry.ModSounds.BLIP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, isActive ? 0.8f : 1.2f);
                    
                    // Sync the new state to all tracking clients (includes mode)
                    dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToPlayers(
                        player.serverLevel().players(),
                        new SyncFlashlightPacket(player.getId(), !isActive, modeId)
                    );
                }
            }
            player.containerMenu.broadcastChanges();
        });
    }
}
