package fi.dy.masa.litematica.mixin.hud;

import java.util.Collection;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.litematica.render.LitematicaDebugHud;

// Original method (Still Works)
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
			List<String> list = LitematicaDebugHud.getDebugLines();

			if (!list.isEmpty())
			{
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

				for (String entry : list)
				{
					text.add(size++, entry);
				}
			}
		}

		return text;
	}
}
