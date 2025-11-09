package fi.dy.masa.litematica.render.schematic.ao;

import java.util.BitSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import fi.dy.masa.litematica.config.Configs;

public abstract class AOProcessor
{
    public final float[] brightness = new float[4];
    public final int[] light = new int[4];

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

    public void apply(BlockAndTintGetter world, BlockState state, BlockPos pos, Direction direction, float[] box, BitSet shapeState, boolean hasShade)
    {
    }
}
