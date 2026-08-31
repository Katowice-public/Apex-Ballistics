package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import com.apexballistics.entity.BombEntity;
import com.apexballistics.entity.CiwsTracerEntity;
import com.apexballistics.entity.FlareEntity;
import com.apexballistics.entity.GaussSlugEntity;
import com.apexballistics.entity.MissileEntity;
import com.apexballistics.entity.StrikeDroneEntity;
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
                    .sized(1.4f, 1.4f)
                    .clientTrackingRange(64)
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

    public static final RegistryObject<EntityType<FlareEntity>> FLARE = ENTITY_TYPES.register("flare",
            () -> EntityType.Builder.<FlareEntity>of(FlareEntity::new, MobCategory.MISC)
                    .sized(0.2f, 0.2f)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .fireImmune()
                    .build("flare"));

    public static final RegistryObject<EntityType<StrikeDroneEntity>> STRIKE_DRONE = ENTITY_TYPES.register("strike_drone",
            () -> EntityType.Builder.<StrikeDroneEntity>of(StrikeDroneEntity::new, MobCategory.MISC)
                    .sized(1.6f, 0.55f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .build("strike_drone"));

    public static final RegistryObject<EntityType<BombEntity>> BOMB = ENTITY_TYPES.register("bomb",
            () -> EntityType.Builder.<BombEntity>of(BombEntity::new, MobCategory.MISC)
                    .sized(0.55f, 0.55f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .build("bomb"));

    public static final RegistryObject<EntityType<CiwsTracerEntity>> CIWS_TRACER = ENTITY_TYPES.register("ciws_tracer",
            () -> EntityType.Builder.<CiwsTracerEntity>of(CiwsTracerEntity::new, MobCategory.MISC)
                    .sized(0.28f, 0.28f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .build("ciws_tracer"));

    private ModEntities() {
    }
}
