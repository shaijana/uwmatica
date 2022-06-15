package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class SchematicUtils {
    private static long areaMovedTime;

/*SH    public static boolean saveSchematic(boolean inMemoryOnly)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null)
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                String title = "litematica.gui.title.schematic_projects.save_new_version";
                SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();
                GuiTextInput gui = new GuiTextInput(512, title, project.getCurrentVersionName(), GuiUtils.getCurrentScreen(), new SchematicVersionCreator());
                GuiBase.openGui(gui);
            }
            else if (inMemoryOnly)
            {
                String title = "litematica.gui.title.create_in_memory_schematic";
                GuiTextInput gui = new GuiTextInput(512, title, area.getName(), GuiUtils.getCurrentScreen(), new InMemorySchematicCreator(area));
                GuiBase.openGui(gui);
            }
            else
            {
                GuiSchematicSave gui = new GuiSchematicSave();
                gui.setParent(GuiUtils.getCurrentScreen());
                GuiBase.openGui(gui);
            }

            return true;
        }

        return false;
    }*/

    public static void unloadCurrentlySelectedSchematic() {
        final SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null) {
            SchematicHolder.getInstance().removeSchematic(placement.getSchematic());
        } else {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
        }
    }

    public static boolean breakSchematicBlock(final MinecraftClient mc) {
        return setTargetedSchematicBlockState(mc, Blocks.AIR.getDefaultState());
    }

    public static boolean placeSchematicBlock(final MinecraftClient mc) {
        final ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null) {
            final BlockPos pos = info.pos.offset(info.side);

            if (DataManager.getRenderLayerRange().isPositionWithinRange(pos)) {
                return setTargetedSchematicBlockState(pos, info.stateNew);
            }
        }

        return false;
    }

    public static boolean replaceSchematicBlocksInDirection(final MinecraftClient mc) {
        final ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null) {
            final Direction playerFacingH = mc.player.getHorizontalFacing();
            Direction direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(info.side, playerFacingH, info.pos, info.hitVec);

            // Center region
            if (direction == info.side) {
                direction = direction.getOpposite();
            }

            final BlockPos posEnd = getReplacementBoxEndPos(info.pos, direction);
            return setSchematicBlockStates(info.pos, posEnd, info.stateNew);
        }

        return false;
    }

    public static boolean replaceAllIdenticalSchematicBlocks(final MinecraftClient mc) {
        final ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null) {
            return setAllIdenticalSchematicBlockStates(info.pos, info.stateOriginal, info.stateNew, mc.world);
        }

        return false;
    }

    public static boolean replaceBlocksKeepingProperties(final MinecraftClient mc) {
        final ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null && info.stateNew != info.stateOriginal &&
                BlockUtils.blocksHaveSameProperties(info.stateOriginal, info.stateNew)) {
            final Object2ObjectOpenHashMap<BlockState, BlockState> map = new Object2ObjectOpenHashMap<>();
            final BiPredicate<BlockState, BlockState> blockStateTest = (testedState, originalState) -> testedState.getBlock() == originalState.getBlock();
            final BiFunction<BlockState, BlockState, BlockState> blockModifier = (newState, originalState) -> map.computeIfAbsent(originalState, (k) -> {
                BlockState finalState = newState;

                for (final Property<?> prop : newState.getProperties()) {
                    finalState = BlockUtils.getBlockStateWithProperty(finalState, prop, originalState.get(prop));
                }

                return finalState;
            });

            return setAllIdenticalSchematicBlockStates(info.pos, info.stateOriginal, info.stateNew, blockStateTest, blockModifier, mc.world);
        }

        return false;
    }

    public static boolean breakSchematicBlocks(final MinecraftClient mc) {
        final Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        final RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(mc.world, entity, 10);

        if (wrapper != null && wrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
            final BlockHitResult trace = wrapper.getBlockHitResult();
            final BlockPos pos = trace.getBlockPos();
            final Direction playerFacingH = mc.player.getHorizontalFacing();
            Direction direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(trace.getSide(), playerFacingH, pos, trace.getPos());

            // Center region
            if (direction == trace.getSide()) {
                direction = direction.getOpposite();
            }

            final BlockPos posEnd = getReplacementBoxEndPos(pos, direction);

            return setSchematicBlockStates(pos, posEnd, Blocks.AIR.getDefaultState());
        }

        return false;
    }

    public static boolean breakAllIdenticalSchematicBlocks(final MinecraftClient mc) {
        final Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        final RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(mc.world, entity, 10);

        if (wrapper != null && wrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
            final BlockHitResult trace = wrapper.getBlockHitResult();
            final BlockPos pos = trace.getBlockPos();
            final BlockState stateOriginal = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);

            return setAllIdenticalSchematicBlockStates(pos, stateOriginal, Blocks.AIR.getDefaultState(), mc.world);
        }

        return false;
    }

    public static boolean placeSchematicBlocksInDirection(final MinecraftClient mc) {
        final ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null && mc.player != null) {
            final Direction playerFacingH = mc.player.getHorizontalFacing();
            final Direction direction = fi.dy.masa.malilib.util.PositionUtils.getTargetedDirection(info.side, playerFacingH, info.pos, info.hitVec);
            final BlockPos posStart = info.pos.offset(info.side); // offset to the adjacent air block

            if (SchematicWorldHandler.getSchematicWorld().getBlockState(posStart).isAir()) {
                final BlockPos posEnd = getReplacementBoxEndPos(posStart, direction);
                return setSchematicBlockStates(posStart, posEnd, info.stateNew);
            }
        }

        return false;
    }

    public static boolean breakAllSchematicBlocksExceptTargeted(final MinecraftClient mc) {
        final Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        final RayTraceWrapper wrapper = RayTraceUtils.getSchematicWorldTraceWrapperIfClosest(mc.world, entity, 10);

        if (wrapper != null && wrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
            final BlockHitResult trace = wrapper.getBlockHitResult();
            final BlockPos pos = trace.getBlockPos();
            final BlockState stateOriginal = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);

            return setAllStatesToAirExcept(pos, stateOriginal, mc.world);
        }

        return false;
    }

    public static boolean fillAirWithBlocks(final MinecraftClient mc) {
        final ReplacementInfo info = getTargetInfo(mc);

        // The state can be null in 1.13+
        if (info != null && info.stateNew != null) {
            final BlockPos posStart = info.pos.offset(info.side); // offset to the adjacent air block

            if (SchematicWorldHandler.getSchematicWorld().getBlockState(posStart).isAir()) {
                return setAllIdenticalSchematicBlockStates(posStart, Blocks.AIR.getDefaultState(), info.stateNew, mc.world);
            }
        }

        return false;
    }

    @Nullable
    private static ReplacementInfo getTargetInfo(final MinecraftClient mc) {
        final ItemStack stack = mc.player.getMainHandStack();

        if ((stack.isEmpty() == false && (stack.getItem() instanceof BlockItem))/*SH ||
            (stack.isEmpty() && ToolMode.REBUILD.getPrimaryBlock() != null)*/) {
            final WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
            final Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            final RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, entity, 10);

            if (worldSchematic != null && traceWrapper != null &&
                    traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
                final BlockHitResult trace = traceWrapper.getBlockHitResult();
                final Direction side = trace.getSide();
                final Vec3d hitVec = trace.getPos();
                final BlockPos pos = trace.getBlockPos();
                final BlockState stateOriginal = worldSchematic.getBlockState(pos);
                BlockState stateNew = Blocks.AIR.getDefaultState();

                if (stack.getItem() instanceof BlockItem) {
                    // Smuggle in a reference to the Schematic world to the use context
                    final World worldClient = mc.player.world;
                    mc.player.world = worldSchematic;

                    final BlockHitResult hit = new BlockHitResult(trace.getPos(), side, pos.offset(side), false);
                    final ItemPlacementContext ctx = new ItemPlacementContext(new ItemUsageContext(mc.player, Hand.MAIN_HAND, hit));

                    mc.player.world = worldClient;

                    stateNew = ((BlockItem) stack.getItem()).getBlock().getPlacementState(ctx);
                }
/*SH                else if (ToolMode.REBUILD.getPrimaryBlock() != null)
                {
                    stateNew = ToolMode.REBUILD.getPrimaryBlock();
                }*/

                return new ReplacementInfo(pos, side, hitVec, stateOriginal, stateNew);
            }
        }

        return null;
    }

    private static BlockPos getReplacementBoxEndPos(final BlockPos startPos, final Direction direction) {
        return getReplacementBoxEndPos(startPos, direction, 10000);
    }

    private static BlockPos getReplacementBoxEndPos(final BlockPos startPos, final Direction direction, int maxBlocks) {
        final WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        final LayerRange range = DataManager.getRenderLayerRange();
        final BlockState stateStart = world.getBlockState(startPos);
        final BlockPos.Mutable posMutable = new BlockPos.Mutable();
        posMutable.set(startPos);

        while (maxBlocks-- > 0) {
            posMutable.move(direction);

            if (range.isPositionWithinRange(posMutable) == false ||
                    world.getChunkProvider().isChunkLoaded(posMutable.getX() >> 4, posMutable.getZ() >> 4) == false ||
                    world.getBlockState(posMutable) != stateStart) {
                posMutable.move(direction.getOpposite());
                break;
            }
        }

        return posMutable.toImmutable();
    }

    public static boolean setTargetedSchematicBlockState(final MinecraftClient mc, final BlockState state) {
        final WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        final Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        final RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, entity, 6);

        if (world != null && traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
            final BlockHitResult trace = traceWrapper.getBlockHitResult();
            final BlockPos pos = trace.getBlockPos();
            return setTargetedSchematicBlockState(pos, state);
        }

        return false;
    }

    private static boolean setTargetedSchematicBlockState(final BlockPos pos, BlockState state) {
        if (pos != null) {
            final SubChunkPos cpos = new SubChunkPos(pos);
            final List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false) {
                for (final PlacementPart part : list) {
                    if (part.getBox().containsPos(pos)) {
                        final SchematicPlacement placement = part.getPlacement();
                        final String regionName = part.getSubRegionName();
                        final LitematicaBlockStateContainer container = placement.getSchematic().getSubRegionContainer(regionName);
                        final BlockPos posSchematic = getSchematicContainerPositionFromWorldPosition(pos, placement.getSchematic(),
                                regionName, placement, placement.getRelativeSubRegionPlacement(regionName), container);

                        if (posSchematic != null) {
                            state = getUntransformedBlockState(state, placement, regionName);

                            final BlockState stateOriginal = container.get(posSchematic.getX(), posSchematic.getY(), posSchematic.getZ());

                            int totalBlocks = part.getPlacement().getSchematic().getMetadata().getTotalBlocks();
                            int increment = 0;

                            if (stateOriginal.isAir() == false) {
                                increment = state.isAir() == false ? 0 : -1;
                            } else {
                                increment = state.isAir() == false ? 1 : 0;
                            }

                            totalBlocks += increment;

                            container.set(posSchematic.getX(), posSchematic.getY(), posSchematic.getZ(), state);

                            final SchematicMetadata metadata = part.getPlacement().getSchematic().getMetadata();
                            metadata.setTotalBlocks(totalBlocks);
                            metadata.setTimeModifiedToNow();
                            metadata.setModifiedSinceSaved();

                            DataManager.getSchematicPlacementManager().markChunkForRebuild(new ChunkPos(cpos.getX(), cpos.getZ()));

                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    private static boolean setSchematicBlockStates(final BlockPos posStart, final BlockPos posEnd, BlockState state) {
        if (posStart != null && posEnd != null) {
            final SubChunkPos cpos = new SubChunkPos(posStart);
            final List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false) {
                for (final PlacementPart part : list) {
                    if (part.getBox().containsPos(posStart)) {
                        final SchematicPlacement placement = part.getPlacement();
                        final String regionName = part.getSubRegionName();
                        final LitematicaBlockStateContainer container = placement.getSchematic().getSubRegionContainer(regionName);
                        final BlockPos posStartSchematic = getSchematicContainerPositionFromWorldPosition(posStart, placement.getSchematic(),
                                regionName, placement, placement.getRelativeSubRegionPlacement(regionName), container);
                        final BlockPos posEndSchematic = getSchematicContainerPositionFromWorldPosition(posEnd, placement.getSchematic(),
                                regionName, placement, placement.getRelativeSubRegionPlacement(regionName), container);

                        if (posStartSchematic != null && posEndSchematic != null) {
                            final BlockPos posMin = PositionUtils.getMinCorner(posStartSchematic, posEndSchematic);
                            final BlockPos posMax = PositionUtils.getMaxCorner(posStartSchematic, posEndSchematic);
                            final int minX = Math.max(posMin.getX(), 0);
                            final int minY = Math.max(posMin.getY(), 0);
                            final int minZ = Math.max(posMin.getZ(), 0);
                            final int maxX = Math.min(posMax.getX(), container.getSize().getX() - 1);
                            final int maxY = Math.min(posMax.getY(), container.getSize().getY() - 1);
                            final int maxZ = Math.min(posMax.getZ(), container.getSize().getZ() - 1);
                            int totalBlocks = part.getPlacement().getSchematic().getMetadata().getTotalBlocks();
                            int increment = 0;

                            state = getUntransformedBlockState(state, placement, regionName);

                            for (int y = minY; y <= maxY; ++y) {
                                for (int z = minZ; z <= maxZ; ++z) {
                                    for (int x = minX; x <= maxX; ++x) {
                                        final BlockState stateOriginal = container.get(x, y, z);

                                        if (stateOriginal.isAir() == false) {
                                            increment = state.isAir() == false ? 0 : -1;
                                        } else {
                                            increment = state.isAir() == false ? 1 : 0;
                                        }

                                        totalBlocks += increment;

                                        container.set(x, y, z, state);
                                    }
                                }
                            }

                            final SchematicMetadata metadata = part.getPlacement().getSchematic().getMetadata();
                            metadata.setTotalBlocks(totalBlocks);
                            metadata.setTimeModifiedToNow();
                            metadata.setModifiedSinceSaved();

                            DataManager.getSchematicPlacementManager().markAllPlacementsOfSchematicForRebuild(placement.getSchematic());

                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    private static boolean setAllIdenticalSchematicBlockStates(final BlockPos posStart,
                                                               final BlockState stateOriginal,
                                                               final BlockState stateNew,
                                                               final World world) {
        final BiPredicate<BlockState, BlockState> blockStateTest = (testedState, originalState) -> testedState == originalState;
        final BiFunction<BlockState, BlockState, BlockState> blockModifier = (newState, originalState) -> newState;
        return setAllIdenticalSchematicBlockStates(posStart, stateOriginal, stateNew, blockStateTest, blockModifier, world);
    }

    private static boolean setAllIdenticalSchematicBlockStates(final BlockPos posStart,
                                                               final BlockState stateOriginal,
                                                               final BlockState stateNew,
                                                               final BiPredicate<BlockState, BlockState> blockStateTest,
                                                               final BiFunction<BlockState, BlockState, BlockState> blockModifier,
                                                               final World world) {
        if (posStart != null) {
            final SubChunkPos cpos = new SubChunkPos(posStart);
            final SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            final List<PlacementPart> list = manager.getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false) {
                for (final PlacementPart part : list) {
                    if (part.getBox().containsPos(posStart)) {
                        if (replaceAllIdenticalBlocks(manager, part, stateOriginal, stateNew,
                                blockStateTest, blockModifier, world)) {
                            manager.markAllPlacementsOfSchematicForRebuild(part.getPlacement().getSchematic());
                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    private static boolean setAllStatesToAirExcept(final BlockPos pos, final BlockState state, final World world) {
        if (pos != null) {
            final SubChunkPos cpos = new SubChunkPos(pos);
            final SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            final List<PlacementPart> list = manager.getAllPlacementsTouchingSubChunk(cpos);

            if (list.isEmpty() == false) {
                for (final PlacementPart part : list) {
                    if (part.getBox().containsPos(pos)) {
                        if (setAllStatesToAirExcept(manager, part, state, world)) {
                            manager.markAllPlacementsOfSchematicForRebuild(part.getPlacement().getSchematic());
                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    private static boolean setAllStatesToAirExcept(final SchematicPlacementManager manager,
                                                   final PlacementPart part,
                                                   final BlockState state,
                                                   final World world) {
        final SchematicPlacement schematicPlacement = part.getPlacement();
        final String selected = schematicPlacement.getSelectedSubRegionName();
        final List<String> regions = new ArrayList<>();
        final BlockState air = Blocks.AIR.getDefaultState();

        // Some sub-region selected, only replace in that region
        if (selected != null) {
            regions.add(selected);
        }
        // The entire placement is selected, replace in all sub-regions
        else if (manager.getSelectedSchematicPlacement() == schematicPlacement) {
            regions.addAll(schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).keySet());
        }
        // Nothing from the targeted placement is selected, don't replace anything
        else {
            InfoUtils.showInGameMessage(MessageType.WARNING, 20000, "litematica.message.warn.schematic_rebuild_placement_not_selected");
            return false;
        }

        final LayerRange range = DataManager.getRenderLayerRange();
        int totalBlocks = schematicPlacement.getSchematic().getMetadata().getTotalBlocks();

        for (final String regionName : regions) {
            final LitematicaBlockStateContainer container = schematicPlacement.getSchematic().getSubRegionContainer(regionName);
            final SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);

            if (container == null || placement == null) {
                continue;
            }

            final int minX = range.getClampedValue(-30000000, Direction.Axis.X);
            final int minZ = range.getClampedValue(-30000000, Direction.Axis.Z);
            final int maxX = range.getClampedValue(30000000, Direction.Axis.X);
            final int maxZ = range.getClampedValue(30000000, Direction.Axis.Z);
            final int minY = range.getClampedValue(world.getBottomY(), Direction.Axis.Y);
            final int maxY = range.getClampedValue(world.getTopY() - 1, Direction.Axis.Y);

            final BlockPos posStart = new BlockPos(minX, minY, minZ);
            final BlockPos posEnd = new BlockPos(maxX, maxY, maxZ);

            final BlockPos pos1 = getReverserTransformedWorldPosition(posStart, schematicPlacement.getSchematic(),
                    regionName, schematicPlacement, schematicPlacement.getRelativeSubRegionPlacement(regionName));
            final BlockPos pos2 = getReverserTransformedWorldPosition(posEnd, schematicPlacement.getSchematic(),
                    regionName, schematicPlacement, schematicPlacement.getRelativeSubRegionPlacement(regionName));

            if (pos1 == null || pos2 == null) {
                return false;
            }

            final BlockPos posStartWorld = PositionUtils.getMinCorner(pos1, pos2);
            final BlockPos posEndWorld = PositionUtils.getMaxCorner(pos1, pos2);

            final Vec3i size = container.getSize();
            final int startX = Math.max(posStartWorld.getX(), 0);
            final int startY = Math.max(posStartWorld.getY(), 0);
            final int startZ = Math.max(posStartWorld.getZ(), 0);
            final int endX = Math.min(posEndWorld.getX(), size.getX() - 1);
            final int endY = Math.min(posEndWorld.getY(), size.getY() - 1);
            final int endZ = Math.min(posEndWorld.getZ(), size.getZ() - 1);

            //System.out.printf("DEBUG == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
            //        regionName, startX, startY, startZ, endX, endY, endZ, container.getSize().getX(), container.getSize().getY(), container.getSize().getZ());

            if (endX >= size.getX() || endY >= size.getY() || endZ >= size.getZ()) {
                System.out.printf("OUT OF BOUNDS == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
                        regionName, startX, startY, startZ, endX, endY, endZ, size.getX(), size.getY(), size.getZ());
                return false;
            }

            //System.out.printf("DEBUG == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
            //        regionName, startX, startY, startZ, endX, endY, endZ, size.getX(), size.getY(), size.getZ());

            final BlockState stateOriginal = getUntransformedBlockState(state, schematicPlacement, regionName);

            for (int y = startY; y <= endY; ++y) {
                for (int z = startZ; z <= endZ; ++z) {
                    for (int x = startX; x <= endX; ++x) {
                        final BlockState oldState = container.get(x, y, z);

                        if (oldState != stateOriginal && oldState.isAir() == false) {
                            container.set(x, y, z, air);
                            --totalBlocks;
                        }
                    }
                }
            }
        }

        schematicPlacement.getSchematic().getMetadata().setTotalBlocks(totalBlocks);

        return true;
    }

    private static boolean replaceAllIdenticalBlocks(final SchematicPlacementManager manager,
                                                     final PlacementPart part,
                                                     final BlockState stateOriginalIn,
                                                     final BlockState stateNewIn,
                                                     final BiPredicate<BlockState, BlockState> blockStateTest,
                                                     final BiFunction<BlockState, BlockState, BlockState> blockModifier,
                                                     final World world) {
        final SchematicPlacement schematicPlacement = part.getPlacement();
        final String selected = schematicPlacement.getSelectedSubRegionName();
        final List<String> regions = new ArrayList<>();

        // Some sub-region selected, only replace in that region
        if (selected != null) {
            regions.add(selected);
        }
        // The entire placement is selected, replace in all sub-regions
        else if (manager.getSelectedSchematicPlacement() == schematicPlacement) {
            regions.addAll(schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).keySet());
        }
        // Nothing from the targeted placement is selected, don't replace anything
        else {
            InfoUtils.showInGameMessage(MessageType.WARNING, 20000, "litematica.message.warn.schematic_rebuild_placement_not_selected");
            return false;
        }

        final LayerRange range = DataManager.getRenderLayerRange();

        int totalBlocks = schematicPlacement.getSchematic().getMetadata().getTotalBlocks();
        int increment = 0;

        if (stateOriginalIn.isAir() == false) {
            increment = stateNewIn.isAir() == false ? 0 : -1;
        } else {
            increment = stateNewIn.isAir() == false ? 1 : 0;
        }

        for (final String regionName : regions) {
            final LitematicaBlockStateContainer container = schematicPlacement.getSchematic().getSubRegionContainer(regionName);
            final SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);

            if (container == null || placement == null) {
                continue;
            }

            final int minX = range.getClampedValue(-30000000, Direction.Axis.X);
            final int minZ = range.getClampedValue(-30000000, Direction.Axis.Z);
            final int maxX = range.getClampedValue(30000000, Direction.Axis.X);
            final int maxZ = range.getClampedValue(30000000, Direction.Axis.Z);
            final int minY = range.getClampedValue(world.getBottomY(), Direction.Axis.Y);
            final int maxY = range.getClampedValue(world.getTopY() - 1, Direction.Axis.Y);

            final BlockPos posStart = new BlockPos(minX, minY, minZ);
            final BlockPos posEnd = new BlockPos(maxX, maxY, maxZ);

            final BlockPos pos1 = getReverserTransformedWorldPosition(posStart, schematicPlacement.getSchematic(),
                    regionName, schematicPlacement, schematicPlacement.getRelativeSubRegionPlacement(regionName));
            final BlockPos pos2 = getReverserTransformedWorldPosition(posEnd, schematicPlacement.getSchematic(),
                    regionName, schematicPlacement, schematicPlacement.getRelativeSubRegionPlacement(regionName));

            if (pos1 == null || pos2 == null) {
                return false;
            }

            final BlockPos posStartWorld = PositionUtils.getMinCorner(pos1, pos2);
            final BlockPos posEndWorld = PositionUtils.getMaxCorner(pos1, pos2);

            final Vec3i size = container.getSize();
            final int startX = Math.max(posStartWorld.getX(), 0);
            final int startY = Math.max(posStartWorld.getY(), 0);
            final int startZ = Math.max(posStartWorld.getZ(), 0);
            final int endX = Math.min(posEndWorld.getX(), size.getX() - 1);
            final int endY = Math.min(posEndWorld.getY(), size.getY() - 1);
            final int endZ = Math.min(posEndWorld.getZ(), size.getZ() - 1);

            //System.out.printf("DEBUG == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
            //        regionName, startX, startY, startZ, endX, endY, endZ, container.getSize().getX(), container.getSize().getY(), container.getSize().getZ());

            if (startX < 0 || startY < 0 || startZ < 0 ||
                    endX >= size.getX() ||
                    endY >= size.getY() ||
                    endZ >= size.getZ()) {
                System.out.printf("OUT OF BOUNDS == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
                        regionName, startX, startY, startZ, endX, endY, endZ, size.getX(), size.getY(), size.getZ());
                return false;
            }

            //System.out.printf("DEBUG == region: %s, sx: %d, sy: %s, sz: %d, ex: %d, ey: %d, ez: %d - size x: %d y: %d z: %d =============\n",
            //        regionName, startX, startY, startZ, endX, endY, endZ, size.getX(), size.getY(), size.getZ());

            final BlockState stateOriginal = getUntransformedBlockState(stateOriginalIn, schematicPlacement, regionName);
            final BlockState stateNew = getUntransformedBlockState(stateNewIn, schematicPlacement, regionName);

            for (int y = startY; y <= endY; ++y) {
                for (int z = startZ; z <= endZ; ++z) {
                    for (int x = startX; x <= endX; ++x) {
                        final BlockState oldState = container.get(x, y, z);

                        if (blockStateTest.test(oldState, stateOriginal)) {
                            final BlockState finalState = blockModifier.apply(stateNew, oldState);
                            container.set(x, y, z, finalState);
                            totalBlocks += increment;
                        }
                    }
                }
            }
        }

        final SchematicMetadata metadata = part.getPlacement().getSchematic().getMetadata();
        metadata.setTotalBlocks(totalBlocks);
        metadata.setTimeModifiedToNow();
        metadata.setModifiedSinceSaved();

        return true;
    }

/*SH    public static void moveCurrentlySelectedWorldRegionToLookingDirection(int amount, Entity entity, MinecraftClient mc)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            BlockPos pos = area.getEffectiveOrigin().offset(EntityUtils.getClosestLookingDirection(entity), amount);
            moveCurrentlySelectedWorldRegionTo(pos, mc);
        }
    }*/

/*SH    public static void moveCurrentlySelectedWorldRegionTo(BlockPos pos, MinecraftClient mc)
    {
        if (mc.player == null || EntityUtils.isCreativeMode(mc.player) == false)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
            return;
        }

        TaskScheduler scheduler = TaskScheduler.getServerInstanceIfExistsOrClient();
        long currentTime = System.currentTimeMillis();

        // Add a delay from the previous move operation, to allow time for
        // server -> client chunk/block syncing, otherwise a subsequent move
        // might wipe the area before the new blocks have arrived on the
        // client and thus the new move schematic would just be air.
        if ((currentTime - areaMovedTime) < 400 ||
            scheduler.hasTask(TaskSaveSchematic.class) ||
            scheduler.hasTask(TaskDeleteArea.class) ||
            scheduler.hasTask(TaskPasteSchematicPerChunkCommand.class) ||
            scheduler.hasTask(TaskPasteSchematicPerChunkDirect.class))
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.move.pending_tasks");
            return;
        }

//SH        SelectionManager sm = DataManager.getSelectionManager();
//SH        AreaSelection area = sm.getCurrentSelection();

*//*SH        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, "");
            LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);
            TaskSaveSchematic taskSave = new TaskSaveSchematic(schematic, area, info);
            taskSave.disableCompletionMessage();
            areaMovedTime = System.currentTimeMillis();

            taskSave.setCompletionListener(() ->
            {
                SchematicPlacement placement = SchematicPlacement.createFor(schematic, pos, "-", true, true);
                DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, false);

                TaskDeleteArea taskDelete = new TaskDeleteArea(area.getAllSubRegionBoxes(), true);
                taskDelete.disableCompletionMessage();
                areaMovedTime = System.currentTimeMillis();

                taskDelete.setCompletionListener(() ->
                {
                    TaskBase taskPaste;
                    LayerRange range = new LayerRange(SchematicWorldRefresher.INSTANCE);

                    if (mc.isIntegratedServerRunning())
                    {
                        taskPaste = new TaskPasteSchematicPerChunkDirect(Collections.singletonList(placement), range, false);
                    }
                    else
                    {
                        taskPaste = new TaskPasteSchematicPerChunkCommand(Collections.singletonList(placement), range, false);
                    }

                    taskPaste.disableCompletionMessage();
                    areaMovedTime = System.currentTimeMillis();

                    taskPaste.setCompletionListener(() ->
                    {
                        SchematicHolder.getInstance().removeSchematic(schematic);
                        area.moveEntireSelectionTo(pos, false);
                        areaMovedTime = System.currentTimeMillis();
                    });

                    scheduler.scheduleTask(taskPaste, 1);
                });

                scheduler.scheduleTask(taskDelete, 1);
            });

            scheduler.scheduleTask(taskSave, 1);
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
        }*//*
    }*/

/*SH    public static void cloneSelectionArea(MinecraftClient mc)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, mc.player.getName().getString());
            LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);
            TaskSaveSchematic taskSave = new TaskSaveSchematic(schematic, area, info);
            taskSave.disableCompletionMessage();
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            BlockPos originTmp;

            if (Configs.Generic.CLONE_AT_ORIGINAL_POS.getBooleanValue())
            {
                originTmp = area.getEffectiveOrigin();
            }
            else
            {
                originTmp = RayTraceUtils.getTargetedPosition(mc.world, entity, 6, false);

                if (originTmp == null)
                {
                    originTmp = fi.dy.masa.malilib.util.PositionUtils.getEntityBlockPos(entity);
                }
            }

            final BlockPos origin = originTmp;
            String name = schematic.getMetadata().getName();

            taskSave.setCompletionListener(() ->
            {
                SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
                SchematicPlacement placement = SchematicPlacement.createFor(schematic, origin, name, true, true);

                manager.addSchematicPlacement(placement, false);
                manager.setSelectedSchematicPlacement(placement);

                if (EntityUtils.isCreativeMode(mc.player))
                {
                    DataManager.setToolMode(ToolMode.PASTE_SCHEMATIC);
                }
            });

            TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(taskSave, 10);
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
        }
    }*/

    @Nullable
    public static BlockPos getSchematicContainerPositionFromWorldPosition(final BlockPos worldPos, final LitematicaSchematic schematic, final String regionName,
                                                                          final SchematicPlacement schematicPlacement, final SubRegionPlacement regionPlacement, final LitematicaBlockStateContainer container) {
        final BlockPos boxMinRel = getReverserTransformedWorldPosition(worldPos, schematic, regionName, schematicPlacement, regionPlacement);

        if (boxMinRel == null) {
            return null;
        }

        final int startX = boxMinRel.getX();
        final int startY = boxMinRel.getY();
        final int startZ = boxMinRel.getZ();
        final Vec3i size = container.getSize();

        /*
        if (startX < 0 || startY < 0 || startZ < 0 || startX >= size.getX() || startY >= size.getY() || startZ >= size.getZ())
        {
            System.out.printf("DEBUG ============= OUT OF BOUNDS - region: %s, startX: %d, startY %s, startZ: %d - size x: %d y: %s z: %d =============\n",
                    regionName, startX, startY, startZ, size.getX(), size.getY(), size.getZ());
            return null;
        }

        return boxMinRel;
        */

        return new BlockPos(MathHelper.clamp(startX, 0, size.getX() - 1),
                MathHelper.clamp(startY, 0, size.getY() - 1),
                MathHelper.clamp(startZ, 0, size.getZ() - 1));
    }

    @Nullable
    private static BlockPos getReverserTransformedWorldPosition(final BlockPos worldPos, final LitematicaSchematic schematic,
                                                                final String regionName, final SchematicPlacement schematicPlacement, final SubRegionPlacement regionPlacement) {
        final BlockPos origin = schematicPlacement.getOrigin();
        final BlockPos regionPos = regionPlacement.getPos();
        final BlockPos regionSize = schematic.getAreaSize(regionName);

        if (regionSize == null) {
            return null;
        }

        // These are the untransformed relative positions
        final BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize).add(regionPos);
        final BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

        // The transformed sub-region origin position
        final BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // The relative offset of the affected region's corners, to the sub-region's origin corner
        BlockPos relPos = new BlockPos(worldPos.getX() - origin.getX() - regionPosTransformed.getX(),
                worldPos.getY() - origin.getY() - regionPosTransformed.getY(),
                worldPos.getZ() - origin.getZ() - regionPosTransformed.getZ());

        // Reverse transform that relative offset, to get the untransformed orientation's offsets
        relPos = PositionUtils.getReverseTransformedBlockPos(relPos, regionPlacement.getMirror(), regionPlacement.getRotation());

        relPos = PositionUtils.getReverseTransformedBlockPos(relPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // Get the offset relative to the sub-region's minimum corner, instead of the origin corner (which can be at any corner)
        relPos = relPos.subtract(posMinRel.subtract(regionPos));

        return relPos;
    }

    public static BlockState getUntransformedBlockState(BlockState state, final SchematicPlacement schematicPlacement, final String subRegionName) {
        final SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(subRegionName);

        if (placement != null) {
            final BlockRotation rotationCombined = PositionUtils.getReverseRotation(schematicPlacement.getRotation().rotate(placement.getRotation()));
            final BlockMirror mirrorMain = schematicPlacement.getMirror();
            BlockMirror mirrorSub = placement.getMirror();

            if (mirrorSub != BlockMirror.NONE &&
                    (schematicPlacement.getRotation() == BlockRotation.CLOCKWISE_90 ||
                            schematicPlacement.getRotation() == BlockRotation.COUNTERCLOCKWISE_90)) {
                mirrorSub = mirrorSub == BlockMirror.FRONT_BACK ? BlockMirror.LEFT_RIGHT : BlockMirror.FRONT_BACK;
            }

            if (rotationCombined != BlockRotation.NONE) {
                state = state.rotate(rotationCombined);
            }

            if (mirrorSub != BlockMirror.NONE) {
                state = state.mirror(mirrorSub);
            }

            if (mirrorMain != BlockMirror.NONE) {
                state = state.mirror(mirrorMain);
            }
        }

        return state;
    }

    private static class ReplacementInfo {
        public final BlockPos pos;
        public final Direction side;
        public final Vec3d hitVec;
        public final BlockState stateOriginal;
        public final BlockState stateNew;

        public ReplacementInfo(final BlockPos pos, final Direction side, final Vec3d hitVec, final BlockState stateOriginal, final BlockState stateNew) {
            this.pos = pos;
            this.side = side;
            this.hitVec = hitVec;
            this.stateOriginal = stateOriginal;
            this.stateNew = stateNew;
        }
    }

    public static class SchematicVersionCreator implements IStringConsumerFeedback {
        @Override
        public boolean setString(final String string) {
//SH            return DataManager.getSchematicProjectsManager().commitNewVersion(string);
            return true;
        }
    }
}
