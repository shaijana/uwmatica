package fi.dy.masa.litematica.schematic.conversion;

import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;

import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.litematica.mixin.block.IMixinFenceGateBlock;
import fi.dy.masa.litematica.mixin.block.IMixinRedstoneWireBlock;
import fi.dy.masa.litematica.mixin.block.IMixinStairsBlock;
import fi.dy.masa.litematica.mixin.block.IMixinVineBlock;

public class SchematicConversionFixers
{
    private static final BooleanProperty[] HORIZONTAL_CONNECTING_BLOCK_PROPS = new BooleanProperty[] { null, null, HorizontalConnectingBlock.NORTH, HorizontalConnectingBlock.SOUTH, HorizontalConnectingBlock.WEST, HorizontalConnectingBlock.EAST };
    private static final BlockState REDSTONE_WIRE_DOT_OLD = Blocks.REDSTONE_WIRE.getDefaultState();
    private static final BlockState REDSTONE_WIRE_DOT = Blocks.REDSTONE_WIRE.getDefaultState()
                          .with(RedstoneWireBlock.POWER, 0)
                          .with(RedstoneWireBlock.WIRE_CONNECTION_NORTH, WireConnection.NONE)
                          .with(RedstoneWireBlock.WIRE_CONNECTION_EAST, WireConnection.NONE)
                          .with(RedstoneWireBlock.WIRE_CONNECTION_SOUTH, WireConnection.NONE)
                          .with(RedstoneWireBlock.WIRE_CONNECTION_WEST, WireConnection.NONE);
    private static final BlockState REDSTONE_WIRE_CROSS = Blocks.REDSTONE_WIRE.getDefaultState()
                          .with(RedstoneWireBlock.WIRE_CONNECTION_NORTH, WireConnection.SIDE)
                          .with(RedstoneWireBlock.WIRE_CONNECTION_EAST, WireConnection.SIDE)
                          .with(RedstoneWireBlock.WIRE_CONNECTION_SOUTH, WireConnection.SIDE)
                          .with(RedstoneWireBlock.WIRE_CONNECTION_WEST, WireConnection.SIDE);

