package fi.dy.masa.litematica.event;

import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.SchematicPlacementEventFlag;
import fi.dy.masa.litematica.interfaces.ISchematicPlacementEventListener;
import fi.dy.masa.litematica.interfaces.ISchematicPlacementEventManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

/**
 * This was primarily created for mods like Syncmatica with complicated Mixin's that can be fragile.
 * So here we go adding an event callback system for Schematic Placement management.
 */
public class SchematicPlacementEventHandler implements ISchematicPlacementEventManager
{
    private static final SchematicPlacementEventHandler INSTANCE = new SchematicPlacementEventHandler();
    private final HashMap<ISchematicPlacementEventListener, List<SchematicPlacementEventFlag>> handlers = new HashMap<>();
    public static SchematicPlacementEventHandler getInstance() { return INSTANCE; }

    @Override
    public void registerSchematicPlacementEventListener(ISchematicPlacementEventListener listener,
                                                        List<SchematicPlacementEventFlag> flags)
    {
        if (flags.isEmpty())
        {
            Litematica.LOGGER.warn("registerSchematicPlacementEventListener: provided event flags for '{}' are empty.  Ignoring.",
                                   listener.getClass().getCanonicalName() != null ? listener.getClass().getCanonicalName() : listener.getClass().getName());
            return;
        }

        if (!this.handlers.containsKey(listener))
        {
            this.handlers.put(listener, flags);
        }
    }

    @ApiStatus.Internal
    public void onPlacementInit(SchematicPlacement placement)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.INIT))
                    {
                        handler.onPlacementInit(placement);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementCreateFor(SchematicPlacement placement, LitematicaSchematic schematic, BlockPos origin, String name, boolean enabled, boolean enableRender)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.CREATE_FOR))
                    {
                        handler.onPlacementCreateFor(placement, schematic, origin, name, enabled, enableRender);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementCreateForConversion(SchematicPlacement placement, LitematicaSchematic schematic, BlockPos origin)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.FOR_CONVERSION))
                    {
                        handler.onPlacementCreateForConversion(placement, schematic, origin);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementCreateFromJson(SchematicPlacement placement, LitematicaSchematic schematic, BlockPos origin, String name, BlockRotation rotation, BlockMirror mirror, boolean enabled, boolean enableRender)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.FROM_JSON))
                    {
                        handler.onPlacementCreateFromJson(placement, schematic, origin, name, rotation, mirror, enabled, enableRender);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementCreateFromNbt(SchematicPlacement placement, LitematicaSchematic schematic, BlockPos origin, String name, BlockRotation rotation, BlockMirror mirror, boolean enabled, boolean enableRender)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.FROM_NBT))
                    {
                        handler.onPlacementCreateFromNbt(placement, schematic, origin, name, rotation, mirror, enabled, enableRender);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSavePlacementToJson(SchematicPlacement placement, JsonObject jsonObject)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.TO_JSON))
                    {
                        handler.onSavePlacementToJson(placement, jsonObject);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSavePlacementToNbt(SchematicPlacement placement, NbtCompound nbt)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.TO_NBT))
                    {
                        handler.onSavePlacementToNbt(placement, nbt);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onToggleLocked(SchematicPlacement placement, boolean toggle)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.TOGGLE_LOCKED))
                    {
                        handler.onToggleLocked(placement, toggle);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetName(SchematicPlacement placement, String name)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_NAME))
                    {
                        handler.onSetName(placement, name);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetEnabled(SchematicPlacement placement, boolean enabled)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_ENABLED))
                    {
                        handler.onSetEnabled(placement, enabled);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetRender(SchematicPlacement placement, boolean enabled)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_RENDER))
                    {
                        handler.onSetRender(placement, enabled);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetOrigin(SchematicPlacement placement, BlockPos origin)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_ORIGIN))
                    {
                        handler.onSetOrigin(placement, origin);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetMirror(SchematicPlacement placement, BlockMirror mirror)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_MIRROR))
                    {
                        handler.onSetMirror(placement, mirror);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onSetRotation(SchematicPlacement placement, BlockRotation rotation)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.SET_ROTATION))
                    {
                        handler.onSetRotation(placement, rotation);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementSelected(@Nullable SchematicPlacement prevPlacement, @Nullable SchematicPlacement selected)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.ON_SELECTED))
                    {
                        handler.onPlacementSelected(prevPlacement, selected);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementAdded(SchematicPlacement placement)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.ON_ADDED))
                    {
                        handler.onPlacementAdded(placement);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementRemoved(SchematicPlacement placement)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.ON_REMOVED))
                    {
                        handler.onPlacementRemoved(placement);
                    }
                }
        );
    }

    @ApiStatus.Internal
    public void onPlacementUpdated(SchematicPlacement placement)
    {
        this.handlers.forEach(
                (handler, list) ->
                {
                    if (list.contains(SchematicPlacementEventFlag.ON_UPDATED))
                    {
                        handler.onPlacementUpdated(placement);
                    }
                }
        );
    }
}
