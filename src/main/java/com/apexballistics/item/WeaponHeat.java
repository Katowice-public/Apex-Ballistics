package com.apexballistics.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class WeaponHeat {
    public static final int MAX_HEAT = 100;

    private WeaponHeat() {
    }

    public static int get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.max(0, data.copyTag().getInt("Heat"));
    }

    public static void add(ItemStack stack, int amount) {
        set(stack, get(stack) + amount);
    }

    public static void cool(ItemStack stack, int amount) {
        set(stack, get(stack) - amount);
    }

    public static void set(ItemStack stack, int heat) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt("Heat", Math.clamp(heat, 0, MAX_HEAT)));
    }

    public static boolean overheated(ItemStack stack) {
        return get(stack) >= MAX_HEAT;
    }
}
