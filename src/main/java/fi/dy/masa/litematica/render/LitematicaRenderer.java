package fi.dy.masa.litematica.render;

import javax.annotation.Nullable;

import fi.dy.masa.litematica.compat.sodium.SodiumCompat;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayerGroup;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.compat.iris.IrisCompat;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.world.WorldSchematic;

public class LitematicaRenderer
{
    private static final LitematicaRenderer INSTANCE = new LitematicaRenderer();

    private MinecraftClient mc;
    private WorldRendererSchematic worldRenderer;
    private Frustum frustum;
    private int frameCount;
    private long finishTimeNano;

    // Moved to ChunkRenderBatchDraw
//    private boolean renderCollidingSchematicBlocks;
    private boolean renderPiecewiseSchematic;
    private boolean renderPiecewiseBlocks;
    private boolean renderPiecewiseEntities;
    private boolean renderPiecewiseTileEntities;

    private LitematicaRenderer()
    {
    }

    public static LitematicaRenderer getInstance()
    {
        return INSTANCE;
    }

    public WorldRendererSchematic getWorldRenderer()
    {
        if (this.worldRenderer == null)
        {
            this.mc = MinecraftClient.getInstance();
            this.worldRenderer = new WorldRendererSchematic(this.mc);
        }

        return this.worldRenderer;
    }

    public WorldRendererSchematic resetWorldRenderer()
    {
        if (this.worldRenderer != null)
        {
            this.worldRenderer.setWorldAndLoadRenderers(null);
            this.worldRenderer = null;
        }

        return this.getWorldRenderer();
    }

    public void loadRenderers(@Nullable Profiler profiler)
    {
        this.getWorldRenderer().loadRenderers(profiler);
    }

    public void onSchematicWorldChanged(@Nullable WorldSchematic worldClient)
    {
        this.getWorldRenderer().setWorldAndLoadRenderers(worldClient);
    }

    private void calculateFinishTime()
    {
        // TODO 1.15+
        long fpsTarget = 60L;

        if (Configs.Generic.RENDER_THREAD_NO_TIMEOUT.getBooleanValue())
        {
            this.finishTimeNano = Long.MAX_VALUE;
        }
        else
        {
            this.finishTimeNano = System.nanoTime() + 1000000000L / fpsTarget / 2L;
        }
    }

    /*
    public void renderSchematicWorld(MatrixStack matrices, Matrix4f matrix, float partialTicks)
    {
        if (this.mc.skipGameRender == false)
        {
            this.mc.getProfiler().push("litematica_schematic_world_render");

            if (this.mc.getCameraEntity() == null)
            {
                this.mc.setCameraEntity(this.mc.player);
            }

            RenderSystem.pushMatrix();
            RenderSystem.enableDepthTest();

            this.calculateFinishTime();
            this.renderWorld(matrices, matrix, partialTicks, this.finishTimeNano);
            this.cleanup();

            RenderSystem.popMatrix();

            this.mc.getProfiler().pop();
        }
    }

    private void renderWorld(MatrixStack matrices, Matrix4f matrix, float partialTicks, long finishTimeNano)
    {
        this.mc.getProfiler().push("culling");

        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        Camera camera = this.getCamera();
        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        Frustum frustum = new Frustum(matrices.peek().getModel(), matrix);
        frustum.setPosition(x, y, z);

        this.mc.getProfiler().swap("prepare_terrain");
        this.mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        fi.dy.masa.malilib.render.RenderUtils.disableDiffuseLighting();
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        this.mc.getProfiler().swap("terrain_setup");
        worldRenderer.setupTerrain(camera, frustum, this.frameCount++, this.mc.player.isSpectator());

        this.mc.getProfiler().swap("update_chunks");
        worldRenderer.updateChunks(finishTimeNano);

        this.mc.getProfiler().swap("terrain");
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.disableAlphaTest();

        if (Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue())
        {
            RenderSystem.pushMatrix();

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.2f, -0.4f);
            }

            this.setupAlphaShader();
            this.enableAlphaShader();

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderBlockLayer(RenderLayer.getSolid(), matrices, camera);
            worldRenderer.renderBlockLayer(RenderLayer.getCutoutMipped(), matrices, camera);
            worldRenderer.renderBlockLayer(RenderLayer.getCutout(), matrices, camera);

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            RenderSystem.disableBlend();
            RenderSystem.shadeModel(GL11.GL_FLAT);
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01F);

            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.popMatrix();

            this.mc.getProfiler().swap("entities");

            RenderSystem.pushMatrix();

            fi.dy.masa.malilib.render.RenderUtils.enableDiffuseLightingForLevel(matrices);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderEntities(camera, frustum, matrices, partialTicks);

            RenderSystem.disableFog(); // Fixes Structure Blocks breaking all rendering
            RenderSystem.disableBlend();
            fi.dy.masa.malilib.render.RenderUtils.disableDiffuseLighting();

            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.popMatrix();

            RenderSystem.enableCull();
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.1F);
            this.mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            RenderSystem.shadeModel(GL11.GL_SMOOTH);

            this.mc.getProfiler().swap("translucent");
            RenderSystem.depthMask(false);

            RenderSystem.pushMatrix();

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderBlockLayer(RenderLayer.getTranslucent(), matrices, camera);

            RenderSystem.popMatrix();

            this.disableAlphaShader();
        }

        this.mc.getProfiler().swap("overlay");
        this.renderSchematicOverlay(matrices);

        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableCull();

        this.mc.getProfiler().pop();
    }
    */

