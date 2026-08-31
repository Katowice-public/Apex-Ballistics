package com.apexballistics.entity;

import com.apexballistics.ApexConfig;
import com.apexballistics.item.BombKind;
import com.apexballistics.item.WeaponPerks;
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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

public class BombEntity extends Projectile implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(
            BombEntity.class, EntityDataSerializers.INT);

    private WeaponPerks perks = WeaponPerks.none();
    private int life;
    private boolean detonated;

    public BombEntity(EntityType<? extends BombEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public void setKind(BombKind kind) {
        this.entityData.set(DATA_KIND, kind.ordinal());
    }

    public BombKind kind() {
        return BombKind.byId(this.entityData.get(DATA_KIND));
    }

    public void setPerks(WeaponPerks perks) {
        this.perks = perks;
    }

    public ItemStack getRenderStack() {
        return switch (kind()) {
            case HE -> new ItemStack(ModItems.HE_BOMB.get());
            case CLUSTER -> new ItemStack(ModItems.CLUSTER_BOMB.get());
            case BUNKER -> new ItemStack(ModItems.BUNKER_BOMB.get());
            case INCENDIARY -> new ItemStack(ModItems.INCENDIARY_BOMB.get());
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, BombKind.HE.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (detonated) {
            return;
        }
        life++;
        if (life > 400) {
            detonate();
            return;
        }
        Vec3 movement = getDeltaMovement().add(0.0, -0.045, 0.0);
        setDeltaMovement(movement);
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            return;
        }
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        updateRotation();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);
        }
        if (!level().isClientSide && (onGround() || getY() < level().getMinBuildHeight() - 8)) {
            detonate();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        detonate();
    }

    private void detonate() {
        if (detonated || level().isClientSide) {
            discard();
            detonated = true;
            return;
        }
        detonated = true;
        BombKind kind = kind();
        float power = kind.blast() * perks.damageMultiplier();
        Level.ExplosionInteraction interaction = ApexConfig.missileGriefing
                ? Level.ExplosionInteraction.TNT
                : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), power, kind.ignites(), interaction);
        if (kind.bunker() && ApexConfig.missileGriefing) {
            level().explode(this, getX(), getY() - 2.0, getZ(), power * 0.85f, false, interaction);
            level().explode(this, getX(), getY() - 5.0, getZ(), power * 0.55f, false, interaction);
        }
        if (kind.clusterCount() > 0) {
            for (int i = 0; i < kind.clusterCount(); i++) {
                double angle = (Math.PI * 2.0 * i) / kind.clusterCount();
                double radius = 4.5 + random.nextDouble() * 3.0;
                double x = getX() + Math.cos(angle) * radius;
                double z = getZ() + Math.sin(angle) * radius;
                level().explode(this, x, getY(), z, 2.4f * perks.damageMultiplier(), false, interaction);
            }
        }
        if (kind.ignites() && ApexConfig.missileGriefing) {
            BlockPos center = blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -1, -4), center.offset(4, 2, 4))) {
                if (level().getBlockState(pos).isAir() && random.nextInt(3) == 0) {
                    level().setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                }
            }
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            server.playSound(null, blockPosition(), ModSounds.LIGHT_EXPLOSION.get(),
                    SoundSource.BLOCKS, 4.5f, kind.bunker() ? 0.7f : 1.05f);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kind", kind().getSerializedName());
        tag.putInt("Life", life);
        tag.putInt("PerkDamage", perks.damage());
        tag.putInt("PerkRange", perks.range());
        tag.putInt("PerkAccuracy", perks.accuracy());
        tag.putInt("PerkSpeed", perks.speed());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKind(BombKind.byName(tag.getString("Kind")));
        life = tag.getInt("Life");
        perks = new WeaponPerks(tag.getInt("PerkRange"), tag.getInt("PerkDamage"),
                tag.getInt("PerkAccuracy"), tag.getInt("PerkSpeed"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return ForgeHooks.getEntitySpawnPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(kind().ordinal());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        setKind(BombKind.byId(additionalData.readVarInt()));
    }
}
