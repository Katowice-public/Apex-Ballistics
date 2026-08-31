package com.apexballistics.item;

import com.apexballistics.block.DoorKind;
import com.apexballistics.block.DoorPartBlock;
import com.apexballistics.block.FacilityDoorBlock;
import com.apexballistics.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class FacilityDoorItem extends BlockItem {
    private final DoorKind kind;

    public FacilityDoorItem(Block block, DoorKind kind, Properties properties) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!super.canPlace(context, state)) {
            return false;
        }
        Direction facing = state.getValue(FacilityDoorBlock.FACING);
        for (BlockPos cell : kind.cells(context.getClickedPos(), facing)) {
            if (!cell.equals(context.getClickedPos())
                    && !context.getLevel().getBlockState(cell).canBeReplaced(context)) {
                if (context.getPlayer() != null && !context.getLevel().isClientSide) {
                    context.getPlayer().displayClientMessage(
                            Component.translatable("message.apexballistics.door_blocked")
                                    .withStyle(ChatFormatting.RED), true);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        net.minecraft.world.level.Level level = context.getLevel();
        BlockPos origin = context.getClickedPos();
        Direction facing = state.getValue(FacilityDoorBlock.FACING);
        List<BlockPos> cells = kind.cells(origin, facing);
        if (!level.setBlock(origin, state, 3)) {
            return false;
        }
        for (int i = 1; i < cells.size(); i++) {
            BlockState part = ModBlocks.DOOR_PART.get().defaultBlockState()
                    .setValue(DoorPartBlock.FACING, facing)
                    .setValue(DoorPartBlock.KIND, kind)
                    .setValue(DoorPartBlock.CELL, i)
                    .setValue(DoorPartBlock.OPEN, false);
            level.setBlock(cells.get(i), part, 3);
        }
        return true;
    }
}
