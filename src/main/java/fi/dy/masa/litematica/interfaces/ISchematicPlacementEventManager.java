package fi.dy.masa.litematica.interfaces;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventFlag;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;

import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

public interface ISchematicPlacementEventManager
{
    void registerSchematicPlacementEventListener(@Nonnull ISchematicPlacementEventListener listener, @Nonnull List<SchematicPlacementEventFlag> flags);

    void invokePrePlacementChange(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement);

    void invokePostPlacementChange(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement);

    void invokeSetSubRegionEnabled(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement placement, boolean toggle);

    void invokeSetSubRegionOrigin(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement placement, BlockPos pos);

    void invokeSetSubRegionMirror(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement placement, BlockMirror mirror);

    void invokeSetSubRegionRotation(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement placement, BlockRotation rot);

    void invokeResetSubRegion(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement placement);
}
