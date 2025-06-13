package fi.dy.masa.litematica.data;

/**
 * The flags used by {@link fi.dy.masa.litematica.event.SchematicPlacementEventHandler}
 */
public enum SchematicPlacementEventFlag
{
    INIT,
    CREATE_FOR,
    FOR_CONVERSION,
    FROM_JSON,
    FROM_NBT,
    TO_JSON,
    TO_NBT,

    TOGGLE_LOCKED,
    SET_ENABLED,
    SET_RENDER,
    SET_NAME,
    SET_ORIGIN,
    SET_MIRROR,
    SET_ROTATION,

    ON_SELECTED,
    ON_ADDED,
    ON_REMOVED,
    ON_UPDATED
}
