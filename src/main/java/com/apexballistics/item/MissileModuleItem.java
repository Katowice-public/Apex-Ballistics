package com.apexballistics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MissileModuleItem extends Item {
    public enum Category {
        GUIDANCE,
        PAYLOAD,
        FUSE,
        MOTOR,
        ACCURACY,
        RELIABILITY,
        ANTI_JAM
    }

    private final Category category;
    private final String value;

    public MissileModuleItem(Category category, String value, Properties properties) {
        super(properties);
        this.category = category;
        this.value = value;
    }

    public Category category() {
        return category;
    }

    public String value() {
        return value;
    }

    public MissileSpecification apply(MissileSpecification old) {
        return switch (category) {
            case GUIDANCE -> new MissileSpecification(
                    GuidanceMode.byName(value, old.guidance()), old.payload(), old.fuse(),
                    old.stages(), old.accuracy(), old.reliability(), old.antiJam(),
                    old.airburstHeight(), old.waypoints());
            case PAYLOAD -> new MissileSpecification(
                    old.guidance(), PayloadType.byName(value), old.fuse(), old.stages(),
                    old.accuracy(), old.reliability(), old.antiJam(), old.airburstHeight(),
                    old.waypoints());
            case FUSE -> new MissileSpecification(
                    old.guidance(), old.payload(), FuseMode.byName(value), old.stages(),
                    old.accuracy(), old.reliability(), old.antiJam(),
                    "airburst".equals(value) ? 10 : old.airburstHeight(), old.waypoints());
            case MOTOR -> new MissileSpecification(
                    old.guidance(), old.payload(), old.fuse(), Math.clamp(Integer.parseInt(value), 1, 3),
                    old.accuracy(), old.reliability(), old.antiJam(), old.airburstHeight(),
                    old.waypoints());
            case ACCURACY -> new MissileSpecification(
                    old.guidance(), old.payload(), old.fuse(), old.stages(),
                    Math.clamp(Float.parseFloat(value), 0.0f, 24.0f), old.reliability(),
                    old.antiJam(), old.airburstHeight(), old.waypoints());
            case RELIABILITY -> new MissileSpecification(
                    old.guidance(), old.payload(), old.fuse(), old.stages(), old.accuracy(),
                    Math.clamp(Float.parseFloat(value), 0.5f, 1.0f), old.antiJam(),
                    old.airburstHeight(), old.waypoints());
            case ANTI_JAM -> new MissileSpecification(
                    old.guidance(), old.payload(), old.fuse(), old.stages(), old.accuracy(),
                    old.reliability(), true, old.airburstHeight(), old.waypoints());
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.apexballistics.missile_module.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.apexballistics.module",
                category.name().toLowerCase(), value).withStyle(ChatFormatting.AQUA));
    }
}
