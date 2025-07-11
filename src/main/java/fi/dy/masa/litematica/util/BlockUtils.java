package fi.dy.masa.litematica.util;

import java.util.Iterator;
import java.util.Optional;
import javax.annotation.Nullable;
import com.google.common.base.Splitter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public class BlockUtils
{
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');
    private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);

    public static BlockState fixMirrorDoubleChest(BlockState state, BlockMirror mirror, ChestType type)
    {
        Direction facing = state.get(ChestBlock.FACING);
        Direction.Axis axis = facing.getAxis();

        if (mirror == BlockMirror.FRONT_BACK) // x
        {
            state = state.with(ChestBlock.CHEST_TYPE, type.getOpposite());

            if (axis == Direction.Axis.X)
            {
                state = state.with(ChestBlock.FACING, facing.getOpposite());
            }
        }
        else if (mirror == BlockMirror.LEFT_RIGHT) // z
        {
            state = state.with(ChestBlock.CHEST_TYPE, type.getOpposite());

            if (axis == Direction.Axis.Z)
            {
                state = state.with(ChestBlock.FACING, facing.getOpposite());
            }
        }

        return state;
    }

    /**
     * Parses the provided string into the full block state.<br>
     * The string should be in either one of the following formats:<br>
     * 'minecraft:stone' or 'minecraft:smooth_stone_slab[half=top,waterlogged=false]'
     */
    public static Optional<BlockState> getBlockStateFromString(String str)
    {
        int index = str.indexOf("["); // [f=b]
        String blockName = index != -1 ? str.substring(0, index) : str;

        try
        {
            Identifier id = Identifier.tryParse(blockName);

            if (Registries.BLOCK.containsId(id))
            {
                Optional<RegistryEntry.Reference<Block>> opt = Registries.BLOCK.getEntry(id);
                Block block;

                if (opt.isPresent())
                {
                    block = opt.get().value();
                    BlockState state = block.getDefaultState();

                    if (index != -1 && str.length() > (index + 4) && str.charAt(str.length() - 1) == ']')
                    {
                        StateManager<Block, BlockState> stateManager = block.getStateManager();
                        String propStr = str.substring(index + 1, str.length() - 1);

                        for (String propAndVal : COMMA_SPLITTER.split(propStr))
                        {
                            Iterator<String> valIter = EQUAL_SPLITTER.split(propAndVal).iterator();

                            if (valIter.hasNext() == false)
                            {
                                continue;
                            }

                            Property<?> prop = stateManager.getProperty(valIter.next());

                            if (prop == null || valIter.hasNext() == false)
                            {
                                continue;
                            }

                            Comparable<?> val = getPropertyValueByName(prop, valIter.next());

                            if (val != null)
                            {
                                state = getBlockStateWithProperty(state, prop, val);
                            }
                        }
                    }

                    return Optional.of(state);
                }
            }
        }
        catch (Exception e)
        {
            return Optional.empty();
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> BlockState getBlockStateWithProperty(BlockState state, Property<T> prop, Comparable<?> value)
    {
        return state.with(prop, (T) value);
    }

    @Nullable
    public static <T extends Comparable<T>> T getPropertyValueByName(Property<T> prop, String valStr)
    {
        return prop.parse(valStr).orElse(null);
    }

    public static boolean blocksHaveSameProperties(BlockState state1, BlockState state2)
    {
        StateManager<Block, BlockState> stateManager1 = state1.getBlock().getStateManager();
        StateManager<Block, BlockState> stateManager2 = state2.getBlock().getStateManager();
        return stateManager1.getProperties().equals(stateManager2.getProperties());
    }

    public static Optional<Block> getBlockFromString(String str)
    {
        int index = str.indexOf("["); // [f=b]
        String blockName = index != -1 ? str.substring(0, index) : str;

        try
        {
            Identifier id = Identifier.tryParse(blockName);

            if (Registries.BLOCK.containsId(id))
            {
                Block block = Registries.BLOCK.get(id);

                return Optional.of(block);
            }
        }
        catch (Exception e)
        {
            return Optional.empty();
        }

        return Optional.empty();
    }

    public static Optional<TagKey<Block>> getBlockTagFromString(String str)
    {
        if (str.startsWith("#")) {
            try {
                String tagName = str.substring(1);
                Identifier id = Identifier.tryParse(tagName);

                TagKey<Block> blockTag = TagKey.of(RegistryKeys.BLOCK, id);
                return Optional.of(blockTag);

            } catch (Exception e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
