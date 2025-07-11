package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.StringIdentifiable;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum BlockInfoListType implements IConfigOptionListEntry, StringIdentifiable
{
    ALL             ("all",             "litematica.gui.label.block_info_list_type.all"),
    RENDER_LAYERS   ("render_layers",   "litematica.gui.label.block_info_list_type.render_layers");

    public static final StringIdentifiable.EnumCodec<BlockInfoListType> CODEC = StringIdentifiable.createCodec(BlockInfoListType::values);
    public static final ImmutableList<BlockInfoListType> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    BlockInfoListType(String configString, String translationKey)
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
    public BlockInfoListType fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static BlockInfoListType fromStringStatic(String name)
    {
        for (BlockInfoListType mode : BlockInfoListType.values())
        {
            if (mode.configString.equalsIgnoreCase(name))
            {
                return mode;
            }
        }

        return BlockInfoListType.ALL;
    }
}
