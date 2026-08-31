package com.apexballistics.item;

import com.apexballistics.ApexConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum MissileKind implements StringRepresentable {
    ICBM("icbm", FlightProfile.BALLISTIC, LauncherFit.SILO, 0xE8ECF0, ChatFormatting.WHITE, true),
    SLBM("slbm", FlightProfile.BALLISTIC, LauncherFit.TUBE, 0x3A6CA8, ChatFormatting.BLUE, true),
    SRBM("srbm", FlightProfile.BALLISTIC, LauncherFit.SILO, 0xC4A35A, ChatFormatting.GOLD, false),
    ALCM("alcm", FlightProfile.CRUISE, LauncherFit.PAD_OR_AIR, 0x6B8F3A, ChatFormatting.GREEN, false),
    CRUISE("cruise_missile", FlightProfile.CRUISE, LauncherFit.PAD, 0x4A4A4A, ChatFormatting.DARK_GRAY, false),
    SAM("sam", FlightProfile.HOMING_AIR, LauncherFit.SAM_OR_HAND, 0xD4B45A, ChatFormatting.YELLOW, false),
    AAM("aam", FlightProfile.HOMING_AIR, LauncherFit.HAND, 0xB8C4D4, ChatFormatting.AQUA, false),
    INTERCEPTOR("interceptor", FlightProfile.HOMING_AIR, LauncherFit.SAM_OR_HAND, 0xF4F7FF, ChatFormatting.LIGHT_PURPLE, false);

    private final String id;
    private final FlightProfile profile;
    private final LauncherFit fit;
    private final int trailColor;
    private final ChatFormatting nameColor;
    private final boolean fireOnDetonate;

    MissileKind(String id, FlightProfile profile, LauncherFit fit, int trailColor, ChatFormatting nameColor, boolean fireOnDetonate) {
        this.id = id;
        this.profile = profile;
        this.fit = fit;
        this.trailColor = trailColor;
        this.nameColor = nameColor;
        this.fireOnDetonate = fireOnDetonate;
    }

    public FlightProfile profile() {
        return profile;
    }

    public LauncherFit fit() {
        return fit;
    }

    public int trailColor() {
        return trailColor;
    }

    public ChatFormatting nameColor() {
        return nameColor;
    }

    public boolean fireOnDetonate() {
        return fireOnDetonate;
    }

    public boolean handheld() {
        return fit == LauncherFit.HAND || fit == LauncherFit.SAM_OR_HAND || fit == LauncherFit.PAD_OR_AIR;
    }

    public float blastPower() {
        return switch (this) {
            case ICBM -> (float) ApexConfig.icbmBlast;
            case SLBM -> (float) ApexConfig.slbmBlast;
            case SRBM -> (float) ApexConfig.icbmBlast * 0.55f;
            case ALCM, CRUISE -> (float) ApexConfig.cruiseBlast;
            case SAM, AAM, INTERCEPTOR -> (float) ApexConfig.samBlast;
        };
    }

    public float launchSpeed() {
        return switch (this) {
            case ICBM -> 2.4f;
            case SLBM -> 2.2f;
            case SRBM -> 2.0f;
            case ALCM -> 1.8f;
            case CRUISE -> 1.6f;
            case SAM -> 2.6f;
            case AAM -> 3.0f;
            case INTERCEPTOR -> 3.4f;
        };
    }

    public int maxLife() {
        return switch (this) {
            case ICBM -> 900;
            case SLBM -> 800;
            case SRBM -> 420;
            case ALCM, CRUISE -> 700;
            case SAM, AAM -> 220;
            case INTERCEPTOR -> 360;
        };
    }

    public Component displayName() {
        return Component.translatable("item.apexballistics." + id).withStyle(nameColor);
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static MissileKind byId(int ordinal) {
        MissileKind[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return ICBM;
        }
        return values[ordinal];
    }

    public static MissileKind byName(String name) {
        for (MissileKind kind : values()) {
            if (kind.id.equals(name)) {
                return kind;
            }
        }
        return ICBM;
    }

    public enum FlightProfile {
        BALLISTIC,
        CRUISE,
        HOMING_AIR
    }

    public enum LauncherFit {
        SILO,
        TUBE,
        PAD,
        PAD_OR_AIR,
        SAM_OR_HAND,
        HAND
    }
}
