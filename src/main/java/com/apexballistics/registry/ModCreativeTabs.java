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
                        output.accept(ModItems.ADVANCED_PROPELLANT.get());
                        output.accept(ModItems.ENERGY_CELL.get());
                        output.accept(ModItems.CAPACITOR.get());

                        output.accept(ModItems.ICBM.get());
                        output.accept(ModItems.SLBM.get());
                        output.accept(ModItems.SRBM.get());
                        output.accept(ModItems.ALCM.get());
                        output.accept(ModItems.CRUISE_MISSILE.get());
                        output.accept(ModItems.SAM.get());
                        output.accept(ModItems.AAM.get());
                        output.accept(ModItems.INTERCEPTOR.get());

                        output.accept(ModItems.GUIDANCE_INERTIAL.get());
                        output.accept(ModItems.GUIDANCE_COORDINATE.get());
                        output.accept(ModItems.GUIDANCE_TERRAIN.get());
                        output.accept(ModItems.GUIDANCE_RADAR.get());
                        output.accept(ModItems.GUIDANCE_INFRARED.get());
                        output.accept(ModItems.GUIDANCE_COMMAND.get());
                        output.accept(ModItems.EMP_PAYLOAD.get());
                        output.accept(ModItems.INCENDIARY_PAYLOAD.get());
                        output.accept(ModItems.PENETRATOR_PAYLOAD.get());
                        output.accept(ModItems.FRAGMENTATION_PAYLOAD.get());
                        output.accept(ModItems.DECOY_WARHEAD.get());
                        output.accept(ModItems.MIRV_WARHEAD.get());
                        output.accept(ModItems.PROXIMITY_FUSE.get());
                        output.accept(ModItems.AIRBURST_FUSE.get());
                        output.accept(ModItems.DELAYED_FUSE.get());
                        output.accept(ModItems.TWO_STAGE_MOTOR.get());
                        output.accept(ModItems.THREE_STAGE_MOTOR.get());
                        output.accept(ModItems.PRECISION_PACKAGE.get());
                        output.accept(ModItems.RELIABILITY_PACKAGE.get());
                        output.accept(ModItems.ANTI_JAM_MODULE.get());
                        output.accept(ModItems.FLARE.get());
                        output.accept(ModItems.JAMMER.get());

                        output.accept(ModItems.ICBM_SILO.get());
                        output.accept(ModItems.SLBM_TUBE.get());
                        output.accept(ModItems.CRUISE_PAD.get());
                        output.accept(ModItems.SAM_BATTERY.get());
                        output.accept(ModItems.RADAR.get());
                        output.accept(ModItems.MOBILE_LAUNCHER.get());
                        output.accept(ModItems.VLS.get());
                        output.accept(ModItems.MISSILE_ASSEMBLY.get());
                        output.accept(ModItems.CIWS.get());
                        output.accept(ModItems.LASER_DEFENSE.get());
                        output.accept(ModItems.PASSIVE_RADAR.get());
                        output.accept(ModItems.COMMAND_CONSOLE.get());
                        output.accept(ModItems.SUBMARINE_CONTROL.get());
                        output.accept(ModItems.MISSILE_RACK.get());
                        output.accept(ModItems.LOADING_CRANE.get());
                        output.accept(ModItems.PROPELLANT_REFINERY.get());
                        output.accept(ModItems.MAINTENANCE_STATION.get());
                        output.accept(ModItems.CAPACITOR_CHARGER.get());
                        output.accept(ModItems.TARGETING_TABLET.get());
                        output.accept(ModItems.MANPADS.get());

                        output.accept(ModItems.GAUSS_RIFLE.get());
                        output.accept(ModItems.RAILGUN.get());
                        output.accept(ModItems.PLASMA_BLADE.get());

                        output.accept(ModItems.APEX_HELMET.get());
                        output.accept(ModItems.APEX_CHESTPLATE.get());
                        output.accept(ModItems.APEX_LEGGINGS.get());
                        output.accept(ModItems.APEX_BOOTS.get());
                        output.accept(ModItems.THERMAL_MODULE.get());
                        output.accept(ModItems.RWR_MODULE.get());
                        output.accept(ModItems.SHIELD_MODULE.get());
                        output.accept(ModItems.MOBILITY_MODULE.get());
                        output.accept(ModItems.CAMOUFLAGE_MODULE.get());
                        output.accept(ModItems.MEDICAL_MODULE.get());

                        output.accept(ModItems.REINFORCED_CONCRETE.get());
                        output.accept(ModItems.WHITE_REINFORCED_CONCRETE.get());
                        output.accept(ModItems.BLACK_REINFORCED_CONCRETE.get());
                        output.accept(ModItems.OLIVE_REINFORCED_CONCRETE.get());
                        output.accept(ModItems.HAZARD_CONCRETE.get());
                        output.accept(ModItems.BLAST_STEEL.get());
                        output.accept(ModItems.BUNKER_GLASS.get());
                        output.accept(ModItems.BLAST_DOOR.get());
                        output.accept(ModItems.SECURITY_DOOR.get());
                        output.accept(ModItems.SILO_HATCH.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
