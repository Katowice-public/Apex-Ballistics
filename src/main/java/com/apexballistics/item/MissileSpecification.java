package com.apexballistics.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional per-stack engineering data for modular missiles. Legacy missiles
 * without custom data receive safe defaults derived from their missile kind.
 */
public record MissileSpecification(
        GuidanceMode guidance,
        PayloadType payload,
        FuseMode fuse,
        int stages,
        float accuracy,
        float reliability,
        boolean antiJam,
        int airburstHeight,
        List<BlockPos> waypoints
) {
    public static MissileSpecification defaults(MissileKind kind) {
        GuidanceMode guidance = switch (kind.profile()) {
            case BALLISTIC -> GuidanceMode.COORDINATE;
            case CRUISE -> GuidanceMode.TERRAIN_FOLLOWING;
            case HOMING_AIR -> kind == MissileKind.AAM ? GuidanceMode.INFRARED : GuidanceMode.RADAR;
        };
        FuseMode fuse = kind.profile() == MissileKind.FlightProfile.HOMING_AIR
                ? FuseMode.PROXIMITY : FuseMode.IMPACT;
        int stages = kind.profile() == MissileKind.FlightProfile.BALLISTIC ? 2 : 1;
        return new MissileSpecification(guidance, PayloadType.STANDARD, fuse, stages,
                kind == MissileKind.ICBM ? 10.0f : 4.0f, 0.98f, false, 8, List.of());
    }

    public static MissileSpecification fromStack(ItemStack stack, MissileKind kind) {
        MissileSpecification fallback = defaults(kind);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return fallback;
        }
        CompoundTag tag = data.copyTag();
        List<BlockPos> waypoints = new ArrayList<>();
        int waypointCount = Math.min(6, Math.max(0, tag.getInt("WaypointCount")));
        for (int i = 0; i < waypointCount; i++) {
            if (tag.contains("W" + i + "X")) {
                waypoints.add(new BlockPos(tag.getInt("W" + i + "X"),
                        tag.getInt("W" + i + "Y"), tag.getInt("W" + i + "Z")));
            }
        }
        return new MissileSpecification(
                GuidanceMode.byName(tag.getString("Guidance"), fallback.guidance),
                PayloadType.byName(tag.getString("Payload")),
                FuseMode.byName(tag.getString("Fuse")),
                tag.contains("Stages") ? Math.clamp(tag.getInt("Stages"), 1, 3) : fallback.stages,
                tag.contains("Accuracy") ? Math.clamp(tag.getFloat("Accuracy"), 0.0f, 24.0f) : fallback.accuracy,
                tag.contains("Reliability") ? Math.clamp(tag.getFloat("Reliability"), 0.5f, 1.0f) : fallback.reliability,
                tag.getBoolean("AntiJam"),
                tag.contains("AirburstHeight") ? Math.clamp(tag.getInt("AirburstHeight"), 3, 32) : fallback.airburstHeight,
                List.copyOf(waypoints)
        );
    }

    public static void write(ItemStack stack, MissileSpecification spec) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("Guidance", spec.guidance.getSerializedName());
            tag.putString("Payload", spec.payload.getSerializedName());
            tag.putString("Fuse", spec.fuse.getSerializedName());
            tag.putInt("Stages", spec.stages);
            tag.putFloat("Accuracy", spec.accuracy);
            tag.putFloat("Reliability", spec.reliability);
            tag.putBoolean("AntiJam", spec.antiJam);
            tag.putInt("AirburstHeight", spec.airburstHeight);
            tag.putInt("WaypointCount", spec.waypoints.size());
            for (int i = 0; i < spec.waypoints.size(); i++) {
                BlockPos pos = spec.waypoints.get(i);
                tag.putInt("W" + i + "X", pos.getX());
                tag.putInt("W" + i + "Y", pos.getY());
                tag.putInt("W" + i + "Z", pos.getZ());
            }
        });
    }

    public MissileSpecification withWaypoints(List<BlockPos> points) {
        return new MissileSpecification(guidance, payload, fuse, stages, accuracy,
                reliability, antiJam, airburstHeight, List.copyOf(points));
    }
}
