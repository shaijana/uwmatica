package fi.dy.masa.litematica.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult.BlockMismatchInfo;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchRenderPos;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverlayRenderer {
    private static final OverlayRenderer INSTANCE = new OverlayRenderer();

    // https://stackoverflow.com/questions/470690/how-to-automatically-generate-n-distinct-colors
    public static final int[] KELLY_COLORS = {
            0xFFB300,    // Vivid Yellow
            0x803E75,    // Strong Purple
            0xFF6800,    // Vivid Orange
            0xA6BDD7,    // Very Light Blue
            0xC10020,    // Vivid Red
            0xCEA262,    // Grayish Yellow
            0x817066,    // Medium Gray
            // The following don't work well for people with defective color vision
            0x007D34,    // Vivid Green
            0xF6768E,    // Strong Purplish Pink
            0x00538A,    // Strong Blue
            0xFF7A5C,    // Strong Yellowish Pink
            0x53377A,    // Strong Violet
            0xFF8E00,    // Vivid Orange Yellow
            0xB32851,    // Strong Purplish Red
            0xF4C800,    // Vivid Greenish Yellow
            0x7F180D,    // Strong Reddish Brown
            0x93AA00,    // Vivid Yellowish Green
            0x593315,    // Deep Yellowish Brown
            0xF13A13,    // Vivid Reddish Orange
            0x232C16     // Dark Olive Green
    };

    private final MinecraftClient mc;
    private final Map<SchematicPlacement, ImmutableMap<String, Box>> placements = new HashMap<>();
    private Color4f colorPos1 = new Color4f(1f, 0.0625f, 0.0625f);
    private Color4f colorPos2 = new Color4f(0.0625f, 0.0625f, 1f);
    private Color4f colorOverlapping = new Color4f(1f, 0.0625f, 1f);
    private Color4f colorX = new Color4f(1f, 0.25f, 0.25f);
    private Color4f colorY = new Color4f(0.25f, 1f, 0.25f);
    private Color4f colorZ = new Color4f(0.25f, 0.25f, 1f);
    private Color4f colorArea = new Color4f(1f, 1f, 1f);
    private Color4f colorBoxPlacementSelected = new Color4f(0x16 / 255f, 1f, 1f);
    private Color4f colorSelectedCorner = new Color4f(0f, 1f, 1f);
    private Color4f colorAreaOrigin = new Color4f(1f, 0x90 / 255f, 0x10 / 255f);

    private long infoUpdateTime;
    private List<String> blockInfoLines = new ArrayList<>();
    private int blockInfoX;
    private int blockInfoY;

    private OverlayRenderer() {
        this.mc = MinecraftClient.getInstance();
    }

    public static OverlayRenderer getInstance() {
        return INSTANCE;
    }

    public void updatePlacementCache() {
        this.placements.clear();
        final List<SchematicPlacement> list = DataManager.getSchematicPlacementManager().getAllSchematicsPlacements();

        for (final SchematicPlacement placement : list) {
            if (placement.isEnabled()) {
                this.placements.put(placement, placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED));
            }
        }
    }

    public void renderBoxes(final MatrixStack matrices) {
//SH        SelectionManager sm = DataManager.getSelectionManager();
//SH        AreaSelection currentSelection = sm.getCurrentSelection();
//SH        boolean renderAreas = currentSelection != null && Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING.getBooleanValue();
//SH        boolean renderPlacements = this.placements.isEmpty() == false && Configs.Visuals.ENABLE_PLACEMENT_BOXES_RENDERING.getBooleanValue();
//SH        boolean isProjectMode = DataManager.getSchematicProjectsManager().hasProjectOpen();
        final float expand = 0.001f;
        final float lineWidthBlockBox = 2f;
//SH        float lineWidthArea = isProjectMode ? 3f : 1.5f;

/*SH        if (renderAreas || renderPlacements || isProjectMode)
        {
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableTexture();

            if (renderAreas)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-1.2f, -0.2f);

                Box currentBox = currentSelection.getSelectedSubRegionBox();

                for (Box box : currentSelection.getAllSubRegionBoxes())
                {
                    BoxType type = box == currentBox ? BoxType.AREA_SELECTED : BoxType.AREA_UNSELECTED;
                    this.renderSelectionBox(box, type, expand, lineWidthBlockBox, lineWidthArea, null, matrices);
                }

                BlockPos origin = currentSelection.getExplicitOrigin();

                if (origin != null)
                {
                    if (currentSelection.isOriginSelected())
                    {
                        Color4f colorTmp = Color4f.fromColor(this.colorAreaOrigin, 0.4f);
                        RenderUtils.renderAreaSides(origin, origin, colorTmp, matrices, this.mc);
                    }

                    Color4f color = currentSelection.isOriginSelected() ? this.colorSelectedCorner : this.colorAreaOrigin;
                    RenderUtils.renderBlockOutline(origin, expand, lineWidthBlockBox, color, this.mc);
                }

                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            if (renderPlacements)
            {
                SchematicPlacementManager spm = DataManager.getSchematicPlacementManager();
                SchematicPlacement currentPlacement = spm.getSelectedSchematicPlacement();

                for (Map.Entry<SchematicPlacement, ImmutableMap<String, Box>> entry : this.placements.entrySet())
                {
                    SchematicPlacement schematicPlacement = entry.getKey();
                    ImmutableMap<String, Box> boxMap = entry.getValue();
                    boolean origin = schematicPlacement.getSelectedSubRegionPlacement() == null;

                    for (Map.Entry<String, Box> entryBox : boxMap.entrySet())
                    {
                        String boxName = entryBox.getKey();
                        boolean boxSelected = schematicPlacement == currentPlacement && (origin || boxName.equals(schematicPlacement.getSelectedSubRegionName()));
                        BoxType type = boxSelected ? BoxType.PLACEMENT_SELECTED : BoxType.PLACEMENT_UNSELECTED;
                        this.renderSelectionBox(entryBox.getValue(), type, expand, 1f, 1f, schematicPlacement, matrices);
                    }

                    Color4f color = schematicPlacement == currentPlacement && origin ? this.colorSelectedCorner : schematicPlacement.getBoxesBBColor();
                    RenderUtils.renderBlockOutline(schematicPlacement.getOrigin(), expand, lineWidthBlockBox, color, this.mc);

                    if (Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX.getBooleanValue())
                    {
                        Box box = schematicPlacement.getEclosingBox();

                        if (schematicPlacement.shouldRenderEnclosingBox() && box != null)
                        {
                            RenderUtils.renderAreaOutline(box.getPos1(), box.getPos2(), 1f, color, color, color, this.mc);

                            if (Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX_SIDES.getBooleanValue())
                            {
                                float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
                                color = new Color4f(color.r, color.g, color.b, alpha);
                                RenderUtils.renderAreaSides(box.getPos1(), box.getPos2(), color, matrices, this.mc);
                            }
                        }
                    }
                }
            }

            if (isProjectMode)
            {
                SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

                if (project != null)
                {
                    RenderUtils.renderBlockOutline(project.getOrigin(), expand, 4f, this.colorOverlapping, this.mc);
                }
            }

            RenderSystem.enableTexture();
            RenderSystem.depthMask(true);
        }*/
    }

    public void renderSelectionBox(final Box box, final BoxType boxType, final float expand,
                                   final float lineWidthBlockBox, final float lineWidthArea, @Nullable final SchematicPlacement placement, final MatrixStack matrices) {
        final BlockPos pos1 = box.getPos1();
        final BlockPos pos2 = box.getPos2();

        if (pos1 == null && pos2 == null) {
            return;
        }

        final Color4f color1;
        final Color4f color2;
        final Color4f colorX;
        final Color4f colorY;
        final Color4f colorZ;

        switch (boxType) {
            case AREA_SELECTED:
                colorX = this.colorX;
                colorY = this.colorY;
                colorZ = this.colorZ;
                break;
            case AREA_UNSELECTED:
                colorX = this.colorArea;
                colorY = this.colorArea;
                colorZ = this.colorArea;
                break;
            case PLACEMENT_SELECTED:
                colorX = this.colorBoxPlacementSelected;
                colorY = this.colorBoxPlacementSelected;
                colorZ = this.colorBoxPlacementSelected;
                break;
            case PLACEMENT_UNSELECTED:
                final Color4f color = placement.getBoxesBBColor();
                colorX = color;
                colorY = color;
                colorZ = color;
                break;
            default:
                return;
        }

        Color4f sideColor;

        if (boxType == BoxType.PLACEMENT_SELECTED) {
            color1 = this.colorBoxPlacementSelected;
            color2 = color1;
//SH            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
//SH            sideColor = new Color4f(color1.r, color1.g, color1.b, alpha);
        } else if (boxType == BoxType.PLACEMENT_UNSELECTED) {
            color1 = placement.getBoxesBBColor();
            color2 = color1;
//SH            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
//SH            sideColor = new Color4f(color1.r, color1.g, color1.b, alpha);
        } else {
            color1 = box.getSelectedCorner() == Corner.CORNER_1 ? this.colorSelectedCorner : this.colorPos1;
            color2 = box.getSelectedCorner() == Corner.CORNER_2 ? this.colorSelectedCorner : this.colorPos2;
//SH            sideColor = Color4f.fromColor(Configs.Colors.AREA_SELECTION_BOX_SIDE_COLOR.getIntegerValue());
        }

        if (pos1 != null && pos2 != null) {
            if (pos1.equals(pos2) == false) {
                RenderUtils.renderAreaOutlineNoCorners(pos1, pos2, lineWidthArea, colorX, colorY, colorZ, this.mc);

/*SH                if (((boxType == BoxType.AREA_SELECTED || boxType == BoxType.AREA_UNSELECTED) &&
                      Configs.Visuals.RENDER_AREA_SELECTION_BOX_SIDES.getBooleanValue())
                    ||
                     ((boxType == BoxType.PLACEMENT_SELECTED || boxType == BoxType.PLACEMENT_UNSELECTED) &&
                       Configs.Visuals.RENDER_PLACEMENT_BOX_SIDES.getBooleanValue()))
                {
                    RenderUtils.renderAreaSides(pos1, pos2, sideColor, matrices, this.mc);
                }*/

                if (box.getSelectedCorner() == Corner.CORNER_1) {
                    final Color4f color = Color4f.fromColor(this.colorPos1, 0.4f);
                    RenderUtils.renderAreaSides(pos1, pos1, color, matrices, this.mc);
                } else if (box.getSelectedCorner() == Corner.CORNER_2) {
                    final Color4f color = Color4f.fromColor(this.colorPos2, 0.4f);
                    RenderUtils.renderAreaSides(pos2, pos2, color, matrices, this.mc);
                }

                RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, this.mc);
                RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, this.mc);
            } else {
                RenderUtils.renderBlockOutlineOverlapping(pos1, expand, lineWidthBlockBox, color1, color2, this.colorOverlapping, matrices, this.mc);
            }
        } else {
            if (pos1 != null) {
                RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, this.mc);
            }

            if (pos2 != null) {
                RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, this.mc);
            }
        }
    }

    public void renderSchematicVerifierMismatches(final MatrixStack matrices) {
        final SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null && placement.hasVerifier()) {
            final SchematicVerifier verifier = placement.getSchematicVerifier();

            final List<MismatchRenderPos> list = verifier.getSelectedMismatchPositionsForRender();

            if (list.isEmpty() == false) {
                final Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                final List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
                final BlockHitResult trace = RayTraceUtils.traceToPositions(posList, entity, 128);
                final BlockPos posLook = trace != null && trace.getType() == HitResult.Type.BLOCK ? trace.getBlockPos() : null;
                this.renderSchematicMismatches(list, posLook, matrices);
            }
        }
    }

    private void renderSchematicMismatches(final List<MismatchRenderPos> posList, @Nullable final BlockPos lookPos, final MatrixStack matrices) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.lineWidth(2f);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        RenderUtils.startDrawingLines(buffer);
        MismatchRenderPos lookedEntry = null;
        MismatchRenderPos prevEntry = null;
        final boolean connections = Configs.Visuals.RENDER_ERROR_MARKER_CONNECTIONS.getBooleanValue();

        for (final MismatchRenderPos entry : posList) {
            final Color4f color = entry.type.getColor();

            if (entry.pos.equals(lookPos) == false) {
                RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(entry.pos, color, 0.002, buffer, this.mc);
            } else {
                lookedEntry = entry;
            }

            if (connections && prevEntry != null) {
                RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, entry.pos, false, color, buffer, this.mc);
            }

            prevEntry = entry;
        }

        if (lookedEntry != null) {
            if (connections && prevEntry != null) {
                RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, lookedEntry.pos, false, lookedEntry.type.getColor(), buffer, this.mc);
            }

            tessellator.draw();
            RenderUtils.startDrawingLines(buffer);

            RenderSystem.lineWidth(6f);
            RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(lookPos, lookedEntry.type.getColor(), 0.002, buffer, this.mc);
        }

        tessellator.draw();

        if (Configs.Visuals.RENDER_ERROR_MARKER_SIDES.getBooleanValue()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();

            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            final float alpha = (float) Configs.InfoOverlays.VERIFIER_ERROR_HILIGHT_ALPHA.getDoubleValue();

            for (final MismatchRenderPos entry : posList) {
                Color4f color = entry.type.getColor();
                color = new Color4f(color.r, color.g, color.b, alpha);
                RenderUtils.renderAreaSidesBatched(entry.pos, entry.pos, color, 0.002, buffer, this.mc);
            }

            tessellator.draw();

            RenderSystem.disableBlend();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    public void renderHoverInfo(final MinecraftClient mc, final MatrixStack matrixStack) {
        if (mc.world != null && mc.player != null) {
            final boolean infoOverlayKeyActive = Hotkeys.RENDER_INFO_OVERLAY.getKeybind().isKeybindHeld();
            boolean verifierOverlayRendered = false;

            if (infoOverlayKeyActive && Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED.getBooleanValue()) {
                verifierOverlayRendered = this.renderVerifierOverlay(mc, matrixStack);
            }

            final boolean renderBlockInfoLines = Configs.InfoOverlays.BLOCK_INFO_LINES_ENABLED.getBooleanValue();
            final boolean renderBlockInfoOverlay = verifierOverlayRendered == false && infoOverlayKeyActive && Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ENABLED.getBooleanValue();
            RayTraceWrapper traceWrapper = null;

            if (renderBlockInfoLines || renderBlockInfoOverlay) {
                final Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                final boolean targetFluids = Configs.InfoOverlays.INFO_OVERLAYS_TARGET_FLUIDS.getBooleanValue();
                traceWrapper = RayTraceUtils.getGenericTrace(mc.world, entity, 10, true, targetFluids, false);
            }

            if (traceWrapper != null &&
                    (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK ||
                            traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)) {
                if (renderBlockInfoLines) {
                    this.renderBlockInfoLines(traceWrapper, mc, matrixStack);
                }

                if (renderBlockInfoOverlay) {
                    this.renderBlockInfoOverlay(traceWrapper, mc, matrixStack);
                }
            }
        }
    }

    private void renderBlockInfoLines(final RayTraceWrapper traceWrapper, final MinecraftClient mc, final MatrixStack matrixStack) {
        final long currentTime = System.currentTimeMillis();

        // Only update the text once per game tick
        if (currentTime - this.infoUpdateTime >= 50) {
            this.updateBlockInfoLines(traceWrapper, mc);
            this.infoUpdateTime = currentTime;
        }

        final int x = Configs.InfoOverlays.BLOCK_INFO_LINES_OFFSET_X.getIntegerValue();
        final int y = Configs.InfoOverlays.BLOCK_INFO_LINES_OFFSET_Y.getIntegerValue();
        final double fontScale = Configs.InfoOverlays.BLOCK_INFO_LINES_FONT_SCALE.getDoubleValue();
        final int textColor = 0xFFFFFFFF;
        final int bgColor = 0xA0505050;
        final HudAlignment alignment = (HudAlignment) Configs.InfoOverlays.BLOCK_INFO_LINES_ALIGNMENT.getOptionListValue();
        final boolean useBackground = true;
        final boolean useShadow = false;

        fi.dy.masa.malilib.render.RenderUtils.renderText(x, y, fontScale, textColor, bgColor, alignment, useBackground, useShadow, this.blockInfoLines, matrixStack);
    }

    private boolean renderVerifierOverlay(final MinecraftClient mc, final MatrixStack matrixStack) {
        final SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null && placement.hasVerifier()) {
            final Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            final SchematicVerifier verifier = placement.getSchematicVerifier();
            final List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
            final BlockHitResult trace = RayTraceUtils.traceToPositions(posList, entity, 128);

            if (trace != null && trace.getType() == HitResult.Type.BLOCK) {
                final BlockMismatch mismatch = verifier.getMismatchForPosition(trace.getBlockPos());
                final World worldSchematic = SchematicWorldHandler.getSchematicWorld();

                if (mismatch != null && worldSchematic != null) {
                    final BlockMismatchInfo info = new BlockMismatchInfo(mismatch.stateExpected, mismatch.stateFound);
                    final BlockInfoAlignment align = (BlockInfoAlignment) Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getOptionListValue();
                    final int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
                    final BlockPos pos = trace.getBlockPos();
                    final int invHeight = RenderUtils.renderInventoryOverlays(align, offY, worldSchematic, mc.world, pos, mc);
                    this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
                    info.render(this.blockInfoX, this.blockInfoY, mc, matrixStack);
                    return true;
                }
            }
        }

        return false;
    }

    private void renderBlockInfoOverlay(final RayTraceWrapper traceWrapper, final MinecraftClient mc, final MatrixStack matrixStack) {
        final BlockState air = Blocks.AIR.getDefaultState();
        final World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        final World worldClient = WorldUtils.getBestWorld(mc);
        final BlockPos pos = traceWrapper.getBlockHitResult().getBlockPos();

        final BlockState stateClient = mc.world.getBlockState(pos);
        final BlockState stateSchematic = worldSchematic.getBlockState(pos);
        final boolean hasInvClient = InventoryUtils.getInventory(worldClient, pos) != null;
        final boolean hasInvSchematic = InventoryUtils.getInventory(worldSchematic, pos) != null;
        int invHeight = 0;

        final int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
        final BlockInfoAlignment align = (BlockInfoAlignment) Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getOptionListValue();

        ItemUtils.setItemForBlock(worldSchematic, pos, stateSchematic);
        ItemUtils.setItemForBlock(mc.world, pos, stateClient);

        if (hasInvClient && hasInvSchematic) {
            invHeight = RenderUtils.renderInventoryOverlays(align, offY, worldSchematic, worldClient, pos, mc);
        } else if (hasInvClient) {
            invHeight = RenderUtils.renderInventoryOverlay(align, LeftRight.RIGHT, offY, worldClient, pos, mc);
        } else if (hasInvSchematic) {
            invHeight = RenderUtils.renderInventoryOverlay(align, LeftRight.LEFT, offY, worldSchematic, pos, mc);
        }

        // Not just a missing block
        if (stateSchematic != stateClient && stateClient != air && stateSchematic != air) {
            final BlockMismatchInfo info = new BlockMismatchInfo(stateSchematic, stateClient);
            this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
            info.render(this.blockInfoX, this.blockInfoY, mc, matrixStack);
        } else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK) {
            final BlockInfo info = new BlockInfo(stateClient, "litematica.gui.label.block_info.state_client");
            this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
            info.render(this.blockInfoX, this.blockInfoY, mc, matrixStack);
        } else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
            final BlockInfo info = new BlockInfo(stateSchematic, "litematica.gui.label.block_info.state_schematic");
            this.getOverlayPosition(align, info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
            info.render(this.blockInfoX, this.blockInfoY, mc, matrixStack);
        }
    }

    protected void getOverlayPosition(final BlockInfoAlignment align, final int width, final int height, final int offY, final int invHeight, final MinecraftClient mc) {
        switch (align) {
            case CENTER:
                this.blockInfoX = GuiUtils.getScaledWindowWidth() / 2 - width / 2;
                this.blockInfoY = GuiUtils.getScaledWindowHeight() / 2 + offY;
                break;
            case TOP_CENTER:
                this.blockInfoX = GuiUtils.getScaledWindowWidth() / 2 - width / 2;
                this.blockInfoY = invHeight + offY + (invHeight > 0 ? offY : 0);
                break;
        }
    }

    private void updateBlockInfoLines(final RayTraceWrapper traceWrapper, final MinecraftClient mc) {
        this.blockInfoLines.clear();

        final BlockPos pos = traceWrapper.getBlockHitResult().getBlockPos();
        final BlockState stateClient = mc.world.getBlockState(pos);

        final World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        final BlockState stateSchematic = worldSchematic.getBlockState(pos);
        final String ul = GuiBase.TXT_UNDERLINE;

        if (stateSchematic != stateClient && stateClient.isAir() == false && stateSchematic.isAir() == false) {
            this.blockInfoLines.add(ul + "Schematic:");
            this.addBlockInfoLines(stateSchematic);

            this.blockInfoLines.add("");
            this.blockInfoLines.add(ul + "Client:");
            this.addBlockInfoLines(stateClient);
        } else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
            this.blockInfoLines.add(ul + "Schematic:");
            this.addBlockInfoLines(stateSchematic);
        }
    }

    private void addBlockInfoLines(final BlockState state) {
        this.blockInfoLines.add(String.valueOf(Registry.BLOCK.getId(state.getBlock())));
        this.blockInfoLines.addAll(BlockUtils.getFormattedBlockStateProperties(state));
    }

    public void renderSchematicRebuildTargetingOverlay(final MatrixStack matrixStack) {
        final RayTraceWrapper traceWrapper = null;
        final Color4f color = null;
        final boolean direction = false;
        final Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();

/*SH        if (Hotkeys.SCHEMATIC_EDIT_BREAK_ALL.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_BREAK_ALL_EXCEPT.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_BREAK_EXCEPT_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_BREAK_DIRECTION.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY_COLOR.getColor();
            direction = true;
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_ALL.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_BLOCK.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_DIRECTION.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
            direction = true;
        }*/

        if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK) {
            final BlockHitResult trace = traceWrapper.getBlockHitResult();
            final BlockPos pos = trace.getBlockPos();

            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableTexture();
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-0.8f, -1.8f);

            if (direction) {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlay(
                        entity, pos, trace.getSide(), trace.getPos(), color, matrixStack, this.mc);
            } else {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlaySimple(
                        entity, pos, trace.getSide(), color, matrixStack, this.mc);
            }

            RenderSystem.disablePolygonOffset();
            RenderSystem.enableTexture();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
        }
    }

    public void renderPreviewFrame(final MinecraftClient mc) {
        final int width = GuiUtils.getScaledWindowWidth();
        final int height = GuiUtils.getScaledWindowHeight();
        final int x = width >= height ? (width - height) / 2 : 0;
        final int y = height >= width ? (height - width) / 2 : 0;
        final int longerSide = Math.min(width, height);

        fi.dy.masa.malilib.render.RenderUtils.drawOutline(x, y, longerSide, longerSide, 2, 0xFFFFFFFF);
    }

    private enum BoxType {
        AREA_SELECTED,
        AREA_UNSELECTED,
        PLACEMENT_SELECTED,
        PLACEMENT_UNSELECTED
    }
}
