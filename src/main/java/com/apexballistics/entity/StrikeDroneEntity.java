package com.apexballistics.entity;

import com.apexballistics.ApexConfig;
import com.apexballistics.item.BombKind;
import com.apexballistics.item.WeaponPerks;
import com.apexballistics.registry.ModEntities;
import com.apexballistics.registry.ModItems;
import com.apexballistics.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.jetbrains.annotations.Nullable;

public class StrikeDroneEntity extends Projectile implements AerialThreat, IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> DATA_BOMB = SynchedEntityData.defineId(
            StrikeDroneEntity.class, EntityDataSerializers.INT);

    private enum Phase {
        CLIMB, CRUISE, STRIKE, RTB
    }

    private BlockPos targetPos;
    private BlockPos home;
    private WeaponPerks dronePerks = WeaponPerks.none();
    private WeaponPerks bombPerks = WeaponPerks.none();
    private Phase phase = Phase.CLIMB;
    private int flightAge;
    private int dropAge;
    private boolean dropped;
    private boolean destroyed;
    private double launchY;
    private double cruiseAltitude;

    public StrikeDroneEntity(EntityType<? extends StrikeDroneEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = true;
    }

    public void configureFromStack(ItemStack stack) {
        dronePerks = WeaponPerks.fromStack(stack);
    }

    public void setBombKind(BombKind kind) {
        this.entityData.set(DATA_BOMB, kind.ordinal());
    }

    public BombKind bombKind() {
        return BombKind.byId(this.entityData.get(DATA_BOMB));
    }

    public void setBombPerks(WeaponPerks perks) {
        this.bombPerks = perks;
    }

    public void setTargetPos(BlockPos targetPos) {
        float scatter = 3.5f * dronePerks.accuracyFactor();
        if (!level().isClientSide && scatter > 0.2f) {
            int dx = (int) Math.round(random.nextGaussian() * scatter);
            int dz = (int) Math.round(random.nextGaussian() * scatter);
            this.targetPos = targetPos.offset(dx, 0, dz);
        } else {
            this.targetPos = targetPos;
        }
    }

    public void setHome(BlockPos home) {
        this.home = home;
    }

    public ItemStack getRenderStack() {
        return new ItemStack(ModItems.STRIKE_DRONE.get());
    }

    @Override
    public void intercept() {
        if (destroyed || level().isClientSide) {
            return;
        }
        destroyed = true;
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 8, 0.4, 0.4, 0.4, 0.02);
            server.playSound(null, blockPosition(), ModSounds.LIGHT_EXPLOSION.get(),
                    SoundSource.HOSTILE, 2.4f, 1.35f);
        }
        level().explode(this, getX(), getY(), getZ(), 1.6f, false,
                ApexConfig.missileGriefing ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.NONE);
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BOMB, BombKind.HE.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (destroyed) {
            return;
        }
        flightAge++;
        if (flightAge == 1) {
            launchY = getY();
            cruiseAltitude = Math.min(level().getMaxBuildHeight() - 6,
                    Math.max(launchY + 38.0, 96.0) + 10.0 * dronePerks.range());
        }
        int maxLife = (int) (520 * dronePerks.rangeMultiplier());
        if (flightAge > maxLife) {
            intercept();
            return;
        }
        if (targetPos == null) {
            intercept();
            return;
        }
        float speed = 0.95f * dronePerks.speedMultiplier();
        Vec3 desired = getDeltaMovement();
        switch (phase) {
            case CLIMB -> {
                desired = getDeltaMovement().multiply(1.0, 0.0, 1.0);
                if (desired.horizontalDistanceSqr() < 0.04) {
                    desired = new Vec3(getLookAngle().x, 0, getLookAngle().z);
                }
                desired = new Vec3(desired.x, 0.85, desired.z).normalize().scale(speed);
                if (getY() >= cruiseAltitude - 1.5) {
                    phase = Phase.CRUISE;
                }
            }
            case CRUISE -> {
                Vec3 to = Vec3.atCenterOf(targetPos).subtract(getX(), getY(), getZ());
                Vec3 flat = new Vec3(to.x, 0, to.z);
                if (flat.lengthSqr() < 4.0 * 4.0) {
                    phase = Phase.STRIKE;
                } else {
                    Vec3 dir = flat.normalize().scale(speed);
                    double climb = Math.clamp(cruiseAltitude - getY(), -0.35, 0.35);
                    desired = new Vec3(dir.x, climb, dir.z);
                }
            }
            case STRIKE -> {
                if (!dropped) {
                    dropBomb();
                    dropAge = flightAge;
                }
                phase = Phase.RTB;
                desired = new Vec3(getDeltaMovement().x, 0.25, getDeltaMovement().z);
            }
            case RTB -> {
                BlockPos rally = home == null ? blockPosition() : home;
                Vec3 to = Vec3.atCenterOf(rally).subtract(getX(), getY(), getZ());
                Vec3 flat = new Vec3(to.x, 0.2, to.z);
                if (flat.lengthSqr() > 0.01) {
                    desired = flat.normalize().scale(speed * 0.85);
                }
                if (dropped && flightAge - dropAge > 90) {
                    discard();
                    return;
                }
            }
        }
        Vec3 vel = getDeltaMovement().scale(0.72).add(desired.scale(0.28));
        if (vel.length() > speed * 1.35) {
            vel = vel.normalize().scale(speed * 1.35);
        }
        setDeltaMovement(vel);
        setPos(getX() + vel.x, getY() + vel.y, getZ() + vel.z);
        updateRotation();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0, 0);
        } else if (level() instanceof ServerLevel server) {
            if (flightAge % 2 == 0) {
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY(), getZ(),
                        2, 0.12, 0.04, 0.12, 0.01);
            }
            if (flightAge % 30 == 1) {
                server.playSound(null, blockPosition(), ModSounds.MISSILE_FLIGHT.get(),
                        SoundSource.HOSTILE, 1.1f, 1.45f);
            }
        }
    }

    private void dropBomb() {
        if (dropped || level().isClientSide) {
            return;
        }
        dropped = true;
        BombEntity bomb = new BombEntity(ModEntities.BOMB.get(), level());
        bomb.setOwner(getOwner());
        bomb.setKind(bombKind());
        bomb.setPerks(bombPerks.mergeDamage(dronePerks));
        bomb.setPos(getX(), getY() - 0.45, getZ());
        bomb.setDeltaMovement(getDeltaMovement().x * 0.15, -0.12, getDeltaMovement().z * 0.15);
        level().addFreshEntity(bomb);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() - 0.2, getZ(), 8, 0.2, 0.1, 0.2, 0.01);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FlightAge", flightAge);
        tag.putString("Phase", phase.name());
        tag.putBoolean("Dropped", dropped);
        tag.putInt("DropAge", dropAge);
        tag.putInt("Bomb", bombKind().ordinal());
        tag.putInt("PerkRange", dronePerks.range());
        tag.putInt("PerkDamage", dronePerks.damage());
        tag.putInt("PerkAccuracy", dronePerks.accuracy());
        tag.putInt("PerkSpeed", dronePerks.speed());
        tag.putInt("BombRange", bombPerks.range());
        tag.putInt("BombDamage", bombPerks.damage());
        tag.putInt("BombAccuracy", bombPerks.accuracy());
        tag.putInt("BombSpeed", bombPerks.speed());
        tag.putDouble("LaunchY", launchY);
        tag.putDouble("Cruise", cruiseAltitude);
        if (targetPos != null) {
            tag.putInt("Tx", targetPos.getX());
            tag.putInt("Ty", targetPos.getY());
            tag.putInt("Tz", targetPos.getZ());
        }
        if (home != null) {
            tag.putInt("Hx", home.getX());
            tag.putInt("Hy", home.getY());
            tag.putInt("Hz", home.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        flightAge = tag.getInt("FlightAge");
        try {
            phase = Phase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException ignored) {
            phase = Phase.CLIMB;
        }
        dropped = tag.getBoolean("Dropped");
        dropAge = tag.getInt("DropAge");
        setBombKind(BombKind.byId(tag.getInt("Bomb")));
        dronePerks = new WeaponPerks(tag.getInt("PerkRange"), tag.getInt("PerkDamage"),
                tag.getInt("PerkAccuracy"), tag.getInt("PerkSpeed"));
        bombPerks = new WeaponPerks(tag.getInt("BombRange"), tag.getInt("BombDamage"),
                tag.getInt("BombAccuracy"), tag.getInt("BombSpeed"));
        launchY = tag.getDouble("LaunchY");
        cruiseAltitude = tag.getDouble("Cruise");
        if (tag.contains("Tx")) {
            targetPos = new BlockPos(tag.getInt("Tx"), tag.getInt("Ty"), tag.getInt("Tz"));
        }
        if (tag.contains("Hx")) {
            home = new BlockPos(tag.getInt("Hx"), tag.getInt("Hy"), tag.getInt("Hz"));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return ForgeHooks.getEntitySpawnPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(bombKind().ordinal());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        setBombKind(BombKind.byId(additionalData.readVarInt()));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }
}
