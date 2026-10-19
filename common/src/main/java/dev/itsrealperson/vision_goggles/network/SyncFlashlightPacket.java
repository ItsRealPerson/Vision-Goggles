package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.util.FlashlightMode;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SyncFlashlightPacket {
    private final int entityId;
    private final boolean isActive;
    private final int modeId;

    public SyncFlashlightPacket(int entityId, boolean isActive, int modeId) {
        this.entityId = entityId;
        this.isActive = isActive;
        this.modeId = modeId;
    }

    /** Legacy constructor — preserves current mode (defaults to 0 = FOCUSED). */
    public SyncFlashlightPacket(int entityId, boolean isActive) {
        this(entityId, isActive, 0);
    }

    public SyncFlashlightPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.isActive = buf.readBoolean();
        this.modeId = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(isActive);
        buf.writeInt(modeId);
    }

    public void handle(java.util.function.Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        // Server validation: Clients can only synchronize their own flashlight, and modeId must be valid
        if (context.getEnv() != EnvType.CLIENT) {
            Player sender = context.getPlayer();
            if (sender == null || sender.getId() != this.entityId) {
                return;
            }
            if (this.modeId < 0 || this.modeId >= FlashlightMode.values().length) {
                return;
            }
        }

        context.queue(() -> {
            Player localPlayer = contextSupplier.get().getPlayer();
            if (localPlayer == null || localPlayer.level() == null) return;

            Entity entity = localPlayer.level().getEntity(entityId);
            if (entity instanceof Player targetPlayer) {
                ItemStack goggles = PlatformMethods.getEquippedHelmet(targetPlayer);
                if (!goggles.isEmpty()) {
                    boolean wasActive = goggles.getOrCreateTag().getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE);
                    goggles.getOrCreateTag().putBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE, isActive);
                    goggles.getOrCreateTag().putInt(ModConstants.TAG_FLASHLIGHT_MODE, modeId);

                    if (targetPlayer != localPlayer && wasActive != isActive) {
                        localPlayer.level().playSound(localPlayer, targetPlayer.blockPosition(), dev.itsrealperson.vision_goggles.registry.ModSounds.FLASHLIGHT_SWITCH.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, isActive ? 1.0f : 0.85f);
                    }
                }
            }
        });
    }
}
