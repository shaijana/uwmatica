package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;

public class BufferBuilderCache implements AutoCloseable
{
    private final ConcurrentHashMap<BlockRenderLayer, BufferBuilder> blockBufferBuilders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<RenderLayer, BufferBuilder> layerBufferBuilders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<OverlayRenderType, BufferBuilder> overlayBufferBuilders = new ConcurrentHashMap<>();

    protected BufferBuilderCache() { }

    protected boolean hasBufferByBlockLayer(BlockRenderLayer layer)
    {
        return this.blockBufferBuilders.containsKey(layer);
    }

    protected boolean hasBufferByLayer(RenderLayer layer)
    {
        return this.layerBufferBuilders.containsKey(layer);
    }

    protected boolean hasBufferByOverlay(OverlayRenderType type)
    {
        return this.overlayBufferBuilders.containsKey(type);
    }

    protected BufferBuilder getBufferByBlockLayer(BlockRenderLayer layer, @Nonnull BufferAllocatorCache allocators)
    {
        synchronized (this.blockBufferBuilders)
        {
            return this.blockBufferBuilders.computeIfAbsent(layer, (key) -> new BufferBuilder(allocators.getBufferByBlockLayer(key), key.getPipeline().getVertexFormatMode(), key.getPipeline().getVertexFormat()));
        }
    }

    protected BufferBuilder getBufferByLayer(RenderLayer layer, @Nonnull BufferAllocatorCache allocators)
    {
        synchronized (this.layerBufferBuilders)
        {
            return this.layerBufferBuilders.computeIfAbsent(layer, (key) -> new BufferBuilder(allocators.getBufferByLayer(key), key.getDrawMode(), key.getVertexFormat()));
        }
    }

    protected BufferBuilder getBufferByOverlay(OverlayRenderType type, @Nonnull BufferAllocatorCache allocators)
    {
        synchronized (this.overlayBufferBuilders)
        {
            return this.overlayBufferBuilders.computeIfAbsent(type, (key) -> new BufferBuilder(allocators.getBufferByOverlay(key), key.getDrawMode(), key.getVertexFormat()));
        }
    }

    protected void clearAll()
    {
        ArrayList<BufferBuilder> buffers;

        synchronized (this.blockBufferBuilders)
        {
            buffers = new ArrayList<>(this.blockBufferBuilders.values());
            this.blockBufferBuilders.clear();
        }
        synchronized (this.layerBufferBuilders)
        {
            buffers.addAll(this.layerBufferBuilders.values());
            this.layerBufferBuilders.clear();
        }
        synchronized (this.overlayBufferBuilders)
        {
            buffers.addAll(this.overlayBufferBuilders.values());
            this.overlayBufferBuilders.clear();
        }
        for (BufferBuilder buffer : buffers)
        {
            try
            {
                BuiltBuffer built = buffer.endNullable();
                if (built != null)
                {
                    built.close();
                }
            }
            catch (Exception ignored) {}
        }
    }

    @Override
    public void close() throws Exception
    {
        this.clearAll();
    }
}
