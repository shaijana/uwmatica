package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import javax.annotation.Nullable;
import com.google.common.collect.Queues;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.ToBooleanFunction;
import fi.dy.masa.malilib.util.IntBoundingBox;

public abstract class TaskProcessChunkMultiPhase extends TaskProcessChunkBase
{
    protected TaskPhase phase = TaskPhase.INIT;
    @Nullable protected ChunkPos currentChunkPos;
    @Nullable protected IntBoundingBox currentBox;
    @Nullable protected Iterator<Entity> entityIterator;
    @Nullable protected Iterator<BlockPos> positionIterator;

    protected final boolean useWorldEdit;

    protected int maxCommandsPerTick = 16;
    protected int processedChunksThisTick;
    protected int sentCommandsThisTick;
    protected long gameRuleProbeTimeout;
    protected long maxGameRuleProbeTime = 2000000000L; // 2 second timeout
    protected long taskStartTimeForCurrentTick;
    protected boolean shouldEnableFeedback;

    protected final Queue<String> queuedCommands = Queues.newArrayDeque();
    protected ToBooleanFunction<Text> gameRuleListener = this::checkCommandFeedbackGameRuleState;
    protected Runnable initTask = this::initPhaseStartProbe;
    protected Runnable probeTask = this::probePhase;
    protected Runnable waitForChunkTask = this::fetchNextChunk;
    protected Runnable processBoxBlocksTask;
    protected Runnable processBoxEntitiesTask;

    public enum TaskPhase
    {
        INIT,
        GAME_RULE_PROBE,
        WAIT_FOR_CHUNKS,
        PROCESS_BOX_BLOCKS,
        PROCESS_BOX_ENTITIES,
        FINISHED
    }

    protected TaskProcessChunkMultiPhase(String nameOnHud)
    {
        super(nameOnHud);

        this.useWorldEdit = Configs.Generic.COMMAND_USE_WORLDEDIT.getBooleanValue();
    }

    protected boolean executeMultiPhase(Profiler profiler)
    {
        profiler.push("chunk_multi_phase");
        this.taskStartTimeForCurrentTick = Util.getMeasuringTimeNano();
        this.sentCommandsThisTick = 0;
        this.processedChunksThisTick = 0;

        if (this.phase == TaskPhase.INIT)
        {
            this.initTask.run();
        }

        if (this.phase == TaskPhase.GAME_RULE_PROBE)
        {
            this.probeTask.run();
            profiler.pop();
            return false;
        }

        if (this.currentChunkPos != null && this.canProcessChunk(this.currentChunkPos) == false)
        {
            profiler.pop();
            return false;
        }

        int commandsLast = -1;
        int processedChunksLast = -1;

        while (this.sentCommandsThisTick < this.maxCommandsPerTick &&
               (this.sentCommandsThisTick > commandsLast || this.processedChunksThisTick != processedChunksLast))
        {
            long currentTime = Util.getMeasuringTimeNano();
            long elapsedTickTime = (currentTime - this.taskStartTimeForCurrentTick);

            if (elapsedTickTime >= 25000000L)
            {
                break;
            }

            commandsLast = this.sentCommandsThisTick;
            processedChunksLast = this.processedChunksThisTick;

            if (this.phase == TaskPhase.WAIT_FOR_CHUNKS)
            {
                this.waitForChunkTask.run();
            }

            if (this.phase == TaskPhase.PROCESS_BOX_BLOCKS && this.processBoxBlocksTask != null)
            {
                this.processBoxBlocksTask.run();
            }

            if (this.phase == TaskPhase.PROCESS_BOX_ENTITIES && this.processBoxEntitiesTask != null)
            {
                this.processBoxEntitiesTask.run();
            }

            if (this.phase == TaskPhase.FINISHED)
            {
                profiler.pop();
                return true;
            }
        }

        if (this.processedChunksThisTick > 0)
        {
            this.updateInfoHudLines();
        }

        profiler.pop();
        return false;
    }

