package fi.dy.masa.litematica.schematic.projects;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.AreaSelectionSimple;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.*;

public class SchematicProject
{
    private final List<SchematicVersion> versions = new ArrayList<>();
    private final Path directory;
    private Path projectFile;
    private BlockPos origin;
    private String projectName;
    private AreaSelection selection;
    private AreaSelection lastSeenArea;
    private AreaSelectionSimple selectionSimple;
    private SelectionMode selectionMode;
    private int currentVersionId;
    private int lastCheckedOutVersion;
    private int lastPastedVersion;
    private boolean saveInProgress;
    private boolean dirty;
    @Nullable
    private SchematicPlacement currentPlacement;

    public SchematicProject(Path directory, Path projectFile)
    {
        this.directory = directory;
        this.projectFile = projectFile;

        this.origin = BlockPos.ORIGIN;
        this.projectName = "unnamed";
        this.selection = new AreaSelection();
        this.lastSeenArea = new AreaSelection();
        this.selectionSimple = new AreaSelectionSimple(true);
        this.selectionMode = (SelectionMode) Configs.InfoOverlays.DEFAULT_SELECTION_MODE.getOptionListValue();
        this.currentVersionId = -1;
        this.lastCheckedOutVersion = -1;
        this.lastPastedVersion = -1;
        this.dirty = true;
    }

    public Path getDirectory()
    {
        return this.directory;
    }

    public String getName()
    {
        return this.projectName;
    }

    public int getVersionCount()
    {
        return this.versions.size();
    }

    public int getCurrentVersionId()
    {
        return this.currentVersionId;
    }

    public String getCurrentVersionName()
    {
        SchematicVersion currentVersion = this.getCurrentVersion();
        return currentVersion != null ? currentVersion.getName() : this.getSelection().getName();
    }

    public void setName(String name)
    {
        Path newFile = this.directory.resolve(name + ".json");

        if (!Files.exists(newFile))
        {
            try
            {
                if (Files.exists(this.projectFile))
                {
                    FileUtils.move(this.projectFile, newFile);
                }

                this.projectName = name;
                this.projectFile = newFile;
                this.selection.setName(name);
                this.selectionSimple.setName(name);
                SelectionManager.renameSubRegionBoxIfSingle(this.selection, name);
                SelectionManager.renameSubRegionBoxIfSingle(this.selectionSimple, name);
                this.dirty = true;
            }
            catch (Exception e)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.failed_to_rename_project_file_exception", newFile.toAbsolutePath());
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.failed_to_rename_project_file_exists", name);
        }
    }

    public void setOrigin(BlockPos origin)
    {
        BlockPos offset = this.selection.getEffectiveOrigin().subtract(this.origin);
        this.selection.moveEntireSelectionTo(origin.add(offset), false);

        offset = this.selectionSimple.getEffectiveOrigin().subtract(this.origin);
        this.selectionSimple.moveEntireSelectionTo(origin.add(offset), false);

        // Forget the old last seen area, it will be invalid after moving the entire project
        this.lastSeenArea = new AreaSelection();

        this.origin = origin;
        // Reset the last pasted version after moving the entire project, as it makes no sense after that
        this.lastPastedVersion = -1;

        SchematicVersion currentVersion = this.getCurrentVersion();

        if (currentVersion != null)
        {
            BlockPos areaPosition = this.origin.add(currentVersion.getAreaOffset());

            if (this.currentPlacement != null)
            {
                this.currentPlacement.setOrigin(areaPosition, InfoUtils.INFO_MESSAGE_CONSUMER);
            }
        }

        this.dirty = true;
    }

    public Path getProjectFile()
    {
        return this.projectFile;
    }

    public BlockPos getOrigin()
    {
        return this.origin;
    }

    public AreaSelection getSelection()
    {
        if (this.selectionMode == SelectionMode.SIMPLE)
        {
            return this.selectionSimple;
        }

        return this.selection;
    }

    public SelectionMode getSelectionMode()
    {
        return this.selectionMode;
    }

    public void checkSelectionModeConfig()
    {
        if (this.dirty)
        {
            this.selectionMode = (SelectionMode) Configs.InfoOverlays.DEFAULT_SELECTION_MODE.getOptionListValue();
        }
    }

    public void switchSelectionMode()
    {
        this.selectionMode = (SelectionMode) this.selectionMode.cycle(true);
        this.dirty = true;
    }

