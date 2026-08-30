package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import com.apexballistics.block.LauncherBlock;
import com.apexballistics.block.LauncherType;
import com.apexballistics.block.RadarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ApexBallistics.MOD_ID);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(8.0f, 1200.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    public static final RegistryObject<Block> APEX_ALLOY_BLOCK = BLOCKS.register("apex_alloy_block",
            () -> new Block(metal().mapColor(MapColor.COLOR_CYAN)));

    public static final RegistryObject<LauncherBlock> ICBM_SILO = BLOCKS.register("icbm_silo",
            () -> new LauncherBlock(metal().mapColor(MapColor.COLOR_GRAY), LauncherType.SILO));

    public static final RegistryObject<LauncherBlock> SLBM_TUBE = BLOCKS.register("slbm_tube",
            () -> new LauncherBlock(metal().mapColor(MapColor.COLOR_BLUE), LauncherType.TUBE));

    public static final RegistryObject<LauncherBlock> CRUISE_PAD = BLOCKS.register("cruise_pad",
            () -> new LauncherBlock(metal().mapColor(MapColor.COLOR_GREEN), LauncherType.PAD));

    public static final RegistryObject<LauncherBlock> SAM_BATTERY = BLOCKS.register("sam_battery",
            () -> new LauncherBlock(metal().mapColor(MapColor.COLOR_YELLOW), LauncherType.SAM_BATTERY));

    public static final RegistryObject<RadarBlock> RADAR = BLOCKS.register("radar",
            () -> new RadarBlock(metal().mapColor(MapColor.COLOR_LIGHT_BLUE)));

    private ModBlocks() {
    }
}
