package com.apexballistics.menu;

import com.apexballistics.block.LauncherType;
import com.apexballistics.blockentity.LauncherBlockEntity;
import com.apexballistics.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class LauncherMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 10;

    private final LauncherBlockEntity launcher;
    private final BlockPos blockPos;
    private final ContainerData data;

    public LauncherMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(extraData.readBlockPos()) instanceof LauncherBlockEntity launcher
                        ? launcher : null,
                new SimpleContainerData(DATA_COUNT));
    }

    public LauncherMenu(int containerId, Inventory inventory, LauncherBlockEntity launcher) {
        this(containerId, inventory, launcher, createServerData(launcher));
    }

    private LauncherMenu(int containerId, Inventory inventory, LauncherBlockEntity launcher,
                         ContainerData data) {
        super(ModMenus.LAUNCHER.get(), containerId);
        this.launcher = launcher;
        this.blockPos = launcher == null ? inventory.player.blockPosition() : launcher.getBlockPos();
        this.data = data;
        addDataSlots(data);
    }

    private static ContainerData createServerData(LauncherBlockEntity launcher) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> launcher.launcherType().ordinal();
                    case 1 -> launcher.getMissile().getCount();
                    case 2 -> launcher.launcherType().capacity();
                    case 3 -> launcher.getCooldown();
                    case 4 -> launcher.getIntegrity();
                    case 5 -> launcher.getEmpTicks();
                    case 6 -> launcher.getProgrammedAirburstHeight();
                    case 7 -> launcher.getTarget() == null ? 0 : launcher.getTarget().getX();
                    case 8 -> launcher.getTarget() == null ? 0 : launcher.getTarget().getZ();
                    case 9 -> launcher.getTarget() == null ? 0 : 1;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    public LauncherType launcherType() {
        if (launcher != null) {
            return launcher.launcherType();
        }
        int ordinal = data.get(0);
        LauncherType[] values = LauncherType.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : LauncherType.SILO;
    }

    public int loaded() {
        return data.get(1);
    }

    public int capacity() {
        return data.get(2);
    }

    public int cooldown() {
        return data.get(3);
    }

    public int integrity() {
        return data.get(4);
    }

    public int empTicks() {
        return data.get(5);
    }

    public int airburstHeight() {
        return data.get(6);
    }

    public boolean hasTarget() {
        if (launcher != null) {
            return launcher.getTarget() != null;
        }
        return data.get(9) != 0;
    }

    public int targetX() {
        if (launcher != null && launcher.getTarget() != null) {
            return launcher.getTarget().getX();
        }
        return data.get(7);
    }

    public int targetZ() {
        if (launcher != null && launcher.getTarget() != null) {
            return launcher.getTarget().getZ();
        }
        return data.get(8);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (launcher == null) {
            return false;
        }
        return switch (id) {
            case 0 -> launcher.tryLaunch(player);
            case 1 -> launcher.ejectMissile(player);
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return launcher != null && !launcher.isRemoved()
                && player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5) <= 64.0;
    }
}