    public ImmutableList<SchematicVersion> getAllVersions()
    {
        return ImmutableList.copyOf(this.versions);
    }

    @Nullable
    public SchematicVersion getCurrentVersion()
    {
        if (this.currentVersionId >= 0 && this.currentVersionId < this.versions.size())
        {
            return this.versions.get(this.currentVersionId);
        }

        return null;
    }

    @Nullable
    public SchematicPlacement getCurrentPlacement()
    {
        return this.currentPlacement;
    }

    private void createAndAddPlacement()
    {
        SchematicVersion version = this.getCurrentVersion();

        if (version != null)
        {
            if (this.currentVersionId != this.lastCheckedOutVersion)
            {
                this.removeCurrentPlacement();

                String fileName = version.getFileName();
                FileType fileType = FileType.fromName(fileName);

                if (fileType == FileType.UNKNOWN)
                {
                    fileName += LitematicaSchematic.FILE_EXTENSION;
                    fileType = FileType.LITEMATICA_SCHEMATIC;
                }

                LitematicaSchematic schematic = null;

                if (fileType == FileType.LITEMATICA_SCHEMATIC)
                {
                    schematic = LitematicaSchematic.createFromFile(this.directory, fileName);
                }
                else if (fileType == FileType.SCHEMATICA_SCHEMATIC)
                {
                    schematic = WorldUtils.convertSchematicaSchematicToLitematicaSchematic(this.directory, fileName, false, f -> {});
                }
                else if (fileType == FileType.VANILLA_STRUCTURE)
                {
                    schematic = WorldUtils.convertStructureToLitematicaSchematic(this.directory, fileName);
                }
                else if (fileType == FileType.SPONGE_SCHEMATIC)
                {
                    schematic = WorldUtils.convertSpongeSchematicToLitematicaSchematic(this.directory, fileName);
                }

                if (schematic != null)
                {
                    BlockPos areaPosition = this.origin.add(version.getAreaOffset());
                    this.currentPlacement = SchematicPlacement.createFor(schematic, areaPosition, version.getName(), true, true);
                    this.currentPlacement.setShouldBeSaved(false);
                    DataManager.getSchematicPlacementManager().addSchematicPlacement(this.currentPlacement, false);

                    long time = schematic.getMetadata().getTimeCreated();

                    if (time != version.getTimeStamp())
                    {
                        version = new SchematicVersion(version.getName(), version.getFileName(), version.getAreaOffset(), version.getVersion(), time);
                        this.versions.set(this.currentVersionId, version);
                        this.dirty = true;
                    }
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.failed_to_load_schematic");
                }

                this.lastCheckedOutVersion = this.currentVersionId;
            }
        }
    }

