package com.apexballistics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BombItem extends Item {
    private final BombKind kind;

    public BombItem(BombKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public BombKind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics." + kind.getSerializedName() + ".desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.apexballistics.blast", String.format("%.1f", kind.blast()))
                .withStyle(ChatFormatting.DARK_RED));
        WeaponPerks.fromStack(stack).appendTooltip(tooltip);
    }
}
