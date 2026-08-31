package com.apexballistics.item;

import com.apexballistics.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ApexArmorItem extends ArmorItem {
    public ApexArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (player.getItemBySlot(this.getEquipmentSlot()) != stack) {
            return;
        }
        if (!PoweredEquipment.initialized(stack)) {
            PoweredEquipment.setEnergy(stack, 2_500);
        }
        if (level.getGameTime() % 20 != 0 || !PoweredEquipment.consume(stack, 2)) {
            return;
        }
        switch (this.type) {
            case HELMET -> player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
            case CHESTPLATE -> player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
            case LEGGINGS -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false, false));
            case BOOTS -> {
                if (player.fallDistance > 3.0f) {
                    player.fallDistance *= 0.2f;
                }
            }
            default -> {
            }
        }
        if (hasFullSet(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false, false));
        }
        applyModule(stack, player);
    }

    private void applyModule(ItemStack stack, Player player) {
        String module = PoweredEquipment.module(stack);
        switch (module) {
            case "thermal" -> {
                if (type == Type.HELMET && PoweredEquipment.consume(stack, 8)) {
                    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                            new AABB(player.blockPosition()).inflate(24),
                            entity -> entity != player && entity.isAlive())) {
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0,
                                true, false, false));
                    }
                }
            }
            case "mobility" -> {
                if (type == Type.CHESTPLATE && player.isShiftKeyDown() && !player.onGround()
                        && PoweredEquipment.consume(stack, 10)) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0, 0.16, 0));
                    player.fallDistance = 0;
                }
            }
            case "medical" -> {
                if (type == Type.CHESTPLATE && player.getHealth() < player.getMaxHealth() * 0.5f
                        && player.level().getGameTime() % 100 == 0
                        && PoweredEquipment.consume(stack, 40)) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0,
                            true, false, true));
                }
            }
            case "camouflage" -> {
                if (player.isShiftKeyDown() && PoweredEquipment.consume(stack, 5)) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 25, 0,
                            true, false, false));
                }
            }
            default -> {
            }
        }
    }

    public static boolean hasFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.APEX_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.APEX_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.APEX_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.APEX_BOOTS.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.apexballistics.energy",
                PoweredEquipment.energy(stack), PoweredEquipment.MAX_ARMOR_ENERGY)
                .withStyle(ChatFormatting.AQUA));
        String module = PoweredEquipment.module(stack);
        if (!module.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.apexballistics.armor_module", module)
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
