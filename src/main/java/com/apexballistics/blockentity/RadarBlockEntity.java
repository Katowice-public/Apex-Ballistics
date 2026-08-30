package com.apexballistics.blockentity;

import com.apexballistics.entity.MissileEntity;
import com.apexballistics.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class RadarBlockEntity extends BlockEntity {
    private int pulse;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        pulse++;
        if (pulse % 80 != 0) {
            return;
        }
        int contacts = countContacts(64.0);
        if (contacts > 0) {
            List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(16));
            for (Player player : players) {
                player.displayClientMessage(Component.translatable("message.apexballistics.radar_pulse", contacts).withStyle(ChatFormatting.GREEN), true);
            }
        }
    }

    public void scan(Player player) {
        AABB box = new AABB(worldPosition).inflate(96);
        List<Entity> contacts = level.getEntities((Entity) null, box, this::isContact);
        if (contacts.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.apexballistics.radar_clear").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        player.displayClientMessage(Component.translatable("message.apexballistics.radar_header", contacts.size()).withStyle(ChatFormatting.GREEN), false);
        int shown = 0;
        for (Entity entity : contacts) {
            if (shown++ >= 8) {
                break;
            }
            player.displayClientMessage(Component.literal(" - ")
                    .append(entity.getName())
                    .append(" @ ")
                    .append(Component.literal(entity.blockPosition().toShortString()))
                    .withStyle(ChatFormatting.AQUA), false);
        }
    }

    private int countContacts(double range) {
        if (level == null) {
            return 0;
        }
        return level.getEntities((Entity) null, new AABB(worldPosition).inflate(range), this::isContact).size();
    }

    private boolean isContact(Entity entity) {
        if (entity instanceof MissileEntity) {
            return true;
        }
        if (entity instanceof Player player) {
            return player.isFallFlying();
        }
        return entity instanceof LivingEntity living && !living.onGround() && living.getY() > worldPosition.getY() + 4;
    }
}
