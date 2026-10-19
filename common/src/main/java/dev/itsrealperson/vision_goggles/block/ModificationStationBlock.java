package dev.itsrealperson.vision_goggles.block;

import dev.itsrealperson.vision_goggles.block.entity.ModificationStationBlockEntity;
import dev.itsrealperson.vision_goggles.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.core.Direction;

public class ModificationStationBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ModificationStationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModificationStationBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ModificationStationBlockEntity) {
                dev.architectury.registry.menu.MenuRegistry.openExtendedMenu((ServerPlayer) player, (ModificationStationBlockEntity) be);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ModificationStationBlockEntity station) {
                // Clear slot 2 (result preview) to prevent duplication on block break
                station.getItem(2).setCount(0);
                station.setItem(2, net.minecraft.world.item.ItemStack.EMPTY);

                // Only drop slot 0 (goggles) and slot 1 (module/item)
                net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> drops = net.minecraft.core.NonNullList.create();
                if (!station.getItem(0).isEmpty()) {
                    drops.add(station.getItem(0).copy());
                }
                if (!station.getItem(1).isEmpty()) {
                    drops.add(station.getItem(1).copy());
                }
                net.minecraft.world.Containers.dropContents(level, pos, drops);

                // Clear input slots so block entity is empty
                station.setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
                station.setItem(1, net.minecraft.world.item.ItemStack.EMPTY);
                station.getItem(2).setCount(0);
                station.setItem(2, net.minecraft.world.item.ItemStack.EMPTY);

                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
