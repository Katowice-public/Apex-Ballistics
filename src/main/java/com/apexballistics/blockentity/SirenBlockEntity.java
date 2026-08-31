package com.apexballistics.blockentity;

import com.apexballistics.block.CableLinkable;
import com.apexballistics.block.SirenBlock;
import com.apexballistics.block.SirenType;
import com.apexballistics.menu.SirenMenu;
import com.apexballistics.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SirenBlockEntity extends BlockEntity implements CableLinkable, MenuProvider {
    private boolean powered;
    private boolean autoAlert = true;
    private boolean soundEnabled = true;
    private int soundingTicks;
    private int pulse;
    private BlockPos linkedRadar;

    public SirenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIREN.get(), pos, state);
    }

    public SirenType sirenType() {
        if (getBlockState().getBlock() instanceof SirenBlock siren) {
            return siren.sirenType();
        }
        return SirenType.AIR_RAID;
    }

    public boolean powered() {
        return powered;
    }

    public boolean autoAlert() {
        return autoAlert;
    }

    public boolean soundEnabled() {
        return soundEnabled;
    }

    public boolean sounding() {
        return soundEnabled && (powered || soundingTicks > 0);
    }

    public boolean linked() {
        return linkedRadar != null;
    }

    public BlockPos linkedRadar() {
        return linkedRadar;
    }

    public void togglePowered() {
        powered = !powered;
        setChangedAndSync();
    }

    public void toggleAutoAlert() {
        autoAlert = !autoAlert;
        setChangedAndSync();
    }

    public void toggleSound() {
        soundEnabled = !soundEnabled;
        setChangedAndSync();
    }

    public void testWail() {
        soundingTicks = Math.max(soundingTicks, 80);
        setChangedAndSync();
    }

    public void triggerAutoAlert() {
        if (!autoAlert || !soundEnabled) {
            return;
        }
        soundingTicks = Math.max(soundingTicks, 160);
        setChangedAndSync();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (soundingTicks > 0) {
            soundingTicks--;
            if (soundingTicks == 0) {
                setChangedAndSync();
            }
        }
        pulse++;
        if (sounding() && pulse % 45 == 0) {
            level.playSound(null, worldPosition, sirenType().sound().get(),
                    SoundSource.BLOCKS, 3.4f, 1.0f);
        }
    }

    @Override
    public boolean acceptCableFrom(BlockEntity other) {
        return other instanceof RadarBlockEntity;
    }

    @Override
    public void setCablePeer(BlockPos peer) {
        linkedRadar = peer;
        setChangedAndSync();
    }

    @Override
    public BlockPos getCablePeer() {
        return linkedRadar;
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
        tag.putBoolean("Powered", powered);
        tag.putBoolean("AutoAlert", autoAlert);
        tag.putBoolean("SoundEnabled", soundEnabled);
        tag.putInt("SoundingTicks", soundingTicks);
        if (linkedRadar != null) {
            tag.putInt("Rx", linkedRadar.getX());
            tag.putInt("Ry", linkedRadar.getY());
            tag.putInt("Rz", linkedRadar.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        powered = tag.getBoolean("Powered");
        autoAlert = !tag.contains("AutoAlert") || tag.getBoolean("AutoAlert");
        soundEnabled = !tag.contains("SoundEnabled") || tag.getBoolean("SoundEnabled");
        soundingTicks = tag.getInt("SoundingTicks");
        if (tag.contains("Rx")) {
            linkedRadar = new BlockPos(tag.getInt("Rx"), tag.getInt("Ry"), tag.getInt("Rz"));
        } else {
            linkedRadar = null;
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
        return Component.translatable("screen.apexballistics.siren." + sirenType().getSerializedName());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SirenMenu(containerId, inventory, this);
    }
}
