package com.apexballistics.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.joml.Vector3f;

public class CiwsTracerEntity extends Projectile implements IEntityAdditionalSpawnData {
    public static final DustParticleOptions TRACER = new DustParticleOptions(
            new Vector3f(1.00f, 0.38f, 0.05f), 2.0f);

    private int life;

    public CiwsTracerEntity(EntityType<? extends CiwsTracerEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        life++;
        if (life > 24) {
            discard();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            return;
        }
        Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        updateRotation();
        if (level().isClientSide) {
            Vec3 pos = position();
            for (int i = 0; i < 3; i++) {
                double t = i / 3.0;
                level().addParticle(TRACER,
                        pos.x - movement.x * t,
                        pos.y - movement.y * t,
                        pos.z - movement.z * t,
                        0, 0, 0);
            }
            level().addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity instanceof AerialThreat;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof AerialThreat threat) {
            threat.intercept();
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", life);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        life = tag.getInt("Life");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return ForgeHooks.getEntitySpawnPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
    }
}
