package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import com.apexballistics.entity.GaussSlugEntity;
import com.apexballistics.entity.MissileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ApexBallistics.MOD_ID);

    public static final RegistryObject<EntityType<MissileEntity>> MISSILE = ENTITY_TYPES.register("missile",
            () -> EntityType.Builder.<MissileEntity>of(MissileEntity::new, MobCategory.MISC)
                    .sized(0.7f, 0.7f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .build("missile"));

    public static final RegistryObject<EntityType<GaussSlugEntity>> GAUSS_SLUG = ENTITY_TYPES.register("gauss_slug",
            () -> EntityType.Builder.<GaussSlugEntity>of(GaussSlugEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .build("gauss_slug"));

    private ModEntities() {
    }
}
