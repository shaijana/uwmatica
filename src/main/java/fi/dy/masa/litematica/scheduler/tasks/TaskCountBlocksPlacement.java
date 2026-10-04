package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.nbt.NbtView;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntityInfo;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.BlockUtils;

public class TaskCountBlocksPlacement extends TaskCountBlocksBase
{
    protected final SchematicPlacement schematicPlacement;
    protected final boolean ignoreState;

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList)
    {
        this(schematicPlacement, materialList, false);
    }

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList, boolean ignoreState)
    {
        super(materialList, "litematica.gui.label.task_name.material_list");

        this.schematicPlacement = schematicPlacement;
        this.ignoreState = ignoreState;
        Collection<Box> boxes = schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values();

        // Filter/clamp the boxes to intersect with the render layer
        if (materialList.getMaterialListType() == BlockInfoListType.RENDER_LAYERS)
        {
            this.addPerChunkBoxes(boxes, DataManager.getRenderLayerRange());
        }
        else
        {
            this.addPerChunkBoxes(boxes);
        }
    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.schematicWorld != null;
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateSchematic = this.schematicWorld.getBlockState(pos);

        if (stateSchematic.isAir() == false)
        {
            BlockState stateClient = this.clientWorld.getBlockState(pos);

            this.countsTotal.addTo(stateSchematic, 1);

            if (stateClient.isAir())
            {
                this.countsMissing.addTo(stateSchematic, 1);
            }
            else if (stateClient != stateSchematic &&
                    (this.ignoreState == false || stateClient.getBlock() != stateSchematic.getBlock()))
            {
                if (Configs.Visuals.IGNORE_CROP_AGE.getBooleanValue() == false ||
                    BlockUtils.areStatesEqualIgnoringAge(stateSchematic, stateClient) == false)
                {
                    this.countsMissing.addTo(stateSchematic, 1);
                    this.countsMismatch.addTo(stateSchematic, 1);
                }
            }
        }
    }

    /**
     * Only counts Pick Stack entities (Cushions, Item Frames, Armor Stands, etc.)
     */
    @Override
    protected void countEntitiesInBox(final AABB enclosingBox, final List<Entity> clientEntities, final List<Entity> schematicEntities)
    {
        RegistryAccess registry = this.schematicWorld.registryAccess();
        List<UUID> checkedFor = new ArrayList<>();
        List<UUID> accountedFor = new ArrayList<>();

        for (Entity entry : schematicEntities)
        {
            if (enclosingBox.contains(entry.position()))
            {
                ItemStack pickStack = entry.getPickResult();

                if (pickStack != null && !pickStack.isEmpty())
                {
                    NbtView view = NbtView.getWriter(registry);
                    entry.save(view.getWriter());
                    Vec3 pos = entry.position();
                    AABB box = entry.getBoundingBox();
                    CompoundData data = view.readData();

                    if (data != null && !data.isEmpty())
                    {
                        MaterialListEntityInfo entityInfo = new MaterialListEntityInfo(pos, data, pickStack);
                        this.entitiesTotal.addTo(entityInfo, 1);

                        if (clientEntities.isEmpty())
                        {
                            this.entitiesMissing.addTo(entityInfo, 1);
                            continue;
                        }

                        boolean found = false;

                        for (Entity entity : clientEntities)
                        {
                            if (entity.getBoundingBox().intersects(box))
                            {
                                UUID uuid = entity.getUUID();

                                if (accountedFor.contains(uuid))
                                {
                                    continue;
                                }

                                if (this.compareEntity(entityInfo, entity))
                                {
                                    accountedFor.add(uuid);
                                    found = true;
                                }
                                else if (!checkedFor.contains(uuid))
                                {
                                    ItemStack pickStack2 = entry.getPickResult();

                                    if (pickStack2 != null && !pickStack2.isEmpty())
                                    {
                                        NbtView view2 = NbtView.getWriter(registry);
                                        entry.save(view2.getWriter());
                                        Vec3 pos2 = entity.position();
                                        CompoundData data2 = view2.readData();

                                        if (data2 != null && !data2.isEmpty())
                                        {
                                            this.entitiesMismatch.addTo(new MaterialListEntityInfo(pos2, data2, pickStack2), 1);
                                        }
                                    }

                                    accountedFor.add(uuid);
                                }
                            }
                        }

                        if (!found)
                        {
                            this.entitiesMissing.addTo(entityInfo, 1);
                        }
                    }
                }

                checkedFor.add(entry.getUUID());
            }
        }

        // Add any stray entities not accounted for from client world
        if (!clientEntities.isEmpty())
        {
            for (Entity entry : clientEntities)
            {
                if (enclosingBox.contains(entry.position()))
                {
                    UUID uuid = entry.getUUID();

                    if (!accountedFor.contains(uuid) && !checkedFor.contains(uuid))
                    {
                        ItemStack pickStack2 = entry.getPickResult();

                        if (pickStack2 != null && !pickStack2.isEmpty())
                        {
                            NbtView view2 = NbtView.getWriter(registry);
                            entry.save(view2.getWriter());
                            Vec3 pos2 = entry.position();
                            CompoundData data2 = view2.readData();

                            if (data2 != null && !data2.isEmpty())
                            {
                                this.entitiesMismatch.addTo(new MaterialListEntityInfo(pos2, data2, pickStack2), 1);
                            }
                        }
                    }
                }
            }
        }
    }

    // Compare an entity Type and position only
    private boolean compareEntity(MaterialListEntityInfo info, Entity test)
    {
        EntityType<?> infoType = DataEntityUtils.getEntityType(info.data());
        EntityType<?> testType = test.getType();

        // We really can't check anything else reliably here.
        if (infoType != null && infoType.equals(testType))
        {
            return info.pos().equals(test.position());
        }

        return false;
    }
}
