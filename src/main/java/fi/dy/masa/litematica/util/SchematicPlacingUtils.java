package fi.dy.masa.litematica.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.world.World;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.WorldTickScheduler;

import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.malilib.util.nbt.NbtView;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic.EntityInfo;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.world.WorldSchematic;

public class SchematicPlacingUtils
{
    public static boolean placeToWorldWithinChunk(World world,
                                                  ChunkPos chunkPos,
                                                  SchematicPlacement schematicPlacement,
                                                  ReplaceBehavior replace,
                                                  PasteLayerBehavior layerBehavior,
                                                  boolean notifyNeighbors)
    {
        LitematicaSchematic schematic = schematicPlacement.getSchematic();
        Set<String> regionsTouchingChunk = schematicPlacement.getRegionsTouchingChunk(chunkPos.x, chunkPos.z);
        BlockPos origin = schematicPlacement.getOrigin();
        boolean allSuccess = true;

        // Don't enable selective pasting while loading the schematic to the schematic world
        // since this function has a dual purpose; this would cause things to fail to load.
        if (world instanceof WorldSchematic &&
            layerBehavior != PasteLayerBehavior.ALL)
        {
            layerBehavior = PasteLayerBehavior.ALL;
        }

        try
        {
            if (notifyNeighbors == false)
            {
                WorldUtils.setShouldPreventBlockUpdates(world, true);
            }

            for (String regionName : regionsTouchingChunk)
            {
                LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

                if (container == null)
                {
                    allSuccess = false;
                    continue;
                }

                SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);

                if (placement.isEnabled())
                {
                    Map<BlockPos, NbtCompound> blockEntityMap = schematic.getBlockEntityMapForRegion(regionName);
                    Map<BlockPos, OrderedTick<Block>> scheduledBlockTicks = schematic.getScheduledBlockTicksForRegion(regionName);
                    Map<BlockPos, OrderedTick<Fluid>> scheduledFluidTicks = schematic.getScheduledFluidTicksForRegion(regionName);

                    if (placeBlocksWithinChunk(world, chunkPos, regionName, container, blockEntityMap,
                                               origin, schematicPlacement, placement, scheduledBlockTicks,
                                               scheduledFluidTicks, replace, layerBehavior, notifyNeighbors) == false)
                    {
                        allSuccess = false;
                        Litematica.LOGGER.warn("Invalid/missing schematic data in schematic '{}' for sub-region '{}'", schematic.getMetadata().getName(), regionName);
                    }

                    List<EntityInfo> entityList = schematic.getEntityListForRegion(regionName);

                    if (schematicPlacement.ignoreEntities() == false &&
                        placement.ignoreEntities() == false && entityList != null)
                    {
                        placeEntitiesToWorldWithinChunk(world, chunkPos, entityList, origin, schematicPlacement, placement, layerBehavior);
                    }
                }
            }
        }
        finally
        {
            WorldUtils.setShouldPreventBlockUpdates(world, false);
        }

