package fi.dy.masa.litematica.interfaces;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventFlag;

import java.util.List;

public interface ISchematicPlacementEventManager
{
    void registerSchematicPlacementEventListener(ISchematicPlacementEventListener listener, List<SchematicPlacementEventFlag> flags);
}
