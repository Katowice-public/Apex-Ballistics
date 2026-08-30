package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import com.apexballistics.blockentity.LauncherBlockEntity;
import com.apexballistics.blockentity.RadarBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ApexBallistics.MOD_ID);

    public static final RegistryObject<BlockEntityType<LauncherBlockEntity>> LAUNCHER = BLOCK_ENTITIES.register("launcher",
            () -> BlockEntityType.Builder.of(LauncherBlockEntity::new,
                    ModBlocks.ICBM_SILO.get(),
                    ModBlocks.SLBM_TUBE.get(),
                    ModBlocks.CRUISE_PAD.get(),
                    ModBlocks.SAM_BATTERY.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<RadarBlockEntity>> RADAR = BLOCK_ENTITIES.register("radar",
            () -> BlockEntityType.Builder.of(RadarBlockEntity::new, ModBlocks.RADAR.get()).build(null));

    private ModBlockEntities() {
    }
}
