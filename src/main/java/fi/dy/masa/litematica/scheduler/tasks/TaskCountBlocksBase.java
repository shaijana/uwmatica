package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import fi.dy.masa.malilib.util.position.IntBoundingBox;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntityInfo;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

public abstract class TaskCountBlocksBase extends TaskProcessChunkBase
{
    protected final Object2IntOpenHashMap<BlockState> countsTotal = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<BlockState> countsMissing = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<BlockState> countsMismatch = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<MaterialListEntityInfo> entitiesTotal = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<MaterialListEntityInfo> entitiesMissing = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<MaterialListEntityInfo> entitiesMismatch = new Object2IntOpenHashMap<>();
    protected final IMaterialList materialList;
    protected final LayerRange layerRange;

    protected TaskCountBlocksBase(IMaterialList materialList, String nameOnHud)
    {
        super(nameOnHud);

        this.materialList = materialList;

        if (materialList.getMaterialListType() == BlockInfoListType.ALL)
        {
            this.layerRange = new LayerRange(SchematicWorldRefresher.INSTANCE);
        }
        else
        {
            this.layerRange = DataManager.getRenderLayerRange();
        }
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        return this.areSurroundingChunksLoaded(pos, this.clientWorld, 0);
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        this.countBlocksInChunk(pos);
        this.countEntitiesInChunk(pos);
        return true;
    }

    protected void countBlocksInChunk(ChunkPos pos)
    {
        LayerRange range = this.layerRange;
        Direction.Axis axis = range.getAxis();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (IntBoundingBox bb : this.getBoxesInChunk(pos))
        {
            final int startX = axis == Direction.Axis.X ? Math.max(bb.minX(), range.getMinLayerBoundary()) : bb.minX();
            final int startY = axis == Direction.Axis.Y ? Math.max(bb.minY(), range.getMinLayerBoundary()) : bb.minY();
            final int startZ = axis == Direction.Axis.Z ? Math.max(bb.minZ(), range.getMinLayerBoundary()) : bb.minZ();
            final int endX = axis == Direction.Axis.X ? Math.min(bb.maxX(), range.getMaxLayerBoundary()) : bb.maxX();
            final int endY = axis == Direction.Axis.Y ? Math.min(bb.maxY(), range.getMaxLayerBoundary()) : bb.maxY();
            final int endZ = axis == Direction.Axis.Z ? Math.min(bb.maxZ(), range.getMaxLayerBoundary()) : bb.maxZ();

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.set(x, y, z);
                        this.countAtPosition(posMutable);
                    }
                }
            }
        }
    }

    private void countEntitiesInChunk(ChunkPos pos)
    {
        LayerRange range = this.layerRange;
        Direction.Axis axis = range.getAxis();
        List<IntBoundingBox> boxes = this.getBoxesInChunk(pos);

        for (IntBoundingBox bb : boxes)
        {
            final int startX = axis == Direction.Axis.X ? Math.max(bb.minX(), range.getMinLayerBoundary()) : bb.minX();
            final int startY = axis == Direction.Axis.Y ? Math.max(bb.minY(), range.getMinLayerBoundary()) : bb.minY();
            final int startZ = axis == Direction.Axis.Z ? Math.max(bb.minZ(), range.getMinLayerBoundary()) : bb.minZ();
            final int endX = axis == Direction.Axis.X ? Math.min(bb.maxX(), range.getMaxLayerBoundary()) : bb.maxX();
            final int endY = axis == Direction.Axis.Y ? Math.min(bb.maxY(), range.getMaxLayerBoundary()) : bb.maxY();
            final int endZ = axis == Direction.Axis.Z ? Math.min(bb.maxZ(), range.getMaxLayerBoundary()) : bb.maxZ();

            final BlockPos pos1 = new BlockPos(startX, startY, startZ);
            final BlockPos pos2 = new BlockPos(endX, endY, endZ);

            this.countEntitiesInBox(AABB.encapsulatingFullBlocks(pos1, pos2));
        }
    }

    private void countEntitiesInBox(AABB box)
    {
//        box = box.inflate(0.5d);        // With 0.5 we can bleed over the edge slightly; but
        List<Entity> clientEntities = this.clientWorld.getEntities((Entity) null, box, EntityUtils.NOT_PLAYER);
        List<Entity> schematicEntities = this.schematicWorld.getEntities((Entity) null, box, EntityUtils.NOT_PLAYER);
        this.countEntitiesInBox(box, clientEntities, schematicEntities);
    }

    protected abstract void countAtPosition(BlockPos pos);

    protected abstract void countEntitiesInBox(final AABB aabb, final List<Entity> clientEntities, final List<Entity> schematicEntities);

    @Override
    protected void onStop()
    {
        if (this.finished && this.isInWorld())
        {
            List<MaterialListEntry> list = MaterialListUtils.getMaterialList(
                    this.countsTotal, this.countsMissing, this.countsMismatch,
                    this.entitiesTotal, this.entitiesMissing, this.entitiesMismatch,
                    this.mc.player);

            this.materialList.setMaterialListEntries(list);
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        super.onStop();
    }
}
