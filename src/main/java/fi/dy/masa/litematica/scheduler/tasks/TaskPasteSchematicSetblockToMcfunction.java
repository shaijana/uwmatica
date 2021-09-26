package fi.dy.masa.litematica.scheduler.tasks;

/*SH

public class TaskPasteSchematicSetblockToMcfunction extends TaskPasteSchematicPerChunkCommand
{
    protected final String fileNameBase;
    protected BufferedWriter writer;
    protected int fileIndex = 1;
    protected int commandsInCurrentFile;

    public TaskPasteSchematicSetblockToMcfunction(Collection<SchematicPlacement> placements, LayerRange range, boolean changedBlocksOnly)
    {
        super(placements, range, changedBlocksOnly);

        String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date(System.currentTimeMillis()));
        this.fileNameBase = "paste_commands_" + date + "_";

        this.openNextOutputFile();
    }

    @Override
    protected boolean processBox(ChunkPos pos,
                                 IntBoundingBox box,
                                 WorldSchematic worldSchematic,
                                 ClientWorld worldClient,
                                 ClientPlayerEntity player)
    {
        if (this.commandsInCurrentFile > 64000)
        {
            this.openNextOutputFile();
            this.commandsInCurrentFile = 0;
        }

        return super.processBox(pos, box, worldSchematic, worldClient, player);
    }

    @Override
    protected void sendCommandToServer(String command, ClientPlayerEntity player)
    {
        if (this.writer != null)
        {
            try
            {
                this.writer.write(command);
                this.writer.newLine();
                ++this.commandsInCurrentFile;
            }
            catch (IOException e)
            {
                this.finished = true;
            }
        }
    }

    @Override
    public void stop()
    {
        this.closeCurrentFile();
        super.stop();
    }

    protected void openNextOutputFile()
    {
        this.closeCurrentFile();

        try
        {
            String fileName = this.fileNameBase + String.format("%02d", this.fileIndex) + ".mcfunction";
            File file = new File(DataManager.getCurrentConfigDirectory(), fileName);
            this.writer = new BufferedWriter(new FileWriter(file));
        }
        catch (IOException ignore) {}

        ++this.fileIndex;
    }

    protected void closeCurrentFile()
    {
        try
        {
            if (this.writer != null)
            {
                this.writer.close();
            }
        }
        catch (IOException ignore) {}
    }
}
*/