    protected void initPhaseStartProbe()
    {
        if (Configs.Generic.COMMAND_DISABLE_FEEDBACK.getBooleanValue() && this.isInWorld())
        {
            DataManager.addChatListener(this.gameRuleListener);
            this.sendCommand("gamerule sendCommandFeedback");
            this.gameRuleProbeTimeout = Util.getMeasuringTimeNano() + this.maxGameRuleProbeTime;
            this.phase = TaskPhase.GAME_RULE_PROBE;
        }
        else
        {
            this.shouldEnableFeedback = false;
            this.phase = TaskPhase.WAIT_FOR_CHUNKS;
        }
    }

    protected void probePhase()
    {
        if (Util.getMeasuringTimeNano() > this.gameRuleProbeTimeout)
        {
            this.shouldEnableFeedback = false;
            this.phase = TaskPhase.WAIT_FOR_CHUNKS;
        }
    }

    protected boolean checkCommandFeedbackGameRuleState(Text message)
    {
        if (this.isInWorld() && message instanceof MutableText mutableText &&
            mutableText.getContent() instanceof TranslatableTextContent text)
        {
            if ("commands.gamerule.query".equals(text.getKey()))
            {
                Object[] args = text.getArgs();
                this.shouldEnableFeedback = args.length == 1 && args[0].equals("true");
                this.phase = TaskPhase.WAIT_FOR_CHUNKS;

                if (this.shouldEnableFeedback)
                {
                    this.sendCommand("gamerule sendCommandFeedback false");
                }

                return true;
            }
        }

        return false;
    }

    protected void fetchNextChunk()
    {
        if (this.pendingChunks.isEmpty() == false)
        {
            this.sortChunkList();

            ChunkPos pos = this.pendingChunks.get(0);

            if (this.canProcessChunk(pos))
            {
                this.currentChunkPos = pos;
                this.onNextChunkFetched(pos);
            }
        }
        else
        {
            this.phase = TaskPhase.FINISHED;
            this.finished = true;
        }
    }

    protected void onNextChunkFetched(ChunkPos pos)
    {
    }

    protected void startNextBox(ChunkPos pos)
    {
        List<IntBoundingBox> list = this.boxesInChunks.get(pos);

        if (list.isEmpty() == false)
        {
            this.currentBox = list.get(0);
            this.onStartNextBox(this.currentBox);
        }
        else
        {
            this.currentBox = null;
            this.phase = TaskPhase.WAIT_FOR_CHUNKS;
        }
    }

    protected void onStartNextBox(IntBoundingBox box)
    {
    }

    protected void onFinishedProcessingBox(ChunkPos pos, IntBoundingBox box)
    {
        this.boxesInChunks.remove(pos, box);
        this.currentBox = null;
        this.entityIterator = null;
        this.positionIterator = null;

        if (this.boxesInChunks.get(pos).isEmpty())
        {
            this.finishProcessingChunk(pos);
        }
        else
        {
            this.startNextBox(pos);
        }
    }

    protected void finishProcessingChunk(ChunkPos pos)
    {
        this.boxesInChunks.removeAll(pos);
        this.pendingChunks.remove(pos);
        this.currentChunkPos = null;
        ++this.processedChunksThisTick;
        this.phase = TaskPhase.WAIT_FOR_CHUNKS;
        this.onFinishedProcessingChunk(pos);
    }

    protected void onFinishedProcessingChunk(ChunkPos pos)
    {
    }

    protected void sendCommand(String cmd)
    {
        this.sendCommand(cmd, this.mc.player);
    }

    protected void sendCommand(String command, ClientPlayerEntity player)
    {
        player.networkHandler.sendChatCommand(command);
        ++this.sentCommandsThisTick;
    }

    protected void sendQueuedCommands()
    {
        while (this.sentCommandsThisTick < this.maxCommandsPerTick &&
               this.queuedCommands.isEmpty() == false)
        {
            this.sendCommand(this.queuedCommands.poll());
        }

        if (this.queuedCommands.isEmpty())
        {
            this.finishProcessingChunk(this.currentChunkPos);
        }
    }

    protected void sendTaskEndCommands()
    {
        if (this.isInWorld())
        {
            if (this.useWorldEdit)
            {
                this.sendCommand("/perf neighbors on");
            }

            if (this.shouldEnableFeedback)
            {
                this.sendCommand("gamerule sendCommandFeedback true");
            }
        }
    }
}
