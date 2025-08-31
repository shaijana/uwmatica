package fi.dy.masa.litematica.render;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.hud.debug.DebugHudEntry;
import net.minecraft.client.gui.hud.debug.DebugHudLines;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class LitematicaDebugHud implements DebugHudEntry
{
	public static final Identifier LITEMATICA_DEBUG = Identifier.of(Reference.MOD_ID, "litematica_renderer");
	public static final Identifier SECTION_ID = Identifier.ofVanilla(Reference.MOD_ID);

	@Override
	public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk)
	{
		List<String> list = LitematicaDebugHud.getDebugLines();

		if (!list.isEmpty())
		{
			lines.addLinesToSection(SECTION_ID, list);
		}
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo)
	{
		return true;
	}

	public static List<String> getDebugLines()
	{
		WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
		List<String> list = new ArrayList<>();

		if (worldSchematic != null)
		{
			Pair<String, String> pair = EntityUtils.getEntityDebug();
			String pre = GuiBase.TXT_GOLD;
			String rst = GuiBase.TXT_RST;

			WorldRendererSchematic renderer = LitematicaRenderer.getInstance().getWorldRenderer();

			list.add(String.format("%s[Litematica]%s %s",
								   pre, rst, renderer.getDebugInfoRenders()));

			String str = String.format("E: %d TE: %d C: %d, CT: %d, CV: %d",
									   worldSchematic.getRegularEntityCount(),
									   worldSchematic.getChunkProvider().getTileEntityCount(),
									   worldSchematic.getChunkProvider().getLoadedChunkCount(),
									   DataManager.getSchematicPlacementManager().getTouchedChunksCount(),
									   DataManager.getSchematicPlacementManager().getLastVisibleChunksCount()
			);

			list.add(String.format("%s[Litematica]%s %s %s", pre, rst, renderer.getDebugInfoEntities(), str));

			if (!pair.getLeft().isEmpty())
			{
				list.add(String.format("%s[%s]%s %s", pre, pair.getLeft(), rst, pair.getRight()));
			}
		}

		return list;
	}
}
