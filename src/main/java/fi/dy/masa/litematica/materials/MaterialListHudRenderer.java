package fi.dy.masa.litematica.materials;

import java.util.Collections;
import java.util.List;
import org.joml.Matrix3x2fStack;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.mixin.screen.IMixinHandledScreen;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.RenderPhase;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

public class MaterialListHudRenderer implements IInfoHudRenderer
{
    protected static BlockState lastLookedAtBlock = Blocks.AIR.getDefaultState();
    protected static ItemStack lastLookedAtBlocksItem = ItemStack.EMPTY;

    protected final MaterialListBase materialList;
    protected final MaterialListSorter sorter;
    protected boolean shouldRender;
    protected long lastUpdateTime;

    public MaterialListHudRenderer(MaterialListBase materialList)
    {
        this.materialList = materialList;
        this.sorter = new MaterialListSorter(materialList);
    }

    @Override
    public boolean getShouldRenderText(RenderPhase phase)
    {
        return false;
    }

    @Override
    public boolean getShouldRenderCustom()
    {
        return this.shouldRender;
    }

    @Override
    public boolean shouldRenderInGuis()
    {
        return Configs.Generic.RENDER_MATERIALS_IN_GUI.getBooleanValue();
    }

    public void toggleShouldRender()
    {
        this.shouldRender = ! this.shouldRender;
    }

    @Override
    public List<String> getText(RenderPhase phase)
    {
        return Collections.emptyList();
    }

    @Override
    public int render(DrawContext drawContext, int xOffset, int yOffset, HudAlignment alignment)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        long currentTime = System.currentTimeMillis();
        List<MaterialListEntry> list;

        if (currentTime - this.lastUpdateTime > 2000)
        {
            MaterialListUtils.updateAvailableCounts(this.materialList.getMaterialsAll(), mc.player);
            list = this.materialList.getMaterialsMissingOnly(true);
            Collections.sort(list, this.sorter);
            this.lastUpdateTime = currentTime;
        }
        else
        {
            list = this.materialList.getMaterialsMissingOnly(false);
        }

        if (list.size() == 0)
        {
            return 0;
        }

        TextRenderer font = mc.textRenderer;
        final double scale = Configs.InfoOverlays.MATERIAL_LIST_HUD_SCALE.getDoubleValue();
        final int maxLines = Configs.InfoOverlays.MATERIAL_LIST_HUD_MAX_LINES.getIntegerValue();
        int bgMargin = 2;
        int lineHeight = 16;
        int contentHeight = (Math.min(list.size(), maxLines) * lineHeight) + bgMargin + 10;
        int maxTextLength = 0;
        int maxCountLength = 0;
        int posX = xOffset + bgMargin;
        int posY = yOffset + bgMargin;
        int bgColor = 0xA0000000;
        int textColor = 0xFFFFFFFF;
        boolean useBackground = true;
        boolean useShadow = false;
        final int size = Math.min(list.size(), maxLines);

        // Only Chuck Norris can divide by zero
        if (scale == 0d)
        {
            return 0;
        }

        for (int i = 0; i < size; ++i)
        {
            MaterialListEntry entry = list.get(i);
            maxTextLength = Math.max(maxTextLength, font.getWidth(entry.getStack().getName().getString()));
            int multiplier = this.materialList.getMultiplier();
            int count = multiplier == 1 ? entry.getCountMissing() - entry.getCountAvailable() : entry.getCountTotal();
            count *= multiplier;
            String strCount = GuiBase.TXT_RED + this.getFormattedCountString(count, entry.getStack().getMaxCount()) + GuiBase.TXT_RST;
            maxCountLength = Math.max(maxCountLength, font.getWidth(strCount));
        }

        final int maxLineLength = maxTextLength + maxCountLength + 30;

        switch (alignment)
        {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                posX = (int) ((GuiUtils.getScaledWindowWidth() / scale) - maxLineLength - xOffset - bgMargin);
                break;
            case CENTER:
                posX = (int) ((GuiUtils.getScaledWindowWidth() / scale / 2) - (maxLineLength / 2) - xOffset);
                break;
            default:
        }

        if (scale != 1 && scale != 0)
        {
            yOffset = (int) (yOffset / scale);
        }

        posY = RenderUtils.getHudPosY(posY, yOffset, contentHeight, scale, alignment);
        posY += RenderUtils.getHudOffsetForPotions(alignment, scale, mc.player);

        Matrix3x2fStack matrixStack = drawContext.getMatrices();

        if (scale != 1d)
        {
            matrixStack.pushMatrix();
            matrixStack.scale((float) scale, (float) scale);
        }

