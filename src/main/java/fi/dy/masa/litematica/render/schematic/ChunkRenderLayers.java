package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import fi.dy.masa.malilib.render.MaLiLibPipelines;

public record ChunkRenderLayers()
{
    public static final List<ChunkSectionLayer> BLOCK_RENDER_LAYERS = getBlockRenderLayers();
    public static final List<RenderType> RENDER_LAYERS = getRenderLayers();
    public static final List<OverlayRenderType> TYPES = getTypes();
    public static final HashMap<ChunkSectionLayer, Pair<RenderPipeline, RenderPipeline>> PIPELINE_MAP = getBlockRenderPipelineMap();

    private static List<ChunkSectionLayer> getBlockRenderLayers()
    {
        List<ChunkSectionLayer> list = new ArrayList<>();

        // I know that there is the BlockRenderLayer.values(), but this is to customize this.
        list.add(ChunkSectionLayer.SOLID);
        list.add(ChunkSectionLayer.CUTOUT);
//        list.add(BlockRenderLayer.CUTOUT_MIPPED);
        list.add(ChunkSectionLayer.TRANSLUCENT);
        list.add(ChunkSectionLayer.TRIPWIRE);

        return list;
    }

    private static HashMap<ChunkSectionLayer, Pair<RenderPipeline, RenderPipeline>> getBlockRenderPipelineMap()
    {
        HashMap<ChunkSectionLayer, Pair<RenderPipeline, RenderPipeline>> map = new HashMap<>();

        // Maps new "BlockRenderLayers" to MasaPipelines.  getLeft = regular; getRight = renderColliding
//        map.put(ChunkSectionLayer.SOLID,         Pair.of(MaLiLibPipelines.SOLID_TERRAIN_MASA,       MaLiLibPipelines.SOLID_TERRAIN_MASA_OFFSET));
//        map.put(ChunkSectionLayer.CUTOUT,        Pair.of(MaLiLibPipelines.CUTOUT_TERRAIN_MASA,      MaLiLibPipelines.CUTOUT_TERRAIN_MASA_OFFSET));
//        map.put(ChunkSectionLayer.TRANSLUCENT,   Pair.of(MaLiLibPipelines.TRANSLUCENT_MASA,         MaLiLibPipelines.TRANSLUCENT_MASA_OFFSET));
//        map.put(ChunkSectionLayer.TRIPWIRE,      Pair.of(MaLiLibPipelines.TRIPWIRE_TERRAIN_MASA,    MaLiLibPipelines.TRIPWIRE_TERRAIN_MASA_OFFSET));

        // todo -- Doesn't really "work" with shaders, but at least you can see colorless blocks; unless `renderColliding` is enabled.
        map.put(ChunkSectionLayer.SOLID,         Pair.of(RenderPipelines.SOLID_TERRAIN,            MaLiLibPipelines.SOLID_TERRAIN_MASA_OFFSET));
        map.put(ChunkSectionLayer.CUTOUT,        Pair.of(RenderPipelines.CUTOUT_TERRAIN,           MaLiLibPipelines.CUTOUT_TERRAIN_MASA_OFFSET));
        map.put(ChunkSectionLayer.TRANSLUCENT,   Pair.of(RenderPipelines.TRANSLUCENT_TERRAIN,      MaLiLibPipelines.TRANSLUCENT_MASA_OFFSET));
        map.put(ChunkSectionLayer.TRIPWIRE,      Pair.of(RenderPipelines.TRIPWIRE_TERRAIN,         MaLiLibPipelines.TRIPWIRE_TERRAIN_MASA_OFFSET));

        return map;
    }

    private static List<RenderType> getRenderLayers()
    {
        List<RenderType> list = new ArrayList<>();

		// Blocks?
	    list.add(RenderTypes.solidMovingBlock());
	    list.add(RenderTypes.cutoutMovingBlock());
	    list.add(RenderTypes.translucentMovingBlock());
	    list.add(RenderTypes.tripwireMovingBlock());
	    list.add(RenderTypes.endPortal());
	    list.add(RenderTypes.endGateway());

        // Water Rendering
        list.add(RenderTypes.waterMask());

        // Experimental
        /*
        list.add(RenderLayer.getSecondaryBlockOutline());
        list.add(RenderLayer.getArmorEntityGlint());
        list.add(RenderLayer.getEntityGlint());
        list.add(TexturedRenderLayers.getArmorTrims(true));
        list.add(TexturedRenderLayers.getArmorTrims(false));
        list.add(TexturedRenderLayers.getBeds());
        list.add(TexturedRenderLayers.getBannerPatterns());
        list.add(TexturedRenderLayers.getChest());
        list.add(TexturedRenderLayers.getEntitySolid());
        list.add(TexturedRenderLayers.getEntityCutout());
        list.add(TexturedRenderLayers.getHangingSign());
        list.add(TexturedRenderLayers.getItemEntityTranslucentCull());
        list.add(TexturedRenderLayers.getShieldPatterns());
        list.add(TexturedRenderLayers.getShulkerBoxes());
        list.add(TexturedRenderLayers.getSign());
         */

        return list;
    }

    private static List<OverlayRenderType> getTypes()
    {
        // In case we need to add additional Types in the future
        return Arrays.stream(OverlayRenderType.values()).toList();
    }

    public static String getFriendlyName(RenderType layer)
    {
        String base = layer.toString();
        String[] results1;

        if (base.contains(":"))
        {
            String[] results2;

            results1 = base.split(":", 2);

            if (results1[0].contains("["))
            {
                results2 = results1[0].split("\\[");

                return layer.mode().name() + "/" + results2[1];
            }

            return layer.mode().name() + "/" + results1[0];
        }

        return layer.mode().name() + "/" + base;
    }
}
