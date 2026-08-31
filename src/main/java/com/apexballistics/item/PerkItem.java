package com.apexballistics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class PerkItem extends Item {
    private final PerkKind kind;

    public PerkItem(PerkKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public PerkKind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics.perk.desc", kind.getSerializedName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.apexballistics.perk.workbench")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
