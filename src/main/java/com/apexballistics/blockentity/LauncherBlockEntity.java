package com.apexballistics.blockentity;

import com.apexballistics.block.LauncherBlock;
import com.apexballistics.block.LauncherType;
import com.apexballistics.defense.EmpSensitive;
import com.apexballistics.defense.FactionRelations;
import com.apexballistics.entity.MissileEntity;
import com.apexballistics.entity.StrikeDroneEntity;
import com.apexballistics.item.MissileItem;
import com.apexballistics.item.MissileKind;
import com.apexballistics.menu.LauncherMenu;
import com.apexballistics.registry.ModBlockEntities;
import com.apexballistics.registry.ModEntities;
import com.apexballistics.registry.ModBlocks;
import com.apexballistics.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LauncherBlockEntity extends BlockEntity implements EmpSensitive, MenuProvider {
    private ItemStack missile = ItemStack.EMPTY;
    private BlockPos target;
    private List<BlockPos> waypoints = List.of();
    private UUID operator;
    private int cooldown;
    private int autoScan;
    private int integrity = 100;
    private int empTicks;
    private int programmedAirburstHeight = 10;

    public LauncherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHER.get(), pos, state);
    }

    public ItemStack getMissile() {
        return missile;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getIntegrity() {
        return integrity;
    }

    public int getEmpTicks() {
        return empTicks;
    }

    public int getProgrammedAirburstHeight() {
        return programmedAirburstHeight;
    }

    @Nullable
    public BlockPos getTarget() {
        return target;
    }

    public void setMissile(ItemStack missile) {
        this.missile = missile.copyWithCount(Math.min(launcherType().capacity(), missile.getCount()));
        setChangedAndSync();
    }

    public void setTarget(BlockPos target) {
        this.target = target;
        this.waypoints = target == null ? List.of() : List.of(target);
        setChangedAndSync();
    }

    public void setFlightPlan(List<BlockPos> points) {
        this.waypoints = List.copyOf(points);
        this.target = points.isEmpty() ? null : points.get(points.size() - 1);
        setChangedAndSync();
    }

    public void setProgrammedAirburstHeight(int height) {
        programmedAirburstHeight = Math.clamp(height, 5, 30);
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
        if (empTicks > 0) {
            empTicks--;
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            if (cooldown == 0 && launcherType() == LauncherType.SILO) {
                setSiloHatches(false);
            }
        }
        if (level.hasNeighborSignal(worldPosition) && cooldown == 0 && !missile.isEmpty()) {
            tryLaunch(null);
        }
        if (launcherType() == LauncherType.SAM_BATTERY && !missile.isEmpty() && cooldown == 0) {
            autoScan++;
            if (autoScan % 10 == 0) {
                double range = RadarBlockEntity.hasNetworkCoverage(level, worldPosition, operator)
                        ? 96.0 : 48.0;
                Entity airTarget = findAirTarget(range);
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
        if (level == null || level.isClientSide || missile.isEmpty() || cooldown > 0
                || integrity < 25 || empTicks > 0) {
            return false;
        }
        if (!(missile.getItem() instanceof MissileItem missileItem)) {
            return false;
        }
        MissileKind kind = missileItem.kind();
        boolean wetLaunch = true;
        if (launcherType() == LauncherType.TUBE) {
            wetLaunch = !level.getFluidState(worldPosition).isEmpty()
                    || !level.getFluidState(worldPosition.above()).isEmpty()
                    || worldPosition.getY() < level.getSeaLevel();
            if (!wetLaunch && player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.apexballistics.slbm_water"), true);
            }
        }

        MissileEntity entity = new MissileEntity(ModEntities.MISSILE.get(), level);
        entity.setKind(kind);
        entity.configureFromStack(missile);
        entity.setWaypoints(waypoints);
        entity.setLaunchQuality(isHardenedInstallation() ? 1.0f : 0.9f);
        entity.setAirburstHeight(programmedAirburstHeight);
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
            if (waypoints.isEmpty()) {
                entity.setTargetPos(target);
            }
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
        if (!wetLaunch) {
            entity.setDeltaMovement(entity.getDeltaMovement().scale(0.65));
        }
        entity.setDeltaMovement(entity.getDeltaMovement().scale(entity.speedMultiplier()));
        if (launcherType() == LauncherType.SILO) {
            setSiloHatches(true);
        }

        level.addFreshEntity(entity);
        net.minecraft.sounds.SoundEvent launchSound = switch (kind.profile()) {
            case BALLISTIC -> ModSounds.BALLISTIC_LAUNCH.get();
            case CRUISE -> ModSounds.CRUISE_LAUNCH.get();
            case HOMING_AIR -> ModSounds.INTERCEPTOR_LAUNCH.get();
        };
        level.playSound(null, worldPosition, launchSound, SoundSource.BLOCKS,
                kind.profile() == MissileKind.FlightProfile.BALLISTIC ? 4.0f : 2.4f,
                0.92f + randomPitch(kind));
        missile.shrink(1);
        if (missile.isEmpty()) {
            missile = ItemStack.EMPTY;
        }
        cooldown = launcherType() == LauncherType.SAM_BATTERY ? 80 : 40;
        setChangedAndSync();
        return true;
    }

    private float randomPitch(MissileKind kind) {
        int seed = worldPosition.hashCode() ^ kind.ordinal() * 31;
        return (seed & 15) / 100.0f;
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
        if (entity instanceof StrikeDroneEntity drone) {
            Entity own = operator == null || level == null ? null : level.getPlayerByUUID(operator);
            Entity droneOwner = drone.getOwner();
            return own == null || (droneOwner != own && !FactionRelations.isFriendly(own, droneOwner));
        }
        if (entity instanceof MissileEntity missileEntity) {
            Entity own = operator == null || level == null ? null : level.getPlayerByUUID(operator);
            Entity missileOwner = missileEntity.getOwner();
            return missileEntity.getKind().profile() != MissileKind.FlightProfile.HOMING_AIR
                    && (missileOwner == null || FactionRelations.isHostile(own, missileOwner));
        }
        if (entity instanceof Player player) {
            Entity own = operator == null || level == null ? null : level.getPlayerByUUID(operator);
            return player.isFallFlying()
                    && FactionRelations.isHostile(own, player)
                    && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) > 16;
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

    public boolean canLoad(ItemStack stack) {
        return stack.getItem() instanceof MissileItem missileItem
                && launcherType().accepts(missileItem.kind())
                && (missile.isEmpty()
                || ItemStack.isSameItemSameComponents(missile, stack)
                && missile.getCount() < launcherType().capacity());
    }

    public boolean loadOne(ItemStack stack, Player player) {
        if (!canLoad(stack)) {
            return false;
        }
        if (missile.isEmpty()) {
            missile = stack.copyWithCount(1);
        } else {
            missile.grow(1);
        }
        operator = player.getUUID();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        setChangedAndSync();
        return true;
    }

    public boolean ejectMissile(Player player) {
        if (missile.isEmpty()) {
            return false;
        }
        player.getInventory().placeItemBackInInventory(missile.copy());
        missile = ItemStack.EMPTY;
        setChangedAndSync();
        return true;
    }

    public void repair() {
        integrity = 100;
        setChangedAndSync();
    }

    public void damageIntegrity(int amount) {
        integrity = Math.max(0, integrity - amount);
        setChangedAndSync();
    }

    private boolean isHardenedInstallation() {
        if (level == null || launcherType() != LauncherType.SILO) {
            return true;
        }
        int reinforced = 0;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-1, -1, -1),
                worldPosition.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.REINFORCED_CONCRETE.get())
                    || state.is(ModBlocks.BLAST_STEEL.get())) {
                reinforced++;
            }
        }
        return reinforced >= 8;
    }

    private void setSiloHatches(boolean open) {
        if (level == null) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-4, 0, -4),
                worldPosition.offset(4, 8, 4))) {
            BlockState state = level.getBlockState(pos);
            BlockPos origin = null;
            if (state.getBlock() instanceof com.apexballistics.block.FacilityDoorBlock door
                    && door.kind().siloCover()) {
                origin = pos.immutable();
            } else if (state.is(ModBlocks.DOOR_PART.get())
                    && state.getValue(com.apexballistics.block.DoorPartBlock.KIND).siloCover()) {
                origin = com.apexballistics.block.DoorPartBlock.originOf(pos, state);
            }
            if (origin == null) {
                continue;
            }
            BlockState originState = level.getBlockState(origin);
            if (originState.getBlock() instanceof com.apexballistics.block.FacilityDoorBlock
                    && originState.getValue(com.apexballistics.block.FacilityDoorBlock.OPEN) != open) {
                com.apexballistics.block.FacilityDoorBlock.setOpen(level, origin, open);
            }
        }
    }

    @Override
    public void disableFor(int ticks) {
        empTicks = Math.max(empTicks, ticks);
        setChangedAndSync();
    }

    @Override
    public boolean isEmpDisabled() {
        return empTicks > 0;
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
        tag.putInt("WaypointCount", waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos point = waypoints.get(i);
            tag.putInt("W" + i + "X", point.getX());
            tag.putInt("W" + i + "Y", point.getY());
            tag.putInt("W" + i + "Z", point.getZ());
        }
        if (operator != null) {
            tag.putUUID("Operator", operator);
        }
        tag.putInt("Cooldown", cooldown);
        tag.putInt("Integrity", integrity);
        tag.putInt("EmpTicks", empTicks);
        tag.putInt("AirburstHeight", programmedAirburstHeight);
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
        int waypointCount = Math.min(6, Math.max(0, tag.getInt("WaypointCount")));
        List<BlockPos> loadedWaypoints = new ArrayList<>(waypointCount);
        for (int i = 0; i < waypointCount; i++) {
            if (tag.contains("W" + i + "X")) {
                loadedWaypoints.add(new BlockPos(tag.getInt("W" + i + "X"),
                        tag.getInt("W" + i + "Y"), tag.getInt("W" + i + "Z")));
            }
        }
        waypoints = loadedWaypoints.isEmpty() && target != null
                ? List.of(target) : List.copyOf(loadedWaypoints);
        operator = tag.hasUUID("Operator") ? tag.getUUID("Operator") : null;
        cooldown = tag.getInt("Cooldown");
        integrity = tag.contains("Integrity") ? tag.getInt("Integrity") : 100;
        empTicks = tag.getInt("EmpTicks");
        programmedAirburstHeight = tag.contains("AirburstHeight")
                ? Math.clamp(tag.getInt("AirburstHeight"), 5, 30) : 10;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.apexballistics.launcher."
                + launcherType().name().toLowerCase());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LauncherMenu(containerId, inventory, this);
    }
}