    public void renderSchematicOverlays(Camera camera, Profiler profiler)
    {
        boolean invert = Hotkeys.INVERT_OVERLAY_RENDER_STATE.getKeybind().isKeybindHeld();

        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue() != invert)
        {
            boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();
            float lineWidth = (float) (renderThrough ? Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue() : Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());

            profiler.push("schematic_overlay");

            if (!IrisCompat.isShadowPassActive())
            {
                // this.getCamera()
                this.getWorldRenderer().renderBlockOverlays(camera, lineWidth, profiler);
            }

            profiler.pop();
        }
    }

    public void piecewisePrepareAndUpdate(Frustum frustum, Profiler profiler)
    {
        boolean render = Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() &&
                         Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                         this.mc.getCameraEntity() != null;
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
        this.renderPiecewiseEntities = false;
        this.renderPiecewiseTileEntities = false;
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        if (render && frustum != null && worldRenderer.hasWorld() && this.mc.player != null)
        {
            boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();
            this.renderPiecewiseSchematic = Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert;
            this.renderPiecewiseBlocks = this.renderPiecewiseSchematic && Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue();
//            this.renderCollidingSchematicBlocks = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();
            this.renderPiecewiseEntities = this.renderPiecewiseSchematic && Configs.Visuals.RENDER_SCHEMATIC_ENTITIES.getBooleanValue();
            this.renderPiecewiseTileEntities = this.renderPiecewiseSchematic && Configs.Visuals.RENDER_SCHEMATIC_TILE_ENTITIES.getBooleanValue();

            if (this.renderPiecewiseSchematic)
            {
                profiler.push(Reference.MOD_ID+"_culling");
                this.calculateFinishTime();

                profiler.swap(Reference.MOD_ID+"_terrain_setup");
                worldRenderer.setupTerrain(this.getCamera(), frustum, this.frameCount++, this.mc.player.isSpectator(), profiler);

                profiler.swap(Reference.MOD_ID+"_update_chunks");
                worldRenderer.updateChunks(this.finishTimeNano, profiler);

                profiler.pop();

                this.frustum = frustum;
            }
        }
    }

    public void scheduleTranslucentSorting(Vec3d camera, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push(Reference.MOD_ID + "_schedule_translucent_sorting");
            this.getWorldRenderer().scheduleTranslucentSorting(camera, profiler);
            profiler.pop();
        }
    }

    public void capturePreMainValues(GpuBufferSlice fogBuffer, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push(Reference.MOD_ID+"_pre_main_capture");
            this.getWorldRenderer().capturePreMainValues(fogBuffer, profiler);
            profiler.pop();
        }
    }

