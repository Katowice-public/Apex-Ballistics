package com.apexballistics.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PoweredEquipment {
    public static final int MAX_ARMOR_ENERGY = 10_000;

    private PoweredEquipment() {
    }

    public static int energy(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.max(0, data.copyTag().getInt("Energy"));
    }

    public static boolean initialized(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean("EnergyInitialized");
    }

    public static void setEnergy(ItemStack stack, int energy) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("Energy", Math.clamp(energy, 0, MAX_ARMOR_ENERGY));
            tag.putBoolean("EnergyInitialized", true);
        });
    }

    public static boolean consume(ItemStack stack, int amount) {
        int current = energy(stack);
        if (current < amount) {
            return false;
        }
        setEnergy(stack, current - amount);
        return true;
    }

    public static String module(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString("ArmorModule");
    }

    public static void setModule(ItemStack stack, String module) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString("ArmorModule", module));
    }
}
