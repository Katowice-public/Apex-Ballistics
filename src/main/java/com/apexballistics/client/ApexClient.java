package com.apexballistics.client;

import com.apexballistics.ApexBallistics;
import com.apexballistics.registry.ModEntities;
import com.apexballistics.registry.ModBlockEntities;
import com.apexballistics.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApexBallistics.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ApexClient {
    private ApexClient() {
    }

    public static void init() {
        MenuScreens.register(ModMenus.LAUNCHER.get(), LauncherScreen::new);
        MenuScreens.register(ModMenus.SIREN.get(), SirenScreen::new);
        MenuScreens.register(ModMenus.SHOWCASE.get(), ShowcaseScreen::new);
        ApexBallistics.LOGGER.info("Apex Ballistics client systems online.");
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MISSILE.get(), MissileRenderer::new);
        event.registerEntityRenderer(ModEntities.GAUSS_SLUG.get(), GaussSlugRenderer::new);
        event.registerEntityRenderer(ModEntities.FLARE.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RADAR.get(), RadarBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DEFENSE_SYSTEM.get(), DefenseSystemRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LAUNCHER.get(), LauncherBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SIREN.get(), SirenRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SHOWCASE.get(), ShowcaseRenderer::new);
    }
}
