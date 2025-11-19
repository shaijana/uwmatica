package fi.dy.masa.litematica.mixin.render;

import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockRenderManager.class)
public interface IMixinBlockRenderManager
{
	@Accessor("fluidRenderer")
	FluidRenderer litematica_getFluidRenderer();
}
