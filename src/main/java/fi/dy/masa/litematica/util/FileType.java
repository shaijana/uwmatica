package fi.dy.masa.litematica.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.common.collect.ImmutableList;

import net.minecraft.util.StringIdentifiable;

public enum FileType implements StringIdentifiable
{
	INVALID,
	UNKNOWN,
	JSON,
	LITEMATICA_SCHEMATIC,
	SCHEMATICA_SCHEMATIC,
	SPONGE_SCHEMATIC,
	VANILLA_STRUCTURE;

	public static final StringIdentifiable.EnumCodec<FileType> CODEC = StringIdentifiable.createCodec(FileType::values);
	public static final ImmutableList<FileType> VALUES = ImmutableList.copyOf(values());

	public static FileType fromName(String fileName)
	{
		if (fileName.endsWith(".litematic"))
		{
			return LITEMATICA_SCHEMATIC;
		}
		else if (fileName.endsWith(".schematic"))
		{
			return SCHEMATICA_SCHEMATIC;
		}
		else if (fileName.endsWith(".nbt"))
		{
			return VANILLA_STRUCTURE;
		}
		else if (fileName.endsWith(".schem"))
		{
			return SPONGE_SCHEMATIC;
		}
		else if (fileName.endsWith(".json"))
		{
			return JSON;
		}

		return UNKNOWN;
	}

	@Deprecated
	public static FileType fromFile(File file)
	{
		if (file.isFile() && file.canRead())
		{
			return fromName(file.getName());
		}
		else
		{
			return INVALID;
		}
	}

	public static FileType fromFile(Path file)
	{
		if (Files.exists(file) && Files.isReadable(file))
		{
			return fromName(file.getFileName().toString());
		}
		else
		{
			return INVALID;
		}
	}

	public static String getFileExt(FileType type)
	{
		return switch (type)
		{
			case LITEMATICA_SCHEMATIC -> ".litematic";
			case SCHEMATICA_SCHEMATIC -> ".schematic";
			case SPONGE_SCHEMATIC -> ".schem";
			case VANILLA_STRUCTURE -> ".nbt";
			case JSON -> ".json";
			case INVALID -> ".invalid";
			case UNKNOWN -> ".unknown";
		};
	}

	public static String getString(FileType type)
	{
		return switch (type)
		{
			case LITEMATICA_SCHEMATIC -> "litematic";
			case SCHEMATICA_SCHEMATIC -> "schematic";
			case SPONGE_SCHEMATIC -> "sponge";
			case VANILLA_STRUCTURE -> "vanilla_nbt";
			case JSON -> "JSON";
			case INVALID -> "invalid";
			case UNKNOWN -> "unknown";
		};
	}

	@Override
	public String asString()
	{
		return getString(this);
	}
}
