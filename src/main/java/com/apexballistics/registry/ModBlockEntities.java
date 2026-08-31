package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import com.apexballistics.blockentity.DefenseSystemBlockEntity;
import com.apexballistics.blockentity.DroneLauncherBlockEntity;
import com.apexballistics.blockentity.LauncherBlockEntity;
import com.apexballistics.blockentity.MissileShowcaseBlockEntity;
import com.apexballistics.blockentity.PerkWorkbenchBlockEntity;
import com.apexballistics.blockentity.RadarBlockEntity;
import com.apexballistics.blockentity.SirenBlockEntity;
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
                    ModBlocks.SAM_BATTERY.get(),
                    ModBlocks.MOBILE_LAUNCHER.get(),
                    ModBlocks.VLS.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<RadarBlockEntity>> RADAR = BLOCK_ENTITIES.register("radar",
            () -> BlockEntityType.Builder.of(RadarBlockEntity::new, ModBlocks.RADAR.get()).build(null));

    public static final RegistryObject<BlockEntityType<DefenseSystemBlockEntity>> DEFENSE_SYSTEM =
            BLOCK_ENTITIES.register("defense_system", () -> BlockEntityType.Builder.of(
                    DefenseSystemBlockEntity::new,
                    ModBlocks.CIWS.get(),
                    ModBlocks.LASER_DEFENSE.get(),
                    ModBlocks.PASSIVE_RADAR.get(),
                    ModBlocks.COMMAND_CONSOLE.get(),
                    ModBlocks.SUBMARINE_CONTROL.get(),
                    ModBlocks.MISSILE_RACK.get(),
                    ModBlocks.LOADING_CRANE.get(),
                    ModBlocks.PROPELLANT_REFINERY.get(),
                    ModBlocks.MAINTENANCE_STATION.get(),
                    ModBlocks.CAPACITOR_CHARGER.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<SirenBlockEntity>> SIREN = BLOCK_ENTITIES.register("siren",
            () -> BlockEntityType.Builder.of(SirenBlockEntity::new,
                    ModBlocks.AIR_RAID_SIREN.get(),
                    ModBlocks.INDUSTRIAL_SIREN.get(),
                    ModBlocks.NUCLEAR_WARNING_SIREN.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<MissileShowcaseBlockEntity>> SHOWCASE =
            BLOCK_ENTITIES.register("showcase", () -> BlockEntityType.Builder.of(
                    MissileShowcaseBlockEntity::new, ModBlocks.MISSILE_SHOWCASE.get()).build(null));

    public static final RegistryObject<BlockEntityType<DroneLauncherBlockEntity>> DRONE_LAUNCHER =
            BLOCK_ENTITIES.register("drone_launcher", () -> BlockEntityType.Builder.of(
                    DroneLauncherBlockEntity::new, ModBlocks.DRONE_LAUNCHER.get()).build(null));

    public static final RegistryObject<BlockEntityType<PerkWorkbenchBlockEntity>> PERK_WORKBENCH =
            BLOCK_ENTITIES.register("perk_workbench", () -> BlockEntityType.Builder.of(
                    PerkWorkbenchBlockEntity::new, ModBlocks.PERK_WORKBENCH.get()).build(null));

    private ModBlockEntities() {
    }
}
