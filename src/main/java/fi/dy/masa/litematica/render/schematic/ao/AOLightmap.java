package fi.dy.masa.litematica.render.schematic.ao;

import net.minecraft.util.math.BlockPos;

import fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic;

public class AOLightmap
{
	public final BlockPos.Mutable pos = new BlockPos.Mutable();
	public boolean hasOffset;       // has Offset (field_58160)
	public boolean hasNeighbors;    // has NeighborData (field_58161)
	public final float[] fs = new float[4];
	public final int[] is = new int[4];
	public int lastTintIndex = -1;
	public int colorOfLastTintIndex;
	public final AOBrightness brightnessCache = BlockModelRendererSchematic.BRIGHTNESS_CACHE.get();
}
