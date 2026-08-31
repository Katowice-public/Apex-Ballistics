package com.apexballistics.menu;

import com.apexballistics.block.SirenType;
import com.apexballistics.blockentity.SirenBlockEntity;
import com.apexballistics.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class SirenMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 6;

    private final SirenBlockEntity siren;
    private final BlockPos blockPos;
    private final ContainerData data;

    public SirenMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(extraData.readBlockPos()) instanceof SirenBlockEntity block
                        ? block : null,
                new SimpleContainerData(DATA_COUNT));
    }

    public SirenMenu(int containerId, Inventory inventory, SirenBlockEntity siren) {
        this(containerId, inventory, siren, createServerData(siren));
    }

    private SirenMenu(int containerId, Inventory inventory, SirenBlockEntity siren, ContainerData data) {
        super(ModMenus.SIREN.get(), containerId);
        this.siren = siren;
        this.blockPos = siren == null ? inventory.player.blockPosition() : siren.getBlockPos();
        this.data = data;
        addDataSlots(data);
    }

    private static ContainerData createServerData(SirenBlockEntity siren) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> siren.powered() ? 1 : 0;
                    case 1 -> siren.autoAlert() ? 1 : 0;
                    case 2 -> siren.soundEnabled() ? 1 : 0;
                    case 3 -> siren.linked() ? 1 : 0;
                    case 4 -> siren.sounding() ? 1 : 0;
                    case 5 -> siren.sirenType().ordinal();
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

    public boolean powered() {
        return data.get(0) != 0;
    }

    public boolean autoAlert() {
        return data.get(1) != 0;
    }

    public boolean soundEnabled() {
        return data.get(2) != 0;
    }

    public boolean linked() {
        return data.get(3) != 0;
    }

    public boolean sounding() {
        return data.get(4) != 0;
    }

    public SirenType sirenType() {
        if (siren != null) {
            return siren.sirenType();
        }
        SirenType[] values = SirenType.values();
        int ordinal = data.get(5);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SirenType.AIR_RAID;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (siren == null) {
            return false;
        }
        switch (id) {
            case 0 -> siren.togglePowered();
            case 1 -> siren.toggleAutoAlert();
            case 2 -> siren.toggleSound();
            case 3 -> siren.testWail();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return siren != null && !siren.isRemoved()
                && player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5) <= 64.0;
    }
}
