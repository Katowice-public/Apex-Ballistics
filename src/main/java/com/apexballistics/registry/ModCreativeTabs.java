package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ApexBallistics.MOD_ID);

    public static final RegistryObject<CreativeModeTab> APEX_TAB = CREATIVE_MODE_TABS.register("apex_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.apexballistics"))
                    .icon(() -> new ItemStack(ModItems.ICBM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.APEX_ALLOY.get());
                        output.accept(ModItems.APEX_ALLOY_BLOCK.get());
                        output.accept(ModItems.CIRCUIT_BOARD.get());
                        output.accept(ModItems.GUIDANCE_CHIP.get());
                        output.accept(ModItems.SOLID_FUEL.get());
                        output.accept(ModItems.WARHEAD.get());
                        output.accept(ModItems.GAUSS_SLUG.get());

                        output.accept(ModItems.ICBM.get());
                        output.accept(ModItems.SLBM.get());
                        output.accept(ModItems.SRBM.get());
                        output.accept(ModItems.ALCM.get());
                        output.accept(ModItems.CRUISE_MISSILE.get());
                        output.accept(ModItems.SAM.get());
                        output.accept(ModItems.AAM.get());

                        output.accept(ModItems.ICBM_SILO.get());
                        output.accept(ModItems.SLBM_TUBE.get());
                        output.accept(ModItems.CRUISE_PAD.get());
                        output.accept(ModItems.SAM_BATTERY.get());
                        output.accept(ModItems.RADAR.get());
                        output.accept(ModItems.TARGETING_TABLET.get());
                        output.accept(ModItems.MANPADS.get());

                        output.accept(ModItems.GAUSS_RIFLE.get());
                        output.accept(ModItems.RAILGUN.get());
                        output.accept(ModItems.PLASMA_BLADE.get());

                        output.accept(ModItems.APEX_HELMET.get());
                        output.accept(ModItems.APEX_CHESTPLATE.get());
                        output.accept(ModItems.APEX_LEGGINGS.get());
                        output.accept(ModItems.APEX_BOOTS.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
