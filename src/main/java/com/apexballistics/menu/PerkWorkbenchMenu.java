package com.apexballistics.menu;

import com.apexballistics.blockentity.PerkWorkbenchBlockEntity;
import com.apexballistics.item.PerkItem;
import com.apexballistics.item.WeaponPerks;
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

public final class PerkWorkbenchMenu extends AbstractContainerMenu {
    private final PerkWorkbenchBlockEntity bench;
    private final BlockPos blockPos;

    public PerkWorkbenchMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(extraData.readBlockPos()) instanceof PerkWorkbenchBlockEntity block
                        ? block : null);
    }

    public PerkWorkbenchMenu(int containerId, Inventory inventory, PerkWorkbenchBlockEntity bench) {
        super(ModMenus.PERK_WORKBENCH.get(), containerId);
        this.bench = bench;
        this.blockPos = bench == null ? inventory.player.blockPosition() : bench.getBlockPos();
        Container container = bench == null ? new SimpleContainer(PerkWorkbenchBlockEntity.SIZE) : bench;
        addSlot(new Slot(container, PerkWorkbenchBlockEntity.SLOT_WEAPON, 38, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WeaponPerks.canUpgrade(stack);
            }
        });
        addSlot(new Slot(container, PerkWorkbenchBlockEntity.SLOT_PERK, 80, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof PerkItem;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 48 + col * 18, 118 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 48 + col * 18, 176));
        }
    }

    public ItemStack weapon() {
        return bench == null ? ItemStack.EMPTY : bench.weapon();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (bench == null || id != 0) {
            return false;
        }
        return bench.applyPerk(player);
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
        if (index < PerkWorkbenchBlockEntity.SIZE) {
            if (!moveItemStackTo(stack, PerkWorkbenchBlockEntity.SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, PerkWorkbenchBlockEntity.SIZE, false)) {
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
        return bench != null && bench.stillValid(player);
    }
}
