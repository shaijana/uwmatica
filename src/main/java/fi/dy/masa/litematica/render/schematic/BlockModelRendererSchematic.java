package fi.dy.masa.litematica.render.schematic;

import java.util.List;
import javax.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3fc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.world.BlockRenderView;

import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.render.IMixinBlockRenderManager;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.ao.*;

public class BlockModelRendererSchematic
{
	public static final ThreadLocal<AOBrightness> BRIGHTNESS_CACHE = ThreadLocal.withInitial(AOBrightness::new);
    private final LocalRandom random;
    private final BlockColors colorMap;
    private FluidRenderer liquidRenderer;
    private BakedModelManager bakedManager;

    public BlockModelRendererSchematic(BlockColors blockColorsIn, BlockRenderManager manager)
    {
		this.random = new LocalRandom(42L);
        this.colorMap = blockColorsIn;
        this.reload(manager);
    }

	public static void enableCache()
	{
		if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
		{
			BRIGHTNESS_CACHE.get().enable();
		}
	}

	public static void disableCache()
	{
		if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
		{
			BRIGHTNESS_CACHE.get().disable();
		}
	}

	public void reload(BlockRenderManager manager)
	{
		this.liquidRenderer = ((IMixinBlockRenderManager) manager).litematica_getFluidRenderer();
	}

    public void setBakedManager(BakedModelManager manager)
    {
        this.bakedManager = manager;
    }

	public LocalRandom getRandom()
	{
		return this.random;
	}

	public void setSeed(long seed)
	{
		this.random.setSeed(seed);
	}

    public boolean renderModel(BlockRenderView worldIn, List<BlockModelPart> modelParts,
                               BlockState stateIn, BlockPos posIn,
                               MatrixStack matrices, VertexConsumer vertexConsumer,
                               boolean cull, int overlay)
    {
		if (!modelParts.isEmpty())
		{
			boolean ao = MinecraftClient.isAmbientOcclusionEnabled() &&
					stateIn.getLuminance() == 0 && modelParts.getFirst().useAmbientOcclusion();

			Vec3d offset = stateIn.getModelOffset(posIn);
			matrices.translate((float) offset.x, (float) offset.y, (float) offset.z);

			try
			{
				if (ao)
				{
//                System.out.printf("renderModelSmooth(): pos [%s] / state [%s] / parts? [%d]\n", posIn.toShortString(), stateIn, modelParts.size());
					return this.renderModelSmooth(worldIn, modelParts, stateIn, posIn, matrices, vertexConsumer, overlay, cull);
				}
				else
				{
//                System.out.printf("renderModelFlat(): pos [%s] / state [%s] / parts? [%d]\n", posIn.toShortString(), stateIn, modelParts.size());
					return this.renderModelFlat(worldIn, modelParts, stateIn, posIn, matrices, vertexConsumer, overlay, cull);
				}
			}
			catch (Throwable throwable)
			{
				//Litematica.logger.error("renderModel: Crash caught: [{}]", !throwable.getMessage().isEmpty() ? throwable.getMessage() : "<EMPTY>");
				CrashReport crashreport = CrashReport.create(throwable, "Tesselating block model");
				CrashReportSection crashreportcategory = crashreport.addElement("Block model being tesselated");
				CrashReportSection.addBlockInfo(crashreportcategory, worldIn, posIn, stateIn);
				crashreportcategory.add("Using AO", ao);
				throw new CrashException(crashreport);
			}
		}

	    return false;
    }

