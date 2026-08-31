package com.apexballistics.block;

import com.apexballistics.blockentity.LauncherBlockEntity;
import com.apexballistics.item.MissileItem;
import com.apexballistics.item.TargetingTabletItem;
import com.apexballistics.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class LauncherBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final LauncherType launcherType;

    public LauncherBlock(Properties properties, LauncherType launcherType) {
        super(properties);
        this.launcherType = launcherType;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public LauncherType launcherType() {
        return launcherType;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LauncherBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.LAUNCHER.get() ? (lvl, pos, st, be) -> {
            if (be instanceof LauncherBlockEntity launcher) {
                launcher.serverTick();
            }
        } : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof MissileItem missileItem) {
            if (!launcherType.accepts(missileItem.kind())) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.apexballistics.wrong_missile").withStyle(ChatFormatting.RED), true);
                }
                return ItemInteractionResult.FAIL;
            }
            if (!launcher.canLoad(stack)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.apexballistics.already_loaded").withStyle(ChatFormatting.YELLOW), true);
                }
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                launcher.loadOne(stack, player);
                player.displayClientMessage(Component.translatable("message.apexballistics.loaded", missileItem.kind().displayName()), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof TargetingTabletItem) {
            TargetingTabletItem.readTarget(stack).ifPresentOrElse(target -> {
                if (!level.isClientSide) {
                    java.util.List<BlockPos> waypoints = TargetingTabletItem.readWaypoints(stack);
                    launcher.setFlightPlan(waypoints.isEmpty() ? java.util.List.of(target) : waypoints);
                    launcher.setProgrammedAirburstHeight(
                            TargetingTabletItem.readAirburstHeight(stack));
                    player.displayClientMessage(Component.translatable("message.apexballistics.launcher_target", target.getX(), target.getY(), target.getZ())
                            .withStyle(ChatFormatting.AQUA), true);
                }
            }, () -> {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.apexballistics.target_none").withStyle(ChatFormatting.GRAY), true);
                }
            });
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            if (launcher.tryLaunch(player)) {
                player.displayClientMessage(Component.translatable("message.apexballistics.launch").withStyle(ChatFormatting.GOLD), true);
            } else if (launcher.getMissile().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.apexballistics.empty").withStyle(ChatFormatting.GRAY), true);
            } else {
                player.displayClientMessage(Component.translatable("message.apexballistics.cooldown").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(launcher, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher) {
            launcher.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
