package fi.dy.masa.litematica.render.schematic;

import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.util.profiling.ProfilerFiller;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

public record ChunkRenderBatchDraw(
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> drawData,
        boolean renderCollidingBlocks, boolean renderTranslucent,
        int maxIndicesRequired,
        GpuBufferSlice[] dynamicTransforms)
{
    public void draw(ChunkSectionLayerGroup group, ProfilerFiller profiler)
    {
        RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer gpuBuffer = this.maxIndicesRequired == 0 ? null : shapeIndexBuffer.getBuffer(this.maxIndicesRequired);
        VertexFormat.IndexType indexType = this.maxIndicesRequired == 0 ? null : shapeIndexBuffer.type();
        ChunkSectionLayer[] layers = group.layers();
        Minecraft mc = Minecraft.getInstance();
        RenderTarget fb = group.outputTarget();

        profiler.push("draw_group");

        try (RenderPass pass = RenderSystem.getDevice()
                                           .createCommandEncoder()
                                           .createRenderPass(
                                                   () -> "litematica:schematic_chunk/"+group.label(),
                                                   fb.getColorTextureView(),
                                                   OptionalInt.empty(),
                                                   fb.getDepthTextureView(),
                                                   OptionalDouble.empty()
                                           ))
        {
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindSampler("Sampler2", mc.gameRenderer.lightTexture().getTextureView());

            for (ChunkSectionLayer layer : layers)
            {
                List<RenderPass.Draw<GpuBufferSlice[]>> list = this.drawData.get(layer);

                profiler.popPush("draw_group_"+layer.label());
                if (!list.isEmpty())
                {
                    if (layer == ChunkSectionLayer.TRANSLUCENT)
                    {
                        list = list.reversed();
                    }

                    pass.setPipeline(
                            this.renderCollidingBlocks() ?
                            ChunkRenderLayers.PIPELINE_MAP.get(layer).getRight() :
                            ChunkRenderLayers.PIPELINE_MAP.get(layer).getLeft()
                    );

                    pass.bindSampler("Sampler0", layer.textureView());
                    pass.drawMultipleIndexed(list, gpuBuffer, indexType, List.of("DynamicTransforms"), this.dynamicTransforms);
                }
            }
        }

        profiler.pop();
    }
}
