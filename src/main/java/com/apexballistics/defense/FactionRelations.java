package com.apexballistics.defense;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

/**
 * Optional IFF built on vanilla scoreboard teams. Players never have to join a
 * faction: unaligned players are neutral. The reserved "outlaws" team and
 * hostile mobs are treated as hostile by automated defenses.
 */
public final class FactionRelations {
    public static final String OUTLAWS = "outlaws";

    private FactionRelations() {
    }

    public static boolean isHostile(Entity owner, Entity candidate) {
        if (candidate == null || !candidate.isAlive() || candidate == owner) {
            return false;
        }
        if (candidate instanceof Enemy) {
            return true;
        }
        Team targetTeam = candidate.getTeam();
        if (isOutlaw(targetTeam)) {
            return true;
        }
        if (!(candidate instanceof Player)) {
            return true;
        }
        if (owner == null) {
            return false;
        }
        Team ownerTeam = owner.getTeam();
        if (ownerTeam == null || targetTeam == null) {
            return false;
        }
        return ownerTeam != targetTeam && !ownerTeam.isAlliedTo(targetTeam);
    }

    public static boolean isFriendly(Entity owner, Entity candidate) {
        if (owner == candidate) {
            return true;
        }
        if (owner == null || candidate == null || isOutlaw(candidate.getTeam())) {
            return false;
        }
        Team ownerTeam = owner.getTeam();
        Team targetTeam = candidate.getTeam();
        return ownerTeam != null && targetTeam != null
                && (ownerTeam == targetTeam || ownerTeam.isAlliedTo(targetTeam));
    }

    public static String factionName(Entity entity) {
        Team team = entity == null ? null : entity.getTeam();
        return team == null ? "neutral" : team.getName();
    }

    private static boolean isOutlaw(Team team) {
        return team != null && OUTLAWS.equalsIgnoreCase(team.getName());
    }
}
