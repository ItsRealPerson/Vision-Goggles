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

            boolean cycled = false;
            ItemStack goggles = PlatformMethods.getEquippedHelmet(player);
            if (!goggles.isEmpty() && goggles.getItem() instanceof ModularGogglesItem modular) {
                List<ModuleType> utils = modular.getUtilityModules(goggles);
                if (utils.contains(ModuleType.FLASHLIGHT)) {
                    boolean isOn = goggles.getOrCreateTag().getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE);
                    if (isOn) {
                        int currentId = goggles.getOrCreateTag().getInt(ModConstants.TAG_FLASHLIGHT_MODE);
                        FlashlightMode next = FlashlightMode.byId(currentId).next();
                        goggles.getOrCreateTag().putInt(ModConstants.TAG_FLASHLIGHT_MODE, next.id);

                        player.level().playSound(null, player.blockPosition(), dev.itsrealperson.vision_goggles.registry.ModSounds.FLASHLIGHT_MODE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.0f + next.id * 0.1f);
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.vision_goggles.flashlight_mode." + next.name), true);

                        dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToPlayers(
                            player.serverLevel().players(),
                            new SyncFlashlightPacket(player.getId(), isOn, next.id)
                        );
                        cycled = true;
                    }
                }
            }

            if (!cycled) {
                ItemStack flashlight = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getAnyHeldFlashlight(player);
                if (!flashlight.isEmpty() && flashlight.getItem() instanceof dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem) {
                    int currentId = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getMode(flashlight);
                    FlashlightMode nextMode = FlashlightMode.byId(currentId).next();
                    int next = nextMode.id;
                    flashlight.getOrCreateTag().putInt(ModConstants.TAG_FLASHLIGHT_MODE, next);
                    player.level().playSound(null, player.blockPosition(), dev.itsrealperson.vision_goggles.registry.ModSounds.FLASHLIGHT_MODE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.0f + next * 0.1f);
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.vision_goggles.flashlight_mode." + nextMode.name), true);
                    cycled = true;
                }
            }

            player.containerMenu.broadcastChanges();
        });
    }
}
