package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.block.entity.ModificationStationBlockEntity;
import dev.itsrealperson.vision_goggles.recipe.StationBlueprints;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class AssembleBlueprintPacket {
    private final BlockPos pos;
    private final String blueprintId;

    public AssembleBlueprintPacket(BlockPos pos, String blueprintId) {
        this.pos = pos;
        this.blueprintId = (blueprintId != null) ? blueprintId : "";
    }

    public AssembleBlueprintPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.blueprintId = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.blueprintId, 64);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.level().isLoaded(this.pos)) {
                BlockEntity be = serverPlayer.level().getBlockEntity(this.pos);
                if (be instanceof ModificationStationBlockEntity station && station.stillValid(serverPlayer)) {
                    StationBlueprints.Blueprint bp = StationBlueprints.byId(this.blueprintId);
                    if (bp != null) {
                        station.startAssembly(serverPlayer, bp);
                    }
                }
            }
        });
    }
}
