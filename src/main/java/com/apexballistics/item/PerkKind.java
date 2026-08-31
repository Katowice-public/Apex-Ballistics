package com.apexballistics.item;

import net.minecraft.util.StringRepresentable;

public enum PerkKind implements StringRepresentable {
    RANGE("range"),
    DAMAGE("damage"),
    ACCURACY("accuracy"),
    SPEED("speed");

    private final String id;

    PerkKind(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public String tagKey() {
        return switch (this) {
            case RANGE -> "PerkRange";
            case DAMAGE -> "PerkDamage";
            case ACCURACY -> "PerkAccuracy";
            case SPEED -> "PerkSpeed";
        };
    }
}