    public void pasteToWorld()
    {
        if (this.currentPlacement != null)
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc.player == null || EntityUtils.isCreativeMode(mc.player) == false)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
                return;
            }

            this.cacheCurrentAreaFromPlacement();

            PlacementDeletionMode mode = (PlacementDeletionMode) Configs.Generic.SCHEMATIC_VCS_DELETE_MODE.getOptionListValue();

            if (mode == PlacementDeletionMode.ENTIRE_VOLUME)
            {
                ToolUtils.deleteSelectionVolumes(this.lastSeenArea, true, this::pasteCurrentPlacement, mc);
            }
            else
            {
                if (this.lastPastedVersion < 0 || this.lastPastedVersion >= this.versions.size())
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "No previous pasted version known, skipping delete");
                    this.pasteCurrentPlacement();
                    return;
                }

                /*
                SchematicVersion version = this.versions.get(this.lastPastedVersion);
                LitematicaSchematic schematic = LitematicaSchematic.createFromFile(this.directory, version.getFileName());

                if (schematic != null)
                {
                    BlockPos areaPosition = this.origin.add(version.getAreaOffset());
                    SchematicPlacement placement = new SchematicPlacement(schematic, areaPosition, version.getName(), true, false);
                    ToolUtils.deleteBlocksByPlacement(placement, mode, this::pasteCurrentPlacement);
                }
                else
                */
                {
                    this.pasteCurrentPlacement();
                }
            }
        }
    }

    protected void pasteCurrentPlacement()
    {
        this.lastPastedVersion = this.currentVersionId;
        this.dirty = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        DataManager.getSchematicPlacementManager().pastePlacementToWorld(this.currentPlacement, false, mc);
    }

    public void deleteLastSeenArea(MinecraftClient mc)
    {
        ToolUtils.deleteSelectionVolumes(this.lastSeenArea, true, mc);
    }

    public void deleteBlocksByPlacement()
    {
        if (this.currentPlacement != null)
        {
            PlacementDeletionMode mode = (PlacementDeletionMode) Configs.Generic.SCHEMATIC_VCS_DELETE_MODE.getOptionListValue();
            ToolUtils.deleteBlocksByPlacement(this.currentPlacement, mode, null);
        }
    }

    public void removeCurrentPlacement()
    {
        if (this.currentPlacement != null)
        {
            DataManager.getSchematicPlacementManager().removeSchematicPlacement(this.currentPlacement);
        }
    }

    public boolean cycleVersion(int amount)
    {
        if (this.currentVersionId >= 0)
        {
            return this.switchVersion(this.currentVersionId + amount, true);
        }

        return false;
    }

    public boolean switchVersion(int version, boolean createPlacement)
    {
        if (version != this.currentVersionId && version >= 0 && version < this.versions.size())
        {
            this.currentVersionId = version;
            this.dirty = true;

            if (createPlacement)
            {
                this.createAndAddPlacement();
            }

            return true;
        }

        return false;
    }

    public boolean switchVersion(SchematicVersion version, boolean createPlacement)
    {
        int index = this.versions.indexOf(version);

        if (index >= 0 && version != this.getCurrentVersion())
        {
            return this.switchVersion(index, createPlacement);
        }

        return false;
    }

    private void cacheCurrentAreaFromPlacement()
    {
        if (this.currentPlacement != null)
        {
            this.lastSeenArea = AreaSelection.fromPlacement(this.currentPlacement);
            this.dirty = true;
        }
    }

    public boolean commitNewVersion(String name)
    {
        if (this.checkCanSaveOrPrintError())
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            String author = mc.player.getName().getString();
            String fileName = this.getNextFileName();

            AreaSelection selection = this.getSelection();
            LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(selection, author);
            schematic.getMetadata().setName(name);
            BlockPos areaOffset = selection.getEffectiveOrigin().subtract(this.origin);
            SaveCompletionListener listener = new SaveCompletionListener(name, fileName, areaOffset);
            LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);

            TaskSaveSchematic task = new TaskSaveSchematic(this.directory, fileName, schematic, selection.copy(), info, false);
            task.setCompletionListener(listener);
            TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 2);
            this.saveInProgress = true;
            this.dirty = true;
            this.saveToFile();

            return true;
        }

        return false;
    }

    private String getNextFileName()
    {
        String nameBase = this.projectName + "_";
        int version = 1;
        int failsafe = 10000000;

        while (failsafe-- > 0)
        {
            String name = nameBase + String.format("%05d", version) + LitematicaSchematic.FILE_EXTENSION;
            Path file = this.directory.resolve(name);

            if (!Files.exists(file))
            {
                return name;
            }

            ++version;
        }

        return nameBase + "error";
    }

    public void clear()
    {
        this.origin = BlockPos.ORIGIN;
        this.versions.clear();
        this.selection = new AreaSelection();
        this.selectionSimple = new AreaSelectionSimple(true);
        this.lastSeenArea = new AreaSelection();
        this.currentVersionId = -1;
        this.lastCheckedOutVersion = -1;
        this.lastPastedVersion = -1;
        this.saveInProgress = false;
    }

    public boolean saveToFile()
    {
        if (this.dirty == false || JsonUtils.writeJsonToFileAsPath(this.toJson(), this.projectFile))
        {
            this.dirty = false;
            return true;
        }

        return false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("name", new JsonPrimitive(this.projectName));
        obj.add("origin", JsonUtils.blockPosToJson(this.origin));
        obj.add("current_version_id", new JsonPrimitive(this.currentVersionId));
        obj.add("last_pasted_version", new JsonPrimitive(this.lastPastedVersion));
        obj.add("selection_normal", this.selection.toJson());
        obj.add("selection_simple", this.selectionSimple.toJson());
        obj.add("last_seen_area", this.lastSeenArea.toJson());
        obj.add("selection_mode", new JsonPrimitive(this.selectionMode.name()));

        JsonArray arr = new JsonArray();

        for (int i = 0; i < this.versions.size(); ++i)
        {
            arr.add(this.versions.get(i).toJson());
        }

        if (arr.size() > 0)
        {
            obj.add("versions", arr);
        }

        return obj;
    }

    @Nullable
    public static SchematicProject fromJson(JsonObject obj, Path projectFile, boolean createPlacement)
    {
        BlockPos origin = JsonUtils.blockPosFromJson(obj, "origin");

        if (JsonUtils.hasString(obj, "name") && JsonUtils.hasInteger(obj, "current_version_id") && origin != null)
        {
            projectFile = FileUtils.getRealPathIfPossible(projectFile);
            SchematicProject project = new SchematicProject(projectFile.getParent(), projectFile);
            project.projectName = JsonUtils.getString(obj, "name");
            project.origin = origin;

            if (JsonUtils.hasObject(obj, "selection_normal"))
            {
                project.selection = AreaSelection.fromJson(JsonUtils.getNestedObject(obj, "selection_normal", false));
            }

            if (JsonUtils.hasObject(obj, "selection_simple"))
            {
                project.selectionSimple = AreaSelectionSimple.fromJson(JsonUtils.getNestedObject(obj, "selection_simple", false));
            }

            if (JsonUtils.hasObject(obj, "last_seen_area"))
            {
                project.lastSeenArea = AreaSelection.fromJson(JsonUtils.getNestedObject(obj, "last_seen_area", false));
            }

            if (JsonUtils.hasString(obj, "selection_mode"))
            {
                project.selectionMode = SelectionMode.fromStringStatic(JsonUtils.getString(obj, "selection_mode"));
            }
            else
            {
                project.selectionMode = (SelectionMode) Configs.InfoOverlays.DEFAULT_SELECTION_MODE.getOptionListValue();
            }

            if (JsonUtils.hasArray(obj, "versions"))
            {
                JsonArray arr = obj.get("versions").getAsJsonArray();

                for (int i = 0; i < arr.size(); ++i)
                {
                    JsonElement el = arr.get(i);

                    if (el.isJsonObject())
                    {
                        SchematicVersion version = SchematicVersion.fromJson(el.getAsJsonObject());

                        if (version != null)
                        {
                            project.versions.add(version);
                        }
                    }
                }
            }

            if (JsonUtils.hasInteger(obj, "last_pasted_version"))
            {
                project.lastPastedVersion = JsonUtils.getInteger(obj, "last_pasted_version");
            }

            int id = project.versions.size() - 1;

            if (JsonUtils.hasInteger(obj, "current_version_id"))
            {
                int tmp = JsonUtils.getInteger(obj, "current_version_id");

                if (tmp >= 0 && tmp < project.versions.size())
                {
                    id = tmp;
                }
            }

            project.switchVersion(id, createPlacement);

            project.dirty = false;

            return project;
        }

        return null;
    }

    boolean checkCanSaveOrPrintError()
    {
        if (this.saveInProgress)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.save_already_in_progress");
            return false;
        }

        if (this.directory == null || !Files.exists(this.directory) || !Files.isDirectory(this.directory))
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.invalid_project_directory");
            return false;
        }

        if (this.getSelection().getAllSubRegionBoxes().size() == 0)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.empty_selection");
            return false;
        }

        if (MinecraftClient.getInstance().player == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.null_player");
            return false;
        }

        return true;
    }

    private class SaveCompletionListener implements ICompletionListener
    {
        private final String name;
        private final String fileName;
        private final BlockPos areaOffset;
        private final int version;

        private SaveCompletionListener(String name, String fileName, BlockPos areaOffset)
        {
            this.name = name;
            this.fileName = fileName;
            this.areaOffset = areaOffset;
            this.version = SchematicProject.this.versions.size() + 1;
        }

        @Override
        public void onTaskCompleted()
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc.isOnThread())
            {
                this.saveVersion();
            }
            else
            {
                mc.execute(SaveCompletionListener.this::saveVersion);
            }
        }

        private void saveVersion()
        {
            SchematicVersion version = new SchematicVersion(this.name, this.fileName, this.areaOffset, this.version, System.currentTimeMillis());
            SchematicProject.this.versions.add(version);
            SchematicProject.this.switchVersion(SchematicProject.this.versions.size() - 1, true);
            SchematicProject.this.cacheCurrentAreaFromPlacement();
            SchematicProject.this.saveInProgress = false;

            if (GuiUtils.getCurrentScreen() instanceof ICompletionListener)
            {
                ((ICompletionListener) GuiUtils.getCurrentScreen()).onTaskCompleted();
            }

            InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.message.schematic_projects.version_saved", this.version, this.name);
        }
    }
}
