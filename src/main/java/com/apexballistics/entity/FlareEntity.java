package com.apexballistics.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;

public class FlareEntity extends Entity {
    private int life;

    public FlareEntity(EntityType<? extends FlareEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        life++;
        setPos(position().add(getDeltaMovement()));
        setDeltaMovement(getDeltaMovement().scale(0.96).add(0, -0.015, 0));
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 5,
                    0.12, 0.12, 0.12, 0.01);
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 2,
                    0.08, 0.08, 0.08, 0.005);
        }
        if (life > 100) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        life = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", life);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return ForgeHooks.getEntitySpawnPacket(this);
    }
}
