package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.block.entity.ModificationStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class AssembleBlueprintPacket {
    public enum Action {
        SELECT_BLUEPRINT,
        ADD_INGREDIENT,
        REMOVE_INGREDIENT,
        FINALIZE_ASSEMBLY
    }

    private final BlockPos pos;
    private final Action action;
    private final String blueprintId;
    private final int index;

    public AssembleBlueprintPacket(BlockPos pos, Action action, String blueprintId, int index) {
        this.pos = pos;
        this.action = (action != null) ? action : Action.FINALIZE_ASSEMBLY;
        this.blueprintId = (blueprintId != null) ? blueprintId : "";
        this.index = index;
    }

    public AssembleBlueprintPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.action = buf.readEnum(Action.class);
        this.blueprintId = buf.readUtf(64);
        this.index = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeEnum(this.action);
        buf.writeUtf(this.blueprintId, 64);
        buf.writeVarInt(this.index);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.level().isLoaded(this.pos)) {
                BlockEntity be = serverPlayer.level().getBlockEntity(this.pos);
                if (be instanceof ModificationStationBlockEntity station && station.stillValid(serverPlayer)) {
                    switch (this.action) {
                        case SELECT_BLUEPRINT -> station.selectBlueprint(serverPlayer, this.blueprintId);
                        case ADD_INGREDIENT -> station.addIngredient(serverPlayer, this.index);
                        case REMOVE_INGREDIENT -> station.removeIngredient(serverPlayer, this.index);
                        case FINALIZE_ASSEMBLY -> station.finalizeAssembly(serverPlayer);
                    }
                }
            }
        });
    }
}
