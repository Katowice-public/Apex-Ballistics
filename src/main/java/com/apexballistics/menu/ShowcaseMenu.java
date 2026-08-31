package com.apexballistics.menu;

import com.apexballistics.blockentity.MissileShowcaseBlockEntity;
import com.apexballistics.item.MissileItem;
import com.apexballistics.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ShowcaseMenu extends AbstractContainerMenu {
    private final MissileShowcaseBlockEntity showcase;
    private final BlockPos blockPos;
    private final Container container;

    public ShowcaseMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(extraData.readBlockPos()) instanceof MissileShowcaseBlockEntity block
                        ? block : null);
    }

    public ShowcaseMenu(int containerId, Inventory inventory, MissileShowcaseBlockEntity showcase) {
        super(ModMenus.SHOWCASE.get(), containerId);
        this.showcase = showcase;
        this.blockPos = showcase == null ? inventory.player.blockPosition() : showcase.getBlockPos();
        this.container = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (showcase != null) {
                    showcase.setMissile(getItem(0));
                }
            }
        };
        if (showcase != null) {
            container.setItem(0, showcase.getMissile().copy());
        }
        addSlot(new Slot(container, 0, 204, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof MissileItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 48 + col * 18, 138 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 48 + col * 18, 196));
        }
    }

    public ItemStack displayed() {
        return container.getItem(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof MissileItem) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return showcase != null && !showcase.isRemoved()
                && player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5) <= 64.0;
    }
}
