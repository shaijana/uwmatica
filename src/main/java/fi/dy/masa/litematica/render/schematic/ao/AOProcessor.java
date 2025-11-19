package fi.dy.masa.litematica.render.schematic.ao;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import fi.dy.masa.litematica.config.Configs;

public abstract class AOProcessor extends AOLightmap
{
	public static final Direction[] DIRECTIONS = Direction.values();

    public static AOProcessor get()
    {
        if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
        {
            return new AOProcessorModern();
        }
        else
        {
            return new AOProcessorLegacy();
        }
    }

    public void apply(BlockRenderView world, BlockState state, BlockPos pos, Direction face, boolean hasShade)
    {
    }
}
