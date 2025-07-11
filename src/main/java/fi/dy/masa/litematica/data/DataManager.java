package fi.dy.masa.litematica.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import fi.dy.masa.malilib.gui.interfaces.IDirectoryCache;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiConfigs.ConfigGuiTab;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.projects.SchematicProjectsManager;
import fi.dy.masa.litematica.schematic.transmit.SchematicBufferManager;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.selection.AreaSelectionSimple;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.tool.ToolModeData;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.litematica.util.ToBooleanFunction;

public class DataManager implements IDirectoryCache
{
    private static final DataManager INSTANCE = new DataManager();

    private static final Map<String, Path> LAST_DIRECTORIES = new HashMap<>();
    private static final ArrayList<ToBooleanFunction<Text>> CHAT_LISTENERS = new ArrayList<>();
    public static final Identifier CARPET_HELLO = Identifier.of("carpet", "hello");

    private static ItemStack toolItem = new ItemStack(Items.STICK);
    private ItemStack toolItemComponents = null;
    private static ConfigGuiTab configGuiTab = ConfigGuiTab.GENERIC;
    private static boolean createPlacementOnLoad = true;
    private static boolean canSave;
    private static boolean isCarpetServer;
    private static long clientTickStart;
    private boolean hasIntegratedServer = false;

    private final SelectionManager selectionManager = new SelectionManager();
    private final SchematicPlacementManager schematicPlacementManager = new SchematicPlacementManager();
    private final SchematicProjectsManager schematicProjectsManager = new SchematicProjectsManager();
    private final SchematicBufferManager schematicBufferManager = new SchematicBufferManager();
    private LayerRange renderRange = new LayerRange(SchematicWorldRefresher.INSTANCE);
    private ToolMode operationMode = ToolMode.SCHEMATIC_PLACEMENT;
    private AreaSelectionSimple areaSimple = new AreaSelectionSimple(true);
    @Nullable
    private MaterialListBase materialList;

    private DataManager()
    {
    }

