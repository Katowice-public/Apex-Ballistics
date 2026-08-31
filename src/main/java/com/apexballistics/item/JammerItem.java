package com.apexballistics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class JammerItem extends Item {
    public JammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean active = !isActive(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("Active", active));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, active);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(active
                    ? "message.apexballistics.jammer_on"
                    : "message.apexballistics.jammer_off").withStyle(
                    active ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (!level.isClientSide && isActive(stack) && level.getGameTime() % 40 == 0) {
            stack.setDamageValue(stack.getDamageValue() + 1);
            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                stack.shrink(1);
            }
        }
    }

    public static boolean isActive(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean("Active");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics.jammer.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(isActive(stack)
                ? "tooltip.apexballistics.active"
                : "tooltip.apexballistics.inactive").withStyle(
                isActive(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }
}
