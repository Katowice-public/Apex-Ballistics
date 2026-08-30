package com.apexballistics.entity;

import com.apexballistics.ApexConfig;
import com.apexballistics.item.MissileKind;
import com.apexballistics.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class MissileEntity extends Projectile implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.INT);

    private BlockPos targetPos;
    private int homingId = -1;
    private int flightAge;
    private boolean detonated;

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public void setKind(MissileKind kind) {
        this.entityData.set(DATA_KIND, kind.ordinal());
    }

    public MissileKind getKind() {
        return MissileKind.byId(this.entityData.get(DATA_KIND));
    }

    public void setTargetPos(BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    public void setHomingTarget(Entity entity) {
        this.homingId = entity.getId();
    }

    public ItemStack getRenderStack() {
        return switch (getKind()) {
            case ICBM -> new ItemStack(ModItems.ICBM.get());
            case SLBM -> new ItemStack(ModItems.SLBM.get());
            case SRBM -> new ItemStack(ModItems.SRBM.get());
            case ALCM -> new ItemStack(ModItems.ALCM.get());
            case CRUISE -> new ItemStack(ModItems.CRUISE_MISSILE.get());
            case SAM -> new ItemStack(ModItems.SAM.get());
            case AAM -> new ItemStack(ModItems.AAM.get());
        };
    }

    public void acquireAirTarget(LivingEntity shooter) {
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        Vec3 look = shooter.getLookAngle();
        Vec3 eye = shooter.getEyePosition();
        for (Entity candidate : level().getEntities(shooter, shooter.getBoundingBox().inflate(72), this::isValidAirTarget)) {
            Vec3 to = candidate.getEyePosition().subtract(eye);
            double dist = to.length();
            if (dist < 4) {
                continue;
            }
            double alignment = look.dot(to.normalize());
            if (alignment < 0.55) {
                continue;
            }
            double score = dist * (1.4 - alignment);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best != null) {
            setHomingTarget(best);
        }
    }

    private boolean isValidAirTarget(Entity entity) {
        if (!entity.isAlive() || entity == this || entity == getOwner()) {
            return false;
        }
        if (entity instanceof MissileEntity other) {
            return other.getKind().profile() != MissileKind.FlightProfile.HOMING_AIR;
        }
        if (entity instanceof LivingEntity living) {
            return !living.onGround() || living.isFallFlying() || living.isInWater();
        }
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, MissileKind.ICBM.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (detonated) {
            return;
        }
        flightAge++;
        MissileKind kind = getKind();
        if (flightAge > kind.maxLife()) {
            detonate();
            return;
        }

        steer(kind);
        Vec3 movement = getDeltaMovement();
        if (flightAge > 4) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.onHit(hit);
                return;
            }
        }

        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        updateRotation();
        spawnTrail(kind);

        if (!level().isClientSide && (getY() < level().getMinBuildHeight() - 16 || onGround())) {
            detonate();
        }
    }

    private void steer(MissileKind kind) {
        Vec3 vel = getDeltaMovement();
        switch (kind.profile()) {
            case BALLISTIC -> {
                if (targetPos != null) {
                    Vec3 to = Vec3.atCenterOf(targetPos).subtract(position());
                    double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
                    if (flightAge < 35) {
                        setDeltaMovement(vel.x * 0.92, Math.max(vel.y, kind.launchSpeed()), vel.z * 0.92);
                    } else if (getY() < level().getMaxBuildHeight() - 8 && horiz > 18 && vel.y > 0.05) {
                        Vec3 coast = new Vec3(to.x, 0, to.z).normalize().scale(kind.launchSpeed() * 0.85).add(0, 0.35, 0);
                        setDeltaMovement(vel.scale(0.82).add(coast.scale(0.22)));
                    } else {
                        Vec3 dive = to.normalize().scale(kind.launchSpeed() * 1.15);
                        setDeltaMovement(vel.scale(0.78).add(dive.scale(0.28)));
                    }
                } else {
                    setDeltaMovement(vel.x * 0.995, vel.y - 0.03, vel.z * 0.995);
                }
            }
            case CRUISE -> {
                Vec3 desired;
                if (targetPos != null) {
                    Vec3 to = Vec3.atCenterOf(targetPos).subtract(position());
                    double cruiseY = targetPos.getY() + 10;
                    desired = new Vec3(to.x, cruiseY - getY(), to.z);
                    if (to.horizontalDistance() < 14) {
                        desired = to;
                    }
                } else {
                    desired = vel.lengthSqr() < 0.01 ? getLookAngle() : vel;
                    desired = new Vec3(desired.x, Math.max(-0.05, Math.min(0.12, desired.y)), desired.z);
                }
                if (desired.lengthSqr() > 1.0E-4) {
                    Vec3 steered = vel.scale(0.86).add(desired.normalize().scale(kind.launchSpeed() * 0.22));
                    if (steered.length() > kind.launchSpeed() * 1.2) {
                        steered = steered.normalize().scale(kind.launchSpeed() * 1.2);
                    }
                    setDeltaMovement(steered);
                }
            }
            case HOMING_AIR -> {
                Entity lock = homingTarget();
                if (lock != null && lock.isAlive()) {
                    Vec3 to = lock.getEyePosition().subtract(position());
                    Vec3 steered = vel.scale(0.72).add(to.normalize().scale(kind.launchSpeed() * 0.42));
                    setDeltaMovement(steered);
                    if (to.lengthSqr() < 4.0) {
                        detonate();
                    }
                } else {
                    setDeltaMovement(vel.x * 0.99, vel.y - 0.01, vel.z * 0.99);
                }
            }
        }
    }

    @Nullable
    private Entity homingTarget() {
        if (homingId < 0) {
            return null;
        }
        return level().getEntity(homingId);
    }

    private void spawnTrail(MissileKind kind) {
        if (!level().isClientSide) {
            return;
        }
        int color = kind.trailColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        level().addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.2f), getX(), getY(), getZ(), 0, 0, 0);
        level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.01, 0);
        if (kind.profile() == MissileKind.FlightProfile.BALLISTIC) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, -0.05, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity == getOwner() || entity instanceof MissileEntity other && other.getOwner() == getOwner()) {
            return false;
        }
        if (getKind().profile() == MissileKind.FlightProfile.HOMING_AIR && entity instanceof MissileEntity) {
            return true;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        detonate();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        detonate();
    }

    private void detonate() {
        if (detonated || level().isClientSide) {
            discard();
            detonated = true;
            return;
        }
        detonated = true;
        MissileKind kind = getKind();
        float power = kind.blastPower();
        level().explode(this, getX(), getY(), getZ(), power, kind.fireOnDetonate(),
                ApexConfig.missileGriefing ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            if (kind == MissileKind.ICBM || kind == MissileKind.SLBM) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 6.0f, 0.6f);
            }
        }
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", getKind().ordinal());
        tag.putInt("FlightAge", flightAge);
        tag.putInt("HomingId", homingId);
        if (targetPos != null) {
            tag.putInt("Tx", targetPos.getX());
            tag.putInt("Ty", targetPos.getY());
            tag.putInt("Tz", targetPos.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKind(MissileKind.byId(tag.getInt("Kind")));
        flightAge = tag.getInt("FlightAge");
        homingId = tag.contains("HomingId") ? tag.getInt("HomingId") : -1;
        if (tag.contains("Tx")) {
            targetPos = new BlockPos(tag.getInt("Tx"), tag.getInt("Ty"), tag.getInt("Tz"));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return ForgeHooks.getEntitySpawnPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(getKind().ordinal());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        setKind(MissileKind.byId(additionalData.readVarInt()));
    }
}
