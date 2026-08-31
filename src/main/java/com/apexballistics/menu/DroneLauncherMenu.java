package com.apexballistics.menu;

import com.apexballistics.blockentity.DroneLauncherBlockEntity;
import com.apexballistics.item.BombItem;
import com.apexballistics.item.StrikeDroneItem;
import com.apexballistics.item.TargetingTabletItem;
import com.apexballistics.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DroneLauncherMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 6;

    private final DroneLauncherBlockEntity launcher;
    private final BlockPos blockPos;
    private final ContainerData data;

    public DroneLauncherMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(extraData.readBlockPos()) instanceof DroneLauncherBlockEntity block
                        ? block : null,
                new SimpleContainerData(DATA_COUNT));
    }

    public DroneLauncherMenu(int containerId, Inventory inventory, DroneLauncherBlockEntity launcher) {
        this(containerId, inventory, launcher, createServerData(launcher));
    }

    private DroneLauncherMenu(int containerId, Inventory inventory, DroneLauncherBlockEntity launcher,
                              ContainerData data) {
        super(ModMenus.DRONE_LAUNCHER.get(), containerId);
        this.launcher = launcher;
        this.blockPos = launcher == null ? inventory.player.blockPosition() : launcher.getBlockPos();
        this.data = data;
        Container container = launcher == null ? new SimpleContainer(DroneLauncherBlockEntity.SIZE) : launcher;
        addSlot(new FilterSlot(container, DroneLauncherBlockEntity.SLOT_DRONE, 20, 48,
                stack -> stack.getItem() instanceof StrikeDroneItem));
        addSlot(new FilterSlot(container, DroneLauncherBlockEntity.SLOT_BOMB, 56, 48,
                stack -> stack.getItem() instanceof BombItem));
        addSlot(new FilterSlot(container, DroneLauncherBlockEntity.SLOT_TABLET, 92, 48,
                stack -> stack.getItem() instanceof TargetingTabletItem));
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 48 + col * 18, 118 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 48 + col * 18, 176));
        }
    }

    private static ContainerData createServerData(DroneLauncherBlockEntity launcher) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> launcher.getCooldown();
                    case 1 -> launcher.getIntegrity();
                    case 2 -> launcher.getEmpTicks();
                    case 3 -> launcher.getTarget() == null ? 0 : launcher.getTarget().getX();
                    case 4 -> launcher.getTarget() == null ? 0 : launcher.getTarget().getZ();
                    case 5 -> launcher.getTarget() == null ? 0 : 1;
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

    public int cooldown() {
        return data.get(0);
    }

    public int integrity() {
        return data.get(1);
    }

    public int empTicks() {
        return data.get(2);
    }

    public boolean hasTarget() {
        return data.get(5) != 0;
    }

    public int targetX() {
        return data.get(3);
    }

    public int targetZ() {
        return data.get(4);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (launcher == null) {
            return false;
        }
        return switch (id) {
            case 0 -> launcher.tryLaunch(player);
            case 1 -> launcher.eject(player);
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < DroneLauncherBlockEntity.SIZE) {
            if (!moveItemStackTo(stack, DroneLauncherBlockEntity.SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, DroneLauncherBlockEntity.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return launcher != null && launcher.stillValid(player);
    }

    private static final class FilterSlot extends Slot {
        private final java.util.function.Predicate<ItemStack> filter;

        private FilterSlot(Container container, int index, int x, int y,
                           java.util.function.Predicate<ItemStack> filter) {
            super(container, index, x, y);
            this.filter = filter;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return filter.test(stack);
        }
    }
}
