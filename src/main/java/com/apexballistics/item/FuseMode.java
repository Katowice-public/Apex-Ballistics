package com.apexballistics.item;

import net.minecraft.util.StringRepresentable;

public enum FuseMode implements StringRepresentable {
    IMPACT("impact"),
    PROXIMITY("proximity"),
    AIRBURST("airburst"),
    DELAYED("delayed");

    private final String id;

    FuseMode(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static FuseMode byName(String name) {
        for (FuseMode mode : values()) {
            if (mode.id.equals(name)) {
                return mode;
            }
        }
        return IMPACT;
    }
}
