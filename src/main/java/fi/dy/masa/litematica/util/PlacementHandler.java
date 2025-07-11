package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableSet;

import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;

public class PlacementHandler
{
    public static final ImmutableSet<Property<?>> WHITELISTED_PROPERTIES = ImmutableSet.of(
            // BooleanProperty:
            // INVERTED - DaylightDetector
            // OPEN - Barrel, Door, FenceGate, Trapdoor
            // PERSISTENT - Leaves (Disabled)
            Properties.INVERTED,
            Properties.OPEN,
            //Properties.PERSISTENT,
            //Properties.POWERED,
            //Properties.LOCKED,
            // EnumProperty:
            // ATTACHMENT - Bells
            // AXIS - Pillar
            // BLOCK_HALF - Stairs, Trapdoor
            // BLOCK_FACE - Button, Grindstone, Lever
            // CHEST_TYPE - Chest
            // COMPARATOR_MODE - Comparator
            // DOOR_HINGE - Door
            // ORIENTATION - Crafter
            // RAIL_SHAPE / STRAIGHT_RAIL_SHAPE - Rails
            // SLAB_TYPE - Slab - PARTIAL ONLY: TOP and BOTTOM, not DOUBLE
            // STAIR_SHAPE - Stairs (needed to get the correct state, otherwise the player facing would be a factor)
            // BLOCK_FACE - Button, Grindstone, Lever
            Properties.ATTACHMENT,
            Properties.AXIS,
            Properties.BLOCK_HALF,
            Properties.BLOCK_FACE,
            Properties.CHEST_TYPE,
            Properties.COMPARATOR_MODE,
            Properties.DOOR_HINGE,
            Properties.FACING,
            Properties.HORIZONTAL_FACING,
            Properties.HOPPER_FACING,
            Properties.ORIENTATION,
            Properties.RAIL_SHAPE,
            Properties.STRAIGHT_RAIL_SHAPE,
            Properties.SLAB_TYPE,
            Properties.STAIR_SHAPE,
            // IntProperty:
            // BITES - Cake
            // DELAY - Repeater
            // NOTE - NoteBlock
            // ROTATION - Banner, Sign, Skull
            Properties.BITES,
            Properties.DELAY,
            Properties.NOTE,
            Properties.ROTATION
    );

    /**
     * BlackList for Block States.  Entries here will be reset to their default value.
     */
    public static final ImmutableSet<Property<?>> BLACKLISTED_PROPERTIES = ImmutableSet.of(
            Properties.WATERLOGGED,
            Properties.POWERED
    );

    public static EasyPlaceProtocol getEffectiveProtocolVersion()
    {
        EasyPlaceProtocol protocol = (EasyPlaceProtocol) Configs.Generic.EASY_PLACE_PROTOCOL.getOptionListValue();

        if (protocol == EasyPlaceProtocol.AUTO)
        {
            if (MinecraftClient.getInstance().isInSingleplayer() || EntitiesDataStorage.getInstance().hasServuxServer())
            {
                return EasyPlaceProtocol.V3;
            }

            if (DataManager.isCarpetServer())
            {
                return EasyPlaceProtocol.V2;
            }

            return EasyPlaceProtocol.SLAB_ONLY;
        }

        return protocol;
    }

    @Nullable
    public static BlockState applyPlacementProtocolToPlacementState(BlockState state, UseContext context)
    {
        EasyPlaceProtocol protocol = getEffectiveProtocolVersion();

        if (protocol == EasyPlaceProtocol.V3)
        {
            return applyPlacementProtocolV3(state, context);
        }
        else if (protocol == EasyPlaceProtocol.V2)
        {
            return applyPlacementProtocolV2(state, context);
        }
        else
        {
            return state;
        }
    }

    public static BlockState applyPlacementProtocolV2(BlockState state, UseContext context)
    {
        int protocolValue = (int) (context.getHitVec().x - (double) context.getPos().getX()) - 2;

        if (protocolValue < 0)
        {
            return state;
        }

        Optional<EnumProperty<Direction>> property = BlockUtils.getFirstDirectionProperty(state);

        if (property.isPresent())
        {
            //System.out.printf("[PHv2] applying: 0x%08X (getFirstDirectionProperty() -> %s)\n", protocolValue, property.get().getName());
            state = applyDirectionProperty(state, context, property.get(), protocolValue);

            if (state == null)
            {
                return null;
            }
        }
        else if (state.contains(Properties.AXIS))
        {
            Direction.Axis axis = Direction.Axis.VALUES[((protocolValue >> 1) & 0x3) % 3];
            //System.out.printf("[PHv2] applying: 0x%08X (Axis -> %s)\n", protocolValue, axis.getName());

            if (Properties.AXIS.getValues().contains(axis))
            {
                state = state.with(Properties.AXIS, axis);
                //System.out.printf("[PHv2] axis stateOut: %s\n", state.toString());
            }
        }

        // Divide by two, and then remove the 4 bits used for the facing
        protocolValue >>>= 5;

        if (protocolValue > 0)
        {
            Block block = state.getBlock();

            if (block instanceof RepeaterBlock)
            {
                Integer delay = protocolValue;

                if (RepeaterBlock.DELAY.getValues().contains(delay))
                {
                    state = state.with(RepeaterBlock.DELAY, delay);
                }
            }
            else if (block instanceof ComparatorBlock)
            {
                state = state.with(ComparatorBlock.MODE, ComparatorMode.SUBTRACT);
            }
        }

        if (state.contains(Properties.BLOCK_HALF))
        {
            state = state.with(Properties.BLOCK_HALF, protocolValue > 0 ? BlockHalf.TOP : BlockHalf.BOTTOM);
        }
        
        //System.out.printf("[PHv2] stateOut: %s\n", state.toString());

        return state;
    }

