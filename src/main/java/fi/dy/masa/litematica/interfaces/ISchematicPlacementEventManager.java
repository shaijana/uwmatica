package fi.dy.masa.litematica.interfaces;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventFlag;

import java.util.List;
import javax.annotation.Nonnull;

public interface ISchematicPlacementEventManager
{
    void registerSchematicPlacementEventListener(@Nonnull ISchematicPlacementEventListener listener, @Nonnull List<SchematicPlacementEventFlag> flags);

    void invokePrePlacementChange(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement);

    void invokePostPlacementChange(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement);
}
