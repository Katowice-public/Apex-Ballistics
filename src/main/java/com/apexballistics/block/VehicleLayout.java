package com.apexballistics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * 4-long × 2-wide × 2-high vehicle footprint. Cell 0 is the origin (rear-left).
 */
public final class VehicleLayout {
    public static final int LENGTH = 4;
    public static final int WIDTH = 2;
    public static final int HEIGHT = 2;
    public static final int CELL_COUNT = LENGTH * WIDTH * HEIGHT;

    private VehicleLayout() {
    }

    public static List<BlockPos> cells(BlockPos origin, Direction facing) {
        Direction right = facing.getClockWise();
        List<BlockPos> cells = new ArrayList<>(CELL_COUNT);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < LENGTH; z++) {
                    cells.add(origin.relative(right, x).relative(facing, z).above(y));
                }
            }
        }
        return cells;
    }

    public static BlockPos originFrom(BlockPos part, Direction facing, int cell) {
        List<BlockPos> zero = cells(BlockPos.ZERO, facing);
        if (cell < 0 || cell >= zero.size()) {
            return part;
        }
        BlockPos offset = zero.get(cell);
        return part.offset(-offset.getX(), -offset.getY(), -offset.getZ());
    }
}
