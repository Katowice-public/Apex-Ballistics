package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import com.apexballistics.block.CableBlock;
import com.apexballistics.block.DoorKind;
import com.apexballistics.block.DoorPartBlock;
import com.apexballistics.block.FacilityDoorBlock;
import com.apexballistics.block.LauncherBlock;
import com.apexballistics.block.LauncherType;
import com.apexballistics.block.MissileAssemblyBlock;
import com.apexballistics.block.MissileShowcaseBlock;
import com.apexballistics.block.RadarBlock;
import com.apexballistics.block.SirenBlock;
import com.apexballistics.block.SirenType;
import com.apexballistics.block.SystemBlock;
import com.apexballistics.block.SystemType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ApexBallistics.MOD_ID);
    public static final Map<String, RegistryObject<Block>> BUILD = new LinkedHashMap<>();

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

    private static BlockBehaviour.Properties doorMetal() {
        return metal().noOcclusion().dynamicShape().pushReaction(PushReaction.BLOCK)
                .isViewBlocking((state, level, pos) -> false);
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

    public static final RegistryObject<SirenBlock> AIR_RAID_SIREN = BLOCKS.register("air_raid_siren",
            () -> new SirenBlock(hardware().mapColor(MapColor.COLOR_ORANGE), SirenType.AIR_RAID));
    public static final RegistryObject<SirenBlock> INDUSTRIAL_SIREN = BLOCKS.register("industrial_siren",
            () -> new SirenBlock(hardware().mapColor(MapColor.COLOR_YELLOW), SirenType.INDUSTRIAL));
    public static final RegistryObject<SirenBlock> NUCLEAR_WARNING_SIREN = BLOCKS.register("nuclear_warning_siren",
            () -> new SirenBlock(hardware().mapColor(MapColor.GOLD), SirenType.NUCLEAR));

    public static final RegistryObject<CableBlock> CABLE = BLOCKS.register("cable",
            () -> new CableBlock(hardware().mapColor(MapColor.COLOR_GREEN).strength(2.0f, 80.0f)));
    public static final RegistryObject<MissileShowcaseBlock> MISSILE_SHOWCASE = BLOCKS.register("missile_showcase",
            () -> new MissileShowcaseBlock(hardware().mapColor(MapColor.COLOR_BLUE)));

    public static final RegistryObject<Block> REINFORCED_CONCRETE = reinforcedBlock("reinforced_concrete", MapColor.COLOR_GRAY);
    public static final RegistryObject<Block> WHITE_REINFORCED_CONCRETE = reinforcedBlock("white_reinforced_concrete", MapColor.SNOW);
    public static final RegistryObject<Block> BLACK_REINFORCED_CONCRETE = reinforcedBlock("black_reinforced_concrete", MapColor.COLOR_BLACK);
    public static final RegistryObject<Block> OLIVE_REINFORCED_CONCRETE = reinforcedBlock("olive_reinforced_concrete", MapColor.COLOR_GREEN);
    public static final RegistryObject<Block> HAZARD_CONCRETE = reinforcedBlock("hazard_concrete", MapColor.COLOR_YELLOW);
    public static final RegistryObject<Block> BLAST_STEEL = BLOCKS.register("blast_steel",
            () -> new Block(reinforced(MapColor.METAL).sound(SoundType.METAL)));
    public static final RegistryObject<Block> BUNKER_GLASS = BLOCKS.register("bunker_glass",
            () -> new TransparentBlock(reinforced(MapColor.COLOR_LIGHT_BLUE).noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)));

    public static final RegistryObject<FacilityDoorBlock> PERSONNEL_DOOR = door("personnel_door", DoorKind.PERSONNEL);
    public static final RegistryObject<FacilityDoorBlock> BLAST_DOOR = door("blast_door", DoorKind.BLAST);
    public static final RegistryObject<FacilityDoorBlock> SECURITY_DOOR = door("security_door", DoorKind.SECURITY);
    public static final RegistryObject<FacilityDoorBlock> AIRLOCK_DOOR = door("airlock_door", DoorKind.AIRLOCK);
    public static final RegistryObject<FacilityDoorBlock> SILO_HATCH = door("silo_hatch", DoorKind.SILO_HATCH);
    public static final RegistryObject<FacilityDoorBlock> SUBMARINE_HATCH = door("submarine_hatch", DoorKind.SUBMARINE_HATCH);
    public static final RegistryObject<FacilityDoorBlock> MAINTENANCE_HATCH = door("maintenance_hatch", DoorKind.MAINTENANCE_HATCH);
    public static final RegistryObject<FacilityDoorBlock> BUNKER_DOOR = door("bunker_door", DoorKind.BUNKER);
    public static final RegistryObject<FacilityDoorBlock> VAULT_DOOR = door("vault_door", DoorKind.VAULT);
    public static final RegistryObject<FacilityDoorBlock> VEHICLE_DOOR = door("vehicle_door", DoorKind.VEHICLE);
    public static final RegistryObject<FacilityDoorBlock> SILO_BLAST_LEAF = door("silo_blast_leaf", DoorKind.SILO_BLAST_LEAF);
    public static final RegistryObject<FacilityDoorBlock> HANGAR_SHUTTER = door("hangar_shutter", DoorKind.HANGAR_SHUTTER);
    public static final RegistryObject<DoorPartBlock> DOOR_PART = BLOCKS.register("door_part",
            () -> new DoorPartBlock(doorMetal().mapColor(MapColor.METAL)));

    static {
        for (String id : BuildCatalog.IDS) {
            BUILD.put(id, BLOCKS.register(id, () -> BuildBlocks.create(id)));
        }
    }

    private static RegistryObject<SystemBlock> system(String name, SystemType type) {
        return BLOCKS.register(name, () -> new SystemBlock(hardware(), type));
    }

    private static RegistryObject<Block> reinforcedBlock(String name, MapColor color) {
        return BLOCKS.register(name, () -> new Block(reinforced(color)));
    }

    private static RegistryObject<FacilityDoorBlock> door(String name, DoorKind kind) {
        return BLOCKS.register(name, () -> new FacilityDoorBlock(doorMetal(), kind));
    }

    public static Item itemForDoor(DoorKind kind) {
        return switch (kind) {
            case PERSONNEL -> PERSONNEL_DOOR.get().asItem();
            case BLAST -> BLAST_DOOR.get().asItem();
            case SECURITY -> SECURITY_DOOR.get().asItem();
            case AIRLOCK -> AIRLOCK_DOOR.get().asItem();
            case SILO_HATCH -> SILO_HATCH.get().asItem();
            case SUBMARINE_HATCH -> SUBMARINE_HATCH.get().asItem();
            case MAINTENANCE_HATCH -> MAINTENANCE_HATCH.get().asItem();
            case BUNKER -> BUNKER_DOOR.get().asItem();
            case VAULT -> VAULT_DOOR.get().asItem();
            case VEHICLE -> VEHICLE_DOOR.get().asItem();
            case SILO_BLAST_LEAF -> SILO_BLAST_LEAF.get().asItem();
            case HANGAR_SHUTTER -> HANGAR_SHUTTER.get().asItem();
        };
    }

    private ModBlocks() {
    }
}
