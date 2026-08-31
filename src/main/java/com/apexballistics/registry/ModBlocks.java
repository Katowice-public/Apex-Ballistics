package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import com.apexballistics.block.LauncherBlock;
import com.apexballistics.block.LauncherType;
import com.apexballistics.block.MissileAssemblyBlock;
import com.apexballistics.block.RadarBlock;
import com.apexballistics.block.SystemBlock;
import com.apexballistics.block.SystemType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ApexBallistics.MOD_ID);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(8.0f, 1200.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties hardware() {
        return metal().noOcclusion().isViewBlocking((state, level, pos) -> false);
    }

    private static BlockBehaviour.Properties reinforced(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(30.0f, 2400.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    public static final RegistryObject<Block> APEX_ALLOY_BLOCK = BLOCKS.register("apex_alloy_block",
            () -> new Block(metal().mapColor(MapColor.COLOR_CYAN)));

    public static final RegistryObject<LauncherBlock> ICBM_SILO = BLOCKS.register("icbm_silo",
            () -> new LauncherBlock(hardware().mapColor(MapColor.COLOR_GRAY), LauncherType.SILO));

    public static final RegistryObject<LauncherBlock> SLBM_TUBE = BLOCKS.register("slbm_tube",
            () -> new LauncherBlock(hardware().mapColor(MapColor.COLOR_BLUE), LauncherType.TUBE));

    public static final RegistryObject<LauncherBlock> CRUISE_PAD = BLOCKS.register("cruise_pad",
            () -> new LauncherBlock(hardware().mapColor(MapColor.COLOR_GREEN), LauncherType.PAD));

    public static final RegistryObject<LauncherBlock> SAM_BATTERY = BLOCKS.register("sam_battery",
            () -> new LauncherBlock(hardware().mapColor(MapColor.COLOR_YELLOW), LauncherType.SAM_BATTERY));

    public static final RegistryObject<LauncherBlock> MOBILE_LAUNCHER = BLOCKS.register("mobile_launcher",
            () -> new LauncherBlock(hardware().mapColor(MapColor.COLOR_GREEN), LauncherType.MOBILE));

    public static final RegistryObject<LauncherBlock> VLS = BLOCKS.register("vls",
            () -> new LauncherBlock(hardware().mapColor(MapColor.COLOR_GRAY), LauncherType.VLS));

    public static final RegistryObject<RadarBlock> RADAR = BLOCKS.register("radar",
            () -> new RadarBlock(hardware().mapColor(MapColor.COLOR_LIGHT_BLUE)));

    public static final RegistryObject<MissileAssemblyBlock> MISSILE_ASSEMBLY = BLOCKS.register("missile_assembly",
            () -> new MissileAssemblyBlock(hardware().mapColor(MapColor.COLOR_CYAN)));

    public static final RegistryObject<SystemBlock> CIWS = system("ciws", SystemType.CIWS);
    public static final RegistryObject<SystemBlock> LASER_DEFENSE = system("laser_defense", SystemType.LASER_DEFENSE);
    public static final RegistryObject<SystemBlock> PASSIVE_RADAR = system("passive_radar", SystemType.PASSIVE_RADAR);
    public static final RegistryObject<SystemBlock> COMMAND_CONSOLE = system("command_console", SystemType.COMMAND_CONSOLE);
    public static final RegistryObject<SystemBlock> SUBMARINE_CONTROL = system("submarine_control", SystemType.SUBMARINE_CONTROL);
    public static final RegistryObject<SystemBlock> MISSILE_RACK = system("missile_rack", SystemType.MISSILE_RACK);
    public static final RegistryObject<SystemBlock> LOADING_CRANE = system("loading_crane", SystemType.LOADING_CRANE);
    public static final RegistryObject<SystemBlock> PROPELLANT_REFINERY = system("propellant_refinery", SystemType.REFINERY);
    public static final RegistryObject<SystemBlock> MAINTENANCE_STATION = system("maintenance_station", SystemType.MAINTENANCE);
    public static final RegistryObject<SystemBlock> CAPACITOR_CHARGER = system("capacitor_charger", SystemType.CAPACITOR_CHARGER);

    public static final RegistryObject<Block> REINFORCED_CONCRETE = reinforcedBlock("reinforced_concrete", MapColor.COLOR_GRAY);
    public static final RegistryObject<Block> WHITE_REINFORCED_CONCRETE = reinforcedBlock("white_reinforced_concrete", MapColor.SNOW);
    public static final RegistryObject<Block> BLACK_REINFORCED_CONCRETE = reinforcedBlock("black_reinforced_concrete", MapColor.COLOR_BLACK);
    public static final RegistryObject<Block> OLIVE_REINFORCED_CONCRETE = reinforcedBlock("olive_reinforced_concrete", MapColor.COLOR_GREEN);
    public static final RegistryObject<Block> HAZARD_CONCRETE = reinforcedBlock("hazard_concrete", MapColor.COLOR_YELLOW);
    public static final RegistryObject<Block> BLAST_STEEL = BLOCKS.register("blast_steel",
            () -> new Block(reinforced(MapColor.METAL).sound(SoundType.METAL)));
    public static final RegistryObject<Block> BUNKER_GLASS = BLOCKS.register("bunker_glass",
            () -> new TransparentBlock(reinforced(MapColor.COLOR_LIGHT_BLUE).noOcclusion()));

    public static final RegistryObject<DoorBlock> BLAST_DOOR = BLOCKS.register("blast_door",
            () -> new DoorBlock(BlockSetType.IRON, reinforced(MapColor.METAL).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<DoorBlock> SECURITY_DOOR = BLOCKS.register("security_door",
            () -> new DoorBlock(BlockSetType.IRON, reinforced(MapColor.COLOR_CYAN).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<TrapDoorBlock> SILO_HATCH = BLOCKS.register("silo_hatch",
            () -> new TrapDoorBlock(BlockSetType.IRON, reinforced(MapColor.METAL).sound(SoundType.METAL).noOcclusion()));

    private static RegistryObject<SystemBlock> system(String name, SystemType type) {
        return BLOCKS.register(name, () -> new SystemBlock(hardware(), type));
    }

    private static RegistryObject<Block> reinforcedBlock(String name, MapColor color) {
        return BLOCKS.register(name, () -> new Block(reinforced(color)));
    }

    private ModBlocks() {
    }
}
