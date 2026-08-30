package com.apexballistics.item;

import com.apexballistics.entity.MissileEntity;
import com.apexballistics.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MissileItem extends Item {
    private final MissileKind kind;

    public MissileItem(MissileKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public MissileKind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!kind.handheld()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.apexballistics.needs_launcher").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            MissileEntity missile = new MissileEntity(ModEntities.MISSILE.get(), level);
            missile.setKind(kind);
            missile.setOwner(player);
            missile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            missile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, kind.launchSpeed(), 0.4f);
            missile.acquireAirTarget(player);
            level.addFreshEntity(missile);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2f, 0.7f);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics." + kind.getSerializedName() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.apexballistics.blast", String.format("%.1f", kind.blastPower())).withStyle(ChatFormatting.DARK_RED));
        if (kind.handheld()) {
            tooltip.add(Component.translatable("tooltip.apexballistics.handheld").withStyle(ChatFormatting.DARK_AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.apexballistics.silo_only").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
