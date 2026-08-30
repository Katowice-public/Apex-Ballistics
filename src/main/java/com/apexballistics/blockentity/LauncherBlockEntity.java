package com.apexballistics.blockentity;

import com.apexballistics.block.LauncherBlock;
import com.apexballistics.block.LauncherType;
import com.apexballistics.entity.MissileEntity;
import com.apexballistics.item.MissileItem;
import com.apexballistics.item.MissileKind;
import com.apexballistics.registry.ModBlockEntities;
import com.apexballistics.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class LauncherBlockEntity extends BlockEntity {
    private ItemStack missile = ItemStack.EMPTY;
    private BlockPos target;
    private UUID operator;
    private int cooldown;
    private int autoScan;

    public LauncherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHER.get(), pos, state);
    }

    public ItemStack getMissile() {
        return missile;
    }

    public void setMissile(ItemStack missile) {
        this.missile = missile;
        setChangedAndSync();
    }

    public void setTarget(BlockPos target) {
        this.target = target;
        setChangedAndSync();
    }

    public void setOperator(UUID operator) {
        this.operator = operator;
        setChanged();
    }

    public LauncherType launcherType() {
        if (getBlockState().getBlock() instanceof LauncherBlock launcherBlock) {
            return launcherBlock.launcherType();
        }
        return LauncherType.PAD;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (level.hasNeighborSignal(worldPosition) && cooldown == 0 && !missile.isEmpty()) {
            tryLaunch(null);
        }
        if (launcherType() == LauncherType.SAM_BATTERY && !missile.isEmpty() && cooldown == 0) {
            autoScan++;
            if (autoScan % 10 == 0) {
                Entity airTarget = findAirTarget(48.0);
                if (airTarget != null) {
                    tryLaunchAt(null, airTarget);
                }
            }
        }
    }

    public boolean tryLaunch(@Nullable Player player) {
        return tryLaunchAt(player, null);
    }

    public boolean tryLaunchAt(@Nullable Player player, @Nullable Entity airTarget) {
        if (level == null || level.isClientSide || missile.isEmpty() || cooldown > 0) {
            return false;
        }
        if (!(missile.getItem() instanceof MissileItem missileItem)) {
            return false;
        }
        MissileKind kind = missileItem.kind();
        if (launcherType() == LauncherType.TUBE) {
            boolean wet = !level.getFluidState(worldPosition).isEmpty()
                    || !level.getFluidState(worldPosition.above()).isEmpty()
                    || worldPosition.getY() < level.getSeaLevel();
            if (!wet && player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.apexballistics.slbm_water"), true);
            }
        }

        MissileEntity entity = new MissileEntity(ModEntities.MISSILE.get(), level);
        entity.setKind(kind);
        Vec3 spawn = Vec3.atCenterOf(worldPosition).add(0, 0.8, 0);
        entity.setPos(spawn.x, spawn.y, spawn.z);
        if (player != null) {
            entity.setOwner(player);
        } else if (operator != null) {
            Player operatorPlayer = level.getPlayerByUUID(operator);
            if (operatorPlayer != null) {
                entity.setOwner(operatorPlayer);
            }
        }

        if (target != null && kind.profile() != MissileKind.FlightProfile.HOMING_AIR) {
            entity.setTargetPos(target);
            Vec3 to = Vec3.atCenterOf(target).subtract(spawn);
            if (kind.profile() == MissileKind.FlightProfile.BALLISTIC) {
                entity.setDeltaMovement(0, kind.launchSpeed(), 0);
            } else {
                Vec3 dir = new Vec3(to.x, 0.15, to.z).normalize().scale(kind.launchSpeed());
                entity.setDeltaMovement(dir);
            }
        } else if (airTarget != null) {
            entity.setHomingTarget(airTarget);
            Vec3 dir = airTarget.getEyePosition().subtract(spawn).normalize().scale(kind.launchSpeed());
            entity.setDeltaMovement(dir);
        } else {
            DirectionLaunch(entity, kind);
        }

        level.addFreshEntity(entity);
        level.playSound(null, worldPosition, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.6f, 0.6f);
        missile = ItemStack.EMPTY;
        cooldown = launcherType() == LauncherType.SAM_BATTERY ? 80 : 40;
        setChangedAndSync();
        return true;
    }

    private void DirectionLaunch(MissileEntity entity, MissileKind kind) {
        net.minecraft.core.Direction facing = getBlockState().hasProperty(LauncherBlock.FACING)
                ? getBlockState().getValue(LauncherBlock.FACING)
                : net.minecraft.core.Direction.NORTH;
        if (kind.profile() == MissileKind.FlightProfile.BALLISTIC) {
            entity.setDeltaMovement(facing.getStepX() * 0.15, kind.launchSpeed(), facing.getStepZ() * 0.15);
        } else {
            entity.setDeltaMovement(facing.getStepX() * kind.launchSpeed(), 0.25, facing.getStepZ() * kind.launchSpeed());
        }
    }

    @Nullable
    public Entity findAirTarget(double range) {
        if (level == null) {
            return null;
        }
        AABB box = new AABB(worldPosition).inflate(range);
        List<Entity> candidates = level.getEntities((Entity) null, box, this::isAirThreat);
        return candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5)))
                .orElse(null);
    }

    private boolean isAirThreat(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof MissileEntity missileEntity) {
            return missileEntity.getKind().profile() != MissileKind.FlightProfile.HOMING_AIR;
        }
        if (entity instanceof Player player) {
            return player.isFallFlying() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) > 16;
        }
        return entity instanceof Phantom
                || entity instanceof Ghast
                || entity instanceof Blaze
                || entity instanceof WitherBoss
                || entity instanceof EnderDragon
                || entity instanceof Vex
                || entity instanceof Bat
                || entity instanceof Bee
                || entity instanceof FlyingAnimal
                || (entity instanceof LivingEntity living && !living.onGround() && living.getY() > worldPosition.getY() + 6 && living.getDeltaMovement().horizontalDistanceSqr() > 0.01);
    }

    public void dropContents() {
        if (level != null && !missile.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), missile);
            missile = ItemStack.EMPTY;
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!missile.isEmpty()) {
            tag.put("Missile", missile.save(registries));
        }
        if (target != null) {
            tag.putInt("Tx", target.getX());
            tag.putInt("Ty", target.getY());
            tag.putInt("Tz", target.getZ());
        }
        if (operator != null) {
            tag.putUUID("Operator", operator);
        }
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Missile")) {
            missile = ItemStack.parseOptional(registries, tag.getCompound("Missile"));
        } else {
            missile = ItemStack.EMPTY;
        }
        if (tag.contains("Tx")) {
            target = new BlockPos(tag.getInt("Tx"), tag.getInt("Ty"), tag.getInt("Tz"));
        } else {
            target = null;
        }
        operator = tag.hasUUID("Operator") ? tag.getUUID("Operator") : null;
        cooldown = tag.getInt("Cooldown");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
