package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

public class PositionUtils {
    public static final BlockPosComparator BLOCK_POS_COMPARATOR = new BlockPosComparator();
    public static final ChunkPosComparator CHUNK_POS_COMPARATOR = new ChunkPosComparator();

    public static final Direction.Axis[] AXES_ALL = new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z};
    public static final Direction[] ADJACENT_SIDES_ZY = new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};
    public static final Direction[] ADJACENT_SIDES_XY = new Direction[]{Direction.DOWN, Direction.UP, Direction.EAST, Direction.WEST};
    public static final Direction[] ADJACENT_SIDES_XZ = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XN_ZN = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(-1, 0, -1)};
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XP_ZN = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(1, 0, -1)};
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XN_ZP = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 1)};
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XP_ZP = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(1, 0, 1)};
    private static final Vec3i[][] EDGE_NEIGHBOR_OFFSETS_Y = new Vec3i[][]{EDGE_NEIGHBOR_OFFSETS_XN_ZN, EDGE_NEIGHBOR_OFFSETS_XP_ZN, EDGE_NEIGHBOR_OFFSETS_XN_ZP, EDGE_NEIGHBOR_OFFSETS_XP_ZP};

    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XN_YN = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, -1, 0), new Vec3i(-1, -1, 0)};
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XP_YN = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(1, 0, 0), new Vec3i(0, -1, 0), new Vec3i(1, -1, 0)};
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XN_YP = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 1, 0), new Vec3i(-1, 1, 0)};
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_XP_YP = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(1, 0, 0), new Vec3i(0, 1, 0), new Vec3i(1, 1, 0)};
    private static final Vec3i[][] EDGE_NEIGHBOR_OFFSETS_Z = new Vec3i[][]{EDGE_NEIGHBOR_OFFSETS_XN_YN, EDGE_NEIGHBOR_OFFSETS_XP_YN, EDGE_NEIGHBOR_OFFSETS_XN_YP, EDGE_NEIGHBOR_OFFSETS_XP_YP};

    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_YN_ZN = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(0, -1, 0), new Vec3i(0, 0, -1), new Vec3i(0, -1, -1)};
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_YP_ZN = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(0, 1, 0), new Vec3i(0, 0, -1), new Vec3i(0, 1, -1)};
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_YN_ZP = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(0, -1, 0), new Vec3i(0, 0, 1), new Vec3i(0, -1, 1)};
    private static final Vec3i[] EDGE_NEIGHBOR_OFFSETS_YP_ZP = new Vec3i[]{new Vec3i(0, 0, 0), new Vec3i(0, 1, 0), new Vec3i(0, 0, 1), new Vec3i(0, 1, 1)};
    private static final Vec3i[][] EDGE_NEIGHBOR_OFFSETS_X = new Vec3i[][]{EDGE_NEIGHBOR_OFFSETS_YN_ZN, EDGE_NEIGHBOR_OFFSETS_YP_ZN, EDGE_NEIGHBOR_OFFSETS_YN_ZP, EDGE_NEIGHBOR_OFFSETS_YP_ZP};

    public static Vec3i[] getEdgeNeighborOffsets(final Direction.Axis axis, final int cornerIndex) {
        switch (axis) {
            case X:
                return EDGE_NEIGHBOR_OFFSETS_X[cornerIndex];
            case Y:
                return EDGE_NEIGHBOR_OFFSETS_Y[cornerIndex];
            case Z:
                return EDGE_NEIGHBOR_OFFSETS_Z[cornerIndex];
        }

        return null;
    }

    public static BlockPos getMinCorner(final BlockPos pos1, final BlockPos pos2) {
        return new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
    }

    public static BlockPos getMaxCorner(final BlockPos pos1, final BlockPos pos2) {
        return new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
    }

    public static boolean isPositionInsideArea(final BlockPos pos, final BlockPos posMin, final BlockPos posMax) {
        return pos.getX() >= posMin.getX() && pos.getX() <= posMax.getX() &&
                pos.getY() >= posMin.getY() && pos.getY() <= posMax.getY() &&
                pos.getZ() >= posMin.getZ() && pos.getZ() <= posMax.getZ();
    }

    public static BlockPos getTransformedPlacementPosition(final BlockPos posWithinSub, final SchematicPlacement schematicPlacement, final SubRegionPlacement placement) {
        BlockPos pos = posWithinSub;
        pos = getTransformedBlockPos(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        pos = getTransformedBlockPos(pos, placement.getMirror(), placement.getRotation());
        return pos;
    }

    public static boolean arePositionsWithinWorld(final World world, final BlockPos pos1, final BlockPos pos2) {
        if (pos1.getY() >= world.getBottomY() && pos1.getY() < world.getTopY() &&
                pos2.getY() >= world.getBottomY() && pos2.getY() < world.getTopY()) {
            final WorldBorder border = world.getWorldBorder();
            return border.contains(pos1) && border.contains(pos2);
        }

        return false;
    }

    public static boolean isBoxWithinWorld(final World world, final Box box) {
        if (box.getPos1() != null && box.getPos2() != null) {
            return arePositionsWithinWorld(world, box.getPos1(), box.getPos2());
        }

        return false;
    }

    public static boolean isPlacementWithinWorld(final World world, final SchematicPlacement schematicPlacement, final boolean respectRenderRange) {
        final LayerRange range = DataManager.getRenderLayerRange();
        final BlockPos.Mutable posMutable1 = new BlockPos.Mutable();
        final BlockPos.Mutable posMutable2 = new BlockPos.Mutable();

        for (final Box box : schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values()) {
            if (respectRenderRange) {
                if (range.intersectsBox(box.getPos1(), box.getPos2())) {
                    final IntBoundingBox bb = range.getClampedArea(box.getPos1(), box.getPos2());

                    if (bb != null) {
                        posMutable1.set(bb.minX, bb.minY, bb.minZ);
                        posMutable2.set(bb.maxX, bb.maxY, bb.maxZ);

                        if (arePositionsWithinWorld(world, posMutable1, posMutable2) == false) {
                            return false;
                        }
                    }
                }
            } else if (isBoxWithinWorld(world, box) == false) {
                return false;
            }
        }

        return true;
    }

    public static BlockPos getAreaSizeFromRelativeEndPosition(final BlockPos posEndRelative) {
        int x = posEndRelative.getX();
        int y = posEndRelative.getY();
        int z = posEndRelative.getZ();

        x = x >= 0 ? x + 1 : x - 1;
        y = y >= 0 ? y + 1 : y - 1;
        z = z >= 0 ? z + 1 : z - 1;

        return new BlockPos(x, y, z);
    }

    public static BlockPos getAreaSizeFromRelativeEndPositionAbs(final BlockPos posEndRelative) {
        int x = posEndRelative.getX();
        int y = posEndRelative.getY();
        int z = posEndRelative.getZ();

        x = x >= 0 ? x + 1 : x - 1;
        y = y >= 0 ? y + 1 : y - 1;
        z = z >= 0 ? z + 1 : z - 1;

        return new BlockPos(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    public static BlockPos getRelativeEndPositionFromAreaSize(final Vec3i size) {
        int x = size.getX();
        int y = size.getY();
        int z = size.getZ();

        x = x >= 0 ? x - 1 : x + 1;
        y = y >= 0 ? y - 1 : y + 1;
        z = z >= 0 ? z - 1 : z + 1;

        return new BlockPos(x, y, z);
    }

    public static List<Box> getValidBoxes(final AreaSelection area) {
        final List<Box> boxes = new ArrayList<>();
        final Collection<Box> originalBoxes = area.getAllSubRegionBoxes();

        for (final Box box : originalBoxes) {
            if (isBoxValid(box)) {
                boxes.add(box);
            }
        }

        return boxes;
    }

    public static boolean isBoxValid(final Box box) {
        return box.getPos1() != null && box.getPos2() != null;
    }

    public static BlockPos getEnclosingAreaSize(final AreaSelection area) {
        return getEnclosingAreaSize(area.getAllSubRegionBoxes());
    }

    public static BlockPos getEnclosingAreaSize(final Collection<Box> boxes) {
        final Pair<BlockPos, BlockPos> pair = getEnclosingAreaCorners(boxes);
        return pair.getRight().subtract(pair.getLeft()).add(1, 1, 1);
    }

    /**
     * Returns the min and max corners of the enclosing box around the given collection of boxes.
     * The minimum corner is the left entry and the maximum corner is the right entry of the pair.
     *
     * @param boxes
     * @return
     */
    @Nullable
    public static Pair<BlockPos, BlockPos> getEnclosingAreaCorners(final Collection<Box> boxes) {
        if (boxes.isEmpty()) {
            return null;
        }

        final BlockPos.Mutable posMin = new BlockPos.Mutable(60000000, 60000000, 60000000);
        final BlockPos.Mutable posMax = new BlockPos.Mutable(-60000000, -60000000, -60000000);

        for (final Box box : boxes) {
            getMinMaxCoords(posMin, posMax, box.getPos1());
            getMinMaxCoords(posMin, posMax, box.getPos2());
        }

        return Pair.of(posMin.toImmutable(), posMax.toImmutable());
    }

    private static void getMinMaxCoords(final BlockPos.Mutable posMin, final BlockPos.Mutable posMax, @Nullable final BlockPos posToCheck) {
        if (posToCheck != null) {
            posMin.set(Math.min(posMin.getX(), posToCheck.getX()),
                    Math.min(posMin.getY(), posToCheck.getY()),
                    Math.min(posMin.getZ(), posToCheck.getZ()));

            posMax.set(Math.max(posMax.getX(), posToCheck.getX()),
                    Math.max(posMax.getY(), posToCheck.getY()),
                    Math.max(posMax.getZ(), posToCheck.getZ()));
        }
    }

    @Nullable
    public static IntBoundingBox clampBoxToWorldHeightRange(IntBoundingBox box, final World world) {
        final int minY = world.getBottomY();
        final int maxY = world.getTopY() - 1;

        if (box.minY > maxY || box.maxY < minY) {
            return null;
        }

        if (box.minY < minY || box.maxY > maxY) {
            box = new IntBoundingBox(box.minX, Math.max(box.minY, minY), box.minZ,
                    box.maxX, Math.min(box.maxY, maxY), box.maxZ);
        }

        return box;
    }

    public static int getTotalVolume(final Collection<Box> boxes) {
        if (boxes.isEmpty()) {
            return 0;
        }

        int volume = 0;

        for (final Box box : boxes) {
            if (isBoxValid(box)) {
                final BlockPos min = getMinCorner(box.getPos1(), box.getPos2());
                final BlockPos max = getMaxCorner(box.getPos1(), box.getPos2());
                volume += (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
            }
        }

        return volume;
    }

    public static ImmutableMap<String, IntBoundingBox> getBoxesWithinChunk(final int chunkX, final int chunkZ, final ImmutableMap<String, Box> subRegions) {
        final ImmutableMap.Builder<String, IntBoundingBox> builder = new ImmutableMap.Builder<>();

        for (final Map.Entry<String, Box> entry : subRegions.entrySet()) {
            final Box box = entry.getValue();
            final IntBoundingBox bb = box != null ? getBoundsWithinChunkForBox(box, chunkX, chunkZ) : null;

            if (bb != null) {
                builder.put(entry.getKey(), bb);
            }
        }

        return builder.build();
    }

    public static ImmutableList<IntBoundingBox> getBoxesWithinChunk(final int chunkX, final int chunkZ, final Collection<Box> boxes) {
        final ImmutableList.Builder<IntBoundingBox> builder = new ImmutableList.Builder<>();

        for (final Box box : boxes) {
            final IntBoundingBox bb = getBoundsWithinChunkForBox(box, chunkX, chunkZ);

            if (bb != null) {
                builder.add(bb);
            }
        }

        return builder.build();
    }

    public static Set<ChunkPos> getTouchedChunks(final ImmutableMap<String, Box> boxes) {
        return getTouchedChunksForBoxes(boxes.values());
    }

    public static Set<ChunkPos> getTouchedChunksForBoxes(final Collection<Box> boxes) {
        final Set<ChunkPos> set = new HashSet<>();

        for (final Box box : boxes) {
            final int boxXMin = Math.min(box.getPos1().getX(), box.getPos2().getX()) >> 4;
            final int boxZMin = Math.min(box.getPos1().getZ(), box.getPos2().getZ()) >> 4;
            final int boxXMax = Math.max(box.getPos1().getX(), box.getPos2().getX()) >> 4;
            final int boxZMax = Math.max(box.getPos1().getZ(), box.getPos2().getZ()) >> 4;

            for (int cz = boxZMin; cz <= boxZMax; ++cz) {
                for (int cx = boxXMin; cx <= boxXMax; ++cx) {
                    set.add(new ChunkPos(cx, cz));
                }
            }
        }

        return set;
    }

    @Nullable
    public static IntBoundingBox getBoundsWithinChunkForBox(final Box box, final int chunkX, final int chunkZ) {
        final int chunkXMin = chunkX << 4;
        final int chunkZMin = chunkZ << 4;
        final int chunkXMax = chunkXMin + 15;
        final int chunkZMax = chunkZMin + 15;

        final int boxXMin = Math.min(box.getPos1().getX(), box.getPos2().getX());
        final int boxZMin = Math.min(box.getPos1().getZ(), box.getPos2().getZ());
        final int boxXMax = Math.max(box.getPos1().getX(), box.getPos2().getX());
        final int boxZMax = Math.max(box.getPos1().getZ(), box.getPos2().getZ());

        final boolean notOverlapping = boxXMin > chunkXMax || boxZMin > chunkZMax || boxXMax < chunkXMin || boxZMax < chunkZMin;

        if (notOverlapping == false) {
            final int xMin = Math.max(chunkXMin, boxXMin);
            final int yMin = Math.min(box.getPos1().getY(), box.getPos2().getY());
            final int zMin = Math.max(chunkZMin, boxZMin);
            final int xMax = Math.min(chunkXMax, boxXMax);
            final int yMax = Math.max(box.getPos1().getY(), box.getPos2().getY());
            final int zMax = Math.min(chunkZMax, boxZMax);

            return new IntBoundingBox(xMin, yMin, zMin, xMax, yMax, zMax);
        }

        return null;
    }

    public static void getPerChunkBoxes(final Collection<Box> boxes, final BiConsumer<ChunkPos, IntBoundingBox> consumer) {
        for (final Box box : boxes) {
            final int boxMinX = Math.min(box.getPos1().getX(), box.getPos2().getX());
            final int boxMinY = Math.min(box.getPos1().getY(), box.getPos2().getY());
            final int boxMinZ = Math.min(box.getPos1().getZ(), box.getPos2().getZ());
            final int boxMaxX = Math.max(box.getPos1().getX(), box.getPos2().getX());
            final int boxMaxY = Math.max(box.getPos1().getY(), box.getPos2().getY());
            final int boxMaxZ = Math.max(box.getPos1().getZ(), box.getPos2().getZ());
            final int boxMinChunkX = boxMinX >> 4;
            final int boxMinChunkZ = boxMinZ >> 4;
            final int boxMaxChunkX = boxMaxX >> 4;
            final int boxMaxChunkZ = boxMaxZ >> 4;

            for (int cz = boxMinChunkZ; cz <= boxMaxChunkZ; ++cz) {
                for (int cx = boxMinChunkX; cx <= boxMaxChunkX; ++cx) {
                    final int chunkMinX = cx << 4;
                    final int chunkMinZ = cz << 4;
                    final int chunkMaxX = chunkMinX + 15;
                    final int chunkMaxZ = chunkMinZ + 15;
                    final int minX = Math.max(chunkMinX, boxMinX);
                    final int minZ = Math.max(chunkMinZ, boxMinZ);
                    final int maxX = Math.min(chunkMaxX, boxMaxX);
                    final int maxZ = Math.min(chunkMaxZ, boxMaxZ);

                    consumer.accept(new ChunkPos(cx, cz), new IntBoundingBox(minX, boxMinY, minZ, maxX, boxMaxY, maxZ));
                }
            }
        }
    }

    public static void getLayerRangeClampedPerChunkBoxes(final Collection<Box> boxes,
                                                         final LayerRange range,
                                                         final BiConsumer<ChunkPos, IntBoundingBox> consumer) {
        for (final Box box : boxes) {
            final int rangeMin = range.getLayerMin();
            final int rangeMax = range.getLayerMax();
            int boxMinX = Math.min(box.getPos1().getX(), box.getPos2().getX());
            int boxMinY = Math.min(box.getPos1().getY(), box.getPos2().getY());
            int boxMinZ = Math.min(box.getPos1().getZ(), box.getPos2().getZ());
            int boxMaxX = Math.max(box.getPos1().getX(), box.getPos2().getX());
            int boxMaxY = Math.max(box.getPos1().getY(), box.getPos2().getY());
            int boxMaxZ = Math.max(box.getPos1().getZ(), box.getPos2().getZ());

            switch (range.getAxis()) {
                case X:
                    if (rangeMax < boxMinX || rangeMin > boxMaxX) {
                        continue;
                    }
                    boxMinX = Math.max(boxMinX, rangeMin);
                    boxMaxX = Math.min(boxMaxX, rangeMax);
                    break;
                case Y:
                    if (rangeMax < boxMinY || rangeMin > boxMaxY) {
                        continue;
                    }
                    boxMinY = Math.max(boxMinY, rangeMin);
                    boxMaxY = Math.min(boxMaxY, rangeMax);
                    break;
                case Z:
                    if (rangeMax < boxMinZ || rangeMin > boxMaxZ) {
                        continue;
                    }
                    boxMinZ = Math.max(boxMinZ, rangeMin);
                    boxMaxZ = Math.min(boxMaxZ, rangeMax);
                    break;
            }

            final int boxMinChunkX = boxMinX >> 4;
            final int boxMinChunkZ = boxMinZ >> 4;
            final int boxMaxChunkX = boxMaxX >> 4;
            final int boxMaxChunkZ = boxMaxZ >> 4;

            for (int cz = boxMinChunkZ; cz <= boxMaxChunkZ; ++cz) {
                for (int cx = boxMinChunkX; cx <= boxMaxChunkX; ++cx) {
                    final int chunkMinX = cx << 4;
                    final int chunkMinZ = cz << 4;
                    final int chunkMaxX = chunkMinX + 15;
                    final int chunkMaxZ = chunkMinZ + 15;
                    final int minX = Math.max(chunkMinX, boxMinX);
                    final int minZ = Math.max(chunkMinZ, boxMinZ);
                    final int maxX = Math.min(chunkMaxX, boxMaxX);
                    final int maxZ = Math.min(chunkMaxZ, boxMaxZ);

                    consumer.accept(new ChunkPos(cx, cz), new IntBoundingBox(minX, boxMinY, minZ, maxX, boxMaxY, maxZ));
                }
            }
        }
    }

    /**
     * Creates an enclosing AABB around the given positions. They will both be inside the box.
     */
    public static net.minecraft.util.math.Box createEnclosingAABB(final BlockPos pos1, final BlockPos pos2) {
        final int minX = Math.min(pos1.getX(), pos2.getX());
        final int minY = Math.min(pos1.getY(), pos2.getY());
        final int minZ = Math.min(pos1.getZ(), pos2.getZ());
        final int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        final int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        final int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;

        return createAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static net.minecraft.util.math.Box createAABBFrom(final IntBoundingBox bb) {
        return createAABB(bb.minX, bb.minY, bb.minZ, bb.maxX + 1, bb.maxY + 1, bb.maxZ + 1);
    }

    /**
     * Creates an AABB for the given position
     */
    public static net.minecraft.util.math.Box createAABBForPosition(final BlockPos pos) {
        return createAABBForPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Creates an AABB for the given position
     */
    public static net.minecraft.util.math.Box createAABBForPosition(final int x, final int y, final int z) {
        return createAABB(x, y, z, x + 1, y + 1, z + 1);
    }

    /**
     * Creates an AABB with the given bounds
     */
    public static net.minecraft.util.math.Box createAABB(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        return new net.minecraft.util.math.Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Returns the given position adjusted such that the coordinate indicated by <b>type</b>
     * is set to the value in <b>value</b>
     *
     * @param pos
     * @param value
     * @param type
     * @return
     */
    public static BlockPos getModifiedPosition(BlockPos pos, final int value, final CoordinateType type) {

        switch (type) {
            case X:
                pos = new BlockPos(value, pos.getY(), pos.getZ());
                break;
            case Y:
                pos = new BlockPos(pos.getX(), value, pos.getZ());
                break;
            case Z:
                pos = new BlockPos(pos.getX(), pos.getY(), value);
                break;
        }

        return pos;
    }

    public static int getCoordinate(final BlockPos pos, final CoordinateType type) {
        switch (type) {
            case X:
                return pos.getX();
            case Y:
                return pos.getY();
            case Z:
                return pos.getZ();
        }

        return 0;
    }

    public static Box growOrShrinkBox(final Box box, final int amount) {
        BlockPos pos1 = box.getPos1();
        BlockPos pos2 = box.getPos2();

        if (pos1 == null || pos2 == null) {
            if (pos1 == null && pos2 == null) {
                return box;
            } else if (pos2 == null) {
                pos2 = pos1;
            } else {
                pos1 = pos2;
            }
        }

        final Pair<Integer, Integer> x = growCoordinatePair(pos1.getX(), pos2.getX(), amount);
        final Pair<Integer, Integer> y = growCoordinatePair(pos1.getY(), pos2.getY(), amount);
        final Pair<Integer, Integer> z = growCoordinatePair(pos1.getZ(), pos2.getZ(), amount);

        final Box boxNew = box.copy();
        boxNew.setPos1(new BlockPos(x.getLeft(), y.getLeft(), z.getLeft()));
        boxNew.setPos2(new BlockPos(x.getRight(), y.getRight(), z.getRight()));

        return boxNew;
    }

    private static Pair<Integer, Integer> growCoordinatePair(int v1, int v2, final int amount) {
        if (v2 >= v1) {
            if (v2 + amount >= v1) {
                v2 += amount;
            }

            if (v1 - amount <= v2) {
                v1 -= amount;
            }
        } else if (v1 > v2) {
            if (v1 + amount >= v2) {
                v1 += amount;
            }

            if (v2 - amount <= v1) {
                v2 -= amount;
            }
        }

        return Pair.of(v1, v2);
    }

/*SH    public static void growOrShrinkCurrentSelection(boolean grow)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();
        World world = MinecraftClient.getInstance().world;

        if (area == null || world == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
            return;
        }

        Box box = area.getSelectedSubRegionBox();

        if (box == null || (box.getPos1() == null && box.getPos2() == null))
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.area_selection.grow.no_sub_region_selected");
            return;
        }

        if (box != null && (box.getPos1() != null || box.getPos2() != null))
        {
            int amount = 1;
            Box boxNew = box.copy();

            for (int i = 0; i < 256; ++i)
            {
                if (grow)
                {
                    boxNew = growOrShrinkBox(boxNew, amount);
                }

                BlockPos pos1 = boxNew.getPos1();
                BlockPos pos2 = boxNew.getPos2();
                int xMin = Math.min(pos1.getX(), pos2.getX());
                int yMin = Math.min(pos1.getY(), pos2.getY());
                int zMin = Math.min(pos1.getZ(), pos2.getZ());
                int xMax = Math.max(pos1.getX(), pos2.getX());
                int yMax = Math.max(pos1.getY(), pos2.getY());
                int zMax = Math.max(pos1.getZ(), pos2.getZ());
                int emptySides = 0;

                // Slices along the z axis
                if (WorldUtils.isSliceEmpty(world, Direction.Axis.X, new BlockPos(xMin, yMin, zMin), new BlockPos(xMin, yMax, zMax)))
                {
                    xMin += amount;
                    ++emptySides;
                }

                if (WorldUtils.isSliceEmpty(world, Direction.Axis.X, new BlockPos(xMax, yMin, zMin), new BlockPos(xMax, yMax, zMax)))
                {
                    xMax -= amount;
                    ++emptySides;
                }

                // Slices along the x/z plane
                if (WorldUtils.isSliceEmpty(world, Direction.Axis.Y, new BlockPos(xMin, yMin, zMin), new BlockPos(xMax, yMin, zMax)))
                {
                    yMin += amount;
                    ++emptySides;
                }

                if (WorldUtils.isSliceEmpty(world, Direction.Axis.Y, new BlockPos(xMin, yMax, zMin), new BlockPos(xMax, yMax, zMax)))
                {
                    yMax -= amount;
                    ++emptySides;
                }

                // Slices along the x axis
                if (WorldUtils.isSliceEmpty(world, Direction.Axis.Z, new BlockPos(xMin, yMin, zMin), new BlockPos(xMax, yMax, zMin)))
                {
                    zMin += amount;
                    ++emptySides;
                }

                if (WorldUtils.isSliceEmpty(world, Direction.Axis.Z, new BlockPos(xMin, yMin, zMax), new BlockPos(xMax, yMax, zMax)))
                {
                    zMax -= amount;
                    ++emptySides;
                }

                boxNew.setPos1(new BlockPos(xMin, yMin, zMin));
                boxNew.setPos2(new BlockPos(xMax, yMax, zMax));

                if (grow && emptySides >= 6)
                {
                    break;
                }
                else if (grow == false && emptySides == 0)
                {
                    break;
                }
            }

            area.setSelectedSubRegionCornerPos(boxNew.getPos1(), Corner.CORNER_1);
            area.setSelectedSubRegionCornerPos(boxNew.getPos2(), Corner.CORNER_2);
        }
    }*/

    /**
     * Mirrors and then rotates the given position around the origin
     */
    public static BlockPos getTransformedBlockPos(final BlockPos pos, final BlockMirror mirror, final BlockRotation rotation) {
        int x = pos.getX();
        final int y = pos.getY();
        int z = pos.getZ();
        boolean isMirrored = true;

        switch (mirror) {
            // LEFT_RIGHT is essentially NORTH_SOUTH
            case LEFT_RIGHT:
                z = -z;
                break;
            // FRONT_BACK is essentially EAST_WEST
            case FRONT_BACK:
                x = -x;
                break;
            default:
                isMirrored = false;
        }

        switch (rotation) {
            case CLOCKWISE_90:
                return new BlockPos(-z, y, x);
            case COUNTERCLOCKWISE_90:
                return new BlockPos(z, y, -x);
            case CLOCKWISE_180:
                return new BlockPos(-x, y, -z);
            default:
                return isMirrored ? new BlockPos(x, y, z) : pos;
        }
    }

    public static BlockPos getReverseTransformedBlockPos(final BlockPos pos, final BlockMirror mirror, final BlockRotation rotation) {
        int x = pos.getX();
        final int y = pos.getY();
        int z = pos.getZ();
        boolean isRotated = true;
        final int tmp = x;

        switch (rotation) {
            case CLOCKWISE_90:
                x = z;
                z = -tmp;
                break;
            case COUNTERCLOCKWISE_90:
                x = -z;
                z = tmp;
                break;
            case CLOCKWISE_180:
                x = -x;
                z = -z;
                break;
            default:
                isRotated = false;
        }

        switch (mirror) {
            // LEFT_RIGHT is essentially NORTH_SOUTH
            case LEFT_RIGHT:
                z = -z;
                break;
            // FRONT_BACK is essentially EAST_WEST
            case FRONT_BACK:
                x = -x;
                break;
            default:
                if (isRotated == false) {
                    return pos;
                }
        }

        return new BlockPos(x, y, z);
    }

    /**
     * Does the opposite transform from getTransformedBlockPos(), to return the original,
     * non-transformed position from the transformed position.
     */
    public static BlockPos getOriginalPositionFromTransformed(final BlockPos pos, final BlockMirror mirror, final BlockRotation rotation) {
        int x = pos.getX();
        final int y = pos.getY();
        int z = pos.getZ();
        int tmp;
        boolean noRotation = false;

        switch (rotation) {
            case CLOCKWISE_90:
                tmp = x;
                x = -z;
                z = tmp;
            case COUNTERCLOCKWISE_90:
                tmp = x;
                x = z;
                z = -tmp;
            case CLOCKWISE_180:
                x = -x;
                z = -z;
            default:
                noRotation = true;
        }

        switch (mirror) {
            case LEFT_RIGHT:
                z = -z;
                break;
            case FRONT_BACK:
                x = -x;
                break;
            default:
                if (noRotation) {
                    return pos;
                }
        }

        return new BlockPos(x, y, z);
    }

    public static Vec3d getTransformedPosition(final Vec3d originalPos, final BlockMirror mirror, final BlockRotation rotation) {
        double x = originalPos.x;
        final double y = originalPos.y;
        double z = originalPos.z;
        boolean transformed = true;

        switch (mirror) {
            case LEFT_RIGHT:
                z = 1.0D - z;
                break;
            case FRONT_BACK:
                x = 1.0D - x;
                break;
            default:
                transformed = false;
        }

        switch (rotation) {
            case COUNTERCLOCKWISE_90:
                return new Vec3d(z, y, 1.0D - x);
            case CLOCKWISE_90:
                return new Vec3d(1.0D - z, y, x);
            case CLOCKWISE_180:
                return new Vec3d(1.0D - x, y, 1.0D - z);
            default:
                return transformed ? new Vec3d(x, y, z) : originalPos;
        }
    }

    public static BlockRotation getReverseRotation(final BlockRotation rotationIn) {
        switch (rotationIn) {
            case COUNTERCLOCKWISE_90:
                return BlockRotation.CLOCKWISE_90;
            case CLOCKWISE_90:
                return BlockRotation.COUNTERCLOCKWISE_90;
            case CLOCKWISE_180:
                return BlockRotation.CLOCKWISE_180;
            default:
                return rotationIn;
        }
    }

    public static BlockPos getModifiedPartiallyLockedPosition(final BlockPos posOriginal, BlockPos posNew, final int lockMask) {
        if (lockMask != 0) {
            int x = posNew.getX();
            int y = posNew.getY();
            int z = posNew.getZ();

            if ((lockMask & (0x1 << CoordinateType.X.ordinal())) != 0) {
                x = posOriginal.getX();
            }

            if ((lockMask & (0x1 << CoordinateType.Y.ordinal())) != 0) {
                y = posOriginal.getY();
            }

            if ((lockMask & (0x1 << CoordinateType.Z.ordinal())) != 0) {
                z = posOriginal.getZ();
            }

            posNew = new BlockPos(x, y, z);
        }

        return posNew;
    }

    /**
     * Gets the "front" facing from the given positions,
     * so that pos1 is in the "front left" corner and pos2 is in the "back right" corner
     * of the area, when looking at the "front" face of the area.
     */
    public static Direction getFacingFromPositions(final BlockPos pos1, final BlockPos pos2) {
        if (pos1 == null || pos2 == null) {
            return null;
        }

        return getFacingFromPositions(pos1.getX(), pos1.getZ(), pos2.getX(), pos2.getZ());
    }

    private static Direction getFacingFromPositions(final int x1, final int z1, final int x2, final int z2) {
        if (x2 == x1) {
            return z2 > z1 ? Direction.SOUTH : Direction.NORTH;
        }

        if (z2 == z1) {
            return x2 > x1 ? Direction.EAST : Direction.WEST;
        }

        if (x2 > x1) {
            return z2 > z1 ? Direction.EAST : Direction.NORTH;
        }

        return z2 > z1 ? Direction.SOUTH : Direction.WEST;
    }

    public static BlockRotation cycleRotation(final BlockRotation rotation, final boolean reverse) {
        int ordinal = rotation.ordinal();

        if (reverse) {
            ordinal = ordinal == 0 ? BlockRotation.values().length - 1 : ordinal - 1;
        } else {
            ordinal = ordinal >= BlockRotation.values().length - 1 ? 0 : ordinal + 1;
        }

        return BlockRotation.values()[ordinal];
    }

    public static BlockMirror cycleMirror(final BlockMirror mirror, final boolean reverse) {
        int ordinal = mirror.ordinal();

        if (reverse) {
            ordinal = ordinal == 0 ? BlockMirror.values().length - 1 : ordinal - 1;
        } else {
            ordinal = ordinal >= BlockMirror.values().length - 1 ? 0 : ordinal + 1;
        }

        return BlockMirror.values()[ordinal];
    }

    public static String getRotationNameShort(final BlockRotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return "CW_90";
            case CLOCKWISE_180:
                return "CW_180";
            case COUNTERCLOCKWISE_90:
                return "CCW_90";
            case NONE:
            default:
                return "NONE";
        }
    }

    public static String getMirrorName(final BlockMirror mirror) {
        switch (mirror) {
            case FRONT_BACK:
                return "FRONT_BACK";
            case LEFT_RIGHT:
                return "LEFT_RIGHT";
            case NONE:
            default:
                return "NONE";
        }
    }

    public static float getRotatedYaw(float yaw, final BlockRotation rotation) {
        yaw = MathHelper.wrapDegrees(yaw);

        switch (rotation) {
            case CLOCKWISE_180:
                yaw += 180.0F;
                break;
            case COUNTERCLOCKWISE_90:
                yaw += 270.0F;
                break;
            case CLOCKWISE_90:
                yaw += 90.0F;
                break;
            default:
        }

        return yaw;
    }

    public static float getMirroredYaw(float yaw, final BlockMirror mirror) {
        yaw = MathHelper.wrapDegrees(yaw);

        switch (mirror) {
            case LEFT_RIGHT:
                yaw = 180.0F - yaw;
                break;
            case FRONT_BACK:
                yaw = -yaw;
                break;
            default:
        }

        return yaw;
    }


    /**
     * Clamps the given box to the layer range bounds.
     *
     * @return the clamped box, or null, if the range does not intersect the original box
     */
    @Nullable
    public static IntBoundingBox getClampedBox(final IntBoundingBox box, final LayerRange range) {
        return getClampedArea(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, range);
    }

    /**
     * Clamps the given box to the layer range bounds.
     *
     * @return the clamped box, or null, if the range does not intersect the original box
     */
    @Nullable
    public static IntBoundingBox getClampedArea(final BlockPos posMin, final BlockPos posMax, final LayerRange range) {
        final int minX = Math.min(posMin.getX(), posMax.getX());
        final int minY = Math.min(posMin.getY(), posMax.getY());
        final int minZ = Math.min(posMin.getZ(), posMax.getZ());
        final int maxX = Math.max(posMin.getX(), posMax.getX());
        final int maxY = Math.max(posMin.getY(), posMax.getY());
        final int maxZ = Math.max(posMin.getZ(), posMax.getZ());

        return getClampedArea(minX, minY, minZ, maxX, maxY, maxZ, range);
    }

    /**
     * Clamps the given box to the layer range bounds.
     *
     * @return the clamped box, or null, if the range does not intersect the original box
     */
    @Nullable
    public static IntBoundingBox getClampedArea(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ, final LayerRange range) {
        if (range.intersectsBox(minX, minY, minZ, maxX, maxY, maxZ) == false) {
            return null;
        }

        switch (range.getAxis()) {
            case X: {
                final int clampedMinX = Math.max(minX, range.getLayerMin());
                final int clampedMaxX = Math.min(maxX, range.getLayerMax());
                return IntBoundingBox.createProper(clampedMinX, minY, minZ, clampedMaxX, maxY, maxZ);
            }
            case Y: {
                final int clampedMinY = Math.max(minY, range.getLayerMin());
                final int clampedMaxY = Math.min(maxY, range.getLayerMax());
                return IntBoundingBox.createProper(minX, clampedMinY, minZ, maxX, clampedMaxY, maxZ);
            }
            case Z: {
                final int clampedMinZ = Math.max(minZ, range.getLayerMin());
                final int clampedMaxZ = Math.min(maxZ, range.getLayerMax());
                return IntBoundingBox.createProper(minX, minY, clampedMinZ, maxX, maxY, clampedMaxZ);
            }
            default:
                return null;
        }
    }

    public static class BlockPosComparator implements Comparator<BlockPos> {
        private BlockPos posReference = BlockPos.ORIGIN;
        private boolean closestFirst;

        public void setClosestFirst(final boolean closestFirst) {
            this.closestFirst = closestFirst;
        }

        public void setReferencePosition(final BlockPos pos) {
            this.posReference = pos;
        }

        @Override
        public int compare(final BlockPos pos1, final BlockPos pos2) {
            final double dist1 = pos1.getSquaredDistance(this.posReference);
            final double dist2 = pos2.getSquaredDistance(this.posReference);

            if (dist1 == dist2) {
                return 0;
            }

            return dist1 < dist2 == this.closestFirst ? -1 : 1;
        }
    }

    public static class ChunkPosComparator implements Comparator<ChunkPos> {
        private BlockPos posReference = BlockPos.ORIGIN;
        private boolean closestFirst;

        public ChunkPosComparator setClosestFirst(final boolean closestFirst) {
            this.closestFirst = closestFirst;
            return this;
        }

        public ChunkPosComparator setReferencePosition(final BlockPos pos) {
            this.posReference = pos;
            return this;
        }

        @Override
        public int compare(final ChunkPos pos1, final ChunkPos pos2) {
            final double dist1 = this.distanceSq(pos1);
            final double dist2 = this.distanceSq(pos2);

            if (dist1 == dist2) {
                return 0;
            }

            return dist1 < dist2 == this.closestFirst ? -1 : 1;
        }

        private double distanceSq(final ChunkPos pos) {
            final double dx = (double) (pos.x << 4) - this.posReference.getX();
            final double dz = (double) (pos.z << 4) - this.posReference.getZ();

            return dx * dx + dz * dz;
        }
    }

    public enum Corner {
        NONE,
        CORNER_1,
        CORNER_2
    }
}
