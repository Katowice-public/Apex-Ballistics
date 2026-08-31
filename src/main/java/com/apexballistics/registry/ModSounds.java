package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ApexBallistics.MOD_ID);

    public static final RegistryObject<SoundEvent> BALLISTIC_LAUNCH = register("ballistic_launch");
    public static final RegistryObject<SoundEvent> CRUISE_LAUNCH = register("cruise_launch");
    public static final RegistryObject<SoundEvent> INTERCEPTOR_LAUNCH = register("interceptor_launch");
    public static final RegistryObject<SoundEvent> MISSILE_FLIGHT = register("missile_flight");
    public static final RegistryObject<SoundEvent> HEAVY_EXPLOSION = register("heavy_explosion");
    public static final RegistryObject<SoundEvent> LIGHT_EXPLOSION = register("light_explosion");
    public static final RegistryObject<SoundEvent> RADAR_SERVO = register("radar_servo");
    public static final RegistryObject<SoundEvent> AIR_RAID_SIREN = register("air_raid_siren");
    public static final RegistryObject<SoundEvent> INDUSTRIAL_SIREN = register("industrial_siren");
    public static final RegistryObject<SoundEvent> NUCLEAR_SIREN = register("nuclear_siren");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(ApexBallistics.MOD_ID, name)));
    }

    private ModSounds() {
    }
}
