package fi.dy.masa.litematica.util;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nullable;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.malilib.util.game.wrap.GameWrap;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.mixin.entity.IMixinSignBlockEntity;
import fi.dy.masa.litematica.mixin.block.IMixinWallMountedBlock;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class WorldUtils
{
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();
    private static long easyPlaceLastPickBlockTime = System.nanoTime();

    public static double getValidBlockRange(MinecraftClient mc)
    {
        return Configs.Generic.EASY_PLACE_VANILLA_REACH.getBooleanValue() ? mc.player.getBlockInteractionRange() : mc.player.getBlockInteractionRange() + 1.0;
    }

    public static boolean shouldPreventBlockUpdates(World world)
    {
        return ((IWorldUpdateSuppressor) world).litematica_getShouldPreventBlockUpdates();
    }

    public static void setShouldPreventBlockUpdates(World world, boolean preventUpdates)
    {
        ((IWorldUpdateSuppressor) world).litematica_setShouldPreventBlockUpdates(preventUpdates);
    }

    public static boolean convertLitematicaSchematicToLitematicaSchematic(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertLitematicaSchematicToLitematicaSchematic(inputDir, inputFileName, outputFileName, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    public static boolean convertSpongeSchematicToLitematicaSchematic(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        DataFixerMode oldMode = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(DataFixerMode.ALWAYS);
        LitematicaSchematic origSchematic = convertSpongeSchematicToLitematicaSchematic(inputDir, inputFileName);

        if (origSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.sponge_to_litematica.failed_to_read_sponge");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return false;
        }

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld(null);
        BlockPos size = new BlockPos(origSchematic.getTotalSize());
        loadChunksSchematicWorld(world, BlockPos.ORIGIN, size);
        SchematicPlacement schematicPlacement = SchematicPlacement.createForSchematicConversion(origSchematic, BlockPos.ORIGIN);
        origSchematic.placeToWorld(world, schematicPlacement, false); // TODO use a per-chunk version for a bit more speed

        String subRegionName = FileUtils.getNameWithoutExtension(inputFileName);
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        Box box = area.getSelectedSubRegionBox();
        area.setSubRegionCornerPos(box, Corner.CORNER_1, BlockPos.ORIGIN);
        area.setSubRegionCornerPos(box, Corner.CORNER_2, size.add(-1, -1, -1));
        LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);

        LitematicaSchematic newSchem = LitematicaSchematic.createFromWorld(world, area, info, "?", feedback);

        if (newSchem == null)
        {
            feedback.setString("litematica.error.schematic_conversion.sponge_to_litematica.failed_to_create_litematic");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return false;
        }

        SchematicMetadata origMetadata = origSchematic.getMetadata();

        if (origMetadata.getAuthor().isEmpty() || origMetadata.getAuthor() == "?")
        {
            newSchem.getMetadata().setAuthor(GameWrap.getPlayerName());
        }
        else
        {
            newSchem.getMetadata().setAuthor(origMetadata.getAuthor());
        }

        if (origMetadata.getName().isEmpty() || origMetadata.getName() == "?")
        {
            newSchem.getMetadata().setName(subRegionName);
        }
        else
        {
            newSchem.getMetadata().setName(origMetadata.getName());
        }

        newSchem.getMetadata().setDescription("Converted Sponge V"+origMetadata.getSchematicVersion()+", Schema "+origMetadata.getSchemaString());
        newSchem.getMetadata().setTimeCreated(origMetadata.getTimeCreated());
        newSchem.getMetadata().setTimeModifiedToNow();

        world.clearEntities();
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
        return newSchem.writeToFile(outputDir, outputFileName, override);
    }

    public static boolean convertSchematicaSchematicToLitematicaSchematic(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertSchematicaSchematicToLitematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    @Nullable
    public static LitematicaSchematic convertLitematicaSchematicToLitematicaSchematic(Path inputDir, String inputFileName,
                                                                                      String outputFilename,
                                                                                      IStringConsumer feedback)
    {
        DataFixerMode oldMode = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(DataFixerMode.ALWAYS);
        LitematicaSchematic newSchematic = LitematicaSchematic.createFromFile(inputDir, inputFileName, FileType.LITEMATICA_SCHEMATIC);

        if (newSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematic_to_litematica.failed_to_read_litematic");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return null;
        }

        SchematicMetadata origMetadata = newSchematic.getMetadata();

        if (origMetadata.getAuthor().isEmpty() || origMetadata.getAuthor() == "?")
        {
            newSchematic.getMetadata().setAuthor(GameWrap.getPlayerName());
        }
        else
        {
            newSchematic.getMetadata().setAuthor(origMetadata.getAuthor());
        }

        if (origMetadata.getName().isEmpty() || origMetadata.getName() == "?")
        {
            newSchematic.getMetadata().setName(outputFilename);
        }
        else
        {
            newSchematic.getMetadata().setName(origMetadata.getName());
        }

        newSchematic.getMetadata().setDescription("Converted Litematic V"+origMetadata.getSchematicVersion()+", Schema "+origMetadata.getSchemaString());
        newSchematic.getMetadata().setTimeCreated(origMetadata.getTimeCreated());
        newSchematic.getMetadata().setTimeModifiedToNow();

        Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
        return newSchematic;
    }

    @Nullable
    public static LitematicaSchematic convertSchematicaSchematicToLitematicaSchematic(Path inputDir, String inputFileName,
            boolean ignoreEntities, IStringConsumer feedback)
    {
        DataFixerMode oldMode = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(DataFixerMode.ALWAYS);
        SchematicaSchematic schematic = SchematicaSchematic.createFromFile(inputDir.resolve(inputFileName));

        if (schematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_read_schematic");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return null;
        }

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld(null);

        loadChunksSchematicWorld(world, BlockPos.ORIGIN, schematic.getSize());
        StructurePlacementData placementSettings = new StructurePlacementData();
        placementSettings.setIgnoreEntities(ignoreEntities);
        schematic.placeSchematicDirectlyToChunks(world, BlockPos.ORIGIN, placementSettings);

        String subRegionName = FileUtils.getNameWithoutExtension(inputFileName) + " (Converted Schematic)";
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        Box box = area.getSelectedSubRegionBox();
        area.setSubRegionCornerPos(box, Corner.CORNER_1, BlockPos.ORIGIN);
        area.setSubRegionCornerPos(box, Corner.CORNER_2, (new BlockPos(schematic.getSize())).add(-1, -1, -1));
        LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);

        LitematicaSchematic newSchematic = LitematicaSchematic.createFromWorld(world, area, info, "?", feedback);

        if (newSchematic != null && ignoreEntities == false)
        {
            newSchematic.takeEntityDataFromSchematicaSchematic(schematic, subRegionName);
        }
        else
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_create_schematic");
        }

        newSchematic.getMetadata().setName(subRegionName);
        newSchematic.getMetadata().setAuthor(GameWrap.getPlayerName());
        newSchematic.getMetadata().setDescription("Converted Schematica Schematic, Schema "+schematic.getMetadata().getSchema());
        newSchematic.getMetadata().setTimeCreated(System.currentTimeMillis());
        newSchematic.getMetadata().setTimeModifiedToNow();

        world.clearEntities();
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
        return newSchematic;
    }

    public static boolean convertStructureToLitematicaSchematic(Path structureDir, String structureFileName,
                                                                Path outputDir, String outputFileName, boolean override)
    {
        LitematicaSchematic litematicaSchematic = convertStructureToLitematicaSchematic(structureDir, structureFileName);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    public static boolean convertStructureToLitematicaSchematic(Path structureDir, String structureFileName,
            Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        DataFixerMode oldMode = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(DataFixerMode.ALWAYS);
        LitematicaSchematic origStructure = convertStructureToLitematicaSchematic(structureDir, structureFileName);

        if (origStructure == null)
        {
            feedback.setString("litematica.error.schematic_conversion.structure_to_litematica.failed_to_read_structure");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return false;
        }

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld(null);
        BlockPos size = new BlockPos(origStructure.getTotalSize());
        loadChunksSchematicWorld(world, BlockPos.ORIGIN, size);
        SchematicPlacement schematicPlacement = SchematicPlacement.createForSchematicConversion(origStructure, BlockPos.ORIGIN);
        origStructure.placeToWorld(world, schematicPlacement, false); // TODO use a per-chunk version for a bit more speed

        String subRegionName = FileUtils.getNameWithoutExtension(structureFileName);
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        Box box = area.getSelectedSubRegionBox();
        area.setSubRegionCornerPos(box, Corner.CORNER_1, BlockPos.ORIGIN);
        area.setSubRegionCornerPos(box, Corner.CORNER_2, size.add(-1, -1, -1));
        LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);

        LitematicaSchematic newSchem = LitematicaSchematic.createFromWorld(world, area, info, "?", feedback);

        if (newSchem == null)
        {
            feedback.setString("litematica.error.schematic_conversion.structure_to_litematica.failed_to_create_litematic");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return false;
        }

        SchematicMetadata origMetadata = origStructure.getMetadata();

        if (origMetadata.getAuthor().isEmpty() || origMetadata.getAuthor() == "?")
        {
            newSchem.getMetadata().setAuthor(GameWrap.getPlayerName());
        }
        else
        {
            newSchem.getMetadata().setAuthor(origMetadata.getAuthor());
        }

        if (origMetadata.getName().isEmpty() || origMetadata.getName() == "?")
        {
            newSchem.getMetadata().setName(subRegionName);
        }
        else
        {
            newSchem.getMetadata().setName(origMetadata.getName());
        }

        newSchem.getMetadata().setDescription("Converted Vanilla Strucutre, Schema "+origMetadata.getSchemaString());
        newSchem.getMetadata().setTimeCreated(origMetadata.getTimeCreated());
        newSchem.getMetadata().setTimeModifiedToNow();

        boolean result = newSchem.writeToFile(outputDir, outputFileName, override);
//        System.out.printf("Vanilla IMPORT DUMP (OUT-2) -->\n%s\n", newSchem.toString());

        world.clearEntities();
        return result;
    }

    @Nullable
    public static LitematicaSchematic convertSpongeSchematicToLitematicaSchematic(Path dir, String fileName)
    {
        try
        {
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(dir, fileName, FileType.SPONGE_SCHEMATIC);

            if (schematic == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to read the Sponge schematic from '" + fileName + '"');
            }

            return schematic;
        }
        catch (Exception e)
        {
            String msg = "Exception while trying to load the Sponge schematic: " + e.getMessage();
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, msg);
            Litematica.LOGGER.error(msg);
        }

        return null;
    }

    @Nullable
    public static LitematicaSchematic convertStructureToLitematicaSchematic(Path structureDir, String structureFileName)
    {
        try
        {
            LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromFile(structureDir, structureFileName, FileType.VANILLA_STRUCTURE);

            if (litematicaSchematic == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to read the vanilla structure template from '" + structureFileName + '"');
            }

            return litematicaSchematic;
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Exception while trying to load the vanilla structure: " + e.getMessage());
            Litematica.LOGGER.error("Exception while trying to load the vanilla structure: " + e.getMessage());
        }

        return null;
    }

    public static boolean convertLitematicaSchematicToSchematicaSchematic(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        //SchematicaSchematic schematic = convertLitematicaSchematicToSchematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        //return schematic != null && schematic.writeToFile(outputDir, outputFileName, override, feedback);
        // TODO 1.13
        return false;
    }

    public static boolean convertLitematicaSchematicToV6LitematicaSchematic(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic v7LitematicaSchematic = LitematicaSchematic.createFromFile(inputDir, inputFileName, FileType.LITEMATICA_SCHEMATIC);

        if (v7LitematicaSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematica_to_schematic.failed_to_read_schematic");
            return false;
        }

        LitematicaSchematic v6LitematicaSchematic = LitematicaSchematic.createEmptySchematicFromExisting(v7LitematicaSchematic, MinecraftClient.getInstance().player.getName().getString());
        v6LitematicaSchematic.downgradeV7toV6Schematic(v7LitematicaSchematic);

        if (v6LitematicaSchematic.writeToFile(outputDir, outputFileName, override, true))
        {
            return true;
        }
        else
        {
            feedback.setString("litematica.error.schematic_conversion.litematica_to_schematic.failed_to_downgrade_litematic");
            return false;
        }
    }

    public static boolean convertLitematicaSchematicToVanillaStructure(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        StructureTemplate template = convertLitematicaSchematicToVanillaStructure(inputDir, inputFileName, ignoreEntities, feedback);
        return writeVanillaStructureToFile(template, outputDir, outputFileName, override, feedback);
    }

    @Nullable
    public static StructureTemplate convertLitematicaSchematicToVanillaStructure(Path inputDir, String inputFileName, boolean ignoreEntities, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromFile(inputDir, inputFileName);

        if (litematicaSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematica_to_schematic.failed_to_read_schematic");
            return null;
        }

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld(null);

        BlockPos size = new BlockPos(litematicaSchematic.getTotalSize());
        loadChunksSchematicWorld(world, BlockPos.ORIGIN, size);
        SchematicPlacement schematicPlacement = SchematicPlacement.createForSchematicConversion(litematicaSchematic, BlockPos.ORIGIN);
        litematicaSchematic.placeToWorld(world, schematicPlacement, false); // TODO use a per-chunk version for a bit more speed

        StructureTemplate template = new StructureTemplate();
        template.saveFromWorld(world, BlockPos.ORIGIN, size, ignoreEntities == false, List.of(Blocks.STRUCTURE_VOID));

        world.clearEntities();
        return template;
    }

    private static boolean writeVanillaStructureToFile(StructureTemplate template, Path dir, String fileNameIn, boolean override, IStringConsumer feedback)
    {
        String fileName = fileNameIn;
        String extension = ".nbt";

        if (fileName.endsWith(extension) == false)
        {
            fileName = fileName + extension;
        }

        Path file = dir.resolve(fileName);
        FileOutputStream os = null;

        try
        {
            if (!Files.exists(dir))
            {
                FileUtils.createDirectoriesIfMissing(dir);
            }

            if (!Files.isDirectory(dir))
            {
                feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.toAbsolutePath()));
                return false;
            }

            if (override == false && Files.exists(file))
            {
                feedback.setString(StringUtils.translate("litematica.error.structure_write_to_file_failed.exists", file.toAbsolutePath()));
                return false;
            }

            /*
            NbtCompound tag = template.writeNbt(new NbtCompound());
            os = new FileOutputStream(file);
            NbtIo.writeCompressed(tag, os);
            os.close();
             */

            NbtIo.writeCompressed(template.writeNbt(new NbtCompound()), file);

            return true;
        }
        catch (Exception e)
        {
            feedback.setString(StringUtils.translate("litematica.error.structure_write_to_file_failed.exception", file.toAbsolutePath()));
        }

        return false;
    }

    public static boolean isClientChunkLoaded(ClientWorld world, int chunkX, int chunkZ)
    {
        boolean test = ((ClientChunkManager) world.getChunkManager()).getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;

        //System.out.printf("isClientChunkLoaded(): World: [%s] / ChunkPos[%d, %d] --> [%s]\n", world.toString(), chunkX, chunkZ, test);

        return test;
    }

    public static void loadChunksSchematicWorld(WorldSchematic world, BlockPos origin, Vec3i areaSize)
    {
        BlockPos posEnd = origin.add(PositionUtils.getRelativeEndPositionFromAreaSize(areaSize));
        BlockPos posMin = PositionUtils.getMinCorner(origin, posEnd);
        BlockPos posMax = PositionUtils.getMaxCorner(origin, posEnd);
        final int cxMin = posMin.getX() >> 4;
        final int czMin = posMin.getZ() >> 4;
        final int cxMax = posMax.getX() >> 4;
        final int czMax = posMax.getZ() >> 4;

        for (int cz = czMin; cz <= czMax; ++cz)
        {
            for (int cx = cxMin; cx <= cxMax; ++cx)
            {
                world.getChunkProvider().loadChunk(cx, cz);
            }
        }
    }

    public static void setToolModeBlockState(ToolMode mode, boolean primary, MinecraftClient mc)
    {
        BlockState state = Blocks.AIR.getDefaultState();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(mc.world, entity, getValidBlockRange(mc));

        if (wrapper != null)
        {
            BlockHitResult trace = wrapper.getBlockHitResult();

            if (trace != null && trace.getType() == HitResult.Type.BLOCK)
            {
                BlockPos pos = trace.getBlockPos();

                if (wrapper.getHitType() == HitType.SCHEMATIC_BLOCK)
                {
                    state = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
                }
                else if (wrapper.getHitType() == HitType.VANILLA_BLOCK)
                {
                    state = mc.world.getBlockState(pos);
                }
            }
        }

        if (primary)
        {
            mode.setPrimaryBlock(state);
        }
        else
        {
            mode.setSecondaryBlock(state);
        }
    }

    /**
     * Does a ray trace to the schematic world, and returns either the closest or the furthest hit block.
     * @param closest
     * @param mc
     * @return true if the correct item was or is in the player's hand after the pick block
     */
    public static boolean doSchematicWorldPickBlock(boolean closest, MinecraftClient mc)
    {
        BlockPos pos;

        if (closest)
        {
            pos = RayTraceUtils.getSchematicWorldTraceIfClosest(mc.world, mc.player, getValidBlockRange(mc));
        }
        else
        {
            pos = RayTraceUtils.getFurthestSchematicWorldBlockBeforeVanilla(mc.world, mc.player, getValidBlockRange(mc), true);
        }

        if (pos != null)
        {
            World world = SchematicWorldHandler.getSchematicWorld();
            BlockState state = world.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);

            InventoryUtils.schematicWorldPickBlock(stack, pos, world, mc);

            return true;
        }

        return false;
    }

    public static void insertSignTextFromSchematic(SignBlockEntity beClient, String[] screenTextArr, boolean front)
    {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic != null)
        {
            BlockEntity beSchem = worldSchematic.getBlockEntity(beClient.getPos());

            if (beSchem instanceof SignBlockEntity)
            {
                IMixinSignBlockEntity beMixinSchem = (IMixinSignBlockEntity) beSchem;
                SignText textSchematic = front ? beMixinSchem.litematica_getFrontText() : beMixinSchem.litematica_getBackText();

                if (textSchematic != null)
                {
                    for (int i = 0; i < screenTextArr.length; ++i)
                    {
                        screenTextArr[i] = textSchematic.getMessage(i, false).getString();
                    }
                    beClient.setText(textSchematic, front);
                }
            }
        }
    }

    public static void easyPlaceOnUseTick(MinecraftClient mc)
    {
        if (mc.player != null && DataManager.getToolMode() != ToolMode.REBUILD &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld())
            //&& Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue() == false)
        {
            WorldUtils.doEasyPlaceAction(mc);
        }
    }

    public static boolean handleEasyPlace(MinecraftClient mc)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            //Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue() == false &&
            DataManager.getToolMode() != ToolMode.REBUILD)
        {
            ActionResult result = doEasyPlaceAction(mc);

            if (result == ActionResult.FAIL)
            {
                MessageOutputType type = (MessageOutputType) Configs.Generic.PLACEMENT_RESTRICTION_WARN.getOptionListValue();

                if (type == MessageOutputType.MESSAGE)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.easy_place_fail");
                }
                else if (type == MessageOutputType.ACTIONBAR)
                {
                    InfoUtils.printActionbarMessage("litematica.message.easy_place_fail");
                }

                return true;
            }

            return result != ActionResult.PASS;
        }

        return false;
    }

    private static ActionResult doEasyPlaceAction(MinecraftClient mc)
    {
        RayTraceWrapper traceWrapper;
        double traceMaxRange = getValidBlockRange(mc);

        if (Configs.Generic.EASY_PLACE_FIRST.getBooleanValue())
        {
            // Temporary hack, using this same config here
            boolean targetFluids = Configs.InfoOverlays.INFO_OVERLAYS_TARGET_FLUIDS.getBooleanValue();
            traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, traceMaxRange, true, targetFluids, false);
        }
        else
        {
            traceWrapper = RayTraceUtils.getFurthestSchematicWorldTraceBeforeVanilla(mc.world, mc.player, traceMaxRange);

            if (traceWrapper == null && placementRestrictionInEffect(mc))
            {
                return ActionResult.FAIL;
            }
        }

        if (traceWrapper == null)
        {
            return ActionResult.PASS;
        }

        if (traceWrapper.getHitType() == HitType.SCHEMATIC_BLOCK)
        {
            BlockHitResult trace = traceWrapper.getBlockHitResult();
            HitResult traceVanilla = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, false, traceMaxRange);
            BlockPos pos = trace.getBlockPos();
            World world = SchematicWorldHandler.getSchematicWorld();
            BlockState stateSchematic = world.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

            // Already placed to that position, possible server sync delay
            if (easyPlaceIsPositionCached(pos))
            {
                return ActionResult.FAIL;
            }

            // Ignore action if too fast
            if (easyPlaceIsTooFast())
            {
                return ActionResult.FAIL;
            }

            if (stack.isEmpty() == false)
            {
                BlockState stateClient = mc.world.getBlockState(pos);

                if (stateSchematic == stateClient)
                {
                    return ActionResult.FAIL;
                }

                // Abort if there is already a block in the target position
                if (easyPlaceBlockChecksCancel(stateSchematic, stateClient, mc.player, traceVanilla, stack))
                {
                    return ActionResult.FAIL;
                }

                InventoryUtils.schematicWorldPickBlock(stack, pos, world, mc);
                Hand hand = EntityUtils.getUsedHandForItem(mc.player, stack);

                // Abort if a wrong item is in the player's hand
                if (hand == null)
                {
                    return ActionResult.FAIL;
                }

                Vec3d hitPos = trace.getPos();
                Direction sideOrig = trace.getSide();
                EasyPlaceProtocol protocol = PlacementHandler.getEffectiveProtocolVersion();

                if (protocol == EasyPlaceProtocol.NONE || protocol == EasyPlaceProtocol.SLAB_ONLY)
                {
                    // If there is a block in the world right behind the targeted schematic block, then use
                    // that block as the click position
                    if (traceVanilla != null && traceVanilla.getType() == HitResult.Type.BLOCK)
                    {
                        BlockHitResult hitResult = (BlockHitResult) traceVanilla;
                        BlockPos posVanilla = hitResult.getBlockPos();
                        Direction sideVanilla = hitResult.getSide();
                        BlockState stateVanilla = mc.world.getBlockState(posVanilla);
                        Vec3d hit = traceVanilla.getPos();
                        ItemPlacementContext ctx = new ItemPlacementContext(new ItemUsageContext(mc.player, hand, hitResult));

                        if (stateVanilla.canReplace(ctx) == false)
                        {
                            posVanilla = posVanilla.offset(sideVanilla);

                            if (pos.equals(posVanilla))
                            {
                                hitPos = hit;
                                sideOrig = sideVanilla;
                            }
                        }
                    }
                }

                //System.out.printf("doEasyPlaceAction - stateSchematic [%s] // sideOrig [%s]\n", stateSchematic.toString(), sideOrig.getName());

                Direction side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);

                // Support for special cases
                PlacementProtocolData placementData = applyPlacementProtocolAll(pos, stateSchematic, hitPos);

                if (placementData.mustFail)
                {
                    return ActionResult.FAIL; //disallowed cases (e.g. trying to place torch with no support block)
                }

                if (placementData.handled)
                {
                    pos = placementData.pos;
                    side = placementData.side;
                    hitPos = placementData.hitVec;
                }

                if (protocol == EasyPlaceProtocol.V3)
                {
                    hitPos = applyPlacementProtocolV3(pos, stateSchematic, hitPos);
                }
                else if (protocol == EasyPlaceProtocol.V2)
                {
                    // Carpet Accurate Block Placement protocol support, plus slab support
                    hitPos = applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);
                }
                else if (protocol == EasyPlaceProtocol.SLAB_ONLY)
                {
                    // Slab support only
                    hitPos = applyBlockSlabProtocol(pos, stateSchematic, hitPos);
                }

                // Mark that this position has been handled (use the non-offset position that is checked above)
                cacheEasyPlacePosition(pos);

                BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);

                //System.out.printf("interact -> pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                // pos, side, hitPos
                ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, hitResult);

                // swing hand fix, see MinecraftClient#doItemUse
                if (ActionResult.SUCCESS.swingSource().equals(ActionResult.SwingSource.CLIENT) &&
                    Configs.Generic.EASY_PLACE_SWING_HAND.getBooleanValue())
                {
                    mc.player.swingHand(hand);
                }

                if (stateSchematic.getBlock() instanceof SlabBlock && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE)
                {
                    stateClient = mc.world.getBlockState(pos);

                    if (stateClient.getBlock() instanceof SlabBlock && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE)
                    {
                        side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        hitResult = new BlockHitResult(hitPos, side, pos, false);
                        mc.interactionManager.interactBlock(mc.player, hand, hitResult);
                    }
                }
            }

            return ActionResult.SUCCESS;
        }
        else if (traceWrapper.getHitType() == HitType.VANILLA_BLOCK)
        {
            return placementRestrictionInEffect(mc) ? ActionResult.FAIL : ActionResult.PASS;
        }

        return ActionResult.PASS;
    }

    private static boolean easyPlaceBlockChecksCancel(BlockState stateSchematic, BlockState stateClient,
            PlayerEntity player, HitResult trace, ItemStack stack)
    {
        Block blockSchematic = stateSchematic.getBlock();

        if (blockSchematic instanceof SlabBlock && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE)
        {
            Block blockClient = stateClient.getBlock();

            if (blockClient instanceof SlabBlock && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE)
            {
                return blockSchematic != blockClient;
            }
        }

        if (trace.getType() != HitResult.Type.BLOCK)
        {
            return false;
        }

        BlockHitResult hitResult = (BlockHitResult) trace;
        ItemPlacementContext ctx = new ItemPlacementContext(new ItemUsageContext(player, Hand.MAIN_HAND, hitResult));

        return !stateClient.canReplace(ctx);
    }

    public static class PlacementProtocolData
    {
        boolean handled;
        boolean mustFail;
        BlockPos pos;
        Direction side;
        Vec3d hitVec;
    }

    public static PlacementProtocolData applyPlacementProtocolAll(BlockPos pos, BlockState stateSchematic, Vec3d hitVecIn)
    {
        PlacementProtocolData placementData = new PlacementProtocolData();

        Block stateBlock = stateSchematic.getBlock();
        final World world = MinecraftClient.getInstance().world;

        //Wall-mountable blocks
        if (stateBlock instanceof AbstractTorchBlock ||
            stateBlock instanceof AbstractBannerBlock ||
            stateBlock instanceof AbstractSignBlock ||
            stateBlock instanceof AbstractSkullBlock)
        {
            placementData.handled = true;
            placementData.hitVec = hitVecIn;

            if (stateBlock instanceof WallTorchBlock ||
                stateBlock instanceof WallRedstoneTorchBlock ||
                stateBlock instanceof WallBannerBlock ||
                stateBlock instanceof WallSignBlock ||
                stateBlock instanceof WallSkullBlock)
            {
                placementData.side = stateSchematic.get(Properties.HORIZONTAL_FACING);
                placementData.pos = pos.offset(placementData.side.getOpposite());
            }
            else
            {
                placementData.side = Direction.UP;
                placementData.pos = pos.down();
            }

            //If the supporting block doesn't exist, fail
            BlockState stateFacing = world.getBlockState(placementData.pos);
            if (stateFacing == null || stateFacing.isAir())
            {
                placementData.mustFail = true;
            }
        }
        else if (stateBlock instanceof WallMountedBlock)
        {
            //If the supporting block doesn't exist, fail
            if (!((IMixinWallMountedBlock)stateBlock).litematica_invokeCanPlaceAt(stateSchematic, world, pos))
                placementData.mustFail = true;
        }

        return placementData;
    }

    /**
     * Apply the Carpet-Extra mod accurate block placement protocol support
     */
    public static Vec3d applyCarpetProtocolHitVec(BlockPos pos, BlockState state, Vec3d hitVecIn)
    {
        double x = hitVecIn.x;
        double y = hitVecIn.y;
        double z = hitVecIn.z;
        Block block = state.getBlock();
        Optional<Direction> facing = BlockUtils.getFirstPropertyFacingValue(state);
        final int propertyIncrement = 16;
        boolean hasData = false;
        int protocolValue = 0;

        if (facing.isPresent())
        {
            //System.out.printf("(WorldUtils):v2: applying: 0x%08X (getFirstDirectionProperty() -> %s)\n", protocolValue, facing.get().getName());

            protocolValue = facing.get().getIndex();
            hasData = true; // without this down rotation would not be detected >_>
        }
        else if (state.contains(Properties.AXIS))
        {
            Direction.Axis axis = state.get(Properties.AXIS);
            //System.out.printf("(WorldUtils):v2: 0x%08X (current axis %s)\n", protocolValue, axis.getName());

            protocolValue = axis.ordinal();
            hasData = true; // without this id 0 would not be detected >_>
            //System.out.printf("(WorldUtils):v2: axis current state: %s, protocolValue 0x%08X\n", state.toString(), protocolValue);
        }

        if (block instanceof RepeaterBlock)
        {
            protocolValue += state.get(RepeaterBlock.DELAY) * propertyIncrement;
        }
        else if (block instanceof ComparatorBlock && state.get(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT)
        {
            protocolValue += propertyIncrement;
        }
        else if (state.contains(Properties.BLOCK_HALF) && state.get(Properties.BLOCK_HALF) == BlockHalf.TOP)
        {
            protocolValue += propertyIncrement;
        }
        else if (state.contains(Properties.SLAB_TYPE) && state.get(Properties.SLAB_TYPE) == SlabType.TOP)
        {
            protocolValue += propertyIncrement;
        }

        y = applySlabOrStairHitVecY(y, pos, state);

        if (protocolValue != 0 || hasData)
        {
            x += (protocolValue * 2) + 2;
        }

        //System.out.printf("(WorldUtils):v2: stateIn: %s // Vec3d Out [%s]\n", state.toString(), new Vec3d(x, y, z).toString());

        return new Vec3d(x, y, z);
    }

    private static double applySlabOrStairHitVecY(double origY, BlockPos pos, BlockState state)
    {
        double y = origY;

        if (state.contains(Properties.SLAB_TYPE))
        {
            y = pos.getY();

            if (state.get(Properties.SLAB_TYPE) == SlabType.TOP)
            {
                y += 0.99;
            }
        }
        else if (state.contains(Properties.BLOCK_HALF))
        {
            y = pos.getY();

            if (state.get(Properties.BLOCK_HALF) == BlockHalf.TOP)
            {
                y += 0.99;
            }
        }

        return y;
    }

    private static Vec3d applyBlockSlabProtocol(BlockPos pos, BlockState state, Vec3d hitVecIn)
    {
        double newY = applySlabOrStairHitVecY(hitVecIn.y, pos, state);
        return newY != hitVecIn.y ? new Vec3d(hitVecIn.x, newY, hitVecIn.z) : hitVecIn;
    }

    public static <T extends Comparable<T>> Vec3d applyPlacementProtocolV3(BlockPos pos, BlockState state, Vec3d hitVecIn)
    {
        Collection<Property<?>> props = state.getBlock().getStateManager().getProperties();

        if (props.isEmpty())
        {
            return hitVecIn;
        }

        double relX = hitVecIn.x - pos.getX();
        int protocolValue = 0;
        int shiftAmount = 1;
        int propCount = 0;

        //System.out.printf("(WorldUtils):v3: hit vec.x %s, pos.x: %s\n", hitVecIn.getX(), pos.getX());
        //System.out.printf("(WorldUtils):v3: raw protocol value in: 0x%08X\n", protocolValue);

        Optional<EnumProperty<Direction>> property = BlockUtils.getFirstDirectionProperty(state);

        // DirectionProperty - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (property.isPresent() && property.get() != Properties.VERTICAL_DIRECTION)
        {
            Direction direction = state.get(property.get());
            protocolValue |= direction.getIndex() << shiftAmount;
            //System.out.printf("(WorldUtils):v3: applying: 0x%08X (getFirstDirection %s)\n", protocolValue, property.get().getName());
            shiftAmount += 3;
            ++propCount;
        }

        List<Property<?>> propList = new ArrayList<>(props);
        propList.sort(Comparator.comparing(Property::getName));

        try
        {
            for (Property<?> p : propList)
            {
                //System.out.printf("(WorldUtils):v3: check property [%s], whitelisted [%s], blacklisted [%s]\n", p.getName(), PlacementHandler.WHITELISTED_PROPERTIES.contains(p), PlacementHandler.BLACKLISTED_PROPERTIES.contains(p));

                if (property.isPresent() && property.get().equals(p))
                {
                    //System.out.printf("(WorldUtils):v3: skipping prot val: 0x%08X [Property %s]\n", protocolValue, p.getName());
                    continue;
                }
                if (PlacementHandler.WHITELISTED_PROPERTIES.contains(p) &&
                    !PlacementHandler.BLACKLISTED_PROPERTIES.contains(p))
                {
                    @SuppressWarnings("unchecked")
                    Property<T> prop = (Property<T>) p;
                    List<T> list = new ArrayList<>(prop.getValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = MathHelper.floorLog2(MathHelper.smallestEncompassingPowerOfTwo(list.size()));
                    int valueIndex = list.indexOf(state.get(prop));

                    //System.out.printf("(WorldUtils):v3: trying to apply valInd: %d, bits: %d, prot val: 0x%08X [Property %s]\n", valueIndex, requiredBits, protocolValue, prop.getName());

                    if (valueIndex != -1)
                    {
                        //System.out.printf("(WorldUtils):v3: requesting: %s = %s, index: %d\n", prop.getName(), state.get(prop), valueIndex);
                        protocolValue |= (valueIndex << shiftAmount);
                        shiftAmount += requiredBits;
                        ++propCount;
                    }
                }
                /*
                else
                {
                    System.out.printf("(WorldUtils):v3: skipping prot val: 0x%08X [Property %s]\n", protocolValue, p.getName());
                }
                 */
            }
        }
        catch (Exception e)
        {
            Litematica.LOGGER.warn("Exception trying to request placement protocol value", e);
        }

        if (propCount > 0)
        {
            double x = pos.getX() + relX + 2 + protocolValue;
            //System.out.printf("(WorldUtils):v3: request prot value 0x%08X\n", protocolValue + 2);
            return new Vec3d(x, hitVecIn.y, hitVecIn.z);
        }

        return hitVecIn;
    }

    private static Direction applyPlacementFacing(BlockState stateSchematic, Direction side, BlockState stateClient)
    {
        Block blockSchematic = stateSchematic.getBlock();
        Block blockClient = stateClient.getBlock();

        if (blockSchematic instanceof SlabBlock)
        {
            if (stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE &&
                blockClient instanceof SlabBlock &&
                stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE)
            {
                if (stateClient.get(SlabBlock.TYPE) == SlabType.TOP)
                {
                    return Direction.DOWN;
                }
                else
                {
                    return Direction.UP;
                }
            }
            // Single slab
            else
            {
                return Direction.NORTH;
            }
        }
        else if (stateSchematic.contains(Properties.BLOCK_HALF))
        {
            side = stateSchematic.get(Properties.BLOCK_HALF) == BlockHalf.TOP ? Direction.DOWN : Direction.UP;
        }

        return side;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @return
     */
    public static boolean handlePlacementRestriction(MinecraftClient mc)
    {
        boolean cancel = placementRestrictionInEffect(mc);

        if (cancel)
        {
            MessageOutputType type = (MessageOutputType) Configs.Generic.PLACEMENT_RESTRICTION_WARN.getOptionListValue();

            if (type == MessageOutputType.MESSAGE)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.placement_restriction_fail");
            }
            else if (type == MessageOutputType.ACTIONBAR)
            {
                InfoUtils.printActionbarMessage("litematica.message.placement_restriction_fail");
            }
        }

        return cancel;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @return true if the use action should be cancelled
     */
    private static boolean placementRestrictionInEffect(MinecraftClient mc)
    {
        HitResult trace = mc.crosshairTarget;

        ItemStack stack = mc.player.getMainHandStack();

        if (stack.isEmpty())
        {
            stack = mc.player.getOffHandStack();
        }

        if (stack.isEmpty())
        {
            return false;
        }

        if (trace != null && trace.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult blockHitResult = (BlockHitResult) trace;
            ItemPlacementContext ctx = new ItemPlacementContext(new ItemUsageContext(mc.player, Hand.MAIN_HAND, blockHitResult));

            // Get the possibly offset position, if the targeted block is not replaceable
            BlockPos pos = ctx.getBlockPos();

            BlockState stateClient = mc.world.getBlockState(pos);

            World worldSchematic = SchematicWorldHandler.getSchematicWorld();
            LayerRange range = DataManager.getRenderLayerRange();
            boolean schematicHasAir = worldSchematic.isAir(pos);

            // The targeted position is outside the current render range
            if (schematicHasAir == false && range.isPositionWithinRange(pos) == false)
            {
                return true;
            }

            // There should not be anything in the targeted position,
            // and the position is within or close to a schematic sub-region
            if (schematicHasAir && isPositionWithinRangeOfSchematicRegions(pos, 2))
            {
                return true;
            }

            blockHitResult = new BlockHitResult(blockHitResult.getPos(), blockHitResult.getSide(), pos, false);
            ctx = new ItemPlacementContext(new ItemUsageContext(mc.player, Hand.MAIN_HAND, blockHitResult));

            // Placement position is already occupied
            if (stateClient.canReplace(ctx) == false)
            {
                return true;
            }

            BlockState stateSchematic = worldSchematic.getBlockState(pos);
            stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

            // The player is holding the wrong item for the targeted position
            if (stack.isEmpty() == false && EntityUtils.getUsedHandForItem(mc.player, stack) == null)
            {
                return true;
            }

            // Ignore if schematic block is wall-mountable and orientation is wrong
            Block schematicBlock = stateSchematic.getBlock();
            if (schematicBlock instanceof WallTorchBlock ||
                schematicBlock instanceof WallRedstoneTorchBlock ||
                schematicBlock instanceof WallBannerBlock ||
                schematicBlock instanceof WallSignBlock ||
                schematicBlock instanceof WallSkullBlock)
            {
                if (blockHitResult.getSide() != stateSchematic.get(Properties.HORIZONTAL_FACING))
                    return true;
            }

            // Orientation is wrong
            BlockState attemptState = schematicBlock.getPlacementState(ctx);
            return !isMatchingStatePlacementRestriction (attemptState, stateSchematic);
        }

        return false;
    }

    private static boolean isMatchingStatePlacementRestriction (BlockState state1, BlockState state2)
    {
        if (state1 == null || state2 == null)
            return false;
        if (state1 == state2)
            return true;

        Property<?>[] orientationProperties = new Property<?>[] {
                Properties.FACING, //pistons
                Properties.BLOCK_HALF, //stairs, trapdoors
                Properties.HOPPER_FACING,
                Properties.DOOR_HINGE,
                Properties.HORIZONTAL_FACING, //small dripleaf
                Properties.AXIS, //logs
                Properties.SLAB_TYPE,
                Properties.VERTICAL_DIRECTION,
                Properties.ROTATION, //banners
                Properties.HANGING, //lanterns
                Properties.BLOCK_FACE, //lever, button, grindstone
                Properties.ATTACHMENT, //bell (double-check for single-wall / double-wall)
                //Properties.HORIZONTAL_AXIS, //Nether portals, though they aren't directly placeable
                //Properties.ORIENTATION, //jigsaw blocks, Crafters
        };

        for (Property<?> property : orientationProperties)
        {
            boolean hasProperty1 = state1.contains(property);
            boolean hasProperty2 = state2.contains(property);

            if (hasProperty1 != hasProperty2)
                return false;
            if (!hasProperty1)
                continue;

            if (state1.get(property) != state2.get(property))
                return false;
        }

        //Other properties are considered as matching
        return true;
    }

    public static boolean isPositionWithinRangeOfSchematicRegions(BlockPos pos, int range)
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        final int minCX = (x - range) >> 4;
        final int minCZ = (z - range) >> 4;
        final int maxCX = (x + range) >> 4;
        final int maxCZ = (z + range) >> 4;

        for (int cz = minCZ; cz <= maxCZ; ++cz)
        {
            for (int cx = minCX; cx <= maxCX; ++cx)
            {
                List<PlacementPart> parts = manager.getPlacementPartsInChunk(cx, cz);

                for (PlacementPart part : parts)
                {
                    IntBoundingBox box = part.bb;

                    if (x >= box.minX - range && x <= box.maxX + range &&
                        y >= box.minY - range && y <= box.maxY + range &&
                        z >= box.minZ - range && z <= box.maxZ + range)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if the given one block thick slice has non-air blocks or not.
     * NOTE: The axis is the perpendicular axis (that goes through the plane).
     * @param axis
     * @param pos1
     * @param pos2
     * @return
     */
    public static boolean isSliceEmpty(World world, Direction.Axis axis, BlockPos pos1, BlockPos pos2)
    {
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

        switch (axis)
        {
            case Z:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int z = pos1.getZ();
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);

                for (int cx = cxMin; cx <= cxMax; ++cx)
                {
                    Chunk chunk = world.getChunk(cx, z >> 4);
                    int xMin = Math.max(x1,  cx << 4      );
                    int xMax = Math.min(x2, (cx << 4) + 15);
                    int yMax = Math.min(y2, fi.dy.masa.malilib.util.WorldUtils.getHighestSectionYOffset(chunk) + 15);

                    for (int x = xMin; x <= xMax; ++x)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }

            case Y:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y = pos1.getY();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    for (int cx = cxMin; cx <= cxMax; ++cx)
                    {
                        Chunk chunk = world.getChunk(cx, cz);

                        if (y > fi.dy.masa.malilib.util.WorldUtils.getHighestSectionYOffset(chunk) + 15)
                        {
                            continue;
                        }

                        int xMin = Math.max(x1,  cx << 4      );
                        int xMax = Math.min(x2, (cx << 4) + 15);
                        int zMin = Math.max(z1,  cz << 4      );
                        int zMax = Math.min(z2, (cz << 4) + 15);

                        for (int z = zMin; z <= zMax; ++z)
                        {
                            for (int x = xMin; x <= xMax; ++x)
                            {
                                if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                                {
                                    return false;
                                }
                            }
                        }
                    }
                }

                break;
            }

            case X:
            {
                int x = pos1.getX();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    Chunk chunk = world.getChunk(x >> 4, cz);
                    int zMin = Math.max(z1,  cz << 4      );
                    int zMax = Math.min(z2, (cz << 4) + 15);
                    int yMax = Math.min(y2, fi.dy.masa.malilib.util.WorldUtils.getHighestSectionYOffset(chunk) + 15);

                    for (int z = zMin; z <= zMax; ++z)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }
        }

        return true;
    }

    public static boolean easyPlaceIsPositionCached(BlockPos pos)
    {
        long currentTime = System.nanoTime();
        boolean cached = false;

        for (int i = 0; i < EASY_PLACE_POSITIONS.size(); ++i)
        {
            PositionCache val = EASY_PLACE_POSITIONS.get(i);
            boolean expired = val.hasExpired(currentTime);

            if (expired)
            {
                EASY_PLACE_POSITIONS.remove(i);
                --i;
            }
            else if (val.getPos().equals(pos))
            {
                cached = true;

                // Keep checking and removing old entries if there are a fair amount
                if (EASY_PLACE_POSITIONS.size() < 16)
                {
                    break;
                }
            }
        }

        return cached;
    }

    private static void cacheEasyPlacePosition(BlockPos pos)
    {
        EASY_PLACE_POSITIONS.add(new PositionCache(pos, System.nanoTime(), 2000000000));
    }

    public static class PositionCache
    {
        private final BlockPos pos;
        private final long time;
        private final long timeout;

        private PositionCache(BlockPos pos, long time, long timeout)
        {
            this.pos = pos;
            this.time = time;
            this.timeout = timeout;
        }

        public BlockPos getPos()
        {
            return this.pos;
        }

        public boolean hasExpired(long currentTime)
        {
            return currentTime - this.time > this.timeout;
        }
    }

    private static boolean easyPlaceIsTooFast()
    {
        return System.nanoTime() - easyPlaceLastPickBlockTime < 1000000L * Configs.Generic.EASY_PLACE_SWAP_INTERVAL.getIntegerValue();
    }

    public static void setEasyPlaceLastPickBlockTime()
    {
        easyPlaceLastPickBlockTime = System.nanoTime();
    }
}
