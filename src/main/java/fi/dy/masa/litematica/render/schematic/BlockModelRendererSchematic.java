package fi.dy.masa.litematica.render.schematic;

import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.BitRandomSource;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.schematic.ao.AOProcessor;
import fi.dy.masa.litematica.render.schematic.ao.AOProcessorModern;

public class BlockModelRendererSchematic
{
	public static final ThreadLocal<AOProcessorModern.BC> CACHE = ThreadLocal.withInitial(AOProcessorModern.BC::new);
    private final SingleThreadedRandomSource random;
    private final BlockColors colorMap;
    private final LiquidBlockRenderer liquidRenderer;
    private ModelManager bakedManager;

    public BlockModelRendererSchematic(BlockColors blockColorsIn)
    {
		this.random = new SingleThreadedRandomSource(0);
        this.colorMap = blockColorsIn;
        this.liquidRenderer = new LiquidBlockRenderer();
    }

    public void setBakedManager(ModelManager manager)
    {
        this.bakedManager = manager;
    }

    public static void enableCache()
    {
        if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
        {
            CACHE.get().enable();
        }
    }

    public static void disableCache()
    {
        if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
        {
            CACHE.get().disable();
        }
    }

    public boolean renderModel(BlockAndTintGetter worldIn, List<BlockModelPart> modelParts, BlockState stateIn,
                               BlockPos posIn, PoseStack matrixStack,
                               VertexConsumer vertexConsumer, long rand)
    {
        boolean ao = Minecraft.useAmbientOcclusion() &&
		        stateIn.getLightEmission() == 0 &&
		        (!modelParts.isEmpty() && modelParts.getFirst().useAmbientOcclusion());

        Vec3 offset = stateIn.getOffset(posIn);
        matrixStack.translate((float) offset.x, (float) offset.y, (float) offset.z);
        int overlay = OverlayTexture.NO_OVERLAY;

        try
        {
            if (ao)
            {
//                System.out.printf("renderModelSmooth(): pos [%s] / state [%s] / parts? [%d]\n", posIn.toShortString(), stateIn, modelParts.size());
                return this.renderModelSmooth(worldIn, modelParts, stateIn, posIn, matrixStack, vertexConsumer, this.random, rand, overlay);
            }
            else
            {
//                System.out.printf("renderModelFlat(): pos [%s] / state [%s] / parts? [%d]\n", posIn.toShortString(), stateIn, modelParts.size());
                return this.renderModelFlat(worldIn, modelParts, stateIn, posIn, matrixStack, vertexConsumer, this.random, rand, overlay);
            }
        }
        catch (Throwable throwable)
        {
            //Litematica.logger.error("renderModel: Crash caught: [{}]", !throwable.getMessage().isEmpty() ? throwable.getMessage() : "<EMPTY>");
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, worldIn, posIn, stateIn);
            crashreportcategory.setDetail("Using AO", ao);
            throw new ReportedException(crashreport);
            //return false;
        }
    }

    public boolean renderModelSmooth(BlockAndTintGetter worldIn, List<BlockModelPart> modelParts, BlockState stateIn, BlockPos posIn, PoseStack matrixStack,
                                      VertexConsumer vertexConsumer, BitRandomSource random, long seedIn, int overlay)
    {
        boolean renderedSomething = false;
        float[] quadBounds = new float[PositionUtils.ALL_DIRECTIONS.length * 2];
        BitSet bitset = new BitSet(3);
        AOProcessor aoFace = AOProcessor.get();
        BlockPos.MutableBlockPos mutablePos = posIn.mutable();

        for (BlockModelPart part : modelParts)
        {
            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
                random.setSeed(seedIn);
                // modelIn.getQuads(stateIn, side, random)
                List<BakedQuad> quads = part.getQuads(side);

                if (!quads.isEmpty())
                {
                    mutablePos.setWithOffset(posIn, side);
                    if (this.shouldRenderModelSide(worldIn, stateIn, posIn, side, mutablePos))
                    {
                        //System.out.printf("renderQuadsSmooth():1: pos [%s] / state [%s]\n", posIn.toShortString(), stateIn);
                        this.renderQuadsSmooth(worldIn, stateIn, posIn, matrixStack, vertexConsumer, quads, quadBounds, bitset, aoFace, overlay);
                        renderedSomething = true;
                    }
                }
            }

            random.setSeed(seedIn);
            // modelIn.getQuads(stateIn, null, random);
            List<BakedQuad> quads = part.getQuads(null);

            if (!quads.isEmpty())
            {
                //System.out.printf("renderQuadsSmooth():2: pos [%s] / state [%s]\n", posIn.toShortString(), stateIn);
                this.renderQuadsSmooth(worldIn, stateIn, posIn, matrixStack, vertexConsumer, quads, quadBounds, bitset, aoFace, overlay);
                renderedSomething = true;
            }
        }

        return renderedSomething;
    }

    public boolean renderModelFlat(BlockAndTintGetter worldIn, List<BlockModelPart> modelParts, BlockState stateIn,
                                    BlockPos posIn, PoseStack matrixStack,
                                    VertexConsumer vertexConsumer, BitRandomSource random, long seedIn, int overlay)
    {
        boolean renderedSomething = false;
        BitSet bitset = new BitSet(3);
        BlockPos.MutableBlockPos mutablePos = posIn.mutable();

        for (BlockModelPart part : modelParts)
        {
            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
                random.setSeed(seedIn);
                // modelIn.getQuads(stateIn, side, random)
                List<BakedQuad> quads = part.getQuads(side);

                if (!quads.isEmpty())
                {
                    mutablePos.setWithOffset(posIn, side);
                    if (this.shouldRenderModelSide(worldIn, stateIn, posIn, side, mutablePos))
                    {
                        //int light = WorldRenderer.getLightmapCoordinates(worldIn, stateIn, posIn.offset(side));
                        int light = LevelRenderer.getLightColor(worldIn, mutablePos);
                        this.renderQuadsFlat(worldIn, stateIn, posIn, light, overlay, false, matrixStack, vertexConsumer, quads, bitset);
                        renderedSomething = true;
                    }
                }
            }

            random.setSeed(seedIn);
            // modelIn.getQuads(stateIn, null, random)
            List<BakedQuad> quads = part.getQuads(null);

            if (!quads.isEmpty())
            {
                this.renderQuadsFlat(worldIn, stateIn, posIn, -1, overlay, true, matrixStack, vertexConsumer, quads, bitset);
                renderedSomething = true;
            }
        }

        return renderedSomething;
    }

    public boolean shouldRenderModelSide(BlockAndTintGetter worldIn, BlockState stateIn, BlockPos posIn, Direction side, BlockPos mutable)
    {
        return DataManager.getRenderLayerRange().isPositionAtRenderEdgeOnSide(posIn, side) ||
                (Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() && Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.getBooleanValue()) ||
                //Block.shouldDrawSide(stateIn, worldIn, posIn, side, posIn.offset(side));
                Block.shouldRenderFace(stateIn, worldIn.getBlockState(mutable), side);
        // TODO --> check
    }

    private void renderQuadsSmooth(BlockAndTintGetter world, BlockState state, BlockPos pos, PoseStack matrixStack,
                                   VertexConsumer vertexConsumer, List<BakedQuad> list, float[] box, BitSet flags, AOProcessor aoCalc, int overlay)
    {
        final int size = list.size();

        //System.out.printf("renderQuad(): pos [%s] / state [%s] / quad size [%d]\n", pos.toShortString(), state, size);

        for (BakedQuad bakedQuad : list)
        {
            this.getQuadDimensions(world, state, pos, bakedQuad.vertices(), bakedQuad.direction(), box, flags);
            aoCalc.apply(world, state, pos, bakedQuad.direction(), box, flags, bakedQuad.shade());

            //System.out.printf("renderQuad(): pos [%s] / state [%s] / quad face [%s]\n", pos.toShortString(), state, bakedQuad.getFace().getName());

            this.renderQuad(world, state, pos, vertexConsumer, matrixStack, bakedQuad, aoCalc.brightness, aoCalc.light, overlay);
        }
    }

    private void renderQuadsFlat(BlockAndTintGetter world, BlockState state, BlockPos pos,
                                 int light, int overlay, boolean useWorldLight, PoseStack matrixStack, VertexConsumer vertexConsumer, List<BakedQuad> list, BitSet flags)
    {
        //final int size = list.size();

        for (BakedQuad bakedQuad : list)
        {
            if (useWorldLight)
            {
                this.getQuadDimensions(world, state, pos, bakedQuad.vertices(), bakedQuad.direction(), null, flags);
                BlockPos blockPos = flags.get(0) ? pos.relative(bakedQuad.direction()) : pos;
                light = LevelRenderer.getLightColor(world, blockPos);
            }

            float b = world.getShade(bakedQuad.direction(), bakedQuad.shade());
            int[] lo = new int[]{light, light, light, light};
            float[] bo = new float[]{b, b, b, b};
            this.renderQuad(world, state, pos, vertexConsumer, matrixStack, bakedQuad, bo, lo, overlay);
        }
    }

    private void renderQuad(BlockAndTintGetter world, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, PoseStack matrixStack,
                            BakedQuad quad, float[] brightness, int[] light, int overlay)
    {
        float r;
        float g;
        float b;

        if (quad.isTinted())
        {
            int color = this.colorMap.getColor(state, world, pos, quad.tintIndex());
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

        //System.out.printf("quad(): pos [%s] / state [%s] --> SPRITE [%s]\n", pos.toShortString(), state, quad.getSprite().toString());
        vertexConsumer.putBulkData(matrixStack.last(), quad, brightness, r, g, b, 1.0f, light, overlay, true);
    }

    private void getQuadDimensions(BlockAndTintGetter world, BlockState state, BlockPos pos, int[] vertexData, Direction face, @Nullable float[] box, BitSet flags)
    {
        float minX = 32.0F;
        float minY = 32.0F;
        float minZ = 32.0F;
        float maxX = -32.0F;
        float maxY = -32.0F;
        float maxZ = -32.0F;
        final int vertexSize = vertexData.length / 4;

        for (int index = 0; index < 4; ++index)
        {
            float x = Float.intBitsToFloat(vertexData[index * vertexSize]);
            float y = Float.intBitsToFloat(vertexData[index * vertexSize + 1]);
            float z = Float.intBitsToFloat(vertexData[index * vertexSize + 2]);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        if (box != null)
        {
            box[Direction.WEST.get3DDataValue()] = minX;
            box[Direction.EAST.get3DDataValue()] = maxX;
            box[Direction.DOWN.get3DDataValue()] = minY;
            box[Direction.UP.get3DDataValue()] = maxY;
            box[Direction.NORTH.get3DDataValue()] = minZ;
            box[Direction.SOUTH.get3DDataValue()] = maxZ;

            box[Direction.WEST.get3DDataValue() + 6] = 1.0F - minX;
            box[Direction.EAST.get3DDataValue() + 6] = 1.0F - maxX;
            box[Direction.DOWN.get3DDataValue() + 6] = 1.0F - minY;
            box[Direction.UP.get3DDataValue() + 6] = 1.0F - maxY;
            box[Direction.NORTH.get3DDataValue() + 6] = 1.0F - minZ;
            box[Direction.SOUTH.get3DDataValue() + 6] = 1.0F - maxZ;
        }

        float min = 1.0E-4F;
        float max = 0.9999F;

        switch (face)
        {
            case DOWN:
                flags.set(1, minX >= min || minZ >= min || maxX <= max || maxZ <= max);
                flags.set(0, minY == maxY && (minY < min || state.isCollisionShapeFullBlock(world, pos)));
                break;
            case UP:
                flags.set(1, minX >= min || minZ >= min || maxX <= max || maxZ <= max);
                flags.set(0, minY == maxY && (maxY > max || state.isCollisionShapeFullBlock(world, pos)));
                break;
            case NORTH:
                flags.set(1, minX >= min || minY >= min || maxX <= max || maxY <= max);
                flags.set(0, minZ == maxZ && (minZ < min || state.isCollisionShapeFullBlock(world, pos)));
                break;
            case SOUTH:
                flags.set(1, minX >= min || minY >= min || maxX <= max || maxY <= max);
                flags.set(0, minZ == maxZ && (maxZ > max || state.isCollisionShapeFullBlock(world, pos)));
                break;
            case WEST:
                flags.set(1, minY >= min || minZ >= min || maxY <= max || maxZ <= max);
                flags.set(0, minX == maxX && (minX < min || state.isCollisionShapeFullBlock(world, pos)));
                break;
            case EAST:
                flags.set(1, minY >= min || minZ >= min || maxY <= max || maxZ <= max);
                flags.set(0, minX == maxX && (maxX > max || state.isCollisionShapeFullBlock(world, pos)));
        }
    }

    /*
    private void fillQuadBounds(BlockRenderView world, BlockState stateIn, BlockPos pos, int[] vertexData, Direction face, @Nullable float[] quadBounds, BitSet boundsFlags)
    {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;

        for (int i = 0; i < 4; ++i)
        {
            float f6 = Float.intBitsToFloat(vertexData[i * 7]);
            float f7 = Float.intBitsToFloat(vertexData[i * 7 + 1]);
            float f8 = Float.intBitsToFloat(vertexData[i * 7 + 2]);
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        if (quadBounds != null)
        {
            quadBounds[Direction.WEST.getIndex()] = f;
            quadBounds[Direction.EAST.getIndex()] = f3;
            quadBounds[Direction.DOWN.getIndex()] = f1;
            quadBounds[Direction.UP.getIndex()] = f4;
            quadBounds[Direction.NORTH.getIndex()] = f2;
            quadBounds[Direction.SOUTH.getIndex()] = f5;
            int j = Direction.values().length;
            quadBounds[Direction.WEST.getIndex() + j] = 1.0F - f;
            quadBounds[Direction.EAST.getIndex() + j] = 1.0F - f3;
            quadBounds[Direction.DOWN.getIndex() + j] = 1.0F - f1;
            quadBounds[Direction.UP.getIndex() + j] = 1.0F - f4;
            quadBounds[Direction.NORTH.getIndex() + j] = 1.0F - f2;
            quadBounds[Direction.SOUTH.getIndex() + j] = 1.0F - f5;
        }

        switch (face)
        {
            case DOWN:
                boundsFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f1 < 1.0E-4F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f1 == f4);
                break;
            case UP:
                boundsFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f4 > 0.9999F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f1 == f4);
                break;
            case NORTH:
                boundsFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                boundsFlags.set(0, (f2 < 1.0E-4F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f2 == f5);
                break;
            case SOUTH:
                boundsFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                boundsFlags.set(0, (f5 > 0.9999F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f2 == f5);
                break;
            case WEST:
                boundsFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f < 1.0E-4F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f == f3);
                break;
            case EAST:
                boundsFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f3 > 0.9999F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f == f3);
        }
    }
    */

    @ApiStatus.Experimental
    public void renderBlockEntity(VertexConsumer vertexConsumer, PoseStack matrixStack, BlockStateModel modelIn,
                             float red, float green, float blue, int light, int overlay)
    {
        List<BlockModelPart> parts = modelIn.collectParts(RandomSource.create(42L));

        for (BlockModelPart part : parts)
        {
            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
                this.renderBlockEntityQuads(vertexConsumer, matrixStack, red, green, blue, part.getQuads(side), light, overlay);
            }

            // modelIn.getQuads(stateIn, null, rand)
            this.renderBlockEntityQuads(vertexConsumer, matrixStack, red, green, blue, part.getQuads(null), light, overlay);
        }
    }

    @ApiStatus.Experimental
    private void renderBlockEntityQuads(VertexConsumer vertexConsumer, PoseStack matrixStack,
                             float red, float green, float blue, List<BakedQuad> quads, int light, int overlay)
    {
        for (BakedQuad quad : quads)
        {
            float h;
            float g;
            float f;

            if (quad.isTinted())
            {
                f = Mth.clamp(red, 0.0f, 1.0f);
                g = Mth.clamp(green, 0.0f, 1.0f);
                h = Mth.clamp(blue, 0.0f, 1.0f);
            }
            else
            {
                h = 1.0F;
                g = 1.0F;
                f = 1.0F;
            }
            vertexConsumer.putBulkData(matrixStack.last(), quad, f, g, h, 1.0f, light, overlay);
        }
    }

    @ApiStatus.Experimental
    public void renderLiquid(VertexConsumer consumer, BlockAndTintGetter world, BlockPos pos, BlockState stateIn, FluidState fluid)
    {
        try
        {
            this.liquidRenderer.tesselate(world, pos, consumer, stateIn, fluid);
        }
        catch (Throwable var9)
        {
            CrashReport crashReport = CrashReport.forThrowable(var9, "Tesselating liquid in world");
            CrashReportCategory crashReportSection = crashReport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashReportSection, world, pos, stateIn);
            throw new ReportedException(crashReport);
        }
    }

    public BlockStateModel getBakedModel(BlockState stateIn)
    {
        return this.bakedManager.getBlockModelShaper().getBlockModel(stateIn);
    }

    @ApiStatus.Experimental
    public boolean renderBlockEntity(MultiBufferSource consumer, PoseStack matrixStack, BlockState stateIn, int light, int overlay)
    {
        RenderShape blockRenderType = stateIn.getRenderShape();

        if (blockRenderType == RenderShape.INVISIBLE)
        {
            return false;
        }

        BlockStateModel bakedModel = this.getBakedModel(stateIn);
        int i = this.colorMap.getColor(stateIn, null, null, 0);
        float red = (float) (i >> 16 & 0xFF) / 255.0f;
        float green = (float) (i >> 8 & 0xFF) / 255.0f;
        float blue = (float) (i & 0xFF) / 255.0f;

        this.renderBlockEntity(consumer.getBuffer(ItemBlockRenderTypes.getRenderType(stateIn)), matrixStack, bakedModel, red, green, blue, light, overlay);
//        this.bakedManager.getBlockEntityModelsSupplier().get()
//                    .render(stateIn.getBlock(), ItemDisplayContext.NONE, matrixStack, consumer, light, overlay);

        return true;
    }
}
