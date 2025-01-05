package fi.dy.masa.litematica.world.fallback;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;

public abstract class SchematicBiomeTypes
{
	public static final RegistryKey<Biome> SCHEMATIC_FALLBACK = register("schematic_fallback_biome");
	public static final Identifier SCHEMATIC_FALLBACK_ID = of("schematic_fallback_biome");

	private static Identifier of(String name)
	{
		return Identifier.of(Reference.MOD_ID, name);
	}

	private static RegistryKey<Biome> register(String name)
	{
		return RegistryKey.of(RegistryKeys.BIOME, of(name));
	}

	public static void registerFallbackBiomes()
	{
		Litematica.debugLog("registerFallbackBiomes()");
	}
}
