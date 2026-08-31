package com.apexballistics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface CableLinkable {
    boolean acceptCableFrom(BlockEntity other);

    void setCablePeer(BlockPos peer);

    BlockPos getCablePeer();
}
