package fi.dy.masa.litematica.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.entity.EntityRenderStates;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ProfilerSystem;
import net.minecraft.util.profiler.Profilers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.mixin.IMixinProfilerSystem;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow private net.minecraft.client.world.ClientWorld world;
    @Shadow @Final private MinecraftClient client;
    @Shadow private Frustum frustum;
	@Shadow @Final private OrderedRenderCommandQueueImpl entityRenderCommandQueue;
	@Unique private Profiler profiler;

    @Inject(method = "reload()V", at = @At("RETURN"))
    private void litematica_onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.world != null && this.world == this.client.world)
        {
            if (this.profiler == null)
            {
                this.profiler = Profilers.get();
            }
            if (this.profiler instanceof ProfilerSystem ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
            {
                this.profiler.startTick();
            }

            LitematicaRenderer.getInstance().loadRenderers(this.profiler);
            SchematicWorldRefresher.INSTANCE.updateAll();
        }
    }

//    @Inject(method = "setupTerrain", at = @At("TAIL"))
//    private void litematica_onPostSetupTerrain(
//            Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci)
//    {
//        if (this.profiler == null)
//        {
//            this.profiler = Profilers.get();
//        }
//        if (this.profiler instanceof ProfilerSystem ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
//        {
//            this.profiler.startTick();
//        }
//
//        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(frustum, this.profiler);
//        LitematicaRenderer.getInstance().scheduleTranslucentSorting(camera.getCameraPos(), this.profiler);
//    }

    @Inject(method = "updateChunks",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/util/profiler/Profiler;pop()V",
                     ordinal = 1)
    )
    private void litematica_onPostUpdateChunks(Camera camera, CallbackInfo ci)
    {
        if (this.profiler == null)
        {
            this.profiler = Profilers.get();
        }
        if (this.profiler instanceof ProfilerSystem ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
        {
            this.profiler.startTick();
        }

        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(this.frustum, this.profiler);

//        if (SodiumCompat.hasSodium())
//        {
            LitematicaRenderer.getInstance().scheduleTranslucentSorting(camera.getPos(), this.profiler);
//        }
    }

//    @Inject(method = "translucencySort", at = @At("TAIL"))
//    private void litematica_onScheduleTranslucentSort(Vec3d cameraPos, CallbackInfo ci)
//    {
//        if (this.profiler == null)
//        {
//            this.profiler = Profilers.get();
//        }
//
//        if (this.profiler instanceof ProfilerSystem ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
//        {
//            this.profiler.startTick();
//        }
//
//        if (!SodiumCompat.hasSodium())
//        {
//            LitematicaRenderer.getInstance().scheduleTranslucentSorting(cameraPos, this.profiler);
//        }
//    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;renderMain(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Frustum;Lnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZLnet/minecraft/client/render/entity/EntityRenderStates;Lnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/util/profiler/Profiler;)V",
                    shift = At.Shift.BEFORE))
    private void litematica_onPreRenderMain(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline,
                                            Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix,
                                            GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci,
                                            @Local Profiler profiler)
    {
        this.profiler = profiler;
        LitematicaRenderer.getInstance().capturePreMainValues(fog, profiler);
		LitematicaRenderer.getInstance().piecewisePrepareEntities(camera, this.frustum, tickCounter, this.profiler);
    }

    @Inject(method = "renderBlockLayers", at = @At("TAIL"))
    private void litematica_onPrepareBlockLayers(Matrix4fc matrix4fc, double d, double e, double f, CallbackInfoReturnable<SectionRenderState> cir)
    {
        if (this.profiler == null)
        {
            this.profiler = Profilers.get();
        }
        if (this.profiler instanceof ProfilerSystem ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
        {
            this.profiler.startTick();
        }

        LitematicaRenderer.getInstance().piecewisePrepareBlockLayers(matrix4fc, d, e, f, this.profiler);

//        // Fix Sodium Compat
//        if (SodiumCompat.hasSodium())
//        {
//            LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.OPAQUE);
//            SodiumCompat.startBlockOutlineEnabled();
//        }
    }

	// BYTECODE Lamba Mixin for Section Group rendering
	@Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/client/render/Camera;Lnet/minecraft/util/profiler/Profiler;Lorg/joml/Matrix4f;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/render/entity/EntityRenderStates;ZLnet/minecraft/client/render/Frustum;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;)V",
			at = @At(value = "INVOKE",
					 target = "net/minecraft/client/render/SectionRenderState.renderSection (Lnet/minecraft/client/render/BlockRenderLayerGroup;)V",
					 ordinal = 0))
		private void litematica_renderMainSection_Opaque(GpuBufferSlice gpuBufferSlice, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f, Handle<Framebuffer> handle, Handle<Framebuffer> handle2, EntityRenderStates entityRenderStates, boolean bl, Frustum frustum, Handle<Framebuffer> handle3, Handle<Framebuffer> handle4, CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.OPAQUE);
	}

	@Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/client/render/Camera;Lnet/minecraft/util/profiler/Profiler;Lorg/joml/Matrix4f;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/render/entity/EntityRenderStates;ZLnet/minecraft/client/render/Frustum;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;)V",
			at = @At(value = "INVOKE",
					 target = "net/minecraft/client/render/SectionRenderState.renderSection (Lnet/minecraft/client/render/BlockRenderLayerGroup;)V",
					 ordinal = 1))
	private void litematica_renderMainSection_Translucent(GpuBufferSlice gpuBufferSlice, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f, Handle<Framebuffer> handle, Handle<Framebuffer> handle2, EntityRenderStates entityRenderStates, boolean bl, Frustum frustum, Handle<Framebuffer> handle3, Handle<Framebuffer> handle4, CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.TRANSLUCENT);
	}

	@Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/client/render/Camera;Lnet/minecraft/util/profiler/Profiler;Lorg/joml/Matrix4f;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/render/entity/EntityRenderStates;ZLnet/minecraft/client/render/Frustum;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;)V",
			at = @At(value = "INVOKE",
					 target = "net/minecraft/client/render/SectionRenderState.renderSection (Lnet/minecraft/client/render/BlockRenderLayerGroup;)V",
					 ordinal = 2))
	private void litematica_renderMainSection_Tripwire(GpuBufferSlice gpuBufferSlice, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f, Handle<Framebuffer> handle, Handle<Framebuffer> handle2, EntityRenderStates entityRenderStates, boolean bl, Frustum frustum, Handle<Framebuffer> handle3, Handle<Framebuffer> handle4, CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.TRIPWIRE);
	}

	// FIXME -- Done too soon, before the chunks are built.
