package fi.dy.masa.litematica.mixin.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.World;
import fi.dy.masa.litematica.util.IWorldUpdateSuppressor;

@Mixin(World.class)
public class MixinWorld implements IWorldUpdateSuppressor
{
    @Unique private boolean litematica_preventBlockUpdates;

    @Override
    public boolean litematica_getShouldPreventBlockUpdates()
    {
        return this.litematica_preventBlockUpdates;
    }

    @Override
    public void litematica_setShouldPreventBlockUpdates(boolean preventUpdates)
    {
        this.litematica_preventBlockUpdates = preventUpdates;
    }
}
