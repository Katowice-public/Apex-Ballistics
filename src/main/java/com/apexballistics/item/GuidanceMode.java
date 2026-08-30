package com.apexballistics.item;

import net.minecraft.util.StringRepresentable;

public enum GuidanceMode implements StringRepresentable {
    INERTIAL("inertial"),
    COORDINATE("coordinate"),
    TERRAIN_FOLLOWING("terrain_following"),
    RADAR("radar"),
    INFRARED("infrared"),
    COMMAND("command");

    private final String id;

    GuidanceMode(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static GuidanceMode byName(String name, GuidanceMode fallback) {
        for (GuidanceMode mode : values()) {
            if (mode.id.equals(name)) {
                return mode;
            }
        }
        return fallback;
    }
}
