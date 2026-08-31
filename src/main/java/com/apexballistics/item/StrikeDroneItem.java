package com.apexballistics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class StrikeDroneItem extends Item {
    public StrikeDroneItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics.strike_drone.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.apexballistics.drone_launcher_only").withStyle(ChatFormatting.DARK_AQUA));
        WeaponPerks.fromStack(stack).appendTooltip(tooltip);
    }
}
