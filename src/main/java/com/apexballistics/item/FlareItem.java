package com.apexballistics.item;

import com.apexballistics.entity.FlareEntity;
import com.apexballistics.registry.ModEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FlareItem extends Item {
    public FlareItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            FlareEntity flare = new FlareEntity(ModEntities.FLARE.get(), level);
            flare.setPos(player.getX(), player.getEyeY(), player.getZ());
            flare.setDeltaMovement(player.getLookAngle().scale(0.6).add(0, 0.15, 0));
            level.addFreshEntity(flare);
            level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS, 0.8f, 1.6f);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
