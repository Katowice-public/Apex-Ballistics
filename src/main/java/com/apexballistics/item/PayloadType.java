package com.apexballistics.item;

import net.minecraft.util.StringRepresentable;

public enum PayloadType implements StringRepresentable {
    STANDARD("standard"),
    EMP("emp"),
    INCENDIARY("incendiary"),
    PENETRATOR("penetrator"),
    FRAGMENTATION("fragmentation"),
    DECOY("decoy"),
    MIRV("mirv");

    private final String id;

    PayloadType(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static PayloadType byName(String name) {
        for (PayloadType type : values()) {
            if (type.id.equals(name)) {
                return type;
            }
        }
        return STANDARD;
    }
}