        return allSuccess;
    }

    public static boolean placeBlocksWithinChunk(World world, ChunkPos chunkPos, String regionName,
                                                 LitematicaBlockStateContainer container,
                                                 Map<BlockPos, NbtCompound> blockEntityMap,
                                                 BlockPos origin,
                                                 SchematicPlacement schematicPlacement,
                                                 SubRegionPlacement placement,
                                                 @Nullable Map<BlockPos, OrderedTick<Block>> scheduledBlockTicks,
                                                 @Nullable Map<BlockPos, OrderedTick<Fluid>> scheduledFluidTicks,
                                                 ReplaceBehavior replace,
                                                 PasteLayerBehavior layerBehavior,
                                                 boolean notifyNeighbors)
    {
        IntBoundingBox bounds = schematicPlacement.getBoxWithinChunkForRegion(regionName, chunkPos.x, chunkPos.z);
        Vec3i regionSize = schematicPlacement.getSchematic().getAreaSizeAsVec3i(regionName);

        if (bounds == null || container == null || blockEntityMap == null || regionSize == null)
        {
            return false;
        }

        BlockPos regionPos = placement.getPos();

        // These are the untransformed relative positions
        BlockPos posEndRel = (new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize))).add(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

        // The transformed sub-region origin position
        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // The relative offset of the affected region's corners, to the sub-region's origin corner
        BlockPos boxMinRel = new BlockPos(bounds.minX - origin.getX() - regionPosTransformed.getX(), 0, bounds.minZ - origin.getZ() - regionPosTransformed.getZ());
        BlockPos boxMaxRel = new BlockPos(bounds.maxX - origin.getX() - regionPosTransformed.getX(), 0, bounds.maxZ - origin.getZ() - regionPosTransformed.getZ());

        // Reverse transform that relative offset, to get the untransformed orientation's offsets
        boxMinRel = PositionUtils.getReverseTransformedBlockPos(boxMinRel, placement.getMirror(), placement.getRotation());
        boxMaxRel = PositionUtils.getReverseTransformedBlockPos(boxMaxRel, placement.getMirror(), placement.getRotation());

        boxMinRel = PositionUtils.getReverseTransformedBlockPos(boxMinRel, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        boxMaxRel = PositionUtils.getReverseTransformedBlockPos(boxMaxRel, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // Get the offset relative to the sub-region's minimum corner, instead of the origin corner (which can be at any corner)
        boxMinRel = boxMinRel.subtract(posMinRel.subtract(regionPos));
        boxMaxRel = boxMaxRel.subtract(posMinRel.subtract(regionPos));

        BlockPos posMin = PositionUtils.getMinCorner(boxMinRel, boxMaxRel);
        BlockPos posMax = PositionUtils.getMaxCorner(boxMinRel, boxMaxRel);

        final int startX = posMin.getX();
        final int startZ = posMin.getZ();
        final int endX = posMax.getX();
        final int endZ = posMax.getZ();

        final int startY = 0;
        final int endY = Math.abs(regionSize.getY()) - 1;
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

        //System.out.printf("sx: %d, sy: %d, sz: %d => ex: %d, ey: %d, ez: %d\n", startX, startY, startZ, endX, endY, endZ);

        if (startX < 0 || startZ < 0 || endX >= container.getSize().getX() || endZ >= container.getSize().getZ())
        {
            System.out.printf("DEBUG ============= OUT OF BOUNDS - region: %s, sx: %d, sz: %d, ex: %d, ez: %d - size x: %d z: %d =============\n",
                              regionName, startX, startZ, endX, endZ, container.getSize().getX(), container.getSize().getZ());
            return false;
        }

        final BlockRotation rotationCombined = schematicPlacement.getRotation().rotate(placement.getRotation());
        final BlockMirror mirrorMain = schematicPlacement.getMirror();
        final BlockState barrier = Blocks.BARRIER.getDefaultState();
        BlockMirror mirrorSub = placement.getMirror();
        final boolean ignoreInventories = Configs.Generic.PASTE_IGNORE_INVENTORY.getBooleanValue();

        if (mirrorSub != BlockMirror.NONE &&
            (schematicPlacement.getRotation() == BlockRotation.CLOCKWISE_90 ||
            schematicPlacement.getRotation() == BlockRotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
        }

        final int posMinRelMinusRegX = posMinRel.getX() - regionPos.getX();
        final int posMinRelMinusRegY = posMinRel.getY() - regionPos.getY();
        final int posMinRelMinusRegZ = posMinRel.getZ() - regionPos.getZ();

//        dumpBlockEntityMap(blockEntityMap);

        for (int y = startY; y <= endY; ++y)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                for (int x = startX; x <= endX; ++x)
                {
                    BlockState state = container.get(x, y, z);

                    if (state.getBlock() == Blocks.STRUCTURE_VOID)
                    {
                        continue;
                    }

                    posMutable.set(x, y, z);
                    NbtCompound teNBT = blockEntityMap.get(posMutable);
                    BlockPos origPos = posMutable.toImmutable();

                    posMutable.set(posMinRelMinusRegX + x,
                                   posMinRelMinusRegY + y,
                                   posMinRelMinusRegZ + z);

                    BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                    pos = pos.add(regionPosTransformed).add(origin);

                    if (!shouldPasteBlock(pos, layerBehavior))
                    {
//                        Litematica.LOGGER.error("placeBlocksWithinChunk(): Skipping block at pos [{}]", pos.toShortString());
                        continue;
                    }

                    BlockState stateOld = world.getBlockState(pos);

                    if ((replace == ReplaceBehavior.NONE && stateOld.isAir() == false) ||
                        (replace == ReplaceBehavior.WITH_NON_AIR && state.isAir() == true))
                    {
                        continue;
                    }

                    // Fix inventory of adjacent chest sides when mirrored
                    if (state.hasBlockEntity() && state.isOf(Blocks.CHEST) &&
                        !ignoreInventories && mirrorMain != BlockMirror.NONE &&
                        !(state.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) &&
                        Configs.Generic.FIX_CHEST_MIRROR.getBooleanValue())
                    {
                        Direction facing = state.get(ChestBlock.FACING);
                        Direction.Axis axis = facing.getAxis();
                        ChestType type = state.get(ChestBlock.CHEST_TYPE).getOpposite();

                        if (mirrorMain != BlockMirror.NONE && axis != Direction.Axis.Y)
                        {
                            Direction facingAdj = type == ChestType.LEFT ? facing.rotateCounterclockwise(Direction.Axis.Y) : facing.rotateClockwise(Direction.Axis.Y);
                            BlockPos posAdj = origPos.offset(facingAdj);

                            if (blockEntityMap.containsKey(posAdj))
                            {
                                teNBT = blockEntityMap.getOrDefault(posAdj, teNBT).copy();
                            }
                        }
                    }

                    if (mirrorMain != BlockMirror.NONE) { state = state.mirror(mirrorMain); }
                    if (mirrorSub != BlockMirror.NONE)  { state = state.mirror(mirrorSub); }
                    if (rotationCombined != BlockRotation.NONE) { state = state.rotate(rotationCombined); }

                    BlockEntity te = world.getBlockEntity(pos);

                    if (te != null)
                    {
                        if (te instanceof Inventory)
                        {
                            ((Inventory) te).clear();
                        }

                        world.setBlockState(pos, barrier, 0x14);
                    }

                    if (world.setBlockState(pos, state, 0x12) && teNBT != null)
                    {
                        te = world.getBlockEntity(pos);

                        if (te != null)
                        {
                            teNBT = teNBT.copy();
                            teNBT.putInt("x", pos.getX());
                            teNBT.putInt("y", pos.getY());
                            teNBT.putInt("z", pos.getZ());

                            if (ignoreInventories)
                            {
                                teNBT.remove("Items");
                            }

                            try
                            {
                                NbtView view = NbtView.getReader(teNBT, world.getRegistryManager());
                                te.read(view.getReader());

                                if (ignoreInventories && te instanceof Inventory)
                                {
                                    ((Inventory) te).clear();
                                }
                            }
                            catch (Exception e)
                            {
                                Litematica.LOGGER.warn("Failed to load BlockEntity data for {} @ {}", state, pos);
                            }
                        }
                    }
                }
            }
        }

        if (world instanceof ServerWorld serverWorld)
        {
            IntBoundingBox box = new IntBoundingBox(startX, startY, startZ, endX, endY, endZ);

            if (scheduledBlockTicks != null && scheduledBlockTicks.isEmpty() == false)
            {
                WorldTickScheduler<Block> scheduler = serverWorld.getBlockTickScheduler();

                for (Map.Entry<BlockPos, OrderedTick<Block>> entry : scheduledBlockTicks.entrySet())
                {
                    BlockPos pos = entry.getKey();

                    if (box.containsPos(pos))
                    {
                        posMutable.set(posMinRelMinusRegX + pos.getX(),
                                       posMinRelMinusRegY + pos.getY(),
                                       posMinRelMinusRegZ + pos.getZ());

                        pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                        pos = pos.add(regionPosTransformed).add(origin);
                        OrderedTick<Block> tick = entry.getValue();

                        if (world.getBlockState(pos).getBlock() == tick.type())
                        {
                            scheduler.scheduleTick(new OrderedTick<>(tick.type(), pos, tick.triggerTick(), tick.priority(), tick.subTickOrder()));
                        }
                    }
                }
            }

            if (scheduledFluidTicks != null && scheduledFluidTicks.isEmpty() == false)
            {
                WorldTickScheduler<Fluid> scheduler = serverWorld.getFluidTickScheduler();

                for (Map.Entry<BlockPos, OrderedTick<Fluid>> entry : scheduledFluidTicks.entrySet())
                {
                    BlockPos pos = entry.getKey();

                    if (box.containsPos(pos))
                    {
                        posMutable.set(posMinRelMinusRegX + pos.getX(),
                                       posMinRelMinusRegY + pos.getY(),
                                       posMinRelMinusRegZ + pos.getZ());

                        pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                        pos = pos.add(regionPosTransformed).add(origin);
                        OrderedTick<Fluid> tick = entry.getValue();

                        if (world.getBlockState(pos).getFluidState().getFluid() == tick.type())
                        {
                            scheduler.scheduleTick(new OrderedTick<>(tick.type(), pos, tick.triggerTick(), tick.priority(), tick.subTickOrder()));
                        }
                    }
                }
            }
        }

        if (notifyNeighbors)
        {
            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.set(posMinRelMinusRegX + x,
                                       posMinRelMinusRegY + y,
                                       posMinRelMinusRegZ + z);
                        BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                        pos = pos.add(regionPosTransformed).add(origin);
                        world.updateNeighbors(pos, world.getBlockState(pos).getBlock());
                    }
                }
            }
        }

        return true;
    }

    private static void dumpBlockEntityMap(Map<BlockPos, NbtCompound> teMap)
    {
        System.out.print("DUMP TE-MAP:\n");

        for (BlockPos pos : teMap.keySet())
        {
            NbtCompound nbt = teMap.get(pos);

            System.out.printf("  pos[%s]: %s\n", pos.toShortString(), nbt.toString());
        }

        System.out.print("DUMP TE-MAP -- END\n");
    }

    public static void placeEntitiesToWorldWithinChunk(World world, ChunkPos chunkPos,
                                                       List<EntityInfo> entityList,
                                                       BlockPos origin,
                                                       SchematicPlacement schematicPlacement,
                                                       SubRegionPlacement placement,
                                                       PasteLayerBehavior layerBehavior)
    {
        BlockPos regionPos = placement.getPos();

        if (entityList == null)
        {
            return;
        }

        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        final int offX = regionPosRelTransformed.getX() + origin.getX();
        final int offY = regionPosRelTransformed.getY() + origin.getY();
        final int offZ = regionPosRelTransformed.getZ() + origin.getZ();
        final double minX = (chunkPos.x << 4);
        final double minZ = (chunkPos.z << 4);
        final double maxX = (chunkPos.x << 4) + 16;
        final double maxZ = (chunkPos.z << 4) + 16;

        final BlockRotation rotationCombined = schematicPlacement.getRotation().rotate(placement.getRotation());
        final BlockMirror mirrorMain = schematicPlacement.getMirror();
        BlockMirror mirrorSub = placement.getMirror();

        if (mirrorSub != BlockMirror.NONE &&
            (schematicPlacement.getRotation() == BlockRotation.CLOCKWISE_90 ||
            schematicPlacement.getRotation() == BlockRotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
        }

        for (EntityInfo info : entityList)
        {
            Vec3d pos = info.posVec;
            pos = PositionUtils.getTransformedPosition(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
            pos = PositionUtils.getTransformedPosition(pos, placement.getMirror(), placement.getRotation());
            double x = pos.x + offX;
            double y = pos.y + offY;
            double z = pos.z + offZ;
            float[] origRot = new float[2];

            if (!shouldPasteEntity(new Vec3d(x, y, z), layerBehavior))
            {
//                Litematica.LOGGER.error("placeEntitiesToWorldWithinChunk(): Skipping Entity at pos [{}]", pos.toString());
                continue;
            }

            if (x >= minX && x < maxX && z >= minZ && z < maxZ)
            {
                NbtCompound tag = info.nbt.copy();
                String id = tag.getString("id", "");

                // Avoid warning about invalid hanging position.
                // Note that this position isn't technically correct, but it only needs to be within 16 blocks
                // of the entity position to avoid the warning.
                if (id.equals("minecraft:glow_item_frame") ||
                    id.equals("minecraft:item_frame") ||
                    id.equals("minecraft:leash_knot") ||
                    id.equals("minecraft:painting"))
                {
                    Vec3d p = NbtUtils.readEntityPositionFromTag(tag);

                    if (p == null)
                    {
                        p = new Vec3d(x, y, z);
//                        NbtUtils.writeEntityPositionToTag(p, tag);
                        NbtUtils.putVec3dCodec(tag, p, "Pos");
                    }

                    tag.putInt("TileX", (int) p.x);
                    tag.putInt("TileY", (int) p.y);
                    tag.putInt("TileZ", (int) p.z);

                    // Block-Attached Pos (1.21.5+) Fix
                    BlockPos px = tag.get("block_pos", BlockPos.CODEC).orElse(null);

                    if (px != null)
                    {
                        tag.put("block_pos", BlockPos.CODEC, new BlockPos((int) x, (int) y, (int) z));
                    }
                }

                NbtList rotation = tag.getListOrEmpty("Rotation");
                origRot[0] = rotation.getFloat(0, 0f);
                origRot[1] = rotation.getFloat(1, 0f);

                Entity entity = EntityUtils.createEntityAndPassengersFromNBT(tag, world);

                if (entity != null)
                {
                    rotateEntity(entity, x, y, z, rotationCombined, mirrorMain, mirrorSub);
                    //System.out.printf("post: %.1f - rot: %s, mm: %s, ms: %s\n", rotationYaw, rotationCombined, mirrorMain, mirrorSub);

                    // Update the sleeping position to the current position
                    if (entity instanceof LivingEntity living && living.isSleeping())
                    {
                        living.setSleepingPosition(BlockPos.ofFloored(x, y, z));
                    }

                    // Hack fix to fix the painting position offsets.
                    // The vanilla code will end up moving the position by one in two of the orientations,
                    // because it sets the hanging position to the given position (floored)
                    // and then it offsets the position from the hanging position
                    // by 0.5 or 1.0 blocks depending on the painting size.
                    if (entity instanceof PaintingEntity paintingEntity)
                    {
                        Direction right = paintingEntity.getHorizontalFacing().rotateYCounterclockwise();

                        if ((paintingEntity.getVariant().value().width() % 2) == 0 &&
                            right.getDirection() == AxisDirection.POSITIVE)
                        {
                            x -= 1.0 * right.getOffsetX();
                            z -= 1.0 * right.getOffsetZ();
                        }

                        if ((paintingEntity.getVariant().value().height() % 2) == 0)
                        {
                            y -= 1.0;
                        }

                        entity.setPosition(x, y, z);
                    }
                    if (entity instanceof ItemFrameEntity frameEntity)
                    {
                        if (frameEntity.getYaw() != origRot[0] && (frameEntity.getPitch() == 90.0F || frameEntity.getPitch() == -90.0F))
                        {
                            // Fix Yaw only if Pitch is +/- 90.0F (Floor, Ceiling mounted)
                            frameEntity.setYaw(origRot[0]);
                        }
                    }

                    EntityUtils.spawnEntityAndPassengersInWorld(entity, world);

                    if (entity instanceof DisplayEntity)
                    {
                        entity.tick(); // Required to set the full data for rendering
                    }
                }
            }
        }
    }

    public static void rotateEntity(Entity entity, double x, double y, double z,
                                    BlockRotation rotationCombined, BlockMirror mirrorMain, BlockMirror mirrorSub)
    {
        float rotationYaw = entity.getYaw();

        if (mirrorMain != BlockMirror.NONE)         { rotationYaw = entity.applyMirror(mirrorMain); }
        if (mirrorSub != BlockMirror.NONE)          { rotationYaw = entity.applyMirror(mirrorSub); }
        if (rotationCombined != BlockRotation.NONE) { rotationYaw += entity.getYaw() - entity.applyRotation(rotationCombined); }

        entity.refreshPositionAndAngles(x, y, z, rotationYaw, entity.getPitch());
        EntityUtils.setEntityRotations(entity, rotationYaw, entity.getPitch());
    }

    public static boolean shouldPasteBlock(BlockPos pos, PasteLayerBehavior layerBehavior)
    {
        if (layerBehavior == PasteLayerBehavior.ALL)
        {
            return true;
        }

        return DataManager.getRenderLayerRange().isPositionWithinRange(pos);
    }

    public static boolean shouldPasteEntity(Vec3d pos, PasteLayerBehavior layerBehavior)
    {
        if (layerBehavior == PasteLayerBehavior.ALL)
        {
            return true;
        }

        return DataManager.getRenderLayerRange().isPositionWithinRange((int) pos.getX(), (int) pos.getY(), (int) pos.getZ());
    }
}
