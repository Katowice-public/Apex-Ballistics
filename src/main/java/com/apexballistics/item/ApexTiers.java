package com.apexballistics.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import com.apexballistics.registry.ModItems;

public enum ApexTiers implements Tier {
    APEX(2500, 12.0f, 5.0f, 18);

    private final int uses;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;

    ApexTiers(int uses, float speed, float attackDamageBonus, int enchantmentValue) {
        this.uses = uses;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamageBonus;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.APEX_ALLOY.get());
    }
}
