package com.apexballistics.item;

import com.apexballistics.block.CableLinkable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CableItem extends BlockItem {
    public CableItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        var player = context.getPlayer();
        if (be instanceof CableLinkable && player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                handleLink(context, be);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }

    private void handleLink(UseOnContext context, BlockEntity target) {
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains("LinkX")) {
            BlockPos pos = context.getClickedPos();
            tag.putInt("LinkX", pos.getX());
            tag.putInt("LinkY", pos.getY());
            tag.putInt("LinkZ", pos.getZ());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.apexballistics.cable_start")
                        .withStyle(ChatFormatting.AQUA), true);
            }
            return;
        }
        BlockPos startPos = new BlockPos(tag.getInt("LinkX"), tag.getInt("LinkY"), tag.getInt("LinkZ"));
        tag.remove("LinkX");
        tag.remove("LinkY");
        tag.remove("LinkZ");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        BlockEntity start = context.getLevel().getBlockEntity(startPos);
        if (!(start instanceof CableLinkable startLink) || !(target instanceof CableLinkable endLink)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.apexballistics.cable_invalid")
                        .withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        if (startPos.equals(context.getClickedPos())
                || !startLink.acceptCableFrom(target)
                || !endLink.acceptCableFrom(start)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.apexballistics.cable_invalid")
                        .withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        startLink.setCablePeer(context.getClickedPos());
        endLink.setCablePeer(startPos);
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.apexballistics.cable_linked")
                    .withStyle(ChatFormatting.GREEN), true);
        }
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
