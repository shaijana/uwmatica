package fi.dy.masa.litematica.world.fallback;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;

public class SchematicDimensionTypes
{
	public static final RegistryKey<DimensionType> SCHEMATIC_FALLBACK = register("schematic_fallback_dimension");
	public static final Identifier SCHEMATIC_FALLBACK_ID = of("schematic_fallback_dimension");

	private static Identifier of(String name)
	{
		return Identifier.of(Reference.MOD_ID, name);
	}

	private static RegistryKey<DimensionType> register(String name)
	{
		return RegistryKey.of(RegistryKeys.DIMENSION_TYPE, of(name));
	}

	public static void registerFallbackDimensions()
	{
		Litematica.debugLog("registerFallbackDimensions()");
	}
}
