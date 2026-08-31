package com.apexballistics.blockentity;

import com.apexballistics.item.PerkItem;
import com.apexballistics.item.WeaponPerks;
import com.apexballistics.menu.PerkWorkbenchMenu;
import com.apexballistics.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
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
import org.jetbrains.annotations.Nullable;

public class PerkWorkbenchBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_WEAPON = 0;
    public static final int SLOT_PERK = 1;
    public static final int SIZE = 2;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public PerkWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PERK_WORKBENCH.get(), pos, state);
    }

    public void serverTick() {
    }

    public boolean applyPerk(Player player) {
        ItemStack weapon = items.get(SLOT_WEAPON);
        ItemStack perkStack = items.get(SLOT_PERK);
        if (!WeaponPerks.canUpgrade(weapon) || !(perkStack.getItem() instanceof PerkItem perkItem)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.apexballistics.perk_invalid"), true);
            }
            return false;
        }
        if (!WeaponPerks.upgrade(weapon, perkItem.kind())) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.apexballistics.perk_max"), true);
            }
            return false;
        }
        perkStack.shrink(1);
        if (perkStack.isEmpty()) {
            items.set(SLOT_PERK, ItemStack.EMPTY);
        }
        setChangedAndSync();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.4f);
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.apexballistics.perk_applied",
                    perkItem.kind().getSerializedName()).withStyle(net.minecraft.ChatFormatting.GREEN), true);
        }
        return true;
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropContents(level, worldPosition, this);
            items.clear();
        }
    }

    public ItemStack weapon() {
        return items.get(SLOT_WEAPON);
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
        return items.stream().allMatch(ItemStack::isEmpty);
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
        setChangedAndSync();
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_WEAPON) {
            return WeaponPerks.canUpgrade(stack);
        }
        return slot == SLOT_PERK && stack.getItem() instanceof PerkItem;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.apexballistics.perk_workbench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PerkWorkbenchMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
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
