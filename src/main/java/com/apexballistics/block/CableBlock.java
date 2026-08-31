package com.apexballistics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Low-profile floor conduit through the center of the block. Arms connect to
 * neighboring cables and cable-linkable devices with no signal decay.
 */
public class CableBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST
    );

    private static final VoxelShape CORE = Block.box(5.0, 0.0, 5.0, 11.0, 4.0, 11.0);
    private static final VoxelShape ARM_NORTH = Block.box(5.0, 0.0, 0.0, 11.0, 4.0, 5.0);
    private static final VoxelShape ARM_SOUTH = Block.box(5.0, 0.0, 11.0, 11.0, 4.0, 16.0);
    private static final VoxelShape ARM_WEST = Block.box(0.0, 0.0, 5.0, 5.0, 4.0, 11.0);
    private static final VoxelShape ARM_EAST = Block.box(11.0, 0.0, 5.0, 16.0, 4.0, 11.0);
    private static final VoxelShape[] SHAPES = buildShapes();

    public CableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connectionState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return connectionState(level, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || movedByPiston) {
            return;
        }
        BlockState connected = connectionState(level, pos, state);
        if (!connected.equals(state)) {
            level.setBlock(pos, connected, 3);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[index(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return CORE;
    }

    public static BlockState connectionState(BlockGetter level, BlockPos pos, BlockState state) {
        return state
                .setValue(NORTH, CableNetwork.connectsTo(level, pos.north()))
                .setValue(EAST, CableNetwork.connectsTo(level, pos.east()))
                .setValue(SOUTH, CableNetwork.connectsTo(level, pos.south()))
                .setValue(WEST, CableNetwork.connectsTo(level, pos.west()));
    }

    private static int index(BlockState state) {
        int value = 0;
        if (state.getValue(NORTH)) {
            value |= 1;
        }
        if (state.getValue(EAST)) {
            value |= 2;
        }
        if (state.getValue(SOUTH)) {
            value |= 4;
        }
        if (state.getValue(WEST)) {
            value |= 8;
        }
        return value;
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[16];
        for (int i = 0; i < 16; i++) {
            VoxelShape shape = CORE;
            if ((i & 1) != 0) {
                shape = Shapes.or(shape, ARM_NORTH);
            }
            if ((i & 2) != 0) {
                shape = Shapes.or(shape, ARM_EAST);
            }
            if ((i & 4) != 0) {
                shape = Shapes.or(shape, ARM_SOUTH);
            }
            if ((i & 8) != 0) {
                shape = Shapes.or(shape, ARM_WEST);
            }
            shapes[i] = shape;
        }
        return shapes;
    }
}
