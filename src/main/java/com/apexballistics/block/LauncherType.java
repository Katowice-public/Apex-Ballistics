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
        return this == VLS ? 4 : 1;
    }
}
