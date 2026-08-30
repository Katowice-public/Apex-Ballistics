package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import com.apexballistics.item.ApexArmorItem;
import com.apexballistics.item.ApexTiers;
import com.apexballistics.item.GaussRifleItem;
import com.apexballistics.item.MissileItem;
import com.apexballistics.item.MissileKind;
import com.apexballistics.item.MissileLauncherItem;
import com.apexballistics.item.PlasmaBladeItem;
import com.apexballistics.item.RailgunItem;
import com.apexballistics.item.TargetingTabletItem;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
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

    public static final RegistryObject<MissileItem> ICBM = missile("icbm", MissileKind.ICBM, Rarity.EPIC);
    public static final RegistryObject<MissileItem> SLBM = missile("slbm", MissileKind.SLBM, Rarity.EPIC);
    public static final RegistryObject<MissileItem> SRBM = missile("srbm", MissileKind.SRBM, Rarity.RARE);
    public static final RegistryObject<MissileItem> ALCM = missile("alcm", MissileKind.ALCM, Rarity.RARE);
    public static final RegistryObject<MissileItem> CRUISE_MISSILE = missile("cruise_missile", MissileKind.CRUISE, Rarity.RARE);
    public static final RegistryObject<MissileItem> SAM = missile("sam", MissileKind.SAM, Rarity.UNCOMMON);
    public static final RegistryObject<MissileItem> AAM = missile("aam", MissileKind.AAM, Rarity.UNCOMMON);

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
            () -> new ApexArmorItem(Holder.direct(ModArmorMaterials.APEX_COMPOSITE.get()), ArmorItem.Type.HELMET,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.HELMET.getDurability(45)).rarity(Rarity.EPIC)));
    public static final RegistryObject<ArmorItem> APEX_CHESTPLATE = ITEMS.register("apex_chestplate",
            () -> new ApexArmorItem(Holder.direct(ModArmorMaterials.APEX_COMPOSITE.get()), ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.CHESTPLATE.getDurability(45)).rarity(Rarity.EPIC)));
    public static final RegistryObject<ArmorItem> APEX_LEGGINGS = ITEMS.register("apex_leggings",
            () -> new ApexArmorItem(Holder.direct(ModArmorMaterials.APEX_COMPOSITE.get()), ArmorItem.Type.LEGGINGS,
                    new Item.Properties().fireResistant().durability(ArmorItem.Type.LEGGINGS.getDurability(45)).rarity(Rarity.EPIC)));
    public static final RegistryObject<ArmorItem> APEX_BOOTS = ITEMS.register("apex_boots",
            () -> new ApexArmorItem(Holder.direct(ModArmorMaterials.APEX_COMPOSITE.get()), ArmorItem.Type.BOOTS,
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

    private static RegistryObject<MissileItem> missile(String name, MissileKind kind, Rarity rarity) {
        return ITEMS.register(name, () -> new MissileItem(kind, new Item.Properties().stacksTo(4).rarity(rarity).fireResistant()));
    }

    private ModItems() {
    }
}
