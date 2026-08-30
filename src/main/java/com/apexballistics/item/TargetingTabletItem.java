package com.apexballistics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

public class TargetingTabletItem extends Item {
    public TargetingTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        writeTarget(context.getItemInHand(), pos);
        if (!context.getLevel().isClientSide) {
            player.displayClientMessage(Component.translatable("message.apexballistics.target_set", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.AQUA), true);
            context.getLevel().playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Optional<BlockPos> target = readTarget(stack);
        if (!level.isClientSide) {
            if (target.isPresent()) {
                BlockPos pos = target.get();
                player.displayClientMessage(Component.translatable("message.apexballistics.target_current", pos.getX(), pos.getY(), pos.getZ())
                        .withStyle(ChatFormatting.AQUA), true);
            } else {
                player.displayClientMessage(Component.translatable("message.apexballistics.target_none").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void writeTarget(ItemStack stack, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Tx", pos.getX());
        tag.putInt("Ty", pos.getY());
        tag.putInt("Tz", pos.getZ());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static Optional<BlockPos> readTarget(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains("Tx")) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(tag.getInt("Tx"), tag.getInt("Ty"), tag.getInt("Tz")));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics.targeting_tablet.desc").withStyle(ChatFormatting.GRAY));
        readTarget(stack).ifPresent(pos -> tooltip.add(Component.translatable("tooltip.apexballistics.target", pos.getX(), pos.getY(), pos.getZ())
                .withStyle(ChatFormatting.AQUA)));
    }
}
