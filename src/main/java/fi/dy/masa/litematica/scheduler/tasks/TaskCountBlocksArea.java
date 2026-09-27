package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.nbt.NbtView;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntityInfo;
import fi.dy.masa.litematica.selection.AreaSelection;

public class TaskCountBlocksArea extends TaskCountBlocksBase
{
    public TaskCountBlocksArea(AreaSelection selection, IMaterialList materialList)
    {
        super(materialList, "litematica.gui.label.task_name.area_analyzer");

        this.addPerChunkBoxes(selection.getAllSubRegionBoxes());
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateClient = this.clientWorld.getBlockState(pos);
        this.countsTotal.addTo(stateClient, 1);
    }

    /**
     * Only counts Pick Stack entities (Cushions, Item Frames, Armor Stands, etc.)
     */
    @Override
    protected void countEntitiesInBox(final AABB aabb, final List<Entity> clientEntities, final List<Entity> schematicEntities)
    {
        for (Entity entry : clientEntities)
        {
            if (aabb.contains(entry.position()))
            {
                ItemStack pickStack = entry.getPickResult();

                if (pickStack != null && !pickStack.isEmpty())
                {
                    NbtView view = NbtView.getWriter(this.clientWorld.registryAccess());
                    entry.save(view.getWriter());
                    Vec3 pos = entry.position();
                    CompoundData data = view.readData();

                    if (data != null && !data.isEmpty())
                    {
                        this.entitiesTotal.addTo(new MaterialListEntityInfo(pos, data, pickStack), 1);
                    }
                }
            }
        }
    }
}
