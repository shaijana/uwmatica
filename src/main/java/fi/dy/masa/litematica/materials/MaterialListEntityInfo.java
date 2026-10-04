package fi.dy.masa.litematica.materials;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.util.data.tag.CompoundData;

public record MaterialListEntityInfo(Vec3 pos, CompoundData data, ItemStack pickStack)
{
}
