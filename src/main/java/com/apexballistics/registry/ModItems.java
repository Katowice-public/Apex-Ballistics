package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import com.apexballistics.item.ApexArmorItem;
import com.apexballistics.item.ApexTiers;
import com.apexballistics.item.ArmorModuleItem;
import com.apexballistics.item.FlareItem;
import com.apexballistics.item.GaussRifleItem;
import com.apexballistics.item.JammerItem;
import com.apexballistics.item.MissileItem;
import com.apexballistics.item.MissileKind;
import com.apexballistics.item.MissileLauncherItem;
import com.apexballistics.item.MissileModuleItem;
import com.apexballistics.item.PlasmaBladeItem;
import com.apexballistics.item.RailgunItem;
import com.apexballistics.item.TargetingTabletItem;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ApexBallistics.MOD_ID);

    public static final RegistryObject<Item> APEX_ALLOY = ITEMS.register("apex_alloy",
            () -> new Item(new Item.Properties().fireResistant().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> CIRCUIT_BOARD = ITEMS.register("circuit_board",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GUIDANCE_CHIP = ITEMS.register("guidance_chip",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SOLID_FUEL = ITEMS.register("solid_fuel",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WARHEAD = ITEMS.register("warhead",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> GAUSS_SLUG = ITEMS.register("gauss_slug",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> ADVANCED_PROPELLANT = ITEMS.register("advanced_propellant",
            () -> new Item(new Item.Properties().stacksTo(32).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ENERGY_CELL = ITEMS.register("energy_cell",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CAPACITOR = ITEMS.register("capacitor",
            () -> new Item(new Item.Properties().stacksTo(32).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> RADAR_DISH_COMPONENT = ITEMS.register("radar_dish_component",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CIWS_TURRET_COMPONENT = ITEMS.register("ciws_turret_component",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LASER_HEAD_COMPONENT = ITEMS.register("laser_head_component",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<MissileItem> ICBM = missile("icbm", MissileKind.ICBM, Rarity.EPIC);
    public static final RegistryObject<MissileItem> SLBM = missile("slbm", MissileKind.SLBM, Rarity.EPIC);
    public static final RegistryObject<MissileItem> SRBM = missile("srbm", MissileKind.SRBM, Rarity.RARE);
    public static final RegistryObject<MissileItem> ALCM = missile("alcm", MissileKind.ALCM, Rarity.RARE);
    public static final RegistryObject<MissileItem> CRUISE_MISSILE = missile("cruise_missile", MissileKind.CRUISE, Rarity.RARE);
    public static final RegistryObject<MissileItem> SAM = missile("sam", MissileKind.SAM, Rarity.UNCOMMON);
    public static final RegistryObject<MissileItem> AAM = missile("aam", MissileKind.AAM, Rarity.UNCOMMON);
    public static final RegistryObject<MissileItem> INTERCEPTOR = missile("interceptor", MissileKind.INTERCEPTOR, Rarity.RARE);

    public static final RegistryObject<Item> GUIDANCE_INERTIAL = module("guidance_inertial", MissileModuleItem.Category.GUIDANCE, "inertial");
    public static final RegistryObject<Item> GUIDANCE_COORDINATE = module("guidance_coordinate", MissileModuleItem.Category.GUIDANCE, "coordinate");
    public static final RegistryObject<Item> GUIDANCE_TERRAIN = module("guidance_terrain", MissileModuleItem.Category.GUIDANCE, "terrain_following");
    public static final RegistryObject<Item> GUIDANCE_RADAR = module("guidance_radar", MissileModuleItem.Category.GUIDANCE, "radar");
    public static final RegistryObject<Item> GUIDANCE_INFRARED = module("guidance_infrared", MissileModuleItem.Category.GUIDANCE, "infrared");
    public static final RegistryObject<Item> GUIDANCE_COMMAND = module("guidance_command", MissileModuleItem.Category.GUIDANCE, "command");

    public static final RegistryObject<Item> EMP_PAYLOAD = module("emp_payload", MissileModuleItem.Category.PAYLOAD, "emp");
    public static final RegistryObject<Item> INCENDIARY_PAYLOAD = module("incendiary_payload", MissileModuleItem.Category.PAYLOAD, "incendiary");
    public static final RegistryObject<Item> PENETRATOR_PAYLOAD = module("penetrator_payload", MissileModuleItem.Category.PAYLOAD, "penetrator");
    public static final RegistryObject<Item> FRAGMENTATION_PAYLOAD = module("fragmentation_payload", MissileModuleItem.Category.PAYLOAD, "fragmentation");
    public static final RegistryObject<Item> DECOY_WARHEAD = module("decoy_warhead", MissileModuleItem.Category.PAYLOAD, "decoy");
    public static final RegistryObject<Item> MIRV_WARHEAD = module("mirv_warhead", MissileModuleItem.Category.PAYLOAD, "mirv");

    public static final RegistryObject<Item> PROXIMITY_FUSE = module("proximity_fuse", MissileModuleItem.Category.FUSE, "proximity");
    public static final RegistryObject<Item> AIRBURST_FUSE = module("airburst_fuse", MissileModuleItem.Category.FUSE, "airburst");
    public static final RegistryObject<Item> DELAYED_FUSE = module("delayed_fuse", MissileModuleItem.Category.FUSE, "delayed");
    public static final RegistryObject<Item> TWO_STAGE_MOTOR = module("two_stage_motor", MissileModuleItem.Category.MOTOR, "2");
    public static final RegistryObject<Item> THREE_STAGE_MOTOR = module("three_stage_motor", MissileModuleItem.Category.MOTOR, "3");
    public static final RegistryObject<Item> PRECISION_PACKAGE = module("precision_package", MissileModuleItem.Category.ACCURACY, "1");
    public static final RegistryObject<Item> RELIABILITY_PACKAGE = module("reliability_package", MissileModuleItem.Category.RELIABILITY, "1");
    public static final RegistryObject<Item> ANTI_JAM_MODULE = module("anti_jam_module", MissileModuleItem.Category.ANTI_JAM, "true");

    public static final RegistryObject<FlareItem> FLARE = ITEMS.register("flare",
            () -> new FlareItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<JammerItem> JAMMER = ITEMS.register("jammer",
            () -> new JammerItem(new Item.Properties().stacksTo(1).durability(600).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> THERMAL_MODULE = armorModule("thermal_module", "thermal");
    public static final RegistryObject<Item> RWR_MODULE = armorModule("rwr_module", "rwr");
    public static final RegistryObject<Item> SHIELD_MODULE = armorModule("shield_module", "shield");
    public static final RegistryObject<Item> MOBILITY_MODULE = armorModule("mobility_module", "mobility");
    public static final RegistryObject<Item> CAMOUFLAGE_MODULE = armorModule("camouflage_module", "camouflage");
    public static final RegistryObject<Item> MEDICAL_MODULE = armorModule("medical_module", "medical");

    public static final RegistryObject<MissileLauncherItem> MANPADS = ITEMS.register("manpads",
            () -> new MissileLauncherItem(new Item.Properties().stacksTo(1).durability(180).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<GaussRifleItem> GAUSS_RIFLE = ITEMS.register("gauss_rifle",
            () -> new GaussRifleItem(new Item.Properties().stacksTo(1).durability(650).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<RailgunItem> RAILGUN = ITEMS.register("railgun",
            () -> new RailgunItem(new Item.Properties().stacksTo(1).durability(280).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<SwordItem> PLASMA_BLADE = ITEMS.register("plasma_blade",
            () -> new PlasmaBladeItem(ApexTiers.APEX, new Item.Properties()
                    .fireResistant()
                    .rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(ApexTiers.APEX, 4, -2.2f))));
    public static final RegistryObject<TargetingTabletItem> TARGETING_TABLET = ITEMS.register("targeting_tablet",
            () -> new TargetingTabletItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<ArmorItem> APEX_HELMET = ITEMS.register("apex_helmet",
            () -> new ApexArmorItem(Holder.direct(ModArmorMaterials.APEX_COMPOSITE_MATERIAL), ArmorItem.Type.HELMET,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.HELMET.getDurability(45)).rarity(Rarity.EPIC)));
    public static final RegistryObject<ArmorItem> APEX_CHESTPLATE = ITEMS.register("apex_chestplate",
            () -> new ApexArmorItem(Holder.direct(ModArmorMaterials.APEX_COMPOSITE_MATERIAL), ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.CHESTPLATE.getDurability(45)).rarity(Rarity.EPIC)));
    public static final RegistryObject<ArmorItem> APEX_LEGGINGS = ITEMS.register("apex_leggings",
            () -> new ApexArmorItem(Holder.direct(ModArmorMaterials.APEX_COMPOSITE_MATERIAL), ArmorItem.Type.LEGGINGS,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.LEGGINGS.getDurability(45)).rarity(Rarity.EPIC)));
    public static final RegistryObject<ArmorItem> APEX_BOOTS = ITEMS.register("apex_boots",
            () -> new ApexArmorItem(Holder.direct(ModArmorMaterials.APEX_COMPOSITE_MATERIAL), ArmorItem.Type.BOOTS,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.BOOTS.getDurability(45)).rarity(Rarity.EPIC)));

    public static final RegistryObject<BlockItem> APEX_ALLOY_BLOCK = ITEMS.register("apex_alloy_block",
            () -> new BlockItem(ModBlocks.APEX_ALLOY_BLOCK.get(), new Item.Properties().fireResistant().rarity(Rarity.RARE)));
    public static final RegistryObject<BlockItem> ICBM_SILO = ITEMS.register("icbm_silo",
            () -> new BlockItem(ModBlocks.ICBM_SILO.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<BlockItem> SLBM_TUBE = ITEMS.register("slbm_tube",
            () -> new BlockItem(ModBlocks.SLBM_TUBE.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<BlockItem> CRUISE_PAD = ITEMS.register("cruise_pad",
            () -> new BlockItem(ModBlocks.CRUISE_PAD.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<BlockItem> SAM_BATTERY = ITEMS.register("sam_battery",
            () -> new BlockItem(ModBlocks.SAM_BATTERY.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<BlockItem> RADAR = ITEMS.register("radar",
            () -> new BlockItem(ModBlocks.RADAR.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<BlockItem> MOBILE_LAUNCHER = blockItem("mobile_launcher", ModBlocks.MOBILE_LAUNCHER, Rarity.RARE);
    public static final RegistryObject<BlockItem> VLS = blockItem("vls", ModBlocks.VLS, Rarity.EPIC);
    public static final RegistryObject<BlockItem> MISSILE_ASSEMBLY = blockItem("missile_assembly", ModBlocks.MISSILE_ASSEMBLY, Rarity.RARE);
    public static final RegistryObject<BlockItem> CIWS = blockItem("ciws", ModBlocks.CIWS, Rarity.RARE);
    public static final RegistryObject<BlockItem> LASER_DEFENSE = blockItem("laser_defense", ModBlocks.LASER_DEFENSE, Rarity.EPIC);
    public static final RegistryObject<BlockItem> PASSIVE_RADAR = blockItem("passive_radar", ModBlocks.PASSIVE_RADAR, Rarity.RARE);
    public static final RegistryObject<BlockItem> COMMAND_CONSOLE = blockItem("command_console", ModBlocks.COMMAND_CONSOLE, Rarity.RARE);
    public static final RegistryObject<BlockItem> SUBMARINE_CONTROL = blockItem("submarine_control", ModBlocks.SUBMARINE_CONTROL, Rarity.RARE);
    public static final RegistryObject<BlockItem> MISSILE_RACK = blockItem("missile_rack", ModBlocks.MISSILE_RACK, Rarity.UNCOMMON);
    public static final RegistryObject<BlockItem> LOADING_CRANE = blockItem("loading_crane", ModBlocks.LOADING_CRANE, Rarity.RARE);
    public static final RegistryObject<BlockItem> PROPELLANT_REFINERY = blockItem("propellant_refinery", ModBlocks.PROPELLANT_REFINERY, Rarity.RARE);
    public static final RegistryObject<BlockItem> MAINTENANCE_STATION = blockItem("maintenance_station", ModBlocks.MAINTENANCE_STATION, Rarity.RARE);
    public static final RegistryObject<BlockItem> CAPACITOR_CHARGER = blockItem("capacitor_charger", ModBlocks.CAPACITOR_CHARGER, Rarity.RARE);

    public static final RegistryObject<BlockItem> REINFORCED_CONCRETE = blockItem("reinforced_concrete", ModBlocks.REINFORCED_CONCRETE, Rarity.COMMON);
    public static final RegistryObject<BlockItem> WHITE_REINFORCED_CONCRETE = blockItem("white_reinforced_concrete", ModBlocks.WHITE_REINFORCED_CONCRETE, Rarity.COMMON);
    public static final RegistryObject<BlockItem> BLACK_REINFORCED_CONCRETE = blockItem("black_reinforced_concrete", ModBlocks.BLACK_REINFORCED_CONCRETE, Rarity.COMMON);
    public static final RegistryObject<BlockItem> OLIVE_REINFORCED_CONCRETE = blockItem("olive_reinforced_concrete", ModBlocks.OLIVE_REINFORCED_CONCRETE, Rarity.COMMON);
    public static final RegistryObject<BlockItem> HAZARD_CONCRETE = blockItem("hazard_concrete", ModBlocks.HAZARD_CONCRETE, Rarity.COMMON);
    public static final RegistryObject<BlockItem> BLAST_STEEL = blockItem("blast_steel", ModBlocks.BLAST_STEEL, Rarity.UNCOMMON);
    public static final RegistryObject<BlockItem> BUNKER_GLASS = blockItem("bunker_glass", ModBlocks.BUNKER_GLASS, Rarity.UNCOMMON);
    public static final RegistryObject<BlockItem> BLAST_DOOR = ITEMS.register("blast_door",
            () -> new DoubleHighBlockItem(ModBlocks.BLAST_DOOR.get(),
                    new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<BlockItem> SECURITY_DOOR = ITEMS.register("security_door",
            () -> new DoubleHighBlockItem(ModBlocks.SECURITY_DOOR.get(),
                    new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<BlockItem> SILO_HATCH = blockItem("silo_hatch", ModBlocks.SILO_HATCH, Rarity.UNCOMMON);

    private static RegistryObject<MissileItem> missile(String name, MissileKind kind, Rarity rarity) {
        return ITEMS.register(name, () -> new MissileItem(kind, new Item.Properties().stacksTo(4).rarity(rarity).fireResistant()));
    }

    private static RegistryObject<Item> module(String name, MissileModuleItem.Category category, String value) {
        return ITEMS.register(name, () -> new MissileModuleItem(category, value,
                new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    }

    private static RegistryObject<Item> armorModule(String name, String moduleId) {
        return ITEMS.register(name, () -> new ArmorModuleItem(moduleId,
                new Item.Properties().stacksTo(4).rarity(Rarity.RARE)));
    }

    private static <T extends net.minecraft.world.level.block.Block> RegistryObject<BlockItem> blockItem(
            String name, RegistryObject<T> block, Rarity rarity) {
        return ITEMS.register(name, () -> new BlockItem(block.get(),
                new Item.Properties().rarity(rarity).fireResistant()));
    }

    private ModItems() {
    }
}
