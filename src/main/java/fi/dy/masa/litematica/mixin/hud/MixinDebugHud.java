package fi.dy.masa.litematica.mixin.hud;

import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

@Mixin(DebugHud.class)
public abstract class MixinDebugHud
{
	@Shadow @Final private MinecraftClient client;

	@Redirect(method = "render(Lnet/minecraft/client/gui/DrawContext;)V",
			  at = @At(value = "INVOKE",
					   target = "Ljava/util/Collection;isEmpty()Z",
					   ordinal = 0))
	private boolean litematica_fixF3WhenAllDisabled(Collection<Identifier> instance)
	{
		return false;
	}

	@ModifyArg(method = "render(Lnet/minecraft/client/gui/DrawContext;)V",
			   at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/client/gui/hud/DebugHud;drawText(Lnet/minecraft/client/gui/DrawContext;Ljava/util/List;Z)V",
						ordinal = 0),
			   index = 1)
	private List<String> litematica_addDebugLines(List<String> text)
	// Left side
    {
		// Always display only when F3 is open.
		if (this.client.debugHudEntryList.isF3Enabled())
		{
			WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

			if (world != null)
			{
				Pair<String, String> pair = EntityUtils.getEntityDebug();
				String pre = GuiBase.TXT_GOLD;
				String rst = GuiBase.TXT_RST;

				WorldRendererSchematic renderer = LitematicaRenderer.getInstance().getWorldRenderer();
				int size = text.size();

				if (size > 3)
				{
					size -= 3;
				}
				else
				{
					size = 0;
					// Insert mode, but do not go beyond '0'
				}

				text.add(size++, String.format("%s[Litematica]%s %s",
									   pre, rst, renderer.getDebugInfoRenders()));

				String str = String.format("E: %d TE: %d C: %d, CT: %d, CV: %d",
										   world.getRegularEntityCount(),
//                                       world.getEntityDebug(),
										   world.getChunkProvider().getTileEntityCount(),
										   world.getChunkProvider().getLoadedChunkCount(),
										   DataManager.getSchematicPlacementManager().getTouchedChunksCount(),
										   DataManager.getSchematicPlacementManager().getLastVisibleChunksCount()
				);

				text.add(size++, String.format("%s[Litematica]%s %s %s", pre, rst, renderer.getDebugInfoEntities(), str));

				if (!pair.getLeft().isEmpty())
				{
					text.add(size, String.format("%s[%s]%s %s", pre, pair.getLeft(), rst, pair.getRight()));
				}
			}
		}
		return text;
	}
}
