package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.util.FlashlightMode;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.ModuleType;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Sent client → server when the player presses the cycle flashlight mode key.
 * The server cycles the mode in NBT and broadcasts a SyncFlashlightPacket to all players.
 */
public class CycleFlashlightModePacket {

    public CycleFlashlightModePacket() {}

    public CycleFlashlightModePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(java.util.function.Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.get().getPlayer();
            if (player == null) return;

            ItemStack goggles = PlatformMethods.getEquippedHelmet(player);
            if (goggles.isEmpty() || !(goggles.getItem() instanceof ModularGogglesItem modular)) return;

            List<ModuleType> utils = modular.getUtilityModules(goggles);
            if (!utils.contains(ModuleType.FLASHLIGHT)) return;

            // Only cycle if the flashlight is on
            boolean isOn = goggles.getOrCreateTag().getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE);
            if (!isOn) return;

            int currentId = goggles.getOrCreateTag().getInt(ModConstants.TAG_FLASHLIGHT_MODE);
            FlashlightMode next = FlashlightMode.byId(currentId).next();
            goggles.getOrCreateTag().putInt(ModConstants.TAG_FLASHLIGHT_MODE, next.id);

            Vision_goggles.LOGGER.debug("Flashlight mode cycled to {} for {}", next.name, player.getName().getString());

            // Broadcast new state (mode + active) to all players in the level
            dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToPlayers(
                player.serverLevel().players(),
                new SyncFlashlightPacket(player.getId(), isOn, next.id)
            );
        });
    }
}
