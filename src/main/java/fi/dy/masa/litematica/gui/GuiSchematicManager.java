package fi.dy.masa.litematica.gui;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;

import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.GuiTextInputFeedback;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonOptionList;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;

public class GuiSchematicManager extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    private static PreviewGenerator previewGenerator;
    private ExportType exportType = ExportType.V6_LITEMATIC;

    public GuiSchematicManager()
    {
        super(10, 24);

        this.title = StringUtils.translate("litematica.gui.title.schematic_manager");
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_manager";
    }

    @Override
    public Path getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.getScreenHeight() - 60;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.createButtons();
    }

    private void createButtons()
    {
        int x = 10;
        int y = this.getScreenHeight() - 26;

        DirectoryEntry selected = this.getListWidget().getLastSelectedEntry();

        if (selected != null)
        {
            FileType type = FileType.fromFile(selected.getFullPath());

            if (type == FileType.LITEMATICA_SCHEMATIC)
            {
                x = this.createButton(x, y, ButtonListener.Type.RENAME_SCHEMATIC);
                x = this.createButton(x, y, ButtonListener.Type.SET_PREVIEW);
                x = this.createButton(x, y, ButtonListener.Type.EXPORT_SCHEMATIC);
                x = this.createButton(x, y, ButtonListener.Type.EXPORT_TYPE);
                x = this.createButton(x, y, ButtonListener.Type.IMPORT_SCHEMATIC);
                x = this.createButton(x, y, ButtonListener.Type.DELETE_SCHEMATIC);
            }
            else if (type == FileType.SPONGE_SCHEMATIC || type == FileType.SCHEMATICA_SCHEMATIC || type == FileType.VANILLA_STRUCTURE)
            {
                x = this.createButton(x, y, ButtonListener.Type.IMPORT_SCHEMATIC);
                x = this.createButton(x, y, ButtonListener.Type.DELETE_SCHEMATIC);
            }
        }

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        String label = StringUtils.translate(type.getLabelKey());
        int buttonWidth = this.getStringWidth(label) + 20;
        this.addButton(new ButtonGeneric(this.getScreenWidth() - buttonWidth - 10, y, buttonWidth, 20, label), new ButtonListenerChangeMenu(type, null));
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        this.clearButtons();
        this.createButtons();
    }

    private int createButton(int x, int y, ButtonListener.Type type)
    {
        String label = type.getLabel();
        String hover = type.getHoverText();
        int buttonWidth = this.getStringWidth(label) + 10;
        ButtonGeneric button;

        if (type == ButtonListener.Type.EXPORT_TYPE)
        {
            buttonWidth = this.getStringWidth(this.exportType.getDisplayName()) + 10;
            button = new ConfigButtonOptionList(x, y, buttonWidth, 20, new ConfigWrapper());
        }
        else if (hover != null)
        {
            button = new ButtonGeneric(x, y, buttonWidth, 20, label, hover);
        }
        else
        {
            button = new ButtonGeneric(x, y, buttonWidth, 20, label);
        }

        this.addButton(button, new ButtonListener(type, this));

        return x + buttonWidth + 4;
    }

    @Override
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return this;
    }

    public static boolean setPreviewImage()
    {
        if (previewGenerator != null)
        {
            previewGenerator.createAndSetPreviewImage();
            previewGenerator = null;
            return true;
        }

        return false;
    }

    public static boolean hasPendingPreviewTask()
    {
        return previewGenerator != null;
    }

    private class ConfigWrapper implements IConfigOptionList
    {
        @Override
        public IConfigOptionListEntry getOptionListValue()
        {
            return GuiSchematicManager.this.exportType;
        }

        @Override
        public IConfigOptionListEntry getDefaultOptionListValue()
        {
            return ExportType.V6_LITEMATIC;
        }

        @Override
        public void setOptionListValue(IConfigOptionListEntry value)
        {
            GuiSchematicManager.this.exportType = (ExportType) value;
            GuiSchematicManager.this.clearButtons();
            GuiSchematicManager.this.createButtons();
        }
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final Type type;
        private final GuiSchematicManager gui;

        public ButtonListener(Type type, GuiSchematicManager gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == Type.SET_PREVIEW && mouseButton == 1)
            {
                if (previewGenerator != null)
                {
                    previewGenerator = null;
                    this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_preview_cancelled");
                }

                return;
            }

            DirectoryEntry entry = this.gui.getListWidget().getLastSelectedEntry();

            if (entry == null)
            {
                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_selected");
                return;
            }

            Path file = entry.getFullPath();

            if (!Files.exists(file) || !Files.isRegularFile(file) || !Files.isReadable(file))
            {
                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getFileName());
                return;
            }

            FileType fileType = FileType.fromFile(entry.getFullPath());

            if (this.type == Type.EXPORT_SCHEMATIC)
            {
                if (fileType == FileType.LITEMATICA_SCHEMATIC)
                {
                    GuiSchematicSaveExported gui = new GuiSchematicSaveExported(entry.getType(), entry.getDirectory(), entry.getName(), this.gui.exportType);
                    gui.setParent(this.gui);
                    GuiBase.openGui(gui);
                }
                else
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_manager.schematic_export.unsupported_type", file.getFileName());
                }
            }
            else if (this.type == Type.IMPORT_SCHEMATIC)
            {
                if (fileType == FileType.LITEMATICA_SCHEMATIC ||
                    fileType == FileType.SPONGE_SCHEMATIC ||
                    fileType == FileType.SCHEMATICA_SCHEMATIC ||
                    fileType == FileType.VANILLA_STRUCTURE)
                {
                    GuiSchematicSaveImported gui = new GuiSchematicSaveImported(entry.getType(), entry.getDirectory(), entry.getName());
                    gui.setParent(this.gui);
                    GuiBase.openGui(gui);
                }
                else
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_manager.schematic_import.unsupported_type", file.getFileName());
                }
            }
            else if (this.type == Type.RENAME_SCHEMATIC)
            {
                LitematicaSchematic schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.getName());
                String oldName = schematic != null ? schematic.getMetadata().getName() : "";
                GuiBase.openGui(new GuiTextInputFeedback(256, "litematica.gui.title.rename_schematic", oldName, this.gui, new SchematicRenamer(entry.getDirectory(), entry.getName(), this.gui)));
            }
            else if (this.type == Type.DELETE_SCHEMATIC)
            {
                FileDeleter deleter = new FileDeleter(entry.getFullPath());
                GuiBase.openGui(new GuiConfirmAction(400, "litematica.gui.title.confirm_file_deletion", deleter, this.gui, "litematica.gui.message.confirm_file_deletion", entry.getName()));
            }
            else if (this.type == Type.SET_PREVIEW)
            {
                if (GuiBase.isShiftDown() && GuiBase.isCtrlDown() && GuiBase.isAltDown() && fileType == FileType.LITEMATICA_SCHEMATIC)
                {
                    Path imageFile = entry.getDirectory().resolve("thumb.png");

                    if (Files.exists(imageFile) && Files.isReadable(imageFile))
                    {
                        LitematicaSchematic schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.getName());

                        if (schematic != null)
                        {
                            try
                            {
                                InputStream inputStream = Files.newInputStream(imageFile);
                                NativeImage image = NativeImage.read(inputStream);
                                int x = image.getWidth() >= image.getHeight() ? (image.getWidth() - image.getHeight()) / 2 : 0;
                                int y = image.getHeight() >= image.getWidth() ? (image.getHeight() - image.getWidth()) / 2 : 0;
                                int longerSide = Math.min(image.getWidth(), image.getHeight());
                                //System.out.printf("w: %d, h: %d, x: %d, y: %d\n", screenshot.getWidth(), screenshot.getHeight(), x, y);
                                //int previewDimensions = 140;
                                int previewDimensions = 120;
                                NativeImage scaled = new NativeImage(previewDimensions, previewDimensions, false);
                                image.resizeSubRectTo(x, y, longerSide, longerSide, scaled);
                                @SuppressWarnings("deprecation")
                                int[] pixels = scaled.makePixelArray();
    
                                schematic.getMetadata().setPreviewImagePixelData(pixels);
                                schematic.getMetadata().setTimeModifiedToNow();
    
                                if (schematic.writeToFile(entry.getDirectory(), entry.getName(), true))
                                {
                                    InfoUtils.showGuiAndInGameMessage(MessageType.SUCCESS, "Custom preview image set");
                                }

                                return;
                            }
                            catch (Exception ignore) {}
                        }
                    }
                    else
                    {
                        InfoUtils.showGuiAndInGameMessage(MessageType.ERROR, "Image 'thumb.png' not found");
                        return;
                    }
                    InfoUtils.showGuiAndInGameMessage(MessageType.ERROR, "Failed to set custom preview image");
                    return;
                }
                previewGenerator = new PreviewGenerator(entry.getDirectory(), entry.getName());
                GuiBase.openGui(null);
                InfoUtils.showGuiAndInGameMessage(MessageType.INFO, "litematica.info.schematic_manager.preview.set_preview_by_taking_a_screenshot");
            }
        }

        public enum Type
        {
            IMPORT_SCHEMATIC            ("litematica.gui.button.import"),
            EXPORT_SCHEMATIC            ("litematica.gui.button.schematic_manager.export_as"),
            RENAME_SCHEMATIC            ("litematica.gui.button.rename"),
            DELETE_SCHEMATIC            ("litematica.gui.button.delete"),
            SET_PREVIEW                 ("litematica.gui.button.set_preview", "litematica.info.schematic_manager.preview.right_click_to_cancel"),
            EXPORT_TYPE                 ("");

            private final String label;
            @Nullable
            private final String hoverText;

            private Type(String label)
            {
                this(label, null);
            }

            private Type(String label, String hoverText)
            {
                this.label = label;
                this.hoverText = hoverText;
            }

            public String getLabel()
            {
                return StringUtils.translate(this.label);
            }

            @Nullable
            public String getHoverText()
            {
                return this.hoverText != null ? StringUtils.translate(this.hoverText) : null;
            }
        }
    }

    private static class SchematicRenamer implements IStringConsumerFeedback
    {
        private final Path dir;
        private final String fileName;
        private final GuiSchematicManager gui;

        public SchematicRenamer(Path dir, String fileName, GuiSchematicManager gui)
        {
            this.dir = dir;
            this.fileName = fileName;
            this.gui = gui;
        }

        @Override
        public boolean setString(String string)
        {
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(this.dir, this.fileName);

            if (schematic != null)
            {
                schematic.getMetadata().setName(string);
                schematic.getMetadata().setTimeModifiedToNow();

                if (schematic.writeToFile(this.dir, this.fileName, true))
                {
                    this.gui.getListWidget().clearSchematicMetadataCache();
                    return true;
                }
            }
            else
            {
                this.gui.setString(StringUtils.translate("litematica.error.schematic_rename.read_failed"));
            }

            return false;
        }
    }

    public static class FileDeleter implements IConfirmationListener
    {
        protected final Path file;

        public FileDeleter(Path file)
        {
           this.file = file;
        }

        @Override
        public boolean onActionCancelled()
        {
            return false;
        }

        @Override
        public boolean onActionConfirmed()
        {
            try
            {
                Files.delete(this.file);
                return true;
            }
            catch (Exception e)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.failed_to_delete_file", this.file.toAbsolutePath());
            }

            return false;
        }
    }

    public static class PreviewGenerator
    {
        private final Path dir;
        private final String fileName;

        public PreviewGenerator(Path dir, String fileName)
        {
            this.dir = dir;
            this.fileName = fileName;
        }

        public void createAndSetPreviewImage()
        {
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(this.dir, this.fileName);

            if (schematic != null)
            {
                try
                {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    ScreenshotRecorder.takeScreenshot(mc.getFramebuffer(), (screenshot) ->
                    {
                        int x = screenshot.getWidth() >= screenshot.getHeight() ? (screenshot.getWidth() - screenshot.getHeight()) / 2 : 0;
                        int y = screenshot.getHeight() >= screenshot.getWidth() ? (screenshot.getHeight() - screenshot.getWidth()) / 2 : 0;
                        int longerSide = Math.min(screenshot.getWidth(), screenshot.getHeight());
                        //System.out.printf("w: %d, h: %d, x: %d, y: %d\n", screenshot.getWidth(), screenshot.getHeight(), x, y);
                        //int previewDimensions = 140;
                        int previewDimensions = 120;
                        NativeImage scaled = new NativeImage(previewDimensions, previewDimensions, false);
                        screenshot.resizeSubRectTo(x, y, longerSide, longerSide, scaled);
                        @SuppressWarnings("deprecation")
                        int[] pixels = scaled.makePixelArray();

                        schematic.getMetadata().setPreviewImagePixelData(pixels);
                        schematic.getMetadata().setTimeModifiedToNow();
                        schematic.writeToFile(this.dir, this.fileName, true);

                        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.info.schematic_manager.preview.success");
                    });
                }
                catch (Exception e)
                {
                    Litematica.LOGGER.warn("Exception while creating preview image", e);
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_rename.read_failed");
            }
        }
    }

    public enum ExportType implements IConfigOptionListEntry
    {
        SCHEMATIC   ("Schematic"),
        V6_LITEMATIC("V6 (1.20.4) Litematic"),
        VANILLA     ("Vanilla Structure");

        private final String displayName;

        ExportType(String displayName)
        {
            this.displayName = displayName;
        }

        @Override
        public String getStringValue()
        {
            return this.name().toLowerCase();
        }

        @Override
        public String getDisplayName()
        {
            return this.displayName;
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
        public ExportType fromString(String name)
        {
            return fromStringStatic(name);
        }

        public static ExportType fromStringStatic(String name)
        {
            for (ExportType al : ExportType.values())
            {
                if (al.name().equalsIgnoreCase(name))
                {
                    return al;
                }
            }

            return ExportType.V6_LITEMATIC;
        }
    }
}
