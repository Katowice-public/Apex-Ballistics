package com.apexballistics;

import com.apexballistics.blockentity.LauncherBlockEntity;
import com.apexballistics.client.ApexClient;
import com.apexballistics.registry.ModArmorMaterials;
import com.apexballistics.registry.ModBlockEntities;
import com.apexballistics.registry.ModBlocks;
import com.apexballistics.registry.ModCreativeTabs;
import com.apexballistics.registry.ModEntities;
import com.apexballistics.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ApexBallistics.MOD_ID)
public class ApexBallistics {
    public static final String MOD_ID = "apexballistics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ApexBallistics(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        ModArmorMaterials.ARMOR_MATERIALS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);

        modBus.addListener(this::commonSetup);
        context.registerConfig(ModConfig.Type.COMMON, ApexConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Apex Ballistics strategic weapons online.");
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof LauncherBlockEntity launcher) {
            launcher.dropContents();
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(ApexClient::init);
        }
    }
}