//    public void piecewiseRenderSolid(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
//    {
//        if (this.renderPiecewiseBlocks)
//        {
//            profiler.push(Reference.MOD_ID+"_solid");
//
//            this.getWorldRenderer().renderBlockLayer(RenderLayer.getSolid(), this.getCamera(), profiler,
//                                                     this.renderCollidingSchematicBlocks ?
//                                                     MaLiLibPipelines.SOLID_MASA_OFFSET :
//                                                     MaLiLibPipelines.SOLID_MASA);
//
//            profiler.pop();
//        }
//    }
//
//    public void piecewiseRenderCutoutMipped(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
//    {
//        if (this.renderPiecewiseBlocks)
//        {
//            profiler.push(Reference.MOD_ID+"_cutout_mipped");
//
//            this.getWorldRenderer().renderBlockLayer(RenderLayer.getCutoutMipped(), this.getCamera(), profiler,
//                                                     this.renderCollidingSchematicBlocks ?
//                                                     MaLiLibPipelines.CUTOUT_MIPPED_MASA_OFFSET :
//                                                     MaLiLibPipelines.CUTOUT_MIPPED_MASA);
//
//            profiler.pop();
//        }
//    }
//
//    public void piecewiseRenderCutout(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
//    {
//        if (this.renderPiecewiseBlocks)
//        {
//            profiler.push(Reference.MOD_ID+"_cutout");
//
//            this.getWorldRenderer().renderBlockLayer(RenderLayer.getCutout(), this.getCamera(), profiler,
//                                                     this.renderCollidingSchematicBlocks ?
//                                                     MaLiLibPipelines.CUTOUT_MASA_OFFSET :
//                                                     MaLiLibPipelines.CUTOUT_MASA);
//
//            profiler.pop();
//        }
//    }
//
//    public void piecewiseRenderTranslucent(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
//    {
//        if (this.renderPiecewiseBlocks)
//        {
//            profiler.push(Reference.MOD_ID+"_translucent");
//
//            this.getWorldRenderer().renderBlockLayer(RenderLayer.getTranslucent(), this.getCamera(), profiler,
//                                                     this.renderCollidingSchematicBlocks ?
//                                                     MaLiLibPipelines.TRANSLUCENT_MASA_OFFSET :
//                                                     MaLiLibPipelines.TRANSLUCENT_MASA);
//
//            profiler.pop();
//        }
//    }
//
//    public void piecewiseRenderTripwire(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
//    {
//        if (this.renderPiecewiseBlocks)
//        {
//            profiler.push(Reference.MOD_ID+"_tripwire");
//
//            this.getWorldRenderer().renderBlockLayer(RenderLayer.getTripwire(), this.getCamera(), profiler,
//                                                     this.renderCollidingSchematicBlocks ?
//                                                     MaLiLibPipelines.TRIPWIRE_MASA_OFFSET :
//                                                     MaLiLibPipelines.TRIPWIRE_MASA);
//
//            profiler.pop();
//        }
//    }

    public void piecewisePrepareBlockLayers(Matrix4fc matrix4fc, double cameraX, double cameraY, double cameraZ, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push(Reference.MOD_ID + "_prepare_block_layers");
            this.getWorldRenderer().prepareBlockLayers(matrix4fc, cameraX, cameraY, cameraZ, profiler);
            profiler.pop();
        }
    }

    public void piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup group)
    {
        if (this.renderPiecewiseBlocks)
        {
            // Use Saved Profiler later
            this.getWorldRenderer().drawBlockLayerGroup(group);
        }
    }

    public void piecewiseRenderEntities(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, float partialTicks, Profiler profiler)
    {
        if (this.renderPiecewiseEntities)
        {
            profiler.push(Reference.MOD_ID+"_entities");
            this.getWorldRenderer().renderEntities(this.getCamera(), this.frustum, matrices, immediate, partialTicks, profiler);
            profiler.pop();
        }
    }

    public void piecewiseRenderBlockEntities(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, VertexConsumerProvider.Immediate immediate2, float partialTicks, Profiler profiler)
    {
        if (this.renderPiecewiseTileEntities)
        {
            profiler.push(Reference.MOD_ID+"_block_entities");
            this.getWorldRenderer().renderBlockEntities(this.getCamera(), this.frustum, matrices, immediate, immediate2, partialTicks, profiler);
            profiler.pop();
        }
    }

    public void piecewiseRenderOverlay(Matrix4f posMatrix, Matrix4f projMatrix, Profiler profiler)
    {
        if (this.renderPiecewiseSchematic)
        {
            profiler.push(Reference.MOD_ID+"_schematic_overlay");
            this.renderSchematicOverlays(this.getCamera(), profiler);
            profiler.pop();
        }

        this.getWorldRenderer().clearBlockBatchDraw();
        this.cleanup();
    }

    private Camera getCamera()
    {
        return this.mc.gameRenderer.getCamera();
    }

    private void cleanup()
    {
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
        this.renderPiecewiseEntities = false;
        this.renderPiecewiseTileEntities = false;
    }
}