    public boolean renderModelFlat(BlockRenderView worldIn, List<BlockModelPart> modelParts,
                                   BlockState stateIn, BlockPos posIn,
                                   MatrixStack matrices, VertexConsumer vertexConsumer,
                                   int overlay, boolean cull)
    {
		AOLightmap lightmap = new AOLightmap();
	    BlockPos.Mutable mutablePos = posIn.mutableCopy();
	    boolean renderedSomething = false;
	    int i = 0;
	    int j = 0;

        for (BlockModelPart part : modelParts)
        {
            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
				int index = 1 << side.ordinal();
				boolean bl = (i & index) == 1;
	            boolean bl2 = (j & index) == 1;

				if (!bl || bl2)
				{
					List<BakedQuad> quads = part.getQuads(side);

					if (!quads.isEmpty())
					{
						BlockPos pos = lightmap.pos.set(posIn, side);
						mutablePos.set(posIn, side);

						if (!bl)
						{
							bl2 = shouldRenderModelSide(worldIn, stateIn, posIn, side, cull, mutablePos);
							i |= index;

							if (bl2)
							{
								j |= index;
							}
						}

						if (bl2)
						{
//							int light = WorldRenderer.getLightmapCoordinates(worldIn, stateIn, posIn.offset(side));
//							int light = WorldRenderer.getLightmapCoordinates(worldIn, mutablePos);
							int light = lightmap.brightnessCache.getInt(stateIn, worldIn, pos);

							this.renderQuadsFlat(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, lightmap, overlay, light, false);
							renderedSomething = true;
						}
					}
				}
            }

//            random.setSeed(seedIn);
//            modelIn.getQuads(stateIn, null, random)
            List<BakedQuad> quads = part.getQuads(null);

            if (!quads.isEmpty())
            {
                this.renderQuadsFlat(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, lightmap, overlay, -1, true);
                renderedSomething = true;
            }
        }

