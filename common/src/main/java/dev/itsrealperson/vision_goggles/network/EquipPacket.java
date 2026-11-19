package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public class EquipPacket {
    private final InteractionHand hand;

    public EquipPacket() {
        this(InteractionHand.MAIN_HAND);
    }

    public EquipPacket(InteractionHand hand) {
        this.hand = (hand != null) ? hand : InteractionHand.MAIN_HAND;
    }

    public EquipPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Player player = context.getPlayer();
            if (player != null) {
                InteractionHand oppositeHand = (this.hand == InteractionHand.MAIN_HAND)
                        ? InteractionHand.OFF_HAND
                        : InteractionHand.MAIN_HAND;

                boolean equipped = PlatformMethods.equipInSlot(player, player.getItemInHand(this.hand))
                        || PlatformMethods.equipInSlot(player, player.getItemInHand(oppositeHand));

                if (equipped && !player.level().isClientSide()) {
                    player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                            SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.70F, 1.18F);
                    player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                            SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.55F, 0.92F);
                }
            }
        });
    }
}
