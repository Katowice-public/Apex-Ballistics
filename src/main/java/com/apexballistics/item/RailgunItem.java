package com.apexballistics.item;

import com.apexballistics.entity.GaussSlugEntity;
import com.apexballistics.registry.ModEntities;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public class RailgunItem extends Item {
    public RailgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        ItemStack ammo = GaussRifleItem.findSlug(player);
        if (ammo.isEmpty() && !player.getAbilities().instabuild) {
            return InteractionResultHolder.fail(weapon);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(weapon);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int charged = getUseDuration(stack, entity) - timeLeft;
        if (charged < 20) {
            return;
        }
        ItemStack ammo = GaussRifleItem.findSlug(player);
        if (ammo.isEmpty() && !player.getAbilities().instabuild) {
            return;
        }
        float power = Math.min(1.0f, charged / 40.0f);
        if (!level.isClientSide) {
            GaussSlugEntity slug = new GaussSlugEntity(ModEntities.GAUSS_SLUG.get(), level);
            slug.setOwner(player);
            slug.setPower(true);
            slug.setDamage(18.0f + 22.0f * power);
            slug.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            slug.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 5.5f + power, 0.05f);
            level.addFreshEntity(slug);
            stack.hurtAndBreak(2, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            if (!player.getAbilities().instabuild && !ammo.isEmpty()) {
                ammo.shrink(1);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 1.4f);
        }
        player.getCooldowns().addCooldown(this, 35);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics.railgun.desc").withStyle(ChatFormatting.GRAY));
    }
}
