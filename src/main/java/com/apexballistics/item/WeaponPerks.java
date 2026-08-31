package com.apexballistics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Upgradeable range / damage / accuracy / speed ranks stored on missiles and drones.
 * Ranks never decay with distance; they only change at a perk workbench.
 */
public record WeaponPerks(int range, int damage, int accuracy, int speed) {
    public static final int MAX = 5;

    public static WeaponPerks none() {
        return new WeaponPerks(0, 0, 0, 0);
    }

    public static WeaponPerks fromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return none();
        }
        CompoundTag tag = data.copyTag();
        return new WeaponPerks(
                clamp(tag.getInt("PerkRange")),
                clamp(tag.getInt("PerkDamage")),
                clamp(tag.getInt("PerkAccuracy")),
                clamp(tag.getInt("PerkSpeed")));
    }

    public static void write(ItemStack stack, WeaponPerks perks) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("PerkRange", perks.range);
            tag.putInt("PerkDamage", perks.damage);
            tag.putInt("PerkAccuracy", perks.accuracy);
            tag.putInt("PerkSpeed", perks.speed);
        });
    }

    public static boolean upgrade(ItemStack stack, PerkKind kind) {
        WeaponPerks current = fromStack(stack);
        if (current.level(kind) >= MAX) {
            return false;
        }
        write(stack, current.increment(kind));
        return true;
    }

    public static boolean canUpgrade(ItemStack stack) {
        return stack.getItem() instanceof MissileItem || stack.getItem() instanceof StrikeDroneItem;
    }

    public int level(PerkKind kind) {
        return switch (kind) {
            case RANGE -> range;
            case DAMAGE -> damage;
            case ACCURACY -> accuracy;
            case SPEED -> speed;
        };
    }

    public WeaponPerks increment(PerkKind kind) {
        return switch (kind) {
            case RANGE -> new WeaponPerks(clamp(range + 1), damage, accuracy, speed);
            case DAMAGE -> new WeaponPerks(range, clamp(damage + 1), accuracy, speed);
            case ACCURACY -> new WeaponPerks(range, damage, clamp(accuracy + 1), speed);
            case SPEED -> new WeaponPerks(range, damage, accuracy, clamp(speed + 1));
        };
    }

    public float rangeMultiplier() {
        return 1.0f + 0.20f * range;
    }

    public float damageMultiplier() {
        return 1.0f + 0.18f * damage;
    }

    /** Multiplies miss CEP; lower is tighter. */
    public float accuracyFactor() {
        return Math.max(0.18f, 1.0f - 0.14f * accuracy);
    }

    public float speedMultiplier() {
        return 1.0f + 0.14f * speed;
    }

    public void appendTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.apexballistics.perk.range", range, MAX)
                .withStyle(range > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.apexballistics.perk.damage", damage, MAX)
                .withStyle(damage > 0 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.apexballistics.perk.accuracy", accuracy, MAX)
                .withStyle(accuracy > 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.apexballistics.perk.speed", speed, MAX)
                .withStyle(speed > 0 ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
    }

    private static int clamp(int value) {
        return Math.clamp(value, 0, MAX);
    }
}
