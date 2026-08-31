package com.apexballistics.blockentity;

import com.apexballistics.item.MissileItem;
import com.apexballistics.menu.ShowcaseMenu;
import com.apexballistics.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MissileShowcaseBlockEntity extends BlockEntity implements MenuProvider {
    private ItemStack missile = ItemStack.EMPTY;
    private float rotation;

    public MissileShowcaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHOWCASE.get(), pos, state);
    }

    public ItemStack getMissile() {
        return missile;
    }

    public float rotation() {
        return rotation;
    }

    public void setMissile(ItemStack stack) {
        missile = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChangedAndSync();
    }

    public void insertFromHand(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof MissileItem) || !missile.isEmpty()) {
            return;
        }
        setMissile(stack);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    public void dropContents() {
        if (level != null && !missile.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), missile);
            missile = ItemStack.EMPTY;
        }
    }

    public void clientTick() {
        rotation += 1.4f;
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Missile")) {
            missile = ItemStack.parseOptional(registries, tag.getCompound("Missile"));
        } else {
            missile = ItemStack.EMPTY;
        }
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
        return Component.translatable("screen.apexballistics.showcase");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ShowcaseMenu(containerId, inventory, this);
    }
}
