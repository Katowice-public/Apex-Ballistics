package com.apexballistics.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class BuildBlocks {
    public static Block create(String id) {
        MapColor color = colorFor(id);
        boolean glass = BuildCatalog.isGlass(id);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(glass ? 6.0f : 18.0f, glass ? 600.0f : 1800.0f)
                .sound(glass ? SoundType.GLASS : SoundType.STONE)
                .requiresCorrectToolForDrops();
        if (glass) {
            return new TransparentBlock(properties.noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false));
        }
        return new Block(properties);
    }

    private static MapColor colorFor(String id) {
        if (id.contains("light_blue")) {
            return MapColor.COLOR_LIGHT_BLUE;
        }
        if (id.contains("light_gray")) {
            return MapColor.COLOR_LIGHT_GRAY;
        }
        if (id.contains("white")) {
            return MapColor.SNOW;
        }
        if (id.contains("orange")) {
            return MapColor.COLOR_ORANGE;
        }
        if (id.contains("magenta")) {
            return MapColor.COLOR_MAGENTA;
        }
        if (id.contains("yellow")) {
            return MapColor.COLOR_YELLOW;
        }
        if (id.contains("lime")) {
            return MapColor.COLOR_LIGHT_GREEN;
        }
        if (id.contains("pink")) {
            return MapColor.COLOR_PINK;
        }
        if (id.contains("cyan")) {
            return MapColor.COLOR_CYAN;
        }
        if (id.contains("purple")) {
            return MapColor.COLOR_PURPLE;
        }
        if (id.contains("blue")) {
            return MapColor.COLOR_BLUE;
        }
        if (id.contains("brown")) {
            return MapColor.COLOR_BROWN;
        }
        if (id.contains("green")) {
            return MapColor.COLOR_GREEN;
        }
        if (id.contains("red")) {
            return MapColor.COLOR_RED;
        }
        if (id.contains("black")) {
            return MapColor.COLOR_BLACK;
        }
        if (id.contains("gray")) {
            return MapColor.COLOR_GRAY;
        }
        if (id.startsWith("steel")) {
            return MapColor.METAL;
        }
        return MapColor.STONE;
    }

    private BuildBlocks() {
    }
}
