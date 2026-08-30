package com.apexballistics.event;

import com.apexballistics.ApexBallistics;
import com.apexballistics.item.ApexArmorItem;
import com.apexballistics.item.PoweredEquipment;
import com.apexballistics.blockentity.DefenseSystemBlockEntity;
import com.apexballistics.blockentity.LauncherBlockEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApexBallistics.MOD_ID)
public final class ApexEvents {
    private ApexEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!ApexArmorItem.hasFullSet(entity)) {
            return;
        }
        net.minecraft.world.item.ItemStack chest = entity.getItemBySlot(
                net.minecraft.world.entity.EquipmentSlot.CHEST);
        if ("shield".equals(PoweredEquipment.module(chest))
                && PoweredEquipment.consume(chest, Math.max(1, Math.round(event.getAmount() * 8)))) {
            event.setAmount(event.getAmount() * 0.65f);
        }
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setAmount(event.getAmount() * 0.35f);
        }
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * 0.4f);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        for (net.minecraft.core.BlockPos pos : event.getAffectedBlocks()) {
            if (event.getLevel().getBlockEntity(pos) instanceof LauncherBlockEntity launcher) {
                launcher.damageIntegrity(35);
            } else if (event.getLevel().getBlockEntity(pos) instanceof DefenseSystemBlockEntity system) {
                system.damageIntegrity(35);
            }
        }
    }
}
