package com.apexballistics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Physical cable graph. Connections never lose strength over length — only a
 * visit cap exists so a world-sized loop cannot stall the server.
 */
public final class CableNetwork {
    public static final int MAX_NODES = 4096;

    private CableNetwork() {
    }

    public static boolean isCable(BlockState state) {
        return state.getBlock() instanceof CableBlock;
    }

    public static boolean connectsTo(BlockGetter level, BlockPos neighbor) {
        BlockState state = level.getBlockState(neighbor);
        if (isCable(state)) {
            return true;
        }
        return level.getBlockEntity(neighbor) instanceof CableLinkable;
    }

    public static List<BlockEntity> findDevices(Level level, BlockPos origin, Predicate<BlockEntity> filter) {
        List<BlockEntity> found = new ArrayList<>();
        if (level == null) {
            return found;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(origin);
        seen.add(origin);
        int visited = 0;
        while (!queue.isEmpty() && visited < MAX_NODES) {
            BlockPos pos = queue.removeFirst();
            visited++;
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!seen.add(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (isCable(state)) {
                    queue.add(next);
                    continue;
                }
                BlockEntity blockEntity = level.getBlockEntity(next);
                if (blockEntity instanceof CableLinkable && filter.test(blockEntity)) {
                    found.add(blockEntity);
                }
            }
        }
        return found;
    }
}
