package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.List;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ApexBallistics.MOD_ID);

    /**
     * Items are registered before the armor-material registry is populated.
     * Keeping the immutable value separately prevents an unsafe RegistryObject
     * lookup during the item registration event.
     */
    public static final ArmorMaterial APEX_COMPOSITE_MATERIAL = createMaterial();

    public static final RegistryObject<ArmorMaterial> APEX_COMPOSITE = ARMOR_MATERIALS.register("apex_composite",
            () -> APEX_COMPOSITE_MATERIAL);

    private static ArmorMaterial createMaterial() {
        return new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                    map.put(ArmorItem.Type.BOOTS, 4);
                    map.put(ArmorItem.Type.LEGGINGS, 8);
                    map.put(ArmorItem.Type.CHESTPLATE, 10);
                    map.put(ArmorItem.Type.HELMET, 4);
                    map.put(ArmorItem.Type.BODY, 8);
                }),
                18,
                SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(ModItems.APEX_ALLOY.get()),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ApexBallistics.MOD_ID, "apex_composite"))),
                4.0f,
                0.15f
        );
    }

    private ModArmorMaterials() {
    }
}
