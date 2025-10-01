package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;

public class SchematicRenderState
{
	protected CameraRenderState cameraState = new CameraRenderState();
	protected List<EntityRenderState> entityStates = new ArrayList<>();
	protected List<BlockEntityRenderState> tileEntityStates = new ArrayList<>();

	protected void clear()
	{
		this.entityStates.clear();
		this.tileEntityStates.clear();
	}
}