    public static final IStateFixer FIXER_BANNER = (reader, state, pos) -> {
        NbtCompound tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("Base"))
        {
            DyeColor colorOrig = ((AbstractBannerBlock) state.getBlock()).getColor();
            DyeColor colorFromData = DyeColor.byIndex(15 - tag.getInt("Base", 0));

            if (colorOrig != colorFromData)
            {
                Integer rotation = state.get(BannerBlock.ROTATION);

                switch (colorFromData)
                {
                    case WHITE:         state = Blocks.WHITE_BANNER.getDefaultState();      break;
                    case ORANGE:        state = Blocks.ORANGE_BANNER.getDefaultState();     break;
                    case MAGENTA:       state = Blocks.MAGENTA_BANNER.getDefaultState();    break;
                    case LIGHT_BLUE:    state = Blocks.LIGHT_BLUE_BANNER.getDefaultState(); break;
                    case YELLOW:        state = Blocks.YELLOW_BANNER.getDefaultState();     break;
                    case LIME:          state = Blocks.LIME_BANNER.getDefaultState();       break;
                    case PINK:          state = Blocks.PINK_BANNER.getDefaultState();       break;
                    case GRAY:          state = Blocks.GRAY_BANNER.getDefaultState();       break;
                    case LIGHT_GRAY:    state = Blocks.LIGHT_GRAY_BANNER.getDefaultState(); break;
                    case CYAN:          state = Blocks.CYAN_BANNER.getDefaultState();       break;
                    case PURPLE:        state = Blocks.PURPLE_BANNER.getDefaultState();     break;
                    case BLUE:          state = Blocks.BLUE_BANNER.getDefaultState();       break;
                    case BROWN:         state = Blocks.BROWN_BANNER.getDefaultState();      break;
                    case GREEN:         state = Blocks.GREEN_BANNER.getDefaultState();      break;
                    case RED:           state = Blocks.RED_BANNER.getDefaultState();        break;
                    case BLACK:         state = Blocks.BLACK_BANNER.getDefaultState();      break;
                }

                state = state.with(BannerBlock.ROTATION, rotation);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_BANNER_WALL = (reader, state, pos) -> {
        NbtCompound tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("Base"))
        {
            DyeColor colorOrig = ((AbstractBannerBlock) state.getBlock()).getColor();
            DyeColor colorFromData = DyeColor.byIndex(15 - tag.getInt("Base", 0));

            if (colorOrig != colorFromData)
            {
                Direction facing = state.get(WallBannerBlock.FACING);

                switch (colorFromData)
                {
                    case WHITE:         state = Blocks.WHITE_WALL_BANNER.getDefaultState();      break;
                    case ORANGE:        state = Blocks.ORANGE_WALL_BANNER.getDefaultState();     break;
                    case MAGENTA:       state = Blocks.MAGENTA_WALL_BANNER.getDefaultState();    break;
                    case LIGHT_BLUE:    state = Blocks.LIGHT_BLUE_WALL_BANNER.getDefaultState(); break;
                    case YELLOW:        state = Blocks.YELLOW_WALL_BANNER.getDefaultState();     break;
                    case LIME:          state = Blocks.LIME_WALL_BANNER.getDefaultState();       break;
                    case PINK:          state = Blocks.PINK_WALL_BANNER.getDefaultState();       break;
                    case GRAY:          state = Blocks.GRAY_WALL_BANNER.getDefaultState();       break;
                    case LIGHT_GRAY:    state = Blocks.LIGHT_GRAY_WALL_BANNER.getDefaultState(); break;
                    case CYAN:          state = Blocks.CYAN_WALL_BANNER.getDefaultState();       break;
                    case PURPLE:        state = Blocks.PURPLE_WALL_BANNER.getDefaultState();     break;
                    case BLUE:          state = Blocks.BLUE_WALL_BANNER.getDefaultState();       break;
                    case BROWN:         state = Blocks.BROWN_WALL_BANNER.getDefaultState();      break;
                    case GREEN:         state = Blocks.GREEN_WALL_BANNER.getDefaultState();      break;
                    case RED:           state = Blocks.RED_WALL_BANNER.getDefaultState();        break;
                    case BLACK:         state = Blocks.BLACK_WALL_BANNER.getDefaultState();      break;
                }

                state = state.with(WallBannerBlock.FACING, facing);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_BED = (reader, state, pos) -> {
        NbtCompound tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("color"))
        {
            int colorId = tag.getInt("color", -1);
            Direction facing = state.get(BedBlock.FACING);
            BedPart part = state.get(BedBlock.PART);
            Boolean occupied = state.get(BedBlock.OCCUPIED);

            switch (colorId)
            {
                case  0: state = Blocks.WHITE_BED.getDefaultState(); break;
                case  1: state = Blocks.ORANGE_BED.getDefaultState(); break;
                case  2: state = Blocks.MAGENTA_BED.getDefaultState(); break;
                case  3: state = Blocks.LIGHT_BLUE_BED.getDefaultState(); break;
                case  4: state = Blocks.YELLOW_BED.getDefaultState(); break;
                case  5: state = Blocks.LIME_BED.getDefaultState(); break;
                case  6: state = Blocks.PINK_BED.getDefaultState(); break;
                case  7: state = Blocks.GRAY_BED.getDefaultState(); break;
                case  8: state = Blocks.LIGHT_GRAY_BED.getDefaultState(); break;
                case  9: state = Blocks.CYAN_BED.getDefaultState(); break;
                case 10: state = Blocks.PURPLE_BED.getDefaultState(); break;
                case 11: state = Blocks.BLUE_BED.getDefaultState(); break;
                case 12: state = Blocks.BROWN_BED.getDefaultState(); break;
                case 13: state =  Blocks.GREEN_BED.getDefaultState(); break;
                case 14: state = Blocks.RED_BED.getDefaultState(); break;
                case 15: state = Blocks.BLACK_BED.getDefaultState(); break;
                default: return state;
            }

            state = state.with(BedBlock.FACING, facing)
                         .with(BedBlock.PART, part)
                         .with(BedBlock.OCCUPIED, occupied);
        }

        return state;
    };

    public static final IStateFixer FIXER_CHRORUS_PLANT = (reader, state, pos) -> ChorusPlantBlock.withConnectionProperties(reader, pos, state);

    public static final IStateFixer FIXER_DIRT_SNOWY = (reader, state, pos) -> {
        Block block = reader.getBlockState(pos.up()).getBlock();
        return state.with(SnowyBlock.SNOWY, (block == Blocks.SNOW_BLOCK || block == Blocks.SNOW));
    };

    public static final IStateFixer FIXER_DOOR = (reader, state, pos) -> {
        if (state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER)
        {
            BlockState stateLower = reader.getBlockState(pos.down());

            if (stateLower.getBlock() == state.getBlock())
            {
                state = state.with(DoorBlock.FACING, stateLower.get(DoorBlock.FACING));
                state = state.with(DoorBlock.OPEN,   stateLower.get(DoorBlock.OPEN));
            }
        }
        else
        {
            BlockState stateUpper = reader.getBlockState(pos.up());

            if (stateUpper.getBlock() == state.getBlock())
            {
                state = state.with(DoorBlock.HINGE,   stateUpper.get(DoorBlock.HINGE));
                state = state.with(DoorBlock.POWERED, stateUpper.get(DoorBlock.POWERED));
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_DOUBLE_PLANT = (reader, state, pos) -> {
        if (state.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER)
        {
            BlockState stateLower = reader.getBlockState(pos.down());

            if (stateLower.getBlock() instanceof TallPlantBlock)
            {
                state = stateLower.with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_FENCE = (reader, state, pos) -> {
        FenceBlock fence = (FenceBlock) state.getBlock();

        for (Direction side : fi.dy.masa.malilib.util.position.PositionUtils.HORIZONTAL_DIRECTIONS)
        {
            BlockPos posAdj = pos.offset(side);
            BlockState stateAdj = reader.getBlockState(posAdj);
            Direction sideOpposite = side.getOpposite();
            boolean flag = stateAdj.isSideSolidFullSquare(reader, posAdj, sideOpposite);
            state = state.with(HORIZONTAL_CONNECTING_BLOCK_PROPS[side.getIndex()], fence.canConnect(stateAdj, flag, sideOpposite));
        }

        return state;
    };

    public static final IStateFixer FIXER_FENCE_GATE = (reader, state, pos) -> {
        FenceGateBlock gate = (FenceGateBlock) state.getBlock();
        Direction facing = state.get(FenceGateBlock.FACING);
        boolean inWall = false;

        if (facing.getAxis() == Direction.Axis.X)
        {
            inWall = (((IMixinFenceGateBlock) gate).litematica_invokeIsWall(reader.getBlockState(pos.offset(Direction.NORTH)))
                   || ((IMixinFenceGateBlock) gate).litematica_invokeIsWall(reader.getBlockState(pos.offset(Direction.SOUTH))));
        }
        else
        {
            inWall = (((IMixinFenceGateBlock) gate).litematica_invokeIsWall(reader.getBlockState(pos.offset(Direction.WEST)))
                   || ((IMixinFenceGateBlock) gate).litematica_invokeIsWall(reader.getBlockState(pos.offset(Direction.EAST))));
        }

        return state.with(FenceGateBlock.IN_WALL, inWall);
    };

    public static final IStateFixer FIXER_FIRE = (reader, state, pos) -> {
        return AbstractFireBlock.getState(reader, pos);
    };

    public static final IStateFixer FIXER_FLOWER_POT = (reader, state, pos) -> {
        NbtCompound tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("Item"))
        {
            String itemName = tag.getString("Item", "");

            if (itemName.length() > 0 && tag.contains("Data"))
            {
                int meta = tag.getInt("Data", 0);

                switch (itemName)
                {
                    case "minecraft:sapling":
                        if (meta == 0)      return Blocks.POTTED_OAK_SAPLING.getDefaultState();
                        if (meta == 1)      return Blocks.POTTED_SPRUCE_SAPLING.getDefaultState();
                        if (meta == 2)      return Blocks.POTTED_BIRCH_SAPLING.getDefaultState();
                        if (meta == 3)      return Blocks.POTTED_JUNGLE_SAPLING.getDefaultState();
                        if (meta == 4)      return Blocks.POTTED_ACACIA_SAPLING.getDefaultState();
                        if (meta == 5)      return Blocks.POTTED_DARK_OAK_SAPLING.getDefaultState();
                        break;
                    case "minecraft:tallgrass":
                        if (meta == 0)      return Blocks.POTTED_DEAD_BUSH.getDefaultState();
                        if (meta == 2)      return Blocks.POTTED_FERN.getDefaultState();
                        break;
                    case "minecraft:red_flower":
                        if (meta == 0)      return Blocks.POTTED_POPPY.getDefaultState();
                        if (meta == 1)      return Blocks.POTTED_BLUE_ORCHID.getDefaultState();
                        if (meta == 2)      return Blocks.POTTED_ALLIUM.getDefaultState();
                        if (meta == 3)      return Blocks.POTTED_AZURE_BLUET.getDefaultState();
                        if (meta == 4)      return Blocks.POTTED_RED_TULIP.getDefaultState();
                        if (meta == 5)      return Blocks.POTTED_ORANGE_TULIP.getDefaultState();
                        if (meta == 6)      return Blocks.POTTED_WHITE_TULIP.getDefaultState();
                        if (meta == 7)      return Blocks.POTTED_PINK_TULIP.getDefaultState();
                        if (meta == 8)      return Blocks.POTTED_OXEYE_DAISY.getDefaultState();
                        break;
                    case "minecraft:yellow_flower":     return Blocks.POTTED_DANDELION.getDefaultState();
                    case "minecraft:brown_mushroom":    return Blocks.POTTED_BROWN_MUSHROOM.getDefaultState();
                    case "minecraft:red_mushroom":      return Blocks.POTTED_RED_MUSHROOM.getDefaultState();
                    case "minecraft:deadbush":          return Blocks.POTTED_DEAD_BUSH.getDefaultState();
                    case "minecraft:cactus":            return Blocks.POTTED_CACTUS.getDefaultState();
                    default:                            return state;
                }
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_NOTE_BLOCK = (reader, state, pos) -> {
        NbtCompound tag = reader.getBlockEntityData(pos);

        if (tag != null)
        {
            state = state
                        .with(NoteBlock.POWERED, tag.getBoolean("powered", false))
                        .with(NoteBlock.NOTE, MathHelper.clamp(tag.getByte("note", (byte) 0), 0, 24))
                        .with(NoteBlock.INSTRUMENT, reader.getBlockState(pos.down()).getInstrument());
        }

        return state;
    };

    public static final IStateFixer FIXER_PANE = (reader, state, pos) -> {
        PaneBlock pane = (PaneBlock) state.getBlock();

        for (Direction side : fi.dy.masa.malilib.util.position.PositionUtils.HORIZONTAL_DIRECTIONS)
        {
            BlockPos posAdj = pos.offset(side);
            BlockState stateAdj = reader.getBlockState(posAdj);
            Direction sideOpposite = side.getOpposite();
            boolean flag = stateAdj.isSideSolidFullSquare(reader, posAdj, sideOpposite);
            state = state.with(HORIZONTAL_CONNECTING_BLOCK_PROPS[side.getIndex()], pane.connectsTo(stateAdj, flag));
        }

        return state;
    };

    public static final IStateFixer FIXER_REDSTONE_REPEATER = (reader, state, pos) -> {
        return state.with(RepeaterBlock.LOCKED, Boolean.valueOf(getIsRepeaterPoweredOnSide(reader, pos, state)));
    };

    public static final IStateFixer FIXER_REDSTONE_WIRE = (reader, state, pos) -> {
        RedstoneWireBlock wire = (RedstoneWireBlock) state.getBlock();
        BlockState stateAdj = ((IMixinRedstoneWireBlock) wire).litematica_GetPlacementState(reader, state, pos);

        if (stateAdj.equals(state) == false)
        {
            // Vanilla breaks Redstone connections when it doesn't detect the neighbor being powered
            stateAdj = state;
        }
        // Turn all old dots into crosses, while keeping the power level
        if (stateAdj.equals(REDSTONE_WIRE_DOT) == false && stateAdj.with(RedstoneWireBlock.POWER, 0) == REDSTONE_WIRE_DOT_OLD)
        {
            stateAdj = REDSTONE_WIRE_CROSS.with(RedstoneWireBlock.POWER, stateAdj.get(RedstoneWireBlock.POWER));
        }

        return stateAdj;
    };

    public static final IStateFixer FIXER_SIGN = (reader, state, pos) -> {
        NbtCompound tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("Text1"))
        {
            NbtList textList = new NbtList();
            textList.add(tag.get("Text1"));
            textList.add(tag.get("Text2"));
            textList.add(tag.get("Text3"));
            textList.add(tag.get("Text4"));

            NbtCompound frontTextTag = new NbtCompound();
            frontTextTag.put("messages", textList);
            frontTextTag.putString("color", tag.getString("Color", ""));
            frontTextTag.putByte("has_glowing_text", tag.getByte("GlowingText", (byte) 0));

            tag.put("front_text", frontTextTag);

            tag.remove("Color");
            tag.remove("GlowingText");
            tag.remove("Text1");
            tag.remove("Text2");
            tag.remove("Text3");
            tag.remove("Text4");
        }

        return state;
    };

    public static final IStateFixer FIXER_SKULL = (reader, state, pos) -> {
        NbtCompound tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("SkullType"))
        {
            int id = MathHelper.clamp(tag.getByte("SkullType", (byte) 0), 0, 5);

            // ;_; >_> <_<
            if (id == 2) { id = 3; } else if (id == 3) { id = 2; }

            SkullBlock.SkullType typeOrig = ((AbstractSkullBlock) state.getBlock()).getSkullType();
            SkullBlock.SkullType typeFromData = SkullBlock.Type.values()[id];

            if (typeOrig != typeFromData)
            {
                if (typeFromData == SkullBlock.Type.SKELETON)
                {
                    state = Blocks.SKELETON_SKULL.getDefaultState();
                }
                else if (typeFromData == SkullBlock.Type.WITHER_SKELETON)
                {
                    state = Blocks.WITHER_SKELETON_SKULL.getDefaultState();
                }
                else if (typeFromData == SkullBlock.Type.PLAYER)
                {
                    state = Blocks.PLAYER_HEAD.getDefaultState();
                }
                else if (typeFromData == SkullBlock.Type.ZOMBIE)
                {
                    state = Blocks.ZOMBIE_HEAD.getDefaultState();
                }
                else if (typeFromData == SkullBlock.Type.CREEPER)
                {
                    state = Blocks.CREEPER_HEAD.getDefaultState();
                }
                else if (typeFromData == SkullBlock.Type.DRAGON)
                {
                    state = Blocks.DRAGON_HEAD.getDefaultState();
                }
            }

            state = state.with(BannerBlock.ROTATION, MathHelper.clamp(tag.getByte("Rot", (byte) 0), 0, 15));
        }

        return state;
    };

    public static final IStateFixer FIXER_SKULL_WALL = (reader, state, pos) -> {
        NbtCompound tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("SkullType"))
        {
            int id = MathHelper.clamp(tag.getByte("SkullType", (byte) 0), 0, 5);

            // ;_; >_> <_<
            if (id == 2) { id = 3; } else if (id == 3) { id = 2; }

            SkullBlock.SkullType typeOrig = ((AbstractSkullBlock) state.getBlock()).getSkullType();
            SkullBlock.SkullType typeFromData = SkullBlock.Type.values()[id];

            if (typeOrig != typeFromData)
            {
                Direction facing = state.get(WallSkullBlock.FACING);

                if (typeFromData == SkullBlock.Type.SKELETON)
                {
                    state = Blocks.SKELETON_WALL_SKULL.getDefaultState();
                }
                else if (typeFromData == SkullBlock.Type.WITHER_SKELETON)
                {
                    state = Blocks.WITHER_SKELETON_WALL_SKULL.getDefaultState();
                }
                else if (typeFromData == SkullBlock.Type.PLAYER)
                {
                    state = Blocks.PLAYER_WALL_HEAD.getDefaultState();
                }
                else if (typeFromData == SkullBlock.Type.ZOMBIE)
                {
                    state = Blocks.ZOMBIE_WALL_HEAD.getDefaultState();
                }
                else if (typeFromData == SkullBlock.Type.CREEPER)
                {
                    state = Blocks.CREEPER_WALL_HEAD.getDefaultState();
                }
                else if (typeFromData == SkullBlock.Type.DRAGON)
                {
                    state = Blocks.DRAGON_WALL_HEAD.getDefaultState();
                }

                state = state.with(WallSkullBlock.FACING, facing);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_STAIRS = (reader, state, pos) -> {
        return state.with(StairsBlock.SHAPE, IMixinStairsBlock.litematica_invokeGetStairShape(state, reader, pos));
    };

    public static final IStateFixer FIXER_STEM = (reader, state, pos) -> {
        /* FIXME 1.20.3 - the gourd block and attached stem are now RegistryKey<Block>, plus they are private...
        StemBlock stem = (StemBlock) state.getBlock();
        GourdBlock crop = stem.getGourdBlock();

        for (Direction side : fi.dy.masa.malilib.util.PositionUtils.HORIZONTAL_DIRECTIONS)
        {
            BlockPos posAdj = pos.offset(side);
            BlockState stateAdj = reader.getBlockState(posAdj);
            Block blockAdj = stateAdj.getBlock();

            if (blockAdj == crop || (stem == Blocks.PUMPKIN_STEM && blockAdj == Blocks.CARVED_PUMPKIN))
            {
                return crop.getAttachedStem().getDefaultState().with(AttachedStemBlock.FACING, side);
            }
        }
        */

        return state;
    };

    public static final IStateFixer FIXER_TRIPWIRE = (reader, state, pos) -> {
        TripwireBlock wire = (TripwireBlock) state.getBlock();

        return state
                .with(TripwireBlock.NORTH, wire.shouldConnectTo(reader.getBlockState(pos.north()), Direction.NORTH))
                .with(TripwireBlock.SOUTH, wire.shouldConnectTo(reader.getBlockState(pos.south()), Direction.SOUTH))
                .with(TripwireBlock.WEST, wire.shouldConnectTo(reader.getBlockState(pos.west()), Direction.WEST))
                .with(TripwireBlock.EAST, wire.shouldConnectTo(reader.getBlockState(pos.east()), Direction.EAST));
    };

    public static final IStateFixer FIXER_VINE = (reader, state, pos) -> {
        VineBlock vine = (VineBlock) state.getBlock();
        return state.with(VineBlock.UP, ((IMixinVineBlock) vine).litematica_invokeShouldConnectUp(reader, pos.up(), Direction.UP));
    };

    private static boolean getIsRepeaterPoweredOnSide(BlockView reader, BlockPos pos, BlockState stateRepeater)
    {
        Direction facing = stateRepeater.get(RepeaterBlock.FACING);
        Direction sideLeft = facing.rotateYCounterclockwise();
        Direction sideRight = facing.rotateYClockwise();

        return getRepeaterPowerOnSide(reader, pos.offset(sideLeft) , sideLeft ) > 0 ||
               getRepeaterPowerOnSide(reader, pos.offset(sideRight), sideRight) > 0;
    }

    private static int getRepeaterPowerOnSide(BlockView reader, BlockPos pos, Direction side)
    {
        BlockState state = reader.getBlockState(pos);
        Block block = state.getBlock();

        if (AbstractRedstoneGateBlock.isRedstoneGate(state))
        {
            if (block == Blocks.REDSTONE_BLOCK)
            {
                return 15;
            }
            else
            {
                return block == Blocks.REDSTONE_WIRE ? state.get(RedstoneWireBlock.POWER) : state.getStrongRedstonePower(reader, pos, side);
            }
        }
        else
        {
            return 0;
        }
    }

    public interface IStateFixer
    {
        BlockState fixState(IBlockReaderWithData reader, BlockState state, BlockPos pos);
    }
}