        if (useBackground)
        {
            int x1 = posX - bgMargin;
            int y1 = posY - bgMargin;
            int x2 = x1 + maxLineLength + bgMargin * 2;
            int y2 = y1 + contentHeight + bgMargin;

            drawContext.fill(x1, y1, x2, y2, bgColor);
        }

        int x = posX;
        int y = posY + 12;

        for (int i = 0; i < size; ++i)
        {
            drawContext.drawItem(list.get(i).getStack(), x, y);
            y += lineHeight;
        }

        String title = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.button.material_list") + GuiBase.TXT_RST;

        drawContext.drawText(font, title, posX + 2, posY + 2, textColor, useShadow);

        final int itemCountTextColor = Configs.Colors.MATERIAL_LIST_HUD_ITEM_COUNTS.getIntegerValue();
        x = posX + 18;
        y = posY + 16;

        for (int i = 0; i < size; ++i)
        {
            MaterialListEntry entry = list.get(i);
            String text = entry.getStack().getName().getString();
            int multiplier = this.materialList.getMultiplier();
            int count = multiplier == 1 ? entry.getCountMissing() - entry.getCountAvailable() : entry.getCountTotal();
            count *= multiplier;
            String strCount = this.getFormattedCountString(count, entry.getStack().getMaxCount());
            int cntLen = font.getWidth(strCount);
            int cntPosX = posX + maxLineLength - cntLen - 2;

            drawContext.drawText(font, text, x, y, textColor, useShadow);
            drawContext.drawText(font, strCount, cntPosX, y, itemCountTextColor, useShadow);

            y += lineHeight;
        }

        if (scale != 1d)
        {
            matrixStack.popMatrix();
        }

        return contentHeight + 4;
    }

    protected String getFormattedCountString(int count, int maxStackSize)
    {
        int stacks = count / maxStackSize;
        int remainder = count % maxStackSize;
        double boxCount = (double) count / (27D * maxStackSize);

        if (count > maxStackSize)
        {
            if (boxCount >= 1.0)
            {
                return String.format("%d (%.2f %s)", count, boxCount, StringUtils.translate("litematica.gui.label.material_list.abbr.shulker_box"));
            }
            else if (remainder > 0)
            {
                return String.format("%d (%d x %d + %d)", count, stacks, maxStackSize, remainder);
            }
            else
            {
                return String.format("%d (%d x %d)", count, stacks, maxStackSize);
            }
        }
        else
        {
            return String.format("%d", count);
        }
    }

    public static void renderLookedAtBlockInInventory(DrawContext drawContext, HandledScreen<?> gui, MinecraftClient mc)
    {
        if (Configs.Generic.HIGHLIGHT_BLOCK_IN_INV.getBooleanValue())
        {
            RayTraceUtils.RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 10);

            if (traceWrapper != null && traceWrapper.getHitType() == RayTraceUtils.RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
            {
                BlockPos pos = traceWrapper.getBlockHitResult().getBlockPos();
                BlockState state = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);

                if (state != lastLookedAtBlock)
                {
                    lastLookedAtBlock = state;
                    lastLookedAtBlocksItem = MaterialCache.getInstance().getRequiredBuildItemForState(state);
                }

                Color4f color = Configs.Colors.HIGHTLIGHT_BLOCK_IN_INV_COLOR.getColor();
                highlightSlotsWithItem(drawContext, lastLookedAtBlocksItem, gui, color, mc);
            }
        }
    }

    public static void highlightSlotsWithItem(DrawContext drawContext, ItemStack referenceItem, HandledScreen<?> gui, Color4f color, MinecraftClient mc)
    {
        List<Slot> slots = gui.getScreenHandler().slots;

//        RenderUtils.blend(true);
        int guiX = ((IMixinHandledScreen) gui).litematica_getX();
        int guiY = ((IMixinHandledScreen) gui).litematica_getY();

        for (Slot slot : slots)
        {
            if (slot.hasStack() &&
                (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt(slot.getStack(), referenceItem) ||
                 InventoryUtils.doesShulkerBoxContainItem(slot.getStack(), referenceItem) ||
                 InventoryUtils.doesBundleContainItem(slot.getStack(), referenceItem)))
            {
                renderOutlinedBox(drawContext, guiX + slot.x, guiY + slot.y, 16, 16, color.intValue, color.intValue | 0xFF000000, 1f);
            }
        }

//        RenderUtils.color(1f, 1f, 1f, 1f);
    }

    public static void renderOutlinedBox(DrawContext drawContext, int x, int y, int width, int height, int colorBg, int colorBorder, float zLevel)
    {
        // Draw the background
        RenderUtils.drawRect(drawContext, x + 1, y + 1, width - 2, height - 2, colorBg);    // zLevel

        // Draw the border
        RenderUtils.drawOutline(drawContext, x, y, width, height, 1, colorBorder);    // zLevel
    }
}
