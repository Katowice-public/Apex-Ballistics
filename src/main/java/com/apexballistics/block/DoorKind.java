package com.apexballistics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public enum DoorKind implements StringRepresentable {
    PERSONNEL("personnel_door", 1, 2, false, 0.14f, 1),
    BLAST("blast_door", 1, 2, false, 0.38f, 1),
    SECURITY("security_door", 1, 2, false, 0.18f, 1),
    AIRLOCK("airlock_door", 1, 2, false, 0.28f, 1),
    SILO_HATCH("silo_hatch", 1, 1, true, 0.28f, 1),
    SUBMARINE_HATCH("submarine_hatch", 1, 1, true, 0.24f, 1),
    MAINTENANCE_HATCH("maintenance_hatch", 1, 1, true, 0.12f, 1),
    BUNKER("bunker_door", 2, 2, false, 0.32f, 1),
    VAULT("vault_door", 2, 3, false, 0.42f, 1),
    VEHICLE("vehicle_door", 3, 3, false, 0.28f, 1),
    SILO_BLAST_LEAF("silo_blast_leaf", 2, 2, true, 0.30f, 2),
    HANGAR_SHUTTER("hangar_shutter", 3, 2, false, 0.16f, 1);

    private final String id;
    private final int width;
    private final int height;
    private final boolean hatch;
    private final float thickness;
    private final int depth;

    DoorKind(String id, int width, int height, boolean hatch, float thickness, int depth) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.hatch = hatch;
        this.thickness = thickness;
        this.depth = depth;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean hatch() {
        return hatch;
    }

    public boolean siloCover() {
        return this == SILO_HATCH || this == SILO_BLAST_LEAF;
    }

    public List<BlockPos> cells(BlockPos origin, Direction facing) {
        List<BlockPos> cells = new ArrayList<>(width * (hatch ? depth : height));
        Direction right = facing.getClockWise();
        if (hatch) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    cells.add(origin.relative(right, x).relative(facing, z));
                }
            }
        } else {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    cells.add(origin.relative(right, x).above(y));
                }
            }
        }
        return cells;
    }

    public BlockPos originFrom(BlockPos part, Direction facing, int cell) {
        List<BlockPos> zero = cells(BlockPos.ZERO, facing);
        if (cell < 0 || cell >= zero.size()) {
            return part;
        }
        return part.subtract(zero.get(cell));
    }

    public VoxelShape shape(Direction facing, boolean open, boolean outline) {
        if (open) {
            if (!outline) {
                return Shapes.empty();
            }
            if (hatch) {
                return Shapes.or(
                        Block.box(0, 0, 0, 16, 3, 3),
                        Block.box(0, 0, 13, 16, 3, 16),
                        Block.box(0, 0, 0, 3, 3, 16),
                        Block.box(13, 0, 0, 16, 3, 16)
                );
            }
            return switch (facing) {
                case SOUTH -> Block.box(0, 0, 14, 3, 16, 16);
                case WEST -> Block.box(0, 0, 0, 3, 16, 3);
                case EAST -> Block.box(13, 0, 13, 16, 16, 16);
                default -> Block.box(0, 0, 0, 3, 16, 3);
            };
        }
        int px = Math.max(2, Math.round(thickness * 16.0f));
        if (hatch) {
            return Block.box(0, 0, 0, 16, px, 16);
        }
        return switch (facing) {
            case SOUTH -> Block.box(0, 0, 16 - px, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, px, 16, 16);
            case EAST -> Block.box(16 - px, 0, 0, 16, 16, 16);
            default -> Block.box(0, 0, 0, 16, 16, px);
        };
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static DoorKind byName(String name) {
        for (DoorKind kind : values()) {
            if (kind.id.equals(name)) {
                return kind;
            }
        }
        return BLAST;
    }
}
