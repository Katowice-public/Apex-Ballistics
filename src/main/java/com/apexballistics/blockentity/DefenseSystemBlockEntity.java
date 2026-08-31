package com.apexballistics.blockentity;

import com.apexballistics.block.SystemBlock;
import com.apexballistics.block.SystemType;
import com.apexballistics.defense.EmpSensitive;
import com.apexballistics.defense.FactionRelations;
import com.apexballistics.entity.AerialThreat;
import com.apexballistics.entity.CiwsTracerEntity;
import com.apexballistics.entity.MissileEntity;
import com.apexballistics.item.ApexArmorItem;
import com.apexballistics.item.ArmorModuleItem;
import com.apexballistics.item.MissileItem;
import com.apexballistics.item.MissileSpecification;
import com.apexballistics.item.PoweredEquipment;
import com.apexballistics.item.RailgunItem;
import com.apexballistics.item.GaussRifleItem;
import com.apexballistics.item.WeaponHeat;
import com.apexballistics.registry.ModBlockEntities;
import com.apexballistics.registry.ModEntities;
import com.apexballistics.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class DefenseSystemBlockEntity extends BlockEntity implements EmpSensitive {
    private ItemStack stored = ItemStack.EMPTY;
    private UUID owner;
    private int energy = 2_000;
    private int integrity = 100;
    private int empTicks;
    private int tick;

    public DefenseSystemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEFENSE_SYSTEM.get(), pos, state);
    }

    public SystemType systemType() {
        return getBlockState().getBlock() instanceof SystemBlock block
                ? block.systemType() : SystemType.COMMAND_CONSOLE;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        tick++;
        if (empTicks > 0) {
            empTicks--;
            return;
        }
        if (energy < 2_000 && tick % 2 == 0) {
            energy++;
        }
        switch (systemType()) {
            case CIWS -> {
                if (tick % 5 == 0) {
                    engageAirTarget(80, 8, true);
                }
            }
            case LASER_DEFENSE -> {
                if (tick % 10 == 0) {
                    engageAirTarget(96, 30, false);
                }
            }
            case PASSIVE_RADAR -> {
                if (tick % 60 == 0) {
                    passiveAlert();
                }
            }
            default -> {
            }
        }
    }

    private void engageAirTarget(double range, int energyCost, boolean tracers) {
        if (!(level instanceof ServerLevel server) || energy < energyCost || integrity < 25) {
            return;
        }
        Player ownerPlayer = owner == null ? null : level.getPlayerByUUID(owner);
        Vec3 origin = Vec3.atCenterOf(worldPosition).add(0, 1.45, 0);
        List<Entity> contacts = level.getEntities((Entity) null, new AABB(worldPosition).inflate(range), entity -> {
            if (!(entity instanceof AerialThreat) || !entity.isAlive()) {
                return false;
            }
            Entity threatOwner = entity instanceof net.minecraft.world.entity.projectile.Projectile projectile
                    ? projectile.getOwner() : null;
            if (ownerPlayer != null && (threatOwner == ownerPlayer
                    || FactionRelations.isFriendly(ownerPlayer, threatOwner))) {
                return false;
            }
            return true;
        });
        Entity target = contacts.stream().min(Comparator.comparingDouble(
                entity -> entity.distanceToSqr(origin))).orElse(null);
        if (target == null) {
            return;
        }
        energy -= energyCost;
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin);
        if (tracers) {
            Vec3 dir = to.normalize();
            int burst = 8;
            for (int i = 0; i < burst; i++) {
                CiwsTracerEntity tracer = new CiwsTracerEntity(ModEntities.CIWS_TRACER.get(), level);
                tracer.setOwner(ownerPlayer);
                tracer.setPos(origin.x, origin.y, origin.z);
                double spread = 0.018;
                Vec3 vel = dir.add(
                        (level.random.nextDouble() - 0.5) * spread,
                        (level.random.nextDouble() - 0.5) * spread,
                        (level.random.nextDouble() - 0.5) * spread).normalize().scale(8.5);
                tracer.setDeltaMovement(vel);
                level.addFreshEntity(tracer);
            }
            int steps = 14;
            for (int i = 0; i <= steps; i++) {
                Vec3 point = origin.add(to.scale(i / (double) steps));
                server.sendParticles(CiwsTracerEntity.TRACER, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            if (target instanceof AerialThreat threat) {
                threat.intercept();
            }
        } else {
            int steps = 16;
            for (int i = 0; i <= steps; i++) {
                Vec3 point = origin.add(to.scale(i / (double) steps));
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.01);
            }
            if (target instanceof AerialThreat threat) {
                threat.intercept();
            }
        }
        level.playSound(null, worldPosition,
                tracers ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.BEACON_POWER_SELECT,
                SoundSource.BLOCKS, tracers ? 1.15f : 1.0f, tracers ? 0.55f : 1.8f);
        setChanged();
    }

    private void passiveAlert() {
        List<MissileEntity> contacts = level.getEntitiesOfClass(MissileEntity.class,
                new AABB(worldPosition).inflate(128));
        if (contacts.isEmpty()) {
            return;
        }
        for (Player player : level.getEntitiesOfClass(Player.class,
                new AABB(worldPosition).inflate(24))) {
            player.displayClientMessage(Component.translatable(
                    "message.apexballistics.passive_contacts", contacts.size())
                    .withStyle(ChatFormatting.YELLOW), true);
        }
    }

    public boolean interact(Player player, ItemStack held) {
        if (level == null || level.isClientSide) {
            return true;
        }
        if (owner == null) {
            owner = player.getUUID();
        }
        return switch (systemType()) {
            case MISSILE_RACK -> rackInteraction(player, held);
            case MAINTENANCE -> maintenanceInteraction(player, held);
            case CAPACITOR_CHARGER -> chargerInteraction(player, held);
            case REFINERY -> refineryInteraction(player, held);
            case LOADING_CRANE -> craneInteraction(player, held);
            case SUBMARINE_CONTROL -> submarineStatus(player);
            case COMMAND_CONSOLE -> commandStatus(player);
            case CIWS, LASER_DEFENSE, PASSIVE_RADAR -> {
                player.displayClientMessage(Component.translatable(
                        "message.apexballistics.system_status", energy, integrity,
                        empTicks).withStyle(ChatFormatting.AQUA), false);
                yield true;
            }
        };
    }

    private boolean rackInteraction(Player player, ItemStack held) {
        if (held.getItem() instanceof MissileItem && (stored.isEmpty()
                || ItemStack.isSameItemSameComponents(stored, held)) && stored.getCount() < 16) {
            if (stored.isEmpty()) {
                stored = held.copyWithCount(1);
            } else {
                stored.grow(1);
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        } else if (held.isEmpty() && !stored.isEmpty()) {
            player.getInventory().placeItemBackInInventory(stored.copyWithCount(1));
            stored.shrink(1);
            if (stored.isEmpty()) {
                stored = ItemStack.EMPTY;
            }
        }
        player.displayClientMessage(Component.translatable(
                "message.apexballistics.rack_status", stored.isEmpty() ? 0 : stored.getCount(),
                stored.isEmpty() ? Component.literal("-") : stored.getHoverName()), true);
        setChanged();
        return true;
    }

    private boolean maintenanceInteraction(Player player, ItemStack held) {
        if (held.getItem() instanceof MissileItem missileItem) {
            MissileSpecification old = MissileSpecification.fromStack(held, missileItem.kind());
            MissileSpecification.write(held, new MissileSpecification(old.guidance(), old.payload(),
                    old.fuse(), old.stages(), old.accuracy(), 1.0f, old.antiJam(),
                    old.airburstHeight(), old.waypoints()));
            player.displayClientMessage(Component.translatable(
                    "message.apexballistics.maintenance_complete").withStyle(ChatFormatting.GREEN), true);
            return true;
        }
        if (held.getItem() instanceof ArmorModuleItem module) {
            ItemStack armor = findArmor(player);
            if (!armor.isEmpty()) {
                PoweredEquipment.setModule(armor, module.moduleId());
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                player.displayClientMessage(Component.translatable(
                        "message.apexballistics.armor_module_installed", module.moduleId())
                        .withStyle(ChatFormatting.GREEN), true);
            }
            return true;
        }
        repairNearbyLaunchers();
        player.displayClientMessage(Component.translatable(
                "message.apexballistics.maintenance_launchers").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    private boolean chargerInteraction(Player player, ItemStack held) {
        if (held.getItem() instanceof ApexArmorItem) {
            PoweredEquipment.setEnergy(held, PoweredEquipment.MAX_ARMOR_ENERGY);
        } else if (held.getItem() instanceof GaussRifleItem || held.getItem() instanceof RailgunItem) {
            WeaponHeat.set(held, 0);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.apexballistics.charger_invalid").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        player.displayClientMessage(Component.translatable(
                "message.apexballistics.charged").withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS, 0.8f, 1.4f);
        return true;
    }

    private boolean refineryInteraction(Player player, ItemStack held) {
        if (!held.is(Items.BLAZE_POWDER)) {
            player.displayClientMessage(Component.translatable(
                    "message.apexballistics.refinery_input").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.ADVANCED_PROPELLANT.get(), 2));
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW,
                SoundSource.BLOCKS, 0.8f, 0.8f);
        return true;
    }

    private boolean craneInteraction(Player player, ItemStack held) {
        if (!(held.getItem() instanceof MissileItem)) {
            player.displayClientMessage(Component.translatable(
                    "message.apexballistics.crane_needs_missile").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-5, -2, -5),
                worldPosition.offset(5, 2, 5))) {
            if (level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher
                    && launcher.canLoad(held)) {
                launcher.loadOne(held, player);
                player.displayClientMessage(Component.translatable(
                        "message.apexballistics.crane_loaded").withStyle(ChatFormatting.GREEN), true);
                return true;
            }
        }
        player.displayClientMessage(Component.translatable(
                "message.apexballistics.crane_no_launcher").withStyle(ChatFormatting.RED), true);
        return true;
    }

    private boolean submarineStatus(Player player) {
        long wetTubes = BlockPos.betweenClosedStream(worldPosition.offset(-8, -4, -8),
                        worldPosition.offset(8, 4, 8))
                .filter(pos -> level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher
                        && launcher.launcherType() == com.apexballistics.block.LauncherType.TUBE
                        && !level.getFluidState(pos).isEmpty())
                .count();
        player.displayClientMessage(Component.translatable(
                "message.apexballistics.submarine_status", wetTubes)
                .withStyle(wetTubes > 0 ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        return true;
    }

    private boolean commandStatus(Player player) {
        List<MissileEntity> missiles = level.getEntitiesOfClass(MissileEntity.class,
                new AABB(worldPosition).inflate(192));
        long hostile = missiles.stream().filter(missile -> {
            Entity missileOwner = missile.getOwner();
            return missileOwner == null || FactionRelations.isHostile(player, missileOwner);
        }).count();
        player.displayClientMessage(Component.translatable(
                "message.apexballistics.command_status", missiles.size(), hostile,
                FactionRelations.factionName(player)).withStyle(ChatFormatting.AQUA), false);
        return true;
    }

    private void repairNearbyLaunchers() {
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-6, -3, -6),
                worldPosition.offset(6, 3, 6))) {
            if (level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher) {
                launcher.repair();
            }
        }
    }

    private static ItemStack findArmor(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ApexArmorItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public void damageIntegrity(int amount) {
        integrity = Math.max(0, integrity - amount);
        setChanged();
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
        if (!stored.isEmpty()) {
            tag.put("Stored", stored.save(registries));
        }
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("Energy", energy);
        tag.putInt("Integrity", integrity);
        tag.putInt("EmpTicks", empTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.contains("Stored")
                ? ItemStack.parseOptional(registries, tag.getCompound("Stored")) : ItemStack.EMPTY;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        energy = tag.contains("Energy") ? tag.getInt("Energy") : 2_000;
        integrity = tag.contains("Integrity") ? tag.getInt("Integrity") : 100;
        empTicks = tag.getInt("EmpTicks");
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
