package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.StringIdentifiable;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum PlacementDeletionMode implements IConfigOptionListEntry, StringIdentifiable
{
    MATCHING_BLOCK          ("matching_block",      "litematica.gui.label.placement_deletion_mode.matching_block"),
    NON_MATCHING_BLOCK      ("non_matching_block",  "litematica.gui.label.placement_deletion_mode.non_matching_block"),
    ANY_SCHEMATIC_BLOCK     ("any_schematic_block", "litematica.gui.label.placement_deletion_mode.any_schematic_block"),
    NO_SCHEMATIC_BLOCK      ("no_schematic_block",  "litematica.gui.label.placement_deletion_mode.no_schematic_block"),
    ENTIRE_VOLUME           ("entire_volume",       "litematica.gui.label.placement_deletion_mode.entire_volume");

    public static final StringIdentifiable.EnumCodec<PlacementDeletionMode> CODEC = StringIdentifiable.createCodec(PlacementDeletionMode::values);
    public static final ImmutableList<PlacementDeletionMode> VALUES = ImmutableList.copyOf(values());
    private final String configString;
    private final String translationKey;

    PlacementDeletionMode(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public String asString()
    {
        return this.configString;
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        int id = this.ordinal();

        if (forward)
        {
            if (++id >= values().length)
            {
                id = 0;
            }
        }
        else
        {
            if (--id < 0)
            {
                id = values().length - 1;
            }
        }

        return values()[id % values().length];
    }

    @Override
    public PlacementDeletionMode fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static PlacementDeletionMode fromStringStatic(String name)
    {
        for (PlacementDeletionMode val : PlacementDeletionMode.values())
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return PlacementDeletionMode.ENTIRE_VOLUME;
    }
}