//	@Inject(method = "fillEntityRenderStates", at = @At(value = "RETURN"))
//    private void litematica_onPostPrepareEntities(Camera camera, Frustum frustum, RenderTickCounter tickCounter, EntityRenderStates entityRenderStates, CallbackInfo ci)
//    {
//        if (this.profiler == null)
//        {
//            this.profiler = Profilers.get();
//        }
//
//        if (this.profiler instanceof ProfilerSystem ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
//        {
//            this.profiler.startTick();
//        }
//
//        LitematicaRenderer.getInstance().piecewisePrepareEntities(camera, frustum, tickCounter, entityRenderStates, this.profiler);
//    }

	@Inject(method = "pushEntityRenders", at = @At("RETURN"))
	private void litematica_onPostRenderEntities(MatrixStack matrices, Camera camera, EntityRenderStates renderStates, OrderedRenderCommandQueue queue, CallbackInfo ci)
	{
		if (this.profiler == null)
		{
			this.profiler = Profilers.get();
		}

		if (this.profiler instanceof ProfilerSystem ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
		{
			this.profiler.startTick();
		}

		LitematicaRenderer.getInstance().piecewiseRenderEntities(matrices, renderStates, queue, this.profiler);
	}

    @Inject(method = "renderBlockEntities", at = @At(value = "RETURN"))
    private void litematica_onPostRenderBlockEntities(MatrixStack matrices, Camera camera, float f, CallbackInfo ci)
    {
        if (this.profiler == null)
        {
            this.profiler = Profilers.get();
        }

        if (this.profiler instanceof ProfilerSystem ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
        {
            this.profiler.startTick();
        }

        LitematicaRenderer.getInstance().piecewisePrepareBlockEntities(matrices, this.entityRenderCommandQueue, f, this.profiler);
    }

//    @Inject(method = "renderTargetBlockOutline(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/util/math/MatrixStack;Z)V",
//            at = @At("HEAD"))
//    private void litematica_onRenderTargetOutline(Camera camera, VertexConsumerProvider.Immediate vertexConsumers,
//                                                  MatrixStack matrices, boolean translucent, CallbackInfo ci)
//    {
//        // Fix Sodium Compat
//        if (SodiumCompat.hasSodium() && translucent)
//        {
//            LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.TRANSLUCENT);
//            LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.TRIPWIRE);
//            SodiumCompat.endBlockOutlineEnabled();
//        }
//    }
}
