package dev.hephaestus.vauban.block.properties;

import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

public enum OxidizationLevel implements StringIdentifiable {
    UNAFFECTED(0.75F),
    EXPOSED(1F),
    WEATHERED(1F),
    OXIDIZED(1F);

    public static final EnumProperty<OxidizationLevel> PROPERTY = EnumProperty.of("oxidization", OxidizationLevel.class);
    public static final OxidizationLevel[] VALUES = values();

    @Override
    public String asString() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public final float degradationChanceMultiplier;

    OxidizationLevel(float degradationChanceMultiplier) {
        this.degradationChanceMultiplier = degradationChanceMultiplier;
    }
}