    public static DataManager getInstance()
    {
        return INSTANCE;
    }

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            Litematica.debugLog("DataManager#reset() - log-out");
            this.hasIntegratedServer = false;
            this.toolItemComponents = null;
        }
        else
        {
            Litematica.debugLog("DataManager#reset() - dimension change or log-in");
        }
    }

    public static IDirectoryCache getDirectoryCache()
    {
        return INSTANCE;
    }

    public static void onClientTickStart()
    {
        clientTickStart = System.nanoTime();
    }

    public static long getClientTickStartTime()
    {
        return clientTickStart;
    }

    public void onWorldPre(@Nonnull DynamicRegistryManager registryManager)
    {
        Litematica.debugLog("DataManager#onWorldPre()");
        setToolItemComponents(Configs.Generic.TOOL_ITEM_COMPONENTS.getStringValue(), registryManager);
    }

    public static ItemStack getToolItem()
    {
        return toolItem;
    }

    public boolean hasToolItemComponents()
    {
        return this.toolItemComponents != null;
    }

    public ItemStack getToolItemComponents()
    {
        return this.toolItemComponents;
    }

    public static void setIsCarpetServer(boolean isCarpetServer)
    {
        DataManager.isCarpetServer = isCarpetServer;
    }

    public static boolean isCarpetServer()
    {
        return isCarpetServer;
    }

    public boolean hasIntegratedServer() { return this.hasIntegratedServer; }

    public void setHasIntegratedServer(boolean toggle)
    {
        this.hasIntegratedServer = toggle;
    }

    public static void addChatListener(ToBooleanFunction<Text> listener)
    {
        synchronized (CHAT_LISTENERS)
        {
            CHAT_LISTENERS.add(listener);
        }
    }

    public static void removeChatListener(ToBooleanFunction<Text> listener)
    {
        synchronized (CHAT_LISTENERS)
        {
            CHAT_LISTENERS.remove(listener);
        }
    }

    public static void clearChatListeners()
    {
        synchronized (CHAT_LISTENERS)
        {
            CHAT_LISTENERS.clear();
        }
    }

    public static boolean onChatMessage(Text text)
    {
        synchronized (CHAT_LISTENERS)
        {
            boolean cancel = false;

            for (ToBooleanFunction<Text> listener : CHAT_LISTENERS)
            {
                cancel |= listener.applyAsBoolean(text);
            }

            return cancel;
        }
    }

    public static boolean getCreatePlacementOnLoad()
    {
        return createPlacementOnLoad;
    }

    public static void setCreatePlacementOnLoad(boolean create)
    {
        createPlacementOnLoad = create;
    }

    public static ConfigGuiTab getConfigGuiTab()
    {
        return configGuiTab;
    }

    public static void setConfigGuiTab(ConfigGuiTab tab)
    {
        configGuiTab = tab;
    }

    public static SelectionManager getSelectionManager()
    {
        return getInstance().selectionManager;
    }

    public static SchematicPlacementManager getSchematicPlacementManager()
    {
        return getInstance().schematicPlacementManager;
    }

    public static SchematicProjectsManager getSchematicProjectsManager()
    {
        return getInstance().schematicProjectsManager;
    }

    public static SchematicBufferManager getSchematicBufferManager()
    {
        return getInstance().schematicBufferManager;
    }

    @Nullable
    public static MaterialListBase getMaterialList()
    {
        return getInstance().materialList;
    }

    public static void setMaterialList(@Nullable MaterialListBase materialList)
    {
        MaterialListBase old = getInstance().materialList;

        if (old != null)
        {
            MaterialListHudRenderer renderer = old.getHudRenderer();

            if (renderer.getShouldRenderCustom())
            {
                renderer.toggleShouldRender();
                InfoHud.getInstance().removeInfoHudRenderer(renderer, false);
            }
        }

        getInstance().materialList = materialList;
    }

    public static ToolMode getToolMode()
    {
        return getInstance().operationMode;
    }

    public static void setToolMode(ToolMode mode)
    {
        getInstance().operationMode = mode;
    }

    public static LayerRange getRenderLayerRange()
    {
        return getInstance().renderRange;
    }

    public static AreaSelectionSimple getSimpleArea()
    {
        return getInstance().areaSimple;
    }

    @Override
    @Nullable
    public Path getCurrentDirectoryForContext(String context)
    {
        return LAST_DIRECTORIES.get(context);
    }

    @Override
    public void setCurrentDirectoryForContext(String context, Path dir)
    {
        LAST_DIRECTORIES.put(context, dir);
    }

    public static void load()
    {
        getInstance().loadPerDimensionData();

        Path file = getCurrentStorageFile(true);
        JsonElement element = JsonUtils.parseJsonFileAsPath(file);

        if (element != null && element.isJsonObject())
        {
            LAST_DIRECTORIES.clear();

            JsonObject root = element.getAsJsonObject();

            if (JsonUtils.hasObject(root, "last_directories"))
            {
                JsonObject obj = root.get("last_directories").getAsJsonObject();

                for (Map.Entry<String, JsonElement> entry : obj.entrySet())
                {
                    String name = entry.getKey();
                    JsonElement el = entry.getValue();

                    if (el.isJsonPrimitive())
                    {
                        Path dir = Path.of(el.getAsString());

                        if (Files.exists(dir) && Files.isDirectory(dir))
                        {
                            LAST_DIRECTORIES.put(name, dir);
                        }
                    }
                }
            }

            if (JsonUtils.hasString(root, "config_gui_tab"))
            {
                try
                {
                    configGuiTab = ConfigGuiTab.valueOf(root.get("config_gui_tab").getAsString());
                }
                catch (Exception ignored) {}

                if (configGuiTab == null)
                {
                    configGuiTab = ConfigGuiTab.GENERIC;
                }
            }

            createPlacementOnLoad = JsonUtils.getBooleanOrDefault(root, "create_placement_on_load", true);
        }

        canSave = true;
    }

    public static void save()
    {
        save(false);
    }

    public static void save(boolean forceSave)
    {
        if (canSave == false && forceSave == false)
        {
            return;
        }

        getInstance().savePerDimensionData();

        JsonObject root = new JsonObject();
        JsonObject objDirs = new JsonObject();

        for (Map.Entry<String, Path> entry : LAST_DIRECTORIES.entrySet())
        {
            objDirs.add(entry.getKey(), new JsonPrimitive(entry.getValue().toAbsolutePath().toString()));
        }

        root.add("last_directories", objDirs);

        root.add("create_placement_on_load", new JsonPrimitive(createPlacementOnLoad));
        root.add("config_gui_tab", new JsonPrimitive(configGuiTab.name()));

        Path file = getCurrentStorageFile(true);
        JsonUtils.writeJsonToFileAsPath(root, file);

        canSave = false;
    }

    public static void clear()
    {
        TaskScheduler.getInstanceClient().clearTasks();
        SchematicVerifier.clearActiveVerifiers();

        getSchematicPlacementManager().clear();
        getSchematicProjectsManager().clear();
        getSelectionManager().clear();
        setMaterialList(null);
        clearChatListeners();

        InfoHud.getInstance().reset(); // remove the line providers and clear the data
        setIsCarpetServer(false);
    }

    private void savePerDimensionData()
    {
        this.schematicProjectsManager.saveCurrentProject();
        JsonObject root = this.toJson();

        root.add("block_entities", EntitiesDataStorage.getInstance().toJson());

        Path file = getCurrentStorageFile(false);
        JsonUtils.writeJsonToFileAsPath(root, file);
    }

    private void loadPerDimensionData()
    {
        this.selectionManager.clear();
        this.schematicPlacementManager.clear();
        this.schematicProjectsManager.clear();
        this.materialList = null;

        Path file = getCurrentStorageFile(false);
        JsonElement element = JsonUtils.parseJsonFileAsPath(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();
            this.fromJson(root);

            if (JsonUtils.hasObject(root, "block_entities"))
            {
                EntitiesDataStorage.getInstance().fromJson(JsonUtils.getNestedObject(root, "block_entities", false));
            }
        }
    }

    private void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasObject(obj, "selections"))
        {
            this.selectionManager.loadFromJson(obj.get("selections").getAsJsonObject());
        }

        if (JsonUtils.hasObject(obj, "placements"))
        {
            this.schematicPlacementManager.loadFromJson(obj.get("placements").getAsJsonObject());
        }

        if (JsonUtils.hasObject(obj, "schematic_projects_manager"))
        {
            this.schematicProjectsManager.loadFromJson(obj.get("schematic_projects_manager").getAsJsonObject());
        }

        if (JsonUtils.hasObject(obj, "render_range"))
        {
            this.renderRange = LayerRange.createFromJson(JsonUtils.getNestedObject(obj, "render_range", false), SchematicWorldRefresher.INSTANCE);
        }

        if (JsonUtils.hasString(obj, "operation_mode"))
        {
            try
            {
                this.operationMode = ToolMode.valueOf(obj.get("operation_mode").getAsString());
            }
            catch (Exception ignored) {}

            if (this.operationMode == null)
            {
                this.operationMode = ToolMode.AREA_SELECTION;
            }
        }

        if (JsonUtils.hasObject(obj, "area_simple"))
        {
            this.areaSimple = AreaSelectionSimple.fromJson(obj.get("area_simple").getAsJsonObject());
        }

        if (JsonUtils.hasObject(obj, "tool_mode_data"))
        {
            this.toolModeDataFromJson(obj.get("tool_mode_data").getAsJsonObject());
        }
    }

    private JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("selections", this.selectionManager.toJson());
        obj.add("placements", this.schematicPlacementManager.toJson());
        obj.add("schematic_projects_manager", this.schematicProjectsManager.toJson());
        obj.add("operation_mode", new JsonPrimitive(this.operationMode.name()));
        obj.add("render_range", this.renderRange.toJson());
        obj.add("area_simple", this.areaSimple.toJson());
        obj.add("tool_mode_data", this.toolModeDataToJson());

        return obj;
    }

    private JsonObject toolModeDataToJson()
    {
        JsonObject obj = new JsonObject();
        obj.add("delete", ToolModeData.DELETE.toJson());
        return obj;
    }

    private void toolModeDataFromJson(JsonObject obj)
    {
        if (JsonUtils.hasObject(obj, "delete"))
        {
            ToolModeData.DELETE.fromJson(obj.get("delete").getAsJsonObject());
        }
    }

    public static Path getDefaultBaseSchematicDirectory()
    {
        return FileUtils.getRealPathIfPossible(FileUtils.getMinecraftDirectoryAsPath().resolve("schematics"));
    }

    public static Path getCurrentConfigDirectory()
    {
        return FileUtils.getConfigDirectoryAsPath().resolve(Reference.MOD_ID);
    }

    public static Path getSchematicsBaseDirectory()
    {
        Path dir;

        if (Configs.Generic.CUSTOM_SCHEMATIC_BASE_DIRECTORY_ENABLED.getBooleanValue())
        {
            dir = Path.of(Configs.Generic.CUSTOM_SCHEMATIC_BASE_DIRECTORY.getStringValue());
        }
        else
        {
            dir = getDefaultBaseSchematicDirectory();
        }

        if (!Files.exists(dir) || !Files.isDirectory(dir))
        {
            try
            {
                if (Files.exists(dir))
                {
                    Files.delete(dir);
                }

                Files.createDirectory(dir);
                Litematica.LOGGER.warn("getSchematicsBaseDirectory(): Created schematic directory '{}'", dir.toAbsolutePath().toString());
            }
            catch (Exception err)
            {
                Litematica.LOGGER.error("Failed to create the schematic directory '{}'; {}", dir.toAbsolutePath().toString(), err.getLocalizedMessage());
            }
        }

        if (!Files.isDirectory(dir))
        {
            Litematica.LOGGER.error("Failed to create the schematic directory '{}'", dir.toAbsolutePath().toString());
        }

        if (!Files.isWritable(dir))
        {
            Litematica.LOGGER.error("Schematic directory '{}'; is not writeable.", dir.toAbsolutePath().toString());
        }

//        Litematica.debugLog("getSchematicsBaseDirectory(): Schematic directory debug '{}'", dir.toAbsolutePath().toString());
        return dir;
    }

    public static Path getSchematicTransmitDirectory()
    {
        Path dir = getSchematicsBaseDirectory().resolve("transmit");

        if (!Files.exists(dir) || !Files.isDirectory(dir))
        {
            try
            {
                if (Files.exists(dir))
                {
                    Files.delete(dir);
                }

                Files.createDirectory(dir);
                Litematica.LOGGER.warn("getSchematicTransmitDirectory(): Created schematic transmit directory '{}'", dir.toAbsolutePath().toString());
            }
            catch (Exception err)
            {
                Litematica.LOGGER.error("Failed to create the schematic transmit directory '{}'; {}", dir.toAbsolutePath().toString(), err.getLocalizedMessage());
            }
        }

        if (!Files.isDirectory(dir))
        {
            Litematica.LOGGER.error("Failed to create the schematic transmit directory '{}'", dir.toAbsolutePath().toString());
        }

        if (!Files.isWritable(dir))
        {
            Litematica.LOGGER.error("Schematic transmit directory '{}'; is not writeable.", dir.toAbsolutePath().toString());
        }

//        Litematica.debugLog("getSchematicTransmitDirectory(): Schematic transmit directory debug '{}'", dir.toAbsolutePath().toString());
        return dir;
    }

    public static Path getAreaSelectionsBaseDirectory()
    {
        Path dir;
        String name = StringUtils.getWorldOrServerName();

        if (Configs.Generic.AREAS_PER_WORLD.getBooleanValue() && name != null)
        {
            // The 'area_selections' sub-directory is to prevent showing the world name or server IP in the browser,
            // as the root directory name is shown in the navigation widget
            //dir = FileUtils.getCanonicalFileIfPossible(new File(new File(new File(getCurrentConfigDirectory(), "area_selections_per_world"), name), "area_selections"));

            dir = FileUtils.getRealPathIfPossible(getCurrentConfigDirectory().resolve("area_selections_per_world").resolve(name).resolve("area_selections"));
        }
        else
        {
            dir = FileUtils.getRealPathIfPossible(getCurrentConfigDirectory().resolve("area_selections"));
        }

        if (!Files.exists(dir))
        {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        if (!Files.isDirectory(dir))
        {
            Litematica.LOGGER.warn("Failed to create the area selections base directory '{}'", dir.toAbsolutePath());
        }

        return dir;
    }

    private static Path getCurrentStorageFile(boolean globalData)
    {
        Path dir = getCurrentConfigDirectory();

        if (!Files.exists(dir))
        {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        if (!Files.isDirectory(dir))
        {
            Litematica.LOGGER.warn("Failed to create the config directory '{}'", dir.toAbsolutePath());
        }

        return dir.resolve(StringUtils.getStorageFileName(globalData, Reference.MOD_ID + "_", ".json", "default"));
    }

    /**
     * Sets the current toolItem, if itemNameIn is invalid, sets toolItem to the default minecraft:stick
     * @param itemNameIn (String representation of the item Identifier)
     */
    public static void setToolItem(String itemNameIn)
    {
        toolItem = InventoryUtils.getItemStackFromString(itemNameIn);

        if (toolItem == null)
        {
            // Fall back to a stick
            toolItem = new ItemStack(Items.STICK);
            Configs.Generic.TOOL_ITEM.setValueFromString(Registries.ITEM.getId(Items.STICK).toString());
        }
    }

    /**
     * Sets the current toolItemComponents, if toolItemString is invalid or empty, use the regular toolItem setting
     * Must be called "after" onWorldPre, so that the DynamicRegistry is valid.
     *
     * @param toolItemString (String representation of the Data Component aware id type)
     */
    public void setToolItemComponents(String toolItemString, @Nonnull DynamicRegistryManager registryManager)
    {
        if (registryManager.equals(DynamicRegistryManager.EMPTY) || toolItemString.isEmpty() || toolItemString.equals("empty"))
        {
            this.toolItemComponents = null;
        }
        else
        {
            this.toolItemComponents = InventoryUtils.getItemStackFromString(toolItemString, registryManager);
        }

        if (this.toolItemComponents == null)
        {
            Configs.Generic.TOOL_ITEM_COMPONENTS.setValueFromString("empty");
        }
        // This is meant to re-validate the syntax, and save it back to the Config.
        // This is kind of janky in practice, but it does work; so I left the code here.
        /*
        else
        {
            Configs.Generic.TOOL_ITEM_COMPONENTS.setValueFromString(fi.dy.masa.litematica.util.InventoryUtils.convertItemNbtToString((NbtCompound) this.toolItemComponents.encode(registryManager)));
        }
         */
    }
}
