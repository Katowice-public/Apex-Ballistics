package com.apexballistics.blockentity;

import com.apexballistics.block.DroneLauncherBlock;
import com.apexballistics.defense.EmpSensitive;
import com.apexballistics.entity.StrikeDroneEntity;
import com.apexballistics.item.BombItem;
import com.apexballistics.item.StrikeDroneItem;
import com.apexballistics.item.TargetingTabletItem;
import com.apexballistics.item.WeaponPerks;
import com.apexballistics.menu.DroneLauncherMenu;
import com.apexballistics.registry.ModBlockEntities;
import com.apexballistics.registry.ModEntities;
import com.apexballistics.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DroneLauncherBlockEntity extends BlockEntity implements Container, MenuProvider, EmpSensitive {
    public static final int SLOT_DRONE = 0;
    public static final int SLOT_BOMB = 1;
    public static final int SLOT_TABLET = 2;
    public static final int SIZE = 3;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private BlockPos target;
    private UUID operator;
    private int cooldown;
    private int empTicks;
    private int integrity = 100;

    public DroneLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_LAUNCHER.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (empTicks > 0) {
            empTicks--;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        ItemStack tablet = items.get(SLOT_TABLET);
        if (tablet.getItem() instanceof TargetingTabletItem) {
            TargetingTabletItem.readTarget(tablet).ifPresent(pos -> target = pos);
        }
        if (cooldown == 0 && empTicks == 0 && integrity >= 25 && level.hasNeighborSignal(worldPosition)) {
            tryLaunch(null);
        }
    }

    public boolean tryLaunch(@Nullable Player player) {
        if (level == null || level.isClientSide || cooldown > 0 || integrity < 25 || empTicks > 0) {
            return false;
        }
        ItemStack droneStack = items.get(SLOT_DRONE);
        ItemStack bombStack = items.get(SLOT_BOMB);
        if (!(droneStack.getItem() instanceof StrikeDroneItem) || bombStack.isEmpty()
                || !(bombStack.getItem() instanceof BombItem bombItem)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.apexballistics.drone_needs_load"), true);
            }
            return false;
        }
        if (target == null) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.apexballistics.drone_needs_target"), true);
            }
            return false;
        }
        Direction facing = getBlockState().hasProperty(DroneLauncherBlock.FACING)
                ? getBlockState().getValue(DroneLauncherBlock.FACING)
                : Direction.NORTH;
        Vec3 spawn = Vec3.atCenterOf(worldPosition)
                .add(facing.getStepX() * 3.65, 2.35, facing.getStepZ() * 3.65);
        StrikeDroneEntity drone = new StrikeDroneEntity(ModEntities.STRIKE_DRONE.get(), level);
        drone.setPos(spawn.x, spawn.y, spawn.z);
        drone.configureFromStack(droneStack);
        drone.setBombKind(bombItem.kind());
        drone.setBombPerks(WeaponPerks.fromStack(bombStack));
        drone.setTargetPos(target);
        drone.setHome(worldPosition);
        if (player != null) {
            drone.setOwner(player);
            operator = player.getUUID();
        } else if (operator != null && level.getPlayerByUUID(operator) != null) {
            drone.setOwner(level.getPlayerByUUID(operator));
        }
        float speed = 1.15f * WeaponPerks.fromStack(droneStack).speedMultiplier();
        Vec3 impulse = new Vec3(facing.getStepX(), 1.0, facing.getStepZ()).normalize().scale(speed);
        drone.setDeltaMovement(impulse);
        level.addFreshEntity(drone);
        level.playSound(null, worldPosition, ModSounds.CRUISE_LAUNCH.get(), SoundSource.BLOCKS, 2.6f, 1.15f);
        Component launched = Component.translatable(
                "message.apexballistics.drone_launched", target.getX(), target.getZ());
        if (player != null) {
            player.displayClientMessage(launched, false);
        } else if (level instanceof ServerLevel server) {
            Vec3 origin = Vec3.atCenterOf(worldPosition);
            for (ServerPlayer nearby : server.players()) {
                if (nearby.distanceToSqr(origin) < 96.0 * 96.0) {
                    nearby.displayClientMessage(launched, false);
                }
            }
        }
        droneStack.shrink(1);
        bombStack.shrink(1);
        if (droneStack.isEmpty()) {
            items.set(SLOT_DRONE, ItemStack.EMPTY);
        }
        if (bombStack.isEmpty()) {
            items.set(SLOT_BOMB, ItemStack.EMPTY);
        }
        cooldown = 80;
        setChangedAndSync();
        return true;
    }

    public boolean eject(Player player) {
        boolean any = false;
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
                items.set(i, ItemStack.EMPTY);
                any = true;
            }
        }
        if (any) {
            setChangedAndSync();
        }
        return any;
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        Containers.dropContents(level, worldPosition, this);
        items.clear();
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

    @Nullable
    public BlockPos getTarget() {
        return target;
    }

    public ItemStack getDrone() {
        return items.get(SLOT_DRONE);
    }

    public ItemStack getBomb() {
        return items.get(SLOT_BOMB);
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) {
            setChangedAndSync();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChangedAndSync();
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_DRONE -> stack.getItem() instanceof StrikeDroneItem;
            case SLOT_BOMB -> stack.getItem() instanceof BombItem;
            case SLOT_TABLET -> stack.getItem() instanceof TargetingTabletItem;
            default -> false;
        };
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.apexballistics.drone_launcher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (operator == null) {
            operator = player.getUUID();
        }
        return new DroneLauncherMenu(containerId, inventory, this);
    }

    @Override
    public void disableFor(int ticks) {
        empTicks = Math.max(empTicks, ticks);
        setChanged();
    }

    @Override
    public boolean isEmpDisabled() {
        return empTicks > 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Cooldown", cooldown);
        tag.putInt("Integrity", integrity);
        tag.putInt("EmpTicks", empTicks);
        if (operator != null) {
            tag.putUUID("Operator", operator);
        }
        if (target != null) {
            tag.putInt("Tx", target.getX());
            tag.putInt("Ty", target.getY());
            tag.putInt("Tz", target.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        cooldown = tag.getInt("Cooldown");
        integrity = tag.contains("Integrity") ? tag.getInt("Integrity") : 100;
        empTicks = tag.getInt("EmpTicks");
        operator = tag.hasUUID("Operator") ? tag.getUUID("Operator") : null;
        if (tag.contains("Tx")) {
            target = new BlockPos(tag.getInt("Tx"), tag.getInt("Ty"), tag.getInt("Tz"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
