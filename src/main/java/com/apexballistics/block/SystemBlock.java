package com.apexballistics.block;

import com.apexballistics.blockentity.DefenseSystemBlockEntity;
import com.apexballistics.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SystemBlock extends Block implements EntityBlock {
    private final SystemType systemType;

    public SystemBlock(Properties properties, SystemType systemType) {
        super(properties);
        this.systemType = systemType;
    }

    public SystemType systemType() {
        return systemType;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DefenseSystemBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == ModBlockEntities.DEFENSE_SYSTEM.get() ? (lvl, pos, st, be) -> {
            if (be instanceof DefenseSystemBlockEntity system) {
                system.serverTick();
            }
        } : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DefenseSystemBlockEntity system
                && system.interact(player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level,
                                                                    BlockPos pos, Player player,
                                                                    BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DefenseSystemBlockEntity system
                && system.interact(player, ItemStack.EMPTY)) {
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
}
