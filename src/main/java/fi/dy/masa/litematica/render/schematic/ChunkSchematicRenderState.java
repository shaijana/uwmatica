package fi.dy.masa.litematica.render.schematic;

import java.util.EnumMap;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BlockRenderLayer;

public record ChunkSchematicRenderState(
		Matrix4fc modelView, Vector4f colorMod, Vector3f modelOffset, Matrix4f texView,
		EnumMap<BlockRenderLayer, List<RenderPass.RenderObject<GpuBufferSlice[]>>> drawData,
		GpuBufferSlice[] chunkSections)
{
	public GpuBufferSlice asDynamicUniform()
	{
		return RenderSystem.getDynamicUniforms().write(this.modelView(), this.colorMod(), this.modelOffset(), this.texView());
	}
}
