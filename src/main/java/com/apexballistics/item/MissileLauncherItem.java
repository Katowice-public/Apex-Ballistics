package com.apexballistics.item;

import com.apexballistics.entity.MissileEntity;
import com.apexballistics.registry.ModEntities;
import com.apexballistics.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

public class MissileLauncherItem extends Item {
    public MissileLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack launcher = player.getItemInHand(hand);
        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty() && !player.getAbilities().instabuild) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.apexballistics.no_ammo").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(launcher);
        }

        MissileKind kind = MissileKind.SAM;
        if (!ammo.isEmpty() && ammo.getItem() instanceof MissileItem missileItem) {
            kind = missileItem.kind();
        }

        if (!level.isClientSide) {
            MissileEntity missile = new MissileEntity(ModEntities.MISSILE.get(), level);
            missile.setKind(kind);
            if (!ammo.isEmpty()) {
                missile.configureFromStack(ammo);
            }
            missile.setOwner(player);
            missile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            missile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, kind.launchSpeed() + 0.4f, 0.15f);
            missile.acquireAirTarget(player);
            level.addFreshEntity(missile);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.INTERCEPTOR_LAUNCH.get(), SoundSource.PLAYERS, 2.4f, 1.0f);
            launcher.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            if (!player.getAbilities().instabuild && !ammo.isEmpty()) {
                ammo.shrink(1);
            }
        }
        player.getCooldowns().addCooldown(this, 25);
        return InteractionResultHolder.sidedSuccess(launcher, level.isClientSide);
    }

    public static ItemStack findAmmo(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof MissileItem missileItem && missileItem.kind().handheld()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics.manpads.desc").withStyle(ChatFormatting.GRAY));
    }
}
