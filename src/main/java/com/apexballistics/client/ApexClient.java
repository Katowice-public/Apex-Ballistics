package com.apexballistics.client;

import com.apexballistics.ApexBallistics;
import com.apexballistics.registry.ModEntities;
import com.apexballistics.registry.ModBlockEntities;
import com.apexballistics.registry.ModMenus;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterMenuScreensEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApexBallistics.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ApexClient {
    private ApexClient() {
    }

    public static void init() {
        ApexBallistics.LOGGER.info("Apex Ballistics client systems online.");
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MISSILE.get(), MissileRenderer::new);
        event.registerEntityRenderer(ModEntities.GAUSS_SLUG.get(), GaussSlugRenderer::new);
        event.registerEntityRenderer(ModEntities.FLARE.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RADAR.get(), RadarBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.LAUNCHER.get(), LauncherScreen::new);
    }
}
