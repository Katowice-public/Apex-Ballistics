package com.apexballistics.item;

import com.apexballistics.block.DroneLauncherBlock;
import com.apexballistics.block.VehicleLayout;
import com.apexballistics.block.VehiclePartBlock;
import com.apexballistics.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class DroneLauncherItem extends BlockItem {
    public DroneLauncherItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!super.canPlace(context, state)) {
            return false;
        }
        Direction facing = state.getValue(DroneLauncherBlock.FACING);
        if (!DroneLauncherBlock.spaceClear(context.getLevel(), context.getClickedPos(), facing, context)) {
            if (context.getPlayer() != null && !context.getLevel().isClientSide) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("message.apexballistics.drone_launcher_blocked")
                                .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        return true;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        var level = context.getLevel();
        BlockPos origin = context.getClickedPos();
        Direction facing = state.getValue(DroneLauncherBlock.FACING);
        List<BlockPos> cells = VehicleLayout.cells(origin, facing);
        if (!level.setBlock(origin, state, 3)) {
            return false;
        }
        for (int i = 1; i < cells.size(); i++) {
            BlockState part = ModBlocks.VEHICLE_PART.get().defaultBlockState()
                    .setValue(VehiclePartBlock.FACING, facing)
                    .setValue(VehiclePartBlock.CELL, i);
            level.setBlock(cells.get(i), part, 3);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics.drone_launcher.desc").withStyle(ChatFormatting.GRAY));
    }
}
