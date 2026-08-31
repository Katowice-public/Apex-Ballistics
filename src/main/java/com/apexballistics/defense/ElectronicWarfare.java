package com.apexballistics.defense;

import com.apexballistics.entity.FlareEntity;
import com.apexballistics.item.JammerItem;
import com.apexballistics.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class ElectronicWarfare {
    private ElectronicWarfare() {
    }

    public static boolean isJammed(Level level, Vec3 position) {
        return level.getEntitiesOfClass(Player.class, new AABB(position, position).inflate(48),
                ElectronicWarfare::hasActiveJammer).size() > 0;
    }

    public static FlareEntity nearestFlare(Level level, Vec3 position, double range) {
        return level.getEntitiesOfClass(FlareEntity.class,
                        new AABB(position, position).inflate(range), flare -> flare.isAlive())
                .stream()
                .min(Comparator.comparingDouble(flare -> flare.distanceToSqr(position)))
                .orElse(null);
    }

    private static boolean hasActiveJammer(Player player) {
        if (player.getMainHandItem().is(ModItems.JAMMER.get())
                && JammerItem.isActive(player.getMainHandItem())) {
            return true;
        }
        if (player.getOffhandItem().is(ModItems.JAMMER.get())
                && JammerItem.isActive(player.getOffhandItem())) {
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.JAMMER.get())
                    && JammerItem.isActive(player.getInventory().getItem(i))) {
                return true;
            }
        }
        return false;
    }
}
