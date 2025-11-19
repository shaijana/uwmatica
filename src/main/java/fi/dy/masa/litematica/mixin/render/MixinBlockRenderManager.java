package fi.dy.masa.litematica.mixin.render;

import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.LitematicaRenderer;

@Mixin(BlockRenderManager.class)
public class MixinBlockRenderManager
{
	@Inject(method = "reload", at = @At("TAIL"))
	private void litematica_onBlockRenderManagerReload(ResourceManager manager, CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().onBlockModelRendererReload((BlockRenderManager) (Object) this);
	}
}
