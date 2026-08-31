package com.apexballistics.block;

public enum LauncherType {
    SILO,
    TUBE,
    PAD,
    SAM_BATTERY,
    MOBILE,
    VLS;

    public boolean accepts(com.apexballistics.item.MissileKind kind) {
        return switch (this) {
            case SILO -> kind == com.apexballistics.item.MissileKind.ICBM || kind == com.apexballistics.item.MissileKind.SRBM;
            case TUBE -> kind == com.apexballistics.item.MissileKind.SLBM;
            case PAD -> kind == com.apexballistics.item.MissileKind.CRUISE || kind == com.apexballistics.item.MissileKind.ALCM;
            case SAM_BATTERY -> kind == com.apexballistics.item.MissileKind.SAM
                    || kind == com.apexballistics.item.MissileKind.INTERCEPTOR;
            case MOBILE -> kind == com.apexballistics.item.MissileKind.SRBM
                    || kind == com.apexballistics.item.MissileKind.CRUISE
                    || kind == com.apexballistics.item.MissileKind.SAM;
            case VLS -> kind == com.apexballistics.item.MissileKind.CRUISE
                    || kind == com.apexballistics.item.MissileKind.SAM
                    || kind == com.apexballistics.item.MissileKind.INTERCEPTOR;
        };
    }

    public int capacity() {
        return switch (this) {
            case VLS, SAM_BATTERY -> 4;
            case MOBILE -> 2;
            default -> 1;
        };
    }

    public net.minecraft.world.phys.Vec3[] mountPoints() {
        return switch (this) {
            case SILO -> new net.minecraft.world.phys.Vec3[]{new net.minecraft.world.phys.Vec3(0.50, 2.20, 0.50)};
            case TUBE -> new net.minecraft.world.phys.Vec3[]{new net.minecraft.world.phys.Vec3(0.50, 1.35, 0.50)};
            case PAD -> new net.minecraft.world.phys.Vec3[]{new net.minecraft.world.phys.Vec3(0.50, 1.05, 0.48)};
            case SAM_BATTERY -> new net.minecraft.world.phys.Vec3[]{
                    new net.minecraft.world.phys.Vec3(0.32, 0.92, 0.50),
                    new net.minecraft.world.phys.Vec3(0.46, 0.92, 0.50),
                    new net.minecraft.world.phys.Vec3(0.60, 0.92, 0.50),
                    new net.minecraft.world.phys.Vec3(0.74, 0.92, 0.50)
            };
            case MOBILE -> new net.minecraft.world.phys.Vec3[]{
                    new net.minecraft.world.phys.Vec3(0.22, 1.05, 0.36),
                    new net.minecraft.world.phys.Vec3(0.38, 1.05, 0.52)
            };
            case VLS -> new net.minecraft.world.phys.Vec3[]{
                    new net.minecraft.world.phys.Vec3(0.26, 1.05, 0.26),
                    new net.minecraft.world.phys.Vec3(0.74, 1.05, 0.26),
                    new net.minecraft.world.phys.Vec3(0.26, 1.05, 0.74),
                    new net.minecraft.world.phys.Vec3(0.74, 1.05, 0.74)
            };
        };
    }

    public float mountScale() {
        return switch (this) {
            case SILO -> 0.52f;
            case TUBE -> 0.40f;
            case PAD -> 0.36f;
            case SAM_BATTERY -> 0.22f;
            case MOBILE -> 0.30f;
            case VLS -> 0.24f;
        };
    }
}
