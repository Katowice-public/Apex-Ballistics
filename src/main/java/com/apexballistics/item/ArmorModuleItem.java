package com.apexballistics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ArmorModuleItem extends Item {
    private final String moduleId;

    public ArmorModuleItem(String moduleId, Properties properties) {
        super(properties);
        this.moduleId = moduleId;
    }

    public String moduleId() {
        return moduleId;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics.armor_module." + moduleId + ".desc")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.apexballistics.armor_module.install")
                .withStyle(ChatFormatting.GRAY));
    }
}
