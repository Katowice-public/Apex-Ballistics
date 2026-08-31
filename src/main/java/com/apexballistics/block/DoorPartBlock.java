package com.apexballistics.block;

import com.apexballistics.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DoorPartBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoorKind> KIND = EnumProperty.create("kind", DoorKind.class);
    public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, 8);

    public DoorPartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(KIND, DoorKind.BLAST)
                .setValue(CELL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, KIND, CELL);
    }

    public static BlockPos originOf(BlockPos part, BlockState state) {
        return state.getValue(KIND).originFrom(part, state.getValue(FACING), state.getValue(CELL));
    }

    private static boolean originOpen(BlockGetter level, BlockPos origin) {
        BlockState state = level.getBlockState(origin);
        return state.hasProperty(FacilityDoorBlock.OPEN) && state.getValue(FacilityDoorBlock.OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(KIND).shape(state.getValue(FACING), originOpen(level, originOf(pos, state)), true);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(KIND).shape(state.getValue(FACING), originOpen(level, originOf(pos, state)), false);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(KIND).shape(state.getValue(FACING), originOpen(level, originOf(pos, state)), true);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockPos origin = originOf(pos, state);
            BlockState originState = level.getBlockState(origin);
            if (originState.getBlock() instanceof FacilityDoorBlock) {
                FacilityDoorBlock.setOpen(level, origin, !originState.getValue(FacilityDoorBlock.OPEN));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            FacilityDoorBlock.destroyStructure(level, originOf(pos, state),
                    state.getValue(KIND), state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(ModBlocks.itemForDoor(state.getValue(KIND)));
    }
}