        return renderedSomething;
    }

	public boolean renderModelSmooth(BlockRenderView worldIn, List<BlockModelPart> modelParts, BlockState stateIn, BlockPos posIn,
	                                 MatrixStack matrices, VertexConsumer vertexConsumer,
	                                 int overlay, boolean cull)
	{
		AOProcessor ao = AOProcessor.get();
		BlockPos.Mutable mutablePos = posIn.mutableCopy();
		boolean renderedSomething = false;
		int i = 0;
		int j = 0;

		for (BlockModelPart part : modelParts)
		{
			for (Direction side : PositionUtils.ALL_DIRECTIONS)
			{
				int index = 1 << side.ordinal();
				boolean bl = (i & index) == 1;
				boolean bl2 = (j & index) == 1;

				if (!bl || bl2)
				{
					List<BakedQuad> quads = part.getQuads(side);

					if (!quads.isEmpty())
					{
						mutablePos.set(posIn, side);

						if (!bl)
						{
							bl2 = shouldRenderModelSide(worldIn, stateIn, posIn, side, cull, mutablePos);
							i |= index;

							if (bl2)
							{
								j |= index;
							}
						}

						if (bl2)
						{
							this.renderQuadsSmooth(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, ao, overlay);
							renderedSomething = true;
						}
					}
				}
			}

//            random.setSeed(seedIn);
//            modelIn.getQuads(stateIn, null, random)
			List<BakedQuad> quads = part.getQuads(null);

			if (!quads.isEmpty())
			{
				this.renderQuadsSmooth(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, ao, overlay);
				renderedSomething = true;
			}
		}

		return renderedSomething;
	}

	public static boolean shouldRenderModelSide(BlockRenderView worldIn, BlockState stateIn, BlockPos posIn,
	                                            Direction side, boolean cull, BlockPos mutable)
    {
//		if (!cull) return true;
        return (DataManager.getRenderLayerRange().isPositionAtRenderEdgeOnSide(posIn, side) ||
		        // Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() &&
		        (Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.getBooleanValue())) ||
		        Block.shouldDrawSide(stateIn, worldIn.getBlockState(mutable), side);
	}

    private void renderQuadsFlat(BlockRenderView world, BlockState state, BlockPos pos,
                                 MatrixStack matrices, VertexConsumer vertexConsumer,
                                 List<BakedQuad> quads, AOLightmap lightmap,
                                 int overlay, int light, boolean useWorldLight)
    {
        //final int size = list.size();

        for (BakedQuad bakedQuad : quads)
        {
            if (useWorldLight)
            {
                this.getQuadDimensions(world, state, pos, bakedQuad, lightmap);
                BlockPos blockPos = lightmap.hasOffset ? lightmap.pos.set(pos, bakedQuad.face()) : pos;
                light = lightmap.brightnessCache.isEnabled()
                        ? lightmap.brightnessCache.getInt(state, world, pos)
                        : WorldRenderer.getLightmapCoordinates(world, blockPos);
            }

            float b = world.getBrightness(bakedQuad.face(), bakedQuad.shade());
//            float[] bo = new float[]{b, b, b, b};
//	          int[] lo = new int[]{light, light, light, light};

	        lightmap.fs[0] = b;
	        lightmap.fs[1] = b;
	        lightmap.fs[2] = b;
	        lightmap.fs[3] = b;
			lightmap.is[0] = light;
	        lightmap.is[1] = light;
	        lightmap.is[2] = light;
	        lightmap.is[3] = light;

            this.renderQuad(world, state, pos, vertexConsumer, matrices, bakedQuad, lightmap, overlay);
        }
    }

	private void renderQuadsSmooth(BlockRenderView world, BlockState state, BlockPos pos,
	                               MatrixStack matrices, VertexConsumer vertexConsumer,
	                               List<BakedQuad> quads, AOProcessor ao,
	                               int overlay)
	{
		//System.out.printf("renderQuad(): pos [%s] / state [%s] / quad size [%d]\n", pos.toShortString(), state, size);
		for (BakedQuad bakedQuad : quads)
		{
			this.getQuadDimensions(world, state, pos, bakedQuad, ao);
			ao.apply(world, state, pos, bakedQuad.face(), bakedQuad.shade());
			//System.out.printf("renderQuad(): pos [%s] / state [%s] / quad face [%s]\n", pos.toShortString(), state, bakedQuad.getFace().getName());
			this.renderQuad(world, state, pos, vertexConsumer, matrices, bakedQuad, ao, overlay);
		}
	}

	private void renderQuad(BlockRenderView world, BlockState state, BlockPos pos,
                            VertexConsumer vertexConsumer, MatrixStack matrices,
                            BakedQuad quad,
                            AOLightmap lightmap,
                            int overlay)
    {
		int tint = quad.tintIndex();
        float r;
        float g;
        float b;
	    float a;

        if (quad.hasTint())
        {
            int color;

			if (lightmap.lastTintIndex == tint)
			{
				color = lightmap.colorOfLastTintIndex;
			}
			else
			{
				color = this.colorMap.getColor(state, world, pos, tint);
				lightmap.lastTintIndex = tint;
				lightmap.colorOfLastTintIndex = color;
			}

            r = (float) (color >> 16 & 0xFF) / 255.0F;
            g = (float) (color >> 8 & 0xFF) / 255.0F;
            b = (float) (color & 0xFF) / 255.0F;
        }
        else
        {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
        }

	    a = 1.0f;

        //System.out.printf("quad(): pos [%s] / state [%s] --> SPRITE [%s]\n", pos.toShortString(), state, quad.getSprite().toString());
        vertexConsumer.quad(matrices.peek(), quad, lightmap.fs, r, g, b, a, lightmap.is, overlay);
    }

    private void getQuadDimensions(BlockRenderView world, BlockState state, BlockPos pos,
                                   BakedQuad quad,
                                   AOLightmap lightmap)
    {
        float minX = 32.0F;
        float minY = 32.0F;
        float minZ = 32.0F;
        float maxX = -32.0F;
        float maxY = -32.0F;
        float maxZ = -32.0F;

        for (int index = 0; index < 4; index++)
        {
	        Vector3fc v3fc = quad.getPosition(index);
            float x = v3fc.x();
            float y = v3fc.y();
            float z = v3fc.z();

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        if (lightmap instanceof AOProcessorModern aoModern)
        {
            aoModern.shapeCache[Direction.WEST.getIndex()] = minX;
	        aoModern.shapeCache[Direction.EAST.getIndex()] = maxX;
	        aoModern.shapeCache[Direction.DOWN.getIndex()] = minY;
	        aoModern.shapeCache[Direction.UP.getIndex()] = maxY;
	        aoModern.shapeCache[Direction.NORTH.getIndex()] = minZ;
	        aoModern.shapeCache[Direction.SOUTH.getIndex()] = maxZ;

	        aoModern.shapeCache[Direction.WEST.getIndex() + 6] = 1.0F - minX;
	        aoModern.shapeCache[Direction.EAST.getIndex() + 6] = 1.0F - maxX;
	        aoModern.shapeCache[Direction.DOWN.getIndex() + 6] = 1.0F - minY;
	        aoModern.shapeCache[Direction.UP.getIndex() + 6] = 1.0F - maxY;
	        aoModern.shapeCache[Direction.NORTH.getIndex() + 6] = 1.0F - minZ;
	        aoModern.shapeCache[Direction.SOUTH.getIndex() + 6] = 1.0F - maxZ;
        }
		else if (lightmap instanceof AOProcessorLegacy aoLegacy)
        {
	        aoLegacy.shapeCache[Direction.WEST.getIndex()] = minX;
	        aoLegacy.shapeCache[Direction.EAST.getIndex()] = maxX;
	        aoLegacy.shapeCache[Direction.DOWN.getIndex()] = minY;
	        aoLegacy.shapeCache[Direction.UP.getIndex()] = maxY;
	        aoLegacy.shapeCache[Direction.NORTH.getIndex()] = minZ;
	        aoLegacy.shapeCache[Direction.SOUTH.getIndex()] = maxZ;

	        aoLegacy.shapeCache[Direction.WEST.getIndex() + 6] = 1.0F - minX;
	        aoLegacy.shapeCache[Direction.EAST.getIndex() + 6] = 1.0F - maxX;
	        aoLegacy.shapeCache[Direction.DOWN.getIndex() + 6] = 1.0F - minY;
	        aoLegacy.shapeCache[Direction.UP.getIndex() + 6] = 1.0F - maxY;
	        aoLegacy.shapeCache[Direction.NORTH.getIndex() + 6] = 1.0F - minZ;
	        aoLegacy.shapeCache[Direction.SOUTH.getIndex() + 6] = 1.0F - maxZ;
        }

        float min = 1.0E-4F;
        float max = 0.9999F;

	    lightmap.hasNeighbors = switch (quad.face())
	    {
		    case DOWN, UP -> minX >= min || minZ >= min || maxX <= max || maxZ <= max;
		    case NORTH, SOUTH -> minX >= min || minY >= min || maxX <= max || maxY <= max;
		    case WEST, EAST -> minY >= min || minZ >= min || maxY <= max || maxZ <= max;
	    };

	    lightmap.hasOffset = switch (quad.face())
	    {
		    case DOWN -> minY == maxY && (minY < min || state.isFullCube(world, pos));
		    case UP -> minY == maxY && (maxY > max || state.isFullCube(world, pos));
		    case NORTH -> minZ == maxZ && (minZ < min || state.isFullCube(world, pos));
		    case SOUTH -> minZ == maxZ && (maxZ > max || state.isFullCube(world, pos));
		    case WEST -> minX == maxX && (minX < min || state.isFullCube(world, pos));
		    case EAST -> minX == maxX && (maxX > max || state.isFullCube(world, pos));
	    };
    }

	public void renderQuadsState(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
	                             BlockState state,
	                             float[] rgb, int light, int overlay)
	{
		if (rgb.length < 3)
		{
			rgb = new float[]{1.0f, 1.0f, 1.0f};
		}

		this.renderQuadsModel(entry, vertexConsumer,
		                      this.bakedManager.getBlockModels().getModel(state),
		                      rgb, light, overlay, state);
	}

	public void renderQuadsModel(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
	                             BlockStateModel model,
	                             float[] rgb, int light, int overlay,
	                             @Nullable BlockState fallbackState)
	{
		List<BlockModelPart> parts = model.getParts(this.random);
		if (rgb.length < 3)
		{
			rgb = new float[]{1.0f, 1.0f, 1.0f};
		}

		if (parts.isEmpty() && fallbackState != null)
		{
			BlockState state = LitematicaRenderer.getInstance().getWorldRenderer().getFallbackState(fallbackState);
			model = this.bakedManager.getBlockModels().getModel(state);
			parts = model.getParts(this.random);
		}

		// Because of other mods.
		if (parts.isEmpty())
		{
			return;
		}

		for (BlockModelPart part : parts)
		{
			this.renderQuadsPart(entry, vertexConsumer, part, rgb, light, overlay);
		}
	}

	public void renderQuadsPart(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
	                            BlockModelPart part,
	                            float[] rgb, int light, int overlay)
	{
		if (rgb.length < 3)
		{
			rgb = new float[]{1.0f, 1.0f, 1.0f};
		}

		for (Direction side : PositionUtils.ALL_DIRECTIONS)
		{
			this.renderQuads(entry, vertexConsumer, part.getQuads(side), rgb, light, overlay);
		}

		this.renderQuads(entry, vertexConsumer, part.getQuads(null), rgb, light, overlay);
	}

	public void renderQuads(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
	                        List<BakedQuad> quads,
	                        float[] rgb, int light, int overlay)
	{
		if (rgb.length < 3)
		{
			rgb = new float[]{1.0f, 1.0f, 1.0f};
		}

		for (BakedQuad quad : quads)
		{
			float red = 1.0f;
			float green = 1.0f;
			float blue = 1.0f;
			float alpha = 1.0f;

			if (quad.hasTint())
			{
				red = MathUtils.clamp(rgb[0], 0.0f, 1.0f);
				green = MathUtils.clamp(rgb[1], 0.0f, 1.0f);
				blue = MathUtils.clamp(rgb[2], 0.0f, 1.0f);
			}

			vertexConsumer.quad(entry, quad, red, green, blue, alpha, light, overlay);
		}
	}

    @ApiStatus.Experimental
    public void renderLiquid(VertexConsumer consumer, BlockRenderView world, BlockPos pos, BlockState stateIn, FluidState fluid)
    {
        try
        {
            this.liquidRenderer.render(world, pos, consumer, stateIn, fluid);
        }
        catch (Throwable var9)
        {
            CrashReport crashReport = CrashReport.create(var9, "Tesselating liquid in world");
            CrashReportSection crashReportSection = crashReport.addElement("Block being tesselated");
            CrashReportSection.addBlockInfo(crashReportSection, world, pos, stateIn);
            throw new CrashException(crashReport);
        }
    }

    public boolean renderBlockState(MatrixStack matrices, VertexConsumerProvider consumer, BlockState stateIn, int light, int overlay)
    {
        BlockRenderType blockRenderType = stateIn.getRenderType();

        if (blockRenderType == BlockRenderType.INVISIBLE)
        {
            return false;
        }

        BlockStateModel model = this.bakedManager.getBlockModels().getModel(stateIn);
        int i = this.colorMap.getColor(stateIn, null, null, 0);
        float red = (float) (i >> 16 & 0xFF) / 255.0f;
        float green = (float) (i >> 8 & 0xFF) / 255.0f;
        float blue = (float) (i & 0xFF) / 255.0f;

        this.renderQuadsModel(matrices.peek(),
                              consumer.getBuffer(BlockRenderLayers.getEntityBlockLayer(stateIn)),
                              model,
                              new float[]{red, green, blue},
                              light, overlay,
                              stateIn);
        return true;
    }
}
