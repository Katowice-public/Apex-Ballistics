package com.apexballistics.entity;

import com.apexballistics.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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

public class GaussSlugEntity extends Projectile implements IEntityAdditionalSpawnData {
    private float damage = 12.0f;
    private boolean heavy;
    private int life;

    public GaussSlugEntity(EntityType<? extends GaussSlugEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setPower(boolean heavy) {
        this.heavy = heavy;
        if (heavy && damage < 20.0f) {
            this.damage = 28.0f;
        }
    }

    public boolean heavy() {
        return heavy;
    }

    public ItemStack getRenderStack() {
        return new ItemStack(ModItems.GAUSS_SLUG.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        life++;
        if (life > 80) {
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
            level().addParticle(heavy ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.CRIT, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();
        target.hurt(this.damageSources().mobProjectile(this, owner instanceof net.minecraft.world.entity.LivingEntity living ? living : null), damage);
        if (heavy) {
            Vec3 knock = getDeltaMovement().normalize().scale(1.4);
            target.setDeltaMovement(target.getDeltaMovement().add(knock.x, 0.25, knock.z));
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putBoolean("Heavy", heavy);
        tag.putInt("Life", life);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        heavy = tag.getBoolean("Heavy");
        life = tag.getInt("Life");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return ForgeHooks.getEntitySpawnPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeBoolean(heavy);
        buffer.writeFloat(damage);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        heavy = additionalData.readBoolean();
        damage = additionalData.readFloat();
    }
}
