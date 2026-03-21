package fi.dy.masa.litematica.mixin.model;

import java.util.Map;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityModelSet.class)
public interface IMixinEntityModelSet
{
	@Accessor("roots")
	Map<ModelLayerLocation, LayerDefinition> litematica_getRoots();
}
