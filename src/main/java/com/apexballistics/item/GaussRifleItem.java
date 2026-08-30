package com.apexballistics.item;

import com.apexballistics.entity.GaussSlugEntity;
import com.apexballistics.registry.ModEntities;
import com.apexballistics.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class GaussRifleItem extends Item {
    public GaussRifleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        ItemStack ammo = findSlug(player);
        if (ammo.isEmpty() && !player.getAbilities().instabuild) {
            return InteractionResultHolder.fail(weapon);
        }
        if (!level.isClientSide) {
            GaussSlugEntity slug = new GaussSlugEntity(ModEntities.GAUSS_SLUG.get(), level);
            slug.setOwner(player);
            slug.setPower(false);
            slug.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            slug.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 4.8f, 0.2f);
            level.addFreshEntity(slug);
            weapon.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            if (!player.getAbilities().instabuild && !ammo.isEmpty()) {
                ammo.shrink(1);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.6f);
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(weapon, level.isClientSide);
    }

    public static ItemStack findSlug(Player player) {
        if (player.getOffhandItem().is(ModItems.GAUSS_SLUG.get())) {
            return player.getOffhandItem();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.GAUSS_SLUG.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics.gauss_rifle.desc").withStyle(ChatFormatting.GRAY));
    }
}
