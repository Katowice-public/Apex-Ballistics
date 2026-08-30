package com.apexballistics;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ApexBallistics.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ApexConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue MISSILE_GRIEFING = BUILDER
            .comment("If true, missile detonations destroy blocks. If false, they only deal entity damage.")
            .define("missileGriefing", true);

    private static final ForgeConfigSpec.DoubleValue ICBM_BLAST = BUILDER
            .comment("ICBM explosion power. Vanilla TNT is 4.0.")
            .defineInRange("icbmBlast", 16.0, 1.0, 48.0);

    private static final ForgeConfigSpec.DoubleValue SLBM_BLAST = BUILDER
            .comment("SLBM explosion power.")
            .defineInRange("slbmBlast", 13.0, 1.0, 48.0);

    private static final ForgeConfigSpec.DoubleValue CRUISE_BLAST = BUILDER
            .comment("ALCM / cruise missile explosion power.")
            .defineInRange("cruiseBlast", 7.0, 1.0, 32.0);

    private static final ForgeConfigSpec.DoubleValue SAM_BLAST = BUILDER
            .comment("SAM / AAM proximity blast power.")
            .defineInRange("samBlast", 4.0, 1.0, 16.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean missileGriefing = true;
    public static double icbmBlast = 16.0;
    public static double slbmBlast = 13.0;
    public static double cruiseBlast = 7.0;
    public static double samBlast = 4.0;

    private ApexConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        missileGriefing = MISSILE_GRIEFING.get();
        icbmBlast = ICBM_BLAST.get();
        slbmBlast = SLBM_BLAST.get();
        cruiseBlast = CRUISE_BLAST.get();
        samBlast = SAM_BLAST.get();
    }
}
