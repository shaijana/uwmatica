package fi.dy.masa.litematica.mixin.hud;

import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.hud.debug.DebugHudEntryVisibility;
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
	@Shadow @Final private List<Identifier> visibleEntries;
	@Shadow public abstract boolean isF3Enabled();

	@Shadow
	private Map<Identifier, DebugHudEntryVisibility> visibilityMap;

	@Inject(method = "updateVisibleEntries", at = @At("TAIL"))
	private void litematica_insertVisiblePosition(CallbackInfo ci)
	{
		if (!this.visibilityMap.containsKey(LitematicaDebugHud.LITEMATICA_DEBUG))
		{
//			Litematica.LOGGER.info("DebugHudProfile: Insert Missing Entry into visibilityMap.");
			this.visibilityMap.put(LitematicaDebugHud.LITEMATICA_DEBUG, DebugHudEntryVisibility.IN_F3);
		}

		if (this.isF3Enabled())
		{
			this.visibleEntries.remove(LitematicaDebugHud.LITEMATICA_DEBUG);
			this.visibleEntries.addFirst(LitematicaDebugHud.LITEMATICA_DEBUG);
		}
	}
}
