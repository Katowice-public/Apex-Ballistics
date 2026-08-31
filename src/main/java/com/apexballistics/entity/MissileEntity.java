package com.apexballistics.entity;

import com.apexballistics.ApexConfig;
import com.apexballistics.defense.ElectronicWarfare;
import com.apexballistics.defense.EmpSensitive;
import com.apexballistics.defense.FactionRelations;
import com.apexballistics.item.FuseMode;
import com.apexballistics.item.GuidanceMode;
import com.apexballistics.item.MissileKind;
import com.apexballistics.item.MissileSpecification;
import com.apexballistics.item.PayloadType;
import com.apexballistics.item.WeaponPerks;
import com.apexballistics.item.PoweredEquipment;
import com.apexballistics.registry.ModItems;
import com.apexballistics.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.joml.Vector3f;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MissileEntity extends Projectile implements AerialThreat, IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(MissileEntity.class, EntityDataSerializers.INT);

    private BlockPos targetPos;
    private int homingId = -1;
    private UUID homingUuid;
    private int flightAge;
    private boolean detonated;
    private GuidanceMode guidance = GuidanceMode.COORDINATE;
    private PayloadType payload = PayloadType.STANDARD;
    private FuseMode fuse = FuseMode.IMPACT;
    private int stages = 1;
    private int currentStage = 1;
    private float accuracy = 4.0f;
    private float reliability = 0.98f;
    private boolean antiJam;
    private int airburstHeight = 8;
    private List<BlockPos> waypoints = List.of();
    private int waypointIndex;
    private boolean reliabilityChecked;
    private boolean malfunction;
    private boolean mirvDeployed;
    private int penetrationsLeft = 3;
    private int delayedFuse = -1;
    private float launchQuality = 1.0f;
    private double launchY;
    private float rangeMul = 1.0f;
    private float damageMul = 1.0f;
    private float speedMul = 1.0f;

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
        if (!level().isClientSide && accuracy > 0.0f) {
            int dx = (int) Math.round(random.nextGaussian() * accuracy * 0.35);
            int dz = (int) Math.round(random.nextGaussian() * accuracy * 0.35);
            this.targetPos = targetPos.offset(dx, 0, dz);
        } else {
            this.targetPos = targetPos;
        }
    }

    public void setHomingTarget(Entity entity) {
        this.homingId = entity.getId();
        this.homingUuid = entity.getUUID();
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
            case INTERCEPTOR -> new ItemStack(ModItems.INTERCEPTOR.get());
        };
    }

    public void configureFromStack(ItemStack stack) {
        MissileSpecification spec = MissileSpecification.fromStack(stack, getKind());
        guidance = spec.guidance();
        payload = spec.payload();
        fuse = spec.fuse();
        stages = spec.stages();
        accuracy = spec.accuracy();
        reliability = spec.reliability();
        antiJam = spec.antiJam();
        airburstHeight = spec.airburstHeight();
        if (!spec.waypoints().isEmpty()) {
            setWaypoints(spec.waypoints());
        }
        WeaponPerks perks = WeaponPerks.fromStack(stack);
        rangeMul = perks.rangeMultiplier();
        damageMul = perks.damageMultiplier();
        speedMul = perks.speedMultiplier();
        accuracy *= perks.accuracyFactor();
    }

    public float speedMultiplier() {
        return speedMul;
    }

    private float flightSpeed() {
        return getKind().launchSpeed() * speedMul;
    }

    public void setWaypoints(List<BlockPos> points) {
        List<BlockPos> adjusted = new ArrayList<>(points.size());
        for (BlockPos point : points) {
            if (!level().isClientSide && accuracy > 0.0f) {
                adjusted.add(point.offset(
                        (int) Math.round(random.nextGaussian() * accuracy * 0.2),
                        0,
                        (int) Math.round(random.nextGaussian() * accuracy * 0.2)));
            } else {
                adjusted.add(point);
            }
        }
        waypoints = List.copyOf(adjusted);
        waypointIndex = 0;
        if (!waypoints.isEmpty()) {
            targetPos = waypoints.get(waypoints.size() - 1);
        }
    }

    public void setLaunchQuality(float launchQuality) {
        this.launchQuality = Math.clamp(launchQuality, 0.5f, 1.0f);
    }

    public void setAirburstHeight(int airburstHeight) {
        this.airburstHeight = Math.clamp(airburstHeight, 5, 30);
    }

    public void intercept() {
        if (!level().isClientSide) {
            payload = PayloadType.FRAGMENTATION;
            detonateWithPower(Math.min(2.5f, getKind().blastPower() * 0.25f));
        }
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
        if (getKind() == MissileKind.INTERCEPTOR) {
            if (entity instanceof StrikeDroneEntity drone) {
                return drone.getOwner() == null
                        || FactionRelations.isHostile(getOwner(), drone.getOwner());
            }
            return entity instanceof MissileEntity other
                    && other.getKind() != MissileKind.INTERCEPTOR
                    && (other.getOwner() == null
                    || FactionRelations.isHostile(getOwner(), other.getOwner()));
        }
        if (entity instanceof StrikeDroneEntity drone) {
            return drone.getOwner() == null
                    || FactionRelations.isHostile(getOwner(), drone.getOwner());
        }
        if (entity instanceof MissileEntity other) {
            return other.getKind().profile() != MissileKind.FlightProfile.HOMING_AIR
                    && (other.getOwner() == null
                    || FactionRelations.isHostile(getOwner(), other.getOwner()));
        }
        if (entity instanceof LivingEntity living) {
            return FactionRelations.isHostile(getOwner(), living)
                    && (!living.onGround() || living.isFallFlying() || living.isInWater());
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
        if (flightAge == 1) {
            launchY = getY();
        }
        if (delayedFuse >= 0) {
            if (delayedFuse-- == 0) {
                detonate();
            }
            return;
        }
        if (!reliabilityChecked && !level().isClientSide) {
            reliabilityChecked = true;
            malfunction = random.nextFloat() > reliability * launchQuality;
        }
        if (malfunction) {
            setDeltaMovement(getDeltaMovement().add(
                    (random.nextDouble() - 0.5) * 0.09,
                    -0.025,
                    (random.nextDouble() - 0.5) * 0.09));
            if (flightAge > 35 && random.nextInt(80) == 0) {
                detonate();
                return;
            }
        }
        if (flightAge > kind.maxLife() * rangeMul) {
            detonate();
            return;
        }

        updateStage(kind);
        if (!level().isClientSide && flightAge % 36 == 1) {
            level().playSound(null, blockPosition(), ModSounds.MISSILE_FLIGHT.get(),
                    SoundSource.HOSTILE,
                    kind.profile() == MissileKind.FlightProfile.BALLISTIC ? 2.8f : 1.7f,
                    kind.profile() == MissileKind.FlightProfile.HOMING_AIR ? 1.22f : 0.92f);
        }
        if (payload == PayloadType.DECOY && !mirvDeployed && flightAge > 40) {
            deployDecoys();
            return;
        }
        if (payload == PayloadType.MIRV && !mirvDeployed && targetPos != null
                && flightAge > 55 && getDeltaMovement().y < 0.12) {
            deployMirv();
            return;
        }
        steer(kind);
        Vec3 movement = getDeltaMovement();
        if (shouldAirburst()) {
            detonate();
            return;
        }
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

        if (!level().isClientSide && delayedFuse < 0
                && (getY() < level().getMinBuildHeight() - 16 || onGround())) {
            detonate();
        }
    }

    @Nullable
    private BlockPos activeTarget() {
        if (!waypoints.isEmpty()) {
            waypointIndex = Math.min(waypointIndex, waypoints.size() - 1);
            BlockPos point = waypoints.get(waypointIndex);
            if (position().distanceToSqr(Vec3.atCenterOf(point)) < 12 * 12
                    && waypointIndex < waypoints.size() - 1) {
                waypointIndex++;
                point = waypoints.get(waypointIndex);
            }
            return point;
        }
        return targetPos;
    }

    private void updateStage(MissileKind kind) {
        if (kind.profile() != MissileKind.FlightProfile.BALLISTIC || stages <= 1) {
            return;
        }
        int nextSeparation = stages == 3
                ? (currentStage == 1 ? 28 : 68)
                : 48;
        if (currentStage < stages && flightAge == nextSeparation) {
            currentStage++;
            setDeltaMovement(getDeltaMovement().scale(1.08).add(0, 0.18, 0));
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY(), getZ(),
                        18, 0.35, 0.35, 0.35, 0.03);
                server.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                        SoundSource.BLOCKS, 1.5f, 0.7f + currentStage * 0.12f);
            }
        }
    }

    private boolean shouldAirburst() {
        if (fuse != FuseMode.AIRBURST || targetPos == null) {
            return false;
        }
        double horizontal = Vec3.atCenterOf(targetPos).subtract(position()).horizontalDistance();
        return horizontal < 6 && getY() <= targetPos.getY() + airburstHeight;
    }

    private void warnTarget(Entity target) {
        if (!(target instanceof Player player) || flightAge % 20 != 0) {
            return;
        }
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if ("rwr".equals(PoweredEquipment.module(helmet))
                && PoweredEquipment.consume(helmet, 1)) {
            player.displayClientMessage(Component.translatable(
                    "message.apexballistics.missile_warning",
                    getKind().displayName()).withStyle(ChatFormatting.RED), true);
        }
    }

    private void deployMirv() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        mirvDeployed = true;
        int[][] offsets = {{-12, -12}, {-12, 12}, {12, -12}, {12, 12}, {0, -16}, {0, 16}};
        for (int[] offset : offsets) {
            MissileEntity child = new MissileEntity(com.apexballistics.registry.ModEntities.MISSILE.get(), level());
            child.setKind(getKind());
            child.setOwner(getOwner());
            child.setPos(position());
            child.guidance = GuidanceMode.COORDINATE;
            child.payload = PayloadType.STANDARD;
            child.fuse = FuseMode.AIRBURST;
            child.airburstHeight = Math.max(4, airburstHeight);
            child.stages = 1;
            child.reliability = reliability;
            child.antiJam = antiJam;
            child.accuracy = Math.max(1.0f, accuracy * 0.45f);
            child.launchQuality = launchQuality;
            child.rangeMul = rangeMul;
            child.damageMul = damageMul;
            child.speedMul = speedMul;
            child.setTargetPos(targetPos.offset(offset[0], 0, offset[1]));
            Vec3 direction = Vec3.atCenterOf(child.targetPos).subtract(position()).normalize();
            child.setDeltaMovement(getDeltaMovement().scale(0.42)
                    .add(direction.scale(flightSpeed() * 0.75)));
            level().addFreshEntity(child);
        }
        server.sendParticles(ParticleTypes.FIREWORK, getX(), getY(), getZ(),
                40, 1.1, 1.1, 1.1, 0.10);
        server.playSound(null, blockPosition(), ModSounds.LIGHT_EXPLOSION.get(),
                SoundSource.HOSTILE, 2.2f, 1.35f);
        discard();
    }

    private void deployDecoys() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        mirvDeployed = true;
        for (int i = 0; i < 5; i++) {
            MissileEntity decoy = new MissileEntity(com.apexballistics.registry.ModEntities.MISSILE.get(), level());
            decoy.setKind(getKind());
            decoy.setOwner(getOwner());
            decoy.setPos(position());
            decoy.payload = PayloadType.DECOY;
            decoy.mirvDeployed = true;
            decoy.reliability = 1.0f;
            decoy.fuse = FuseMode.IMPACT;
            decoy.setDeltaMovement(getDeltaMovement().add(
                    (random.nextDouble() - 0.5) * 0.7,
                    (random.nextDouble() - 0.3) * 0.5,
                    (random.nextDouble() - 0.5) * 0.7));
            level().addFreshEntity(decoy);
        }
        server.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(),
                20, 0.6, 0.6, 0.6, 0.04);
        discard();
    }

    private void steer(MissileKind kind) {
        Vec3 vel = getDeltaMovement();
        switch (kind.profile()) {
            case BALLISTIC -> {
                BlockPos activeTarget = activeTarget();
                if (activeTarget != null) {
                    Vec3 to = Vec3.atCenterOf(activeTarget).subtract(position());
                    double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
                    if (flightAge < 35) {
                        setDeltaMovement(vel.x * 0.92, Math.max(vel.y, flightSpeed()), vel.z * 0.92);
                    } else if (getY() < level().getMaxBuildHeight() - 8 && horiz > 18 && vel.y > 0.05) {
                        Vec3 coast = new Vec3(to.x, 0, to.z).normalize().scale(flightSpeed() * 0.85).add(0, 0.35, 0);
                        setDeltaMovement(vel.scale(0.82).add(coast.scale(0.22)));
                    } else {
                        Vec3 dive = to.normalize().scale(flightSpeed() * 1.15);
                        setDeltaMovement(vel.scale(0.78).add(dive.scale(0.28)));
                    }
                } else {
                    setDeltaMovement(vel.x * 0.995, vel.y - 0.03, vel.z * 0.995);
                }
            }
            case CRUISE -> {
                Vec3 desired;
                BlockPos activeTarget = activeTarget();
                if (activeTarget != null) {
                    Vec3 to = Vec3.atCenterOf(activeTarget).subtract(position());
                    double cruiseY = activeTarget.getY() + 10;
                    if (guidance == GuidanceMode.TERRAIN_FOLLOWING) {
                        Vec3 forward = to.horizontalDistanceSqr() > 0.01
                                ? new Vec3(to.x, 0, to.z).normalize().scale(12)
                                : Vec3.ZERO;
                        int terrain = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                (int) Math.floor(getX() + forward.x),
                                (int) Math.floor(getZ() + forward.z));
                        cruiseY = Math.max(activeTarget.getY() + 6, terrain + 8);
                    }
                    desired = new Vec3(to.x, cruiseY - getY(), to.z);
                    if (to.horizontalDistance() < 14) {
                        desired = to;
                    }
                } else {
                    desired = vel.lengthSqr() < 0.01 ? getLookAngle() : vel;
                    desired = new Vec3(desired.x, Math.max(-0.05, Math.min(0.12, desired.y)), desired.z);
                }
                if (desired.lengthSqr() > 1.0E-4) {
                    Vec3 steered = vel.scale(0.86).add(desired.normalize().scale(flightSpeed() * 0.22));
                    if (steered.length() > flightSpeed() * 1.2) {
                        steered = steered.normalize().scale(flightSpeed() * 1.2);
                    }
                    setDeltaMovement(steered);
                }
            }
            case HOMING_AIR -> {
                Entity lock = homingTarget();
                if (guidance == GuidanceMode.INFRARED) {
                    FlareEntity flare = ElectronicWarfare.nearestFlare(level(), position(), 18);
                    if (flare != null && random.nextFloat() < 0.35f) {
                        setHomingTarget(flare);
                        lock = flare;
                    }
                }
                if (!antiJam && ElectronicWarfare.isJammed(level(), position())
                        && random.nextInt(6) == 0) {
                    homingId = -1;
                    homingUuid = null;
                    lock = null;
                }
                if (lock != null && lock.isAlive()) {
                    Vec3 to = lock.getEyePosition().subtract(position());
                    Vec3 steered = vel.scale(0.72).add(to.normalize().scale(flightSpeed() * 0.42));
                    setDeltaMovement(steered);
                    warnTarget(lock);
                    double fuseRange = fuse == FuseMode.PROXIMITY ? 12.25 : 4.0;
                    if (to.lengthSqr() < fuseRange) {
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
        Entity target = homingId < 0 ? null : level().getEntity(homingId);
        if ((target == null || !target.isAlive()) && homingUuid != null
                && level() instanceof ServerLevel server) {
            target = server.getEntity(homingUuid);
            if (target != null) {
                homingId = target.getId();
            }
        }
        return target;
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
            if (getDeltaMovement().y < -0.35 && getY() > launchY + 28) {
                level().addParticle(ParticleTypes.LAVA, getX(), getY(), getZ(), 0, 0, 0);
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 0, 0, 0);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity == getOwner() || entity instanceof MissileEntity other && other.getOwner() == getOwner()) {
            return false;
        }
        if (FactionRelations.isFriendly(getOwner(), entity)) {
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
        if (fuse == FuseMode.DELAYED) {
            delayedFuse = 10;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        detonate();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (payload == PayloadType.PENETRATOR && penetrationsLeft-- > 0) {
            setPos(position().add(getDeltaMovement().normalize().scale(0.8)));
            return;
        }
        if (fuse == FuseMode.DELAYED) {
            delayedFuse = 20;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        detonate();
    }

    private void detonate() {
        if (detonated || level().isClientSide) {
            discard();
            detonated = true;
            return;
        }
        MissileKind kind = getKind();
        float power = kind.blastPower();
        power *= switch (payload) {
            case EMP -> 0.3f;
            case INCENDIARY -> 0.65f;
            case PENETRATOR -> 1.2f;
            case FRAGMENTATION -> 0.55f;
            case DECOY -> 0.0f;
            case MIRV, STANDARD -> 1.0f;
        };
        detonateWithPower(power * damageMul);
    }

    private void detonateWithPower(float power) {
        if (detonated || level().isClientSide) {
            return;
        }
        detonated = true;
        MissileKind kind = getKind();
        applyPayloadEffects();
        if (power <= 0.0f) {
            discard();
            return;
        }
        level().explode(this, getX(), getY(), getZ(), power, kind.fireOnDetonate(),
                ApexConfig.missileGriefing ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            serverLevel.playSound(null, blockPosition(),
                    kind == MissileKind.ICBM || kind == MissileKind.SLBM
                            ? ModSounds.HEAVY_EXPLOSION.get()
                            : ModSounds.LIGHT_EXPLOSION.get(),
                    SoundSource.BLOCKS,
                    kind == MissileKind.ICBM || kind == MissileKind.SLBM ? 8.0f : 4.0f,
                    kind == MissileKind.ICBM || kind == MissileKind.SLBM ? 0.72f : 1.0f);
        }
        discard();
    }

    private void applyPayloadEffects() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        switch (payload) {
            case EMP -> {
                int radius = 12;
                for (BlockPos pos : BlockPos.betweenClosed(blockPosition().offset(-radius, -radius, -radius),
                        blockPosition().offset(radius, radius, radius))) {
                    BlockEntity blockEntity = level().getBlockEntity(pos);
                    if (blockEntity instanceof EmpSensitive sensitive) {
                        sensitive.disableFor(20 * 20);
                    }
                }
                for (Player player : level().getEntitiesOfClass(Player.class,
                        getBoundingBox().inflate(radius))) {
                    for (ItemStack armor : player.getArmorSlots()) {
                        PoweredEquipment.setEnergy(armor, 0);
                    }
                }
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(),
                        160, radius * 0.45, radius * 0.45, radius * 0.45, 0.12);
            }
            case INCENDIARY -> {
                for (int i = 0; i < 48; i++) {
                    BlockPos pos = blockPosition().offset(random.nextInt(17) - 8,
                            random.nextInt(5) - 1, random.nextInt(17) - 8);
                    if (level().isEmptyBlock(pos) && level().getBlockState(pos.below()).isSolid()) {
                        level().setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    }
                }
            }
            case FRAGMENTATION -> {
                for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(blockPosition()).inflate(10),
                        target -> target != getOwner() && !FactionRelations.isFriendly(getOwner(), target))) {
                    double distance = Math.max(1.0, target.distanceTo(this));
                    target.hurt(damageSources().mobProjectile(this,
                            getOwner() instanceof LivingEntity living ? living : null),
                            (float) Math.max(2.0, 18.0 - distance));
                }
                server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(),
                        120, 4, 4, 4, 0.35);
            }
            default -> {
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("KindName", getKind().getSerializedName());
        tag.putInt("FlightAge", flightAge);
        tag.putInt("HomingId", homingId);
        if (homingUuid != null) {
            tag.putUUID("HomingUuid", homingUuid);
        }
        tag.putString("Guidance", guidance.getSerializedName());
        tag.putString("Payload", payload.getSerializedName());
        tag.putString("Fuse", fuse.getSerializedName());
        tag.putInt("Stages", stages);
        tag.putInt("CurrentStage", currentStage);
        tag.putFloat("Accuracy", accuracy);
        tag.putFloat("Reliability", reliability);
        tag.putBoolean("AntiJam", antiJam);
        tag.putInt("AirburstHeight", airburstHeight);
        tag.putInt("WaypointIndex", waypointIndex);
        tag.putBoolean("MirvDeployed", mirvDeployed);
        tag.putInt("Penetrations", penetrationsLeft);
        tag.putInt("DelayedFuse", delayedFuse);
        tag.putFloat("LaunchQuality", launchQuality);
        tag.putDouble("LaunchY", launchY);
        tag.putFloat("RangeMul", rangeMul);
        tag.putFloat("DamageMul", damageMul);
        tag.putFloat("SpeedMul", speedMul);
        if (targetPos != null) {
            tag.putInt("Tx", targetPos.getX());
            tag.putInt("Ty", targetPos.getY());
            tag.putInt("Tz", targetPos.getZ());
        }
        tag.putInt("WaypointCount", waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos point = waypoints.get(i);
            tag.putInt("W" + i + "X", point.getX());
            tag.putInt("W" + i + "Y", point.getY());
            tag.putInt("W" + i + "Z", point.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKind(tag.contains("KindName")
                ? MissileKind.byName(tag.getString("KindName"))
                : MissileKind.byId(tag.getInt("Kind")));
        flightAge = tag.getInt("FlightAge");
        homingId = tag.contains("HomingId") ? tag.getInt("HomingId") : -1;
        homingUuid = tag.hasUUID("HomingUuid") ? tag.getUUID("HomingUuid") : null;
        MissileSpecification defaults = MissileSpecification.defaults(getKind());
        guidance = GuidanceMode.byName(tag.getString("Guidance"), defaults.guidance());
        payload = PayloadType.byName(tag.getString("Payload"));
        fuse = FuseMode.byName(tag.getString("Fuse"));
        stages = tag.contains("Stages") ? tag.getInt("Stages") : defaults.stages();
        currentStage = tag.contains("CurrentStage") ? tag.getInt("CurrentStage") : 1;
        accuracy = tag.contains("Accuracy") ? tag.getFloat("Accuracy") : defaults.accuracy();
        reliability = tag.contains("Reliability") ? tag.getFloat("Reliability") : defaults.reliability();
        antiJam = tag.getBoolean("AntiJam");
        airburstHeight = tag.contains("AirburstHeight") ? tag.getInt("AirburstHeight") : defaults.airburstHeight();
        waypointIndex = tag.getInt("WaypointIndex");
        mirvDeployed = tag.getBoolean("MirvDeployed");
        penetrationsLeft = tag.contains("Penetrations") ? tag.getInt("Penetrations") : 3;
        delayedFuse = tag.contains("DelayedFuse") ? tag.getInt("DelayedFuse") : -1;
        launchQuality = tag.contains("LaunchQuality") ? tag.getFloat("LaunchQuality") : 1.0f;
        launchY = tag.contains("LaunchY") ? tag.getDouble("LaunchY") : getY();
        rangeMul = tag.contains("RangeMul") ? tag.getFloat("RangeMul") : 1.0f;
        damageMul = tag.contains("DamageMul") ? tag.getFloat("DamageMul") : 1.0f;
        speedMul = tag.contains("SpeedMul") ? tag.getFloat("SpeedMul") : 1.0f;
        if (tag.contains("Tx")) {
            targetPos = new BlockPos(tag.getInt("Tx"), tag.getInt("Ty"), tag.getInt("Tz"));
        }
        int waypointCount = Math.min(6, Math.max(0, tag.getInt("WaypointCount")));
        List<BlockPos> loadedWaypoints = new ArrayList<>(waypointCount);
        for (int i = 0; i < waypointCount; i++) {
            if (tag.contains("W" + i + "X")) {
                loadedWaypoints.add(new BlockPos(tag.getInt("W" + i + "X"),
                        tag.getInt("W" + i + "Y"), tag.getInt("W" + i + "Z")));
            }
        }
        waypoints = List.copyOf(loadedWaypoints);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return ForgeHooks.getEntitySpawnPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(getKind().ordinal());
        buffer.writeUtf(guidance.getSerializedName());
        buffer.writeUtf(payload.getSerializedName());
        buffer.writeUtf(fuse.getSerializedName());
        buffer.writeVarInt(stages);
        buffer.writeFloat(accuracy);
        buffer.writeFloat(reliability);
        buffer.writeBoolean(antiJam);
        buffer.writeVarInt(airburstHeight);
        buffer.writeBoolean(targetPos != null);
        if (targetPos != null) {
            buffer.writeBlockPos(targetPos);
        }
        buffer.writeVarInt(waypoints.size());
        for (BlockPos point : waypoints) {
            buffer.writeBlockPos(point);
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        setKind(MissileKind.byId(additionalData.readVarInt()));
        MissileSpecification defaults = MissileSpecification.defaults(getKind());
        guidance = GuidanceMode.byName(additionalData.readUtf(), defaults.guidance());
        payload = PayloadType.byName(additionalData.readUtf());
        fuse = FuseMode.byName(additionalData.readUtf());
        stages = additionalData.readVarInt();
        accuracy = additionalData.readFloat();
        reliability = additionalData.readFloat();
        antiJam = additionalData.readBoolean();
        airburstHeight = additionalData.readVarInt();
        targetPos = additionalData.readBoolean() ? additionalData.readBlockPos() : null;
        int count = Math.min(6, additionalData.readVarInt());
        List<BlockPos> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(additionalData.readBlockPos());
        }
        waypoints = List.copyOf(points);
    }
}