    public static <T extends Comparable<T>> BlockState applyPlacementProtocolV3(BlockState state, UseContext context)
    {
        int protocolValue = (int) (context.getHitVec().x - (double) context.getPos().getX()) - 2;
        BlockState oldState = state;
        //System.out.printf("[PHv3] hit vec.x %s, pos.x: %s\n", context.getHitVec().getX(), context.getPos().getX());
        //System.out.printf("[PHv3] raw protocol value in: 0x%08X\n", protocolValue);

        if (protocolValue < 0)
        {
            return oldState;
        }

        Optional<EnumProperty<Direction>> property = BlockUtils.getFirstDirectionProperty(state);

        // DirectionProperty - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (property.isPresent() && property.get() != Properties.VERTICAL_DIRECTION)
        {
            //System.out.printf("[PHv3] applying: 0x%08X (getFirstDirectionProperty() -> %s)\n", protocolValue, property.get().getName());
            state = applyDirectionProperty(state, context, property.get(), protocolValue);

            if (state == null)
            {
                return null;
            }

            if (Configs.Generic.EASY_PLACE_SP_VALIDATION.getBooleanValue())
            {
                if (state.canPlaceAt(context.getWorld(), context.getPos()))
                {
                    //System.out.printf("[PHv3] validator passed for \"%s\"\n", property.get().getName());
                    oldState = state;
                }
                else
                {
                    //System.out.printf("[PHv3] validator failed for \"%s\"\n", property.get().getName());
                    state = oldState;
                }
            }
            else
            {
                oldState = state;
            }
            
            // Consume the bits used for the facing
            protocolValue >>>= 3;
        }

        // Consume the lowest unused bit
        protocolValue >>>= 1;

        List<Property<?>> propList = new ArrayList<>(state.getBlock().getStateManager().getProperties());
        propList.sort(Comparator.comparing(Property::getName));

        try
        {
            for (Property<?> p : propList)
            {
                //System.out.printf("[PHv3] check property [%s], whitelisted [%s], blacklisted [%s]\n", p.getName(), WHITELISTED_PROPERTIES.contains(p), BLACKLISTED_PROPERTIES.contains(p));

                if (property.isPresent() && property.get().equals(p))
                {
                    //System.out.printf("[PHv3] skipping prot val: 0x%08X [Property %s]\n", protocolValue, p.getName());
                    continue;
                }
                else if (WHITELISTED_PROPERTIES.contains(p) &&
                        !BLACKLISTED_PROPERTIES.contains(p))
                {
                    @SuppressWarnings("unchecked")
                    Property<T> prop = (Property<T>) p;
                    List<T> list = new ArrayList<>(prop.getValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = MathHelper.floorLog2(MathHelper.smallestEncompassingPowerOfTwo(list.size()));
                    int bitMask = ~(0xFFFFFFFF << requiredBits);
                    int valueIndex = protocolValue & bitMask;

                    //System.out.printf("[PHv3] trying to apply valInd: %d, bits: %d, prot val: 0x%08X [Property %s]\n", valueIndex, requiredBits, protocolValue, prop.getName());

                    if (valueIndex >= 0 && valueIndex < list.size())
                    {
                        T value = list.get(valueIndex);

                        if (state.get(prop).equals(value) == false &&
                            value != SlabType.DOUBLE) // don't allow duping slabs by forcing a double slab via the protocol
                        {
                            //System.out.printf("[PHv3] applying \"%s\": %s\n", prop.getName(), value);
                            state = state.with(prop, value);

                            if (Configs.Generic.EASY_PLACE_SP_VALIDATION.getBooleanValue())
                            {
                                if (state.canPlaceAt(context.getWorld(), context.getPos()))
                                {
                                    //System.out.printf("[PHv3] validator passed for \"%s\"\n", prop.getName());
                                    oldState = state;
                                }
                                else
                                {
                                    //System.out.printf("[PHv3] validator failed for \"%s\"\n", prop.getName());
                                    state = oldState;
                                }
                            }
                            else
                            {
                                oldState = state;
                            }
                        }

                        protocolValue >>>= requiredBits;
                    }
                }
                /*
                else
                {
                    System.out.printf("[PHv3] skipping prot val: 0x%08X [Property %s]\n", protocolValue, p.getName());
                }
                 */
            }
        }
        catch (Exception e)
        {
            Litematica.LOGGER.warn("Exception trying to apply placement protocol value", e);
        }

        // Strip Blacklisted properties, and use the Block's default state.
        // This needs to be done after the initial loop, or it breaks compatibility
        for (Property<?> p : BLACKLISTED_PROPERTIES)
        {
            if (state.contains(p))
            {
                @SuppressWarnings("unchecked")
                Property<T> prop = (Property<T>) p;
                BlockState def = state.getBlock().getDefaultState();
                state = state.with(prop, def.get(prop));
                //System.out.printf("[PHv3] blacklisted state [%s] found, setting default value\n", prop.getName());
            }
        }

        if (state.contains(Properties.WATERLOGGED) && (
            oldState.contains(Properties.WATERLOGGED) && oldState.get(Properties.WATERLOGGED) ||
            (oldState.getFluidState() != null && oldState.getFluidState().getFluid().matchesType(Fluids.WATER))
        ))
        {
            // Revert only if original state was waterlogged / Still Water already
            state = state.with(Properties.WATERLOGGED, true);
        }

        if (Configs.Generic.EASY_PLACE_SP_VALIDATION.getBooleanValue())
        {
            if (state.canPlaceAt(context.getWorld(), context.getPos()))
            {
                //System.out.printf("[PHv3] validator passed for \"%s\"\n", state);
                return state;
            }
            else
            {
                //System.out.printf("[PHv3] validator failed for \"%s\"\n", state);
                return null;
            }
        }

        return state;
    }

    private static BlockState applyDirectionProperty(BlockState state, UseContext context,
                                                     EnumProperty<Direction> property, int protocolValue)
    {
        Direction facingOrig = state.get(property);
        Direction facing = facingOrig;
        int decodedFacingIndex = (protocolValue & 0xF) >> 1;

        if (decodedFacingIndex == 6) // the opposite of the normal facing requested
        {
            facing = facing.getOpposite();
        }
        else if (decodedFacingIndex >= 0 && decodedFacingIndex <= 5)
        {
            facing = Direction.byIndex(decodedFacingIndex);

            if (property.getValues().contains(facing) == false)
            {
                facing = context.getEntity().getHorizontalFacing().getOpposite();
            }
        }

        //System.out.printf("plop facing: %s -> %s (raw: %d, dec: %d)\n", facingOrig, facing, protocolValue, decodedFacingIndex);

        if (facing != facingOrig && property.getValues().contains(facing))
        {
            if (state.getBlock() instanceof BedBlock)
            {
                BlockPos headPos = context.pos.offset(facing);
                ItemPlacementContext ctx = context.getItemPlacementContext();

                if (context.getWorld().getBlockState(headPos).canReplace(ctx) == false)
                {
                    return null;
                }
            }

            state = state.with(property, facing);
        }

        return state;
    }

    public static class UseContext
    {
        private final World world;
        private final BlockPos pos;
        private final Direction side;
        private final Vec3d hitVec;
        private final LivingEntity entity;
        private final Hand hand;
        @Nullable private final ItemPlacementContext itemPlacementContext;

        public UseContext(World world, BlockPos pos, Direction side, Vec3d hitVec,
                           LivingEntity entity, Hand hand, @Nullable ItemPlacementContext itemPlacementContext)
        {
            this.world = world;
            this.pos = pos;
            this.side = side;
            this.hitVec = hitVec;
            this.entity = entity;
            this.hand = hand;
            this.itemPlacementContext = itemPlacementContext;
        }

        /*
        public static UseContext of(World world, BlockPos pos, Direction side, Vec3d hitVec, LivingEntity entity, Hand hand)
        {
            return new UseContext(world, pos, side, hitVec, entity, hand, null);
        }
        */

        public static UseContext from(ItemPlacementContext ctx, Hand hand)
        {
            Vec3d pos = ctx.getHitPos();
            return new UseContext(ctx.getWorld(), ctx.getBlockPos(), ctx.getSide(), new Vec3d(pos.x, pos.y, pos.z),
                                  ctx.getPlayer(), hand, ctx);
        }

        public World getWorld()
        {
            return this.world;
        }

        public BlockPos getPos()
        {
            return this.pos;
        }

        public Direction getSide()
        {
            return this.side;
        }

        public Vec3d getHitVec()
        {
            return this.hitVec;
        }

        public LivingEntity getEntity()
        {
            return this.entity;
        }

        public Hand getHand()
        {
            return this.hand;
        }

        @Nullable
        public ItemPlacementContext getItemPlacementContext()
        {
            return this.itemPlacementContext;
        }
    }
}
