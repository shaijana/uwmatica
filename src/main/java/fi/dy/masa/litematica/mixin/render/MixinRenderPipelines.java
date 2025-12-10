package fi.dy.masa.litematica.mixin.render;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.compat.iris.IrisCompat;

@Mixin(RenderPipelines.class)
@ApiStatus.Experimental
public abstract class MixinRenderPipelines
{
//	@Shadow @Final private static Map<Identifier, RenderPipeline> PIPELINES_BY_LOCATION;
//	@Shadow @Final private static RenderPipeline.Snippet MATRICES_FOG_SNIPPET;
//
//	@Unique private static final BlendFunction MASA_BLEND = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
//	@Unique private static final BlendFunction MASA_BLEND_SIMPLE = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
//	@Unique private static final String LEGACY_TERRAIN = "core/legacy_terain";
//	@Unique private static final String CORE_TERRAIN = "core/terain";
//
//	@Shadow
//	private static RenderPipeline register(RenderPipeline renderPipeline)
//	{
//		PIPELINES_BY_LOCATION.put(renderPipeline.getLocation(), renderPipeline);
//		return renderPipeline;
//	}
//
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void litematica_onRegisterPipelines(CallbackInfo ci)
	{
//		LitematicaPipelines.TERRAIN_TRANSLUCENT_STAGE =
//				RenderPipeline.builder(MATRICES_FOG_SNIPPET)
//				              .withVertexShader(CORE_TERRAIN)
//				              .withFragmentShader(CORE_TERRAIN)
//				              .withSampler("Sampler0")
//				              .withSampler("Sampler2")
//				              .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
//				              .withBlend(BlendFunction.TRANSLUCENT)
//				              .buildSnippet();
//
//		LitematicaPipelines.TERRAIN_MASA_STAGE =
//				RenderPipeline.builder(MATRICES_FOG_SNIPPET)
//				              .withVertexShader(CORE_TERRAIN)
//				              .withFragmentShader(CORE_TERRAIN)
//				              .withSampler("Sampler0")
//				              .withSampler("Sampler2")
//				              .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
//				              .withBlend(MASA_BLEND)
//				              .buildSnippet();
//
//		// TERRAIN_MASA --> PRE-REGISTER
//		LitematicaPipelines.SOLID_MASA =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/solid/masa"))
//				                       .build());
//
//		LitematicaPipelines.CUTOUT_MIPPED_MASA =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/cutout_mipped/masa"))
//				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
//				                       .build());
//
//		LitematicaPipelines.CUTOUT_MASA =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/cutout/masa"))
//				                       .withShaderDefine("ALPHA_CUTOUT", 0.1F)
//				                       .build());
//
//		LitematicaPipelines.TRANSLUCENT_MASA =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_TRANSLUCENT_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/translucent/masa"))
//				                       .build());
//
//		LitematicaPipelines.TRIPWIRE_MASA =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_TRANSLUCENT_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/tripwire/masa"))
//				                       .withShaderDefine("ALPHA_CUTOUT", 0.1F)
//				                       .build());
//
//		LitematicaPipelines.WIREFRAME_MASA =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/wireframe/masa"))
//				                       .withPolygonMode(PolygonMode.WIREFRAME)
//				                       .build());
//
//		// TERRAIN_MASA_OFFSET --> PRE-REGISTER
//		LitematicaPipelines.SOLID_MASA_OFFSET =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/solid/masa/offset"))
//				                       .withDepthBias(-0.3f, -0.6f)
//				                       .build());
//
//		LitematicaPipelines.CUTOUT_MIPPED_MASA_OFFSET =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/cutout_mipped/masa/offset"))
//				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
//				                       .withDepthBias(-0.3f, -0.6f)
//				                       .build());
//
//		LitematicaPipelines.CUTOUT_MASA_OFFSET =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/cutout/masa/offset"))
//				                       .withShaderDefine("ALPHA_CUTOUT", 0.1F)
//				                       .withDepthBias(-0.3f, -0.6f)
//				                       .build());
//
//		LitematicaPipelines.TRANSLUCENT_MASA_OFFSET =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_TRANSLUCENT_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/translucent/masa/offset"))
//				                       .withDepthBias(-0.3f, -0.6f)
//				                       .build());
//
//		LitematicaPipelines.TRIPWIRE_MASA_OFFSET =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_TRANSLUCENT_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/tripwire/masa/offset"))
//				                       .withShaderDefine("ALPHA_CUTOUT", 0.1F)
//				                       .withDepthBias(-0.3f, -0.6f)
//				                       .build());
//
//		LitematicaPipelines.WIREFRAME_MASA_OFFSET =
//				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
//				                       .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "pipeline/wireframe/masa/offset"))
//				                       .withPolygonMode(PolygonMode.WIREFRAME)
//				                       .withDepthBias(-0.3f, -0.6f)
//				                       .build());
//

		IrisCompat.registerPipelines();
	}
}
