package fi.dy.masa.litematica.render.schematic.ao;

import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;

public enum AOTranslations
{
	DOWN(0, 1, 2, 3),
	UP(2, 3, 0, 1),
	NORTH(3, 0, 1, 2),
	SOUTH(0, 1, 2, 3),
	WEST(3, 0, 1, 2),
	EAST(1, 2, 3, 0);

	final int vert0;
	final int vert1;
	final int vert2;
	final int vert3;

	private static final AOTranslations[] VALUES = Util.make(new AOTranslations[6], (values) ->
	{
		values[Direction.DOWN.getIndex()] = DOWN;
		values[Direction.UP.getIndex()] = UP;
		values[Direction.NORTH.getIndex()] = NORTH;
		values[Direction.SOUTH.getIndex()] = SOUTH;
		values[Direction.WEST.getIndex()] = WEST;
		values[Direction.EAST.getIndex()] = EAST;
	});

	AOTranslations(final int vert0, final int vert1, final int vert2, final int vert3)
	{
		this.vert0 = vert0;
		this.vert1 = vert1;
		this.vert2 = vert2;
		this.vert3 = vert3;
	}

	public static AOTranslations getVertexTranslations(Direction face)
	{
		return VALUES[face.getIndex()];
	}
}
