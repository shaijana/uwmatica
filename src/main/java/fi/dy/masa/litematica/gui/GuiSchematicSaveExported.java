package fi.dy.masa.litematica.gui;

import java.nio.file.Files;
import java.nio.file.Path;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicManager.ExportType;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.WorldUtils;

public class GuiSchematicSaveExported extends GuiSchematicSaveBase
{
    private final ExportType exportType;
    private final DirectoryEntryType type;
    private final Path dirSource;
    private final String inputFileName;

    public GuiSchematicSaveExported(DirectoryEntryType type, Path dirSource, String inputFileName, ExportType exportType)
    {
        super(null);

        this.exportType = exportType;
        this.type = type;
        this.dirSource = dirSource;
        this.inputFileName = inputFileName;
        this.defaultText = FileUtils.getNameWithoutExtension(inputFileName);
        this.title = StringUtils.translate("litematica.gui.title.save_exported_schematic", exportType.getDisplayName(), inputFileName);
        this.useTitleHierarchy = false;
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_save_exported";
    }

    @Override
    public Path getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected IButtonActionListener createButtonListener(ButtonType type)
    {
        return new ButtonListener(type, this);
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final GuiSchematicSaveExported gui;
        private final ButtonType type;

        public ButtonListener(ButtonType type, GuiSchematicSaveExported gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == ButtonType.SAVE)
            {
                Path dir = this.gui.getListWidget().getCurrentDirectory();
                String fileName = this.gui.getTextFieldText();

                if (!Files.isDirectory(dir))
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_directory", dir.toAbsolutePath());
                    return;
                }

                if (fileName.isEmpty())
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_schematic_name", fileName);
                    return;
                }

                if (this.gui.type == DirectoryEntryType.FILE)
                {
                    Path inDir = this.gui.dirSource;
                    String inFile = this.gui.inputFileName;
                    boolean override = GuiBase.isShiftDown();
                    boolean ignoreEntities = this.gui.checkboxIgnoreEntities.isChecked();
                    FileType fileType = FileType.fromFile(inDir.resolve(inFile));

                    if (fileType == FileType.LITEMATICA_SCHEMATIC)
                    {
                        if (this.gui.exportType == ExportType.V6_LITEMATIC)
                        {
                            if (WorldUtils.convertLitematicaSchematicToV6LitematicaSchematic(inDir, inFile, dir, fileName, ignoreEntities, override, this.gui))
                            {
                                this.gui.addMessage(MessageType.SUCCESS, "litematica.message.litematic_downgrade_exported_as", fileName);
                                this.gui.getListWidget().refreshEntries();
                            }
                        }
                        else if (this.gui.exportType == ExportType.SCHEMATIC)
                        {
                            if (WorldUtils.convertLitematicaSchematicToSchematicaSchematic(inDir, inFile, dir, fileName, ignoreEntities, override, this.gui))
                            {
                                this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_exported_as", fileName);
                                this.gui.getListWidget().refreshEntries();
                            }
                        }
                        else if (this.gui.exportType == ExportType.VANILLA)
                        {
                            if (WorldUtils.convertLitematicaSchematicToVanillaStructure(inDir, inFile, dir, fileName, ignoreEntities, override, this.gui))
                            {
                                this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_exported_as", fileName);
                                this.gui.getListWidget().refreshEntries();
                            }
                        }

                        return;
                    }
                }

                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_export.unsupported_type", this.gui.inputFileName);
            }
        }
    }
}
