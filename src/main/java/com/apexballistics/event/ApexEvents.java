package com.apexballistics.event;

import com.apexballistics.ApexBallistics;
import com.apexballistics.item.ApexArmorItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setAmount(event.getAmount() * 0.35f);
        }
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * 0.4f);
        }
    }
}
