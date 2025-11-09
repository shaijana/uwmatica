package fi.dy.masa.litematica.mixin.hud;

import java.util.List;

import net.minecraft.client.gui.hud.debug.DebugHudProfile;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.LitematicaDebugHud;

@Mixin(DebugHudProfile.class)
public abstract class MixinDebugHudProfile
{
	@Shadow @Final public List<Identifier> visibleEntries;

	@Inject(method = "updateVisibleEntries", at = @At("TAIL"))
	private void litematica_updateVisibleEntries(CallbackInfo ci)
	{
		LitematicaDebugHud.INSTANCE.checkConfig();

//		// Shift to right side to "get out of the way" from the "Player position" display.
//		if (LitematicaDebugHud.INSTANCE.getMode() == DebugHudMode.VANILLA &&
//			this.visibleEntries.contains(LitematicaDebugHud.LITEMATICA_DEBUG))
//		{
//			if (LitematicaDebugHud.INSTANCE.shouldUseFallback())
//			{
//				Litematica.LOGGER.error("FALLBACK: {}", this.visibleEntries.toString());
//				this.visibleEntries.remove(LitematicaDebugHud.LITEMATICA_DEBUG);
//			}
//		}
	}
}
