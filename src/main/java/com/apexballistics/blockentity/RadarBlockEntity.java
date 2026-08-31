package com.apexballistics.blockentity;

import com.apexballistics.entity.MissileEntity;
import com.apexballistics.defense.EmpSensitive;
import com.apexballistics.defense.FactionRelations;
import com.apexballistics.registry.ModBlockEntities;
import com.apexballistics.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public class RadarBlockEntity extends BlockEntity implements EmpSensitive {
    private static final Map<Level, Map<Long, RadarBlockEntity>> NETWORK = new WeakHashMap<>();
    private int pulse;
    private int empTicks;
    private UUID owner;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        synchronized (NETWORK) {
            NETWORK.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>())
                    .put(worldPosition.asLong(), this);
        }
        if (empTicks > 0) {
            empTicks--;
            return;
        }
        pulse++;
        if (pulse % 80 != 0) {
            return;
        }
        level.playSound(null, worldPosition, ModSounds.RADAR_SERVO.get(),
                SoundSource.BLOCKS, 0.65f, 1.0f);
        int contacts = countContacts(64.0);
        if (contacts > 0) {
            List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(16));
            for (Player player : players) {
                player.displayClientMessage(Component.translatable("message.apexballistics.radar_pulse", contacts).withStyle(ChatFormatting.GREEN), true);
            }
        }
    }

    public void scan(Player player) {
        owner = player.getUUID();
        setChanged();
        if (empTicks > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.apexballistics.radar_emp").withStyle(ChatFormatting.RED), true);
            return;
        }
        AABB box = new AABB(worldPosition).inflate(96);
        List<Entity> contacts = level.getEntities((Entity) null, box, this::isContact);
        contacts.sort(Comparator.comparingDouble(entity ->
                entity.distanceToSqr(Vec3.atCenterOf(worldPosition))));
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
            int quality = trackingQuality(entity);
            String faction = FactionRelations.factionName(entity);
            player.displayClientMessage(Component.literal(" - ")
                    .append(entity.getName())
                    .append(" @ ")
                    .append(Component.literal(entity.blockPosition().toShortString()))
                    .append(Component.literal("  Q" + quality + "% " + faction))
                    .withStyle(ChatFormatting.AQUA), false);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(),
                        entity.getY() + 0.5, entity.getZ(), 4,
                        0.2, 0.2, 0.2, 0.01);
            }
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

    private int trackingQuality(Entity entity) {
        double distance = Math.sqrt(entity.distanceToSqr(Vec3.atCenterOf(worldPosition)));
        int quality = (int) Math.clamp(100.0 - distance * 0.7, 10.0, 100.0);
        Vec3 start = Vec3.atCenterOf(worldPosition).add(0, 1, 0);
        BlockHitResult hit = level.clip(new ClipContext(start, entity.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() != HitResult.Type.MISS) {
            quality /= 2;
        }
        return quality;
    }

    public static boolean hasNetworkCoverage(Level level, BlockPos pos, UUID requester) {
        synchronized (NETWORK) {
            Map<Long, RadarBlockEntity> radars = NETWORK.get(level);
            if (radars == null) {
                return false;
            }
            Player requesterPlayer = requester == null ? null : level.getPlayerByUUID(requester);
            radars.values().removeIf(BlockEntity::isRemoved);
            return radars.values().stream().anyMatch(radar -> {
                if (radar.empTicks > 0 || radar.worldPosition.distSqr(pos) > 128 * 128) {
                    return false;
                }
                Player radarOwner = radar.owner == null ? null : level.getPlayerByUUID(radar.owner);
                return requesterPlayer == null || radarOwner == null
                        || FactionRelations.isFriendly(requesterPlayer, radarOwner)
                        || requesterPlayer == radarOwner;
            });
        }
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
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("EmpTicks", empTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        empTicks = tag.getInt("EmpTicks");
    }
}
