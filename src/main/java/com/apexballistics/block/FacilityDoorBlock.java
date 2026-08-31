package com.apexballistics.block;

import com.apexballistics.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FacilityDoorBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final DoorKind kind;

    public FacilityDoorBlock(Properties properties, DoorKind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false));
    }

    public DoorKind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.shape(state.getValue(FACING), state.getValue(OPEN), true);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.shape(state.getValue(FACING), state.getValue(OPEN), false);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return kind.shape(state.getValue(FACING), state.getValue(OPEN), true);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return kind.shape(state.getValue(FACING), false, false);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return state.getValue(OPEN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            setOpen(level, pos, !state.getValue(OPEN));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void setOpen(Level level, BlockPos origin, boolean open) {
        BlockState originState = level.getBlockState(origin);
        if (!(originState.getBlock() instanceof FacilityDoorBlock door)) {
            return;
        }
        Direction facing = originState.getValue(FACING);
        if (originState.getValue(OPEN) != open) {
            level.setBlock(origin, originState.setValue(OPEN, open), 3);
        }
        List<BlockPos> cells = door.kind.cells(origin, facing);
        for (int i = 1; i < cells.size(); i++) {
            BlockState part = level.getBlockState(cells.get(i));
            if (part.is(ModBlocks.DOOR_PART.get()) && part.getValue(DoorPartBlock.OPEN) != open) {
                level.setBlock(cells.get(i), part.setValue(DoorPartBlock.OPEN, open), 3);
            }
        }
        level.playSound(null, origin,
                open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS, 1.0f, open ? 0.9f : 1.05f);
    }

    public static void destroyStructure(Level level, BlockPos origin, DoorKind kind, Direction facing) {
        if (Boolean.TRUE.equals(BUSY.get()) || kind == null || facing == null) {
            return;
        }
        BUSY.set(true);
        try {
            for (BlockPos cell : kind.cells(origin, facing)) {
                BlockState state = level.getBlockState(cell);
                if (state.getBlock() instanceof FacilityDoorBlock || state.is(ModBlocks.DOOR_PART.get())) {
                    level.removeBlock(cell, false);
                }
            }
        } finally {
            BUSY.set(false);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            destroyStructure(level, pos, kind, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(asItem());
    }
}
