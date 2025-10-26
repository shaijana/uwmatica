package fi.dy.masa.litematica.mixin.hud;

import java.util.Map;
import com.google.common.collect.ImmutableMap;

import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.hud.debug.DebugHudEntry;
import net.minecraft.client.gui.hud.debug.DebugHudEntryVisibility;
import net.minecraft.client.gui.hud.debug.DebugProfileType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.LitematicaDebugHud;

@Deprecated
//@Mixin(value = DebugHudEntries.class)
public abstract class MixinDebugHudEntries
{
//	@Shadow @Final private static Map<Identifier, DebugHudEntry> ENTRIES;
//	@Mutable @Shadow @Final public static Map<DebugProfileType, Map<Identifier, DebugHudEntryVisibility>> PROFILES;
//
//	@Inject(method = "<clinit>", at = @At("TAIL"))
//	private static void litematica_registerDebugLines(CallbackInfo ci)
//	{
//		Map<Identifier, DebugHudEntryVisibility> defMap = PROFILES.get(DebugProfileType.DEFAULT);
//		Map<Identifier, DebugHudEntryVisibility> perfMap = PROFILES.get(DebugProfileType.PERFORMANCE);
//		ImmutableMap.Builder<Identifier, DebugHudEntryVisibility> builder = new ImmutableMap.Builder<>();
//
//		builder.putAll(defMap);
//		builder.put(LitematicaDebugHud.LITEMATICA_DEBUG, DebugHudEntryVisibility.IN_F3);
//		ENTRIES.put(LitematicaDebugHud.LITEMATICA_DEBUG, new LitematicaDebugHud());
//
//		PROFILES = ImmutableMap.of(
//					DebugProfileType.DEFAULT, builder.build(),
//					DebugProfileType.PERFORMANCE, perfMap
//				);
//	}
}
