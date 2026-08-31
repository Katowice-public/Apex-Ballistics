package com.apexballistics.block;

import com.apexballistics.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.registries.RegistryObject;

public enum SirenType implements StringRepresentable {
    AIR_RAID("air_raid_siren", "air_raid_siren"),
    INDUSTRIAL("industrial_siren", "industrial_siren"),
    NUCLEAR("nuclear_warning_siren", "nuclear_siren");

    private final String id;
    private final String soundId;

    SirenType(String id, String soundId) {
        this.id = id;
        this.soundId = soundId;
    }

    public RegistryObject<SoundEvent> sound() {
        return switch (this) {
            case AIR_RAID -> ModSounds.AIR_RAID_SIREN;
            case INDUSTRIAL -> ModSounds.INDUSTRIAL_SIREN;
            case NUCLEAR -> ModSounds.NUCLEAR_SIREN;
        };
    }

    public String guiTexture() {
        return "textures/gui/siren_" + id + ".png";
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public String soundId() {
        return soundId;
    }
}
