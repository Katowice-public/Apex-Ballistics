package com.apexballistics.item;

import net.minecraft.util.StringRepresentable;

public enum BombKind implements StringRepresentable {
    HE("he_bomb", 6.2f, false, 0),
    CLUSTER("cluster_bomb", 3.4f, false, 6),
    BUNKER("bunker_bomb", 8.4f, true, 0),
    INCENDIARY("incendiary_bomb", 4.2f, false, 0);

    private final String id;
    private final float blast;
    private final boolean bunker;
    private final int clusterCount;

    BombKind(String id, float blast, boolean bunker, int clusterCount) {
        this.id = id;
        this.blast = blast;
        this.bunker = bunker;
        this.clusterCount = clusterCount;
    }

    public float blast() {
        return blast;
    }

    public boolean bunker() {
        return bunker;
    }

    public int clusterCount() {
        return clusterCount;
    }

    public boolean ignites() {
        return this == INCENDIARY;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static BombKind byId(int ordinal) {
        BombKind[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return HE;
        }
        return values[ordinal];
    }

    public static BombKind byName(String name) {
        for (BombKind kind : values()) {
            if (kind.id.equals(name)) {
                return kind;
            }
        }
        return HE;
    }
}
