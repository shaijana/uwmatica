package fi.dy.masa.litematica.world;

import java.util.*;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.FuelRegistry;
import net.minecraft.item.map.MapState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.*;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.tick.EmptyTickSchedulers;
import net.minecraft.world.tick.QueryableTickScheduler;
import net.minecraft.world.tick.TickManager;

import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;

public class WorldSchematic extends World
{
    protected static final RegistryKey<World> REGISTRY_KEY = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(Reference.MOD_ID, "schematic_world"));

    protected final MinecraftClient mc;
    protected final ChunkManagerSchematic chunkManagerSchematic;
    protected RegistryEntry<Biome> biome;
    @Nullable protected final WorldRendererSchematic worldRenderer;
    protected int nextEntityId;
    protected int entityCount;
    private final TickManager tickManager;
    private final RegistryEntry<DimensionType> dimensionType;
    private DimensionEffects dimensionEffects = new DimensionEffects.Overworld();
    private final HashMap<UUID, ChunkPos> entityMap;
    private final SchematicEntityLookup<Entity> entityLookup;

    public WorldSchematic(MutableWorldProperties properties,
                          @Nonnull DynamicRegistryManager registryManager,
                          RegistryEntry<DimensionType> dimension,
                          @Nullable WorldRendererSchematic worldRenderer)
    {
        super(properties, REGISTRY_KEY, !registryManager.equals(DynamicRegistryManager.EMPTY) ? registryManager : SchematicWorldHandler.INSTANCE.getRegistryManager(), dimension, true, false, 0L, 0);

        this.mc = MinecraftClient.getInstance();

        if (this.mc == null || this.mc.world == null)
        {
            throw new RuntimeException("WorldSchematic invoked when MinecraftClient.getInstance() or mc.world is null");
        }

        this.worldRenderer = worldRenderer;
        this.chunkManagerSchematic = new ChunkManagerSchematic(this);
        this.dimensionType = dimension;

        if (!registryManager.equals(DynamicRegistryManager.EMPTY))
        {
            this.setDimension(registryManager);
        }
        else
        {
            this.setDimension(this.mc.world.getRegistryManager());
        }

        this.tickManager = new TickManager();
        this.entityCount = 0;
        this.entityMap = new HashMap<>();
        this.entityLookup = new SchematicEntityLookup<>();
    }

    @Override
    public String toString()
    {
        return "SchematicWorld["+REGISTRY_KEY.getValue().toString()+"]";
    }

    private void setDimension(DynamicRegistryManager registryManager)
    {
        registryManager.getOptional(RegistryKeys.DIMENSION_TYPE).ifPresent(entryLookup -> {
            RegistryEntry<DimensionType> nether = entryLookup.getOptional(DimensionTypes.THE_NETHER).orElse(null);
            RegistryEntry<DimensionType> end = entryLookup.getOptional(DimensionTypes.THE_END).orElse(null);
    
            if (nether != null && this.dimensionType.equals(nether))
            {
                this.biome = WorldUtils.getWastes(registryManager);
            }
            else if (end != null && this.dimensionType.equals(end))
            {
                this.biome = WorldUtils.getTheEnd(registryManager);
            }
            else
            {
                this.biome = WorldUtils.getPlains(registryManager);
            }
        });
    
        this.dimensionEffects = DimensionEffects.byDimensionType(this.dimensionType.value());
    }

    public ChunkManagerSchematic getChunkProvider()
    {
        return this.getChunkManager();
    }

    @Override
    public ChunkManagerSchematic getChunkManager()
    {
        return this.chunkManagerSchematic;
    }

    @Override
    public TickManager getTickManager()
    {
        return this.tickManager;
    }

    @Nullable
    @Override
    public MapState getMapState(MapIdComponent id) { return null; }

    @Override
    public QueryableTickScheduler<Block> getBlockTickScheduler()
    {
        return EmptyTickSchedulers.getClientTickScheduler();
    }

    @Override
    public QueryableTickScheduler<Fluid> getFluidTickScheduler()
    {
        return EmptyTickSchedulers.getClientTickScheduler();
    }

    public int getRegularEntityCount()
    {
//        return this.entityCount;
        return this.entityLookup.size();
    }

    public String getEntityDebug()
    {
        return String.format("eL: %d, eM: %d, cE: %d", this.entityLookup.size(), this.entityMap.size(), this.entityCount);
    }

    @Override
    public WorldChunk getWorldChunk(BlockPos pos)
    {
        return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public ChunkSchematic getChunk(int chunkX, int chunkZ)
    {
        return this.chunkManagerSchematic.getChunk(chunkX, chunkZ);
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean required)
    {
        return this.getChunk(chunkX, chunkZ);
    }

    @Override
    public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ)
    {
        return this.biome;
    }

    @Override
    public int getSeaLevel()
    {
        if (this.mc != null && this.mc.world != null)
        {
            return this.mc.world.getSeaLevel();
        }

        return 0;
    }

    @Override
    public boolean setBlockState(BlockPos pos, BlockState newState, int flags)
    {
        if (pos.getY() < this.getBottomY() || pos.getY() >= this.getTopYInclusive())
        {
            return false;
        }
        else
        {
            return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).setBlockState(pos, newState, 3) != null;
        }
    }

    @Override
    public boolean spawnEntity(Entity entity)
    {
        int chunkX = MathHelper.floor(entity.getX() / 16.0D);
        int chunkZ = MathHelper.floor(entity.getZ() / 16.0D);

        if (!this.chunkManagerSchematic.isChunkLoaded(chunkX, chunkZ))
        {
            return false;
        }
        else
        {
            entity.setId(this.nextEntityId++);
            // TODO --> MOVE TO SchematicEntityLookup
            this.chunkManagerSchematic.getChunk(chunkX, chunkZ).addEntity(entity);
            ++this.entityCount;
            this.entityMap.put(entity.getUuid(), new ChunkPos(chunkX, chunkZ));
            this.entityLookup.put(entity);
            return true;
        }
    }

    public void unloadedEntities(int count)
    {
        this.entityCount -= count;
    }

    protected void unloadEntitiesByChunk(int chunkX, int chunkZ)
    {
        List<UUID> list = new ArrayList<>();

        this.entityMap.forEach(
                (u, cp) ->
                {
                    if (cp.x == chunkX && cp.z == chunkZ)
                    {
                        list.add(u);
                    }
                });

        list.forEach(
                (uuid) ->
                {
                    synchronized (this.entityMap)
                    {
                        this.entityMap.remove(uuid);
                    }

                    this.entityLookup.remove(uuid);
                });
    }

    @Nullable
    @Override
    public Entity getEntityById(int id)
    {
        return this.entityLookup.get(id);
    }

    protected void closeEntityLookup() throws Exception
    {
        this.entityLookup.close();
    }

    public void clearEntities()
    {
        try
        {
            this.closeEntityLookup();
        }
        catch (Exception ignored) { }

        this.entityMap.clear();
        this.entityCount = 0;
        this.nextEntityId = 0;
    }

    @Override
    public Collection<EnderDragonPart> getEnderDragonParts()
    {
        return List.of();
    }

    @Override
    public List<? extends PlayerEntity> getPlayers()
    {
        return ImmutableList.of();
    }

    @Override
    public long getTime()
    {
        return this.mc.world != null ? this.mc.world.getTime() : 0;
    }

    @Override
    public Scoreboard getScoreboard()
    {
        return this.mc.world != null ? this.mc.world.getScoreboard() : null;
    }

    @Override
    public RecipeManager getRecipeManager()
    {
        return this.mc.world != null ? this.mc.world.getRecipeManager() : null;
    }

    @Override
    protected EntityLookup<Entity> getEntityLookup()
    {
        return this.entityLookup;
    }

    @Override
    public List<Entity> getOtherEntities(@Nullable final Entity except, final Box box, Predicate<? super Entity> predicate)
    {
        final List<Entity> entities = new ArrayList<>();
        List<ChunkSchematic> chunks = this.getChunksWithinBox(box);

        // TODO --> MOVE TO SchematicEntityLookup
        for (ChunkSchematic chunk : chunks)
        {
            chunk.getEntityList().forEach((e) -> {
                if (e != except && box.intersects(e.getBoundingBox()) && predicate.test(e)) {
                    entities.add(e);
                }
            });
        }

        return entities;
    }

    @Override
    public <T extends Entity> List<T> getEntitiesByType(TypeFilter<Entity, T> arg, Box box, Predicate<? super T> predicate)
    {
        ArrayList<T> list = new ArrayList<>();

        // TODO --> MOVE TO SchematicEntityLookup
        for (Entity e : this.getOtherEntities(null, box, e -> true))
        {
            T t = arg.downcast(e);

            if (t != null && predicate.test(t))
            {
                list.add(t);
            }
        }

        return list;
    }

    public List<ChunkSchematic> getChunksWithinBox(Box box)
    {
        final int minX = MathHelper.floor(box.minX / 16.0);
        final int minZ = MathHelper.floor(box.minZ / 16.0);
        final int maxX = MathHelper.floor(box.maxX / 16.0);
        final int maxZ = MathHelper.floor(box.maxZ / 16.0);

        List<ChunkSchematic> chunks = new ArrayList<>();

        for (int cx = minX; cx <= maxX; ++cx)
        {
            for (int cz = minZ; cz <= maxZ; ++cz)
            {
                ChunkSchematic chunk = this.chunkManagerSchematic.getChunkIfExists(cx, cz);

                if (chunk != null)
                {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    @Override
    public void scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState stateOld, BlockState stateNew)
    {
        if (stateNew != stateOld)
        {
            this.scheduleChunkRenders(pos.getX() >> 4, pos.getZ() >> 4);
        }
    }

    @Override
    public void playSound(@Nullable Entity source, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed)
    {
        // NO-OP
    }

    @Override
    public void playSoundFromEntity(@Nullable Entity source, Entity entity, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed)
    {
        // NO-OP
    }

    public void scheduleChunkRenders(int chunkX, int chunkZ)
    {
        if (this.worldRenderer != null)
        {
            this.worldRenderer.scheduleChunkRenders(chunkX, chunkZ);
        }
    }

    @Override
    public int getBottomY()
    {
        return this.mc.world != null ? this.mc.world.getBottomY() : -64;
    }

    @Override
    public int getHeight()
    {
        return this.mc.world != null ? this.mc.world.getHeight() : 384;
    }

    // The following HeightLimitView overrides are to work around an incompatibility with Lithium 0.7.4+

    @Override
    public int getTopYInclusive()
    {
        return this.getBottomY() + this.getHeight();
    }

    @Override
    public int getBottomSectionCoord()
    {
        return this.getBottomY() >> 4;
    }

    @Override
    public int getTopSectionCoord()
    {
        return this.getTopYInclusive() >> 4;
    }

    @Override
    public int countVerticalSections()
    {
        return this.getTopSectionCoord() - this.getBottomSectionCoord();
    }

    @Override
    public boolean isOutOfHeightLimit(BlockPos pos)
    {
        return this.isOutOfHeightLimit(pos.getY());
    }

    @Override
    public boolean isOutOfHeightLimit(int y)
    {
        return (y < this.getBottomY()) || (y >= this.getTopYInclusive());
    }

    @Override
    public int getSectionIndex(int y)
    {
        return (y >> 4) - (this.getBottomY() >> 4);
    }

    @Override
    public int sectionCoordToIndex(int coord)
    {
        return coord - (this.getBottomY() >> 4);
    }

    @Override
    public int sectionIndexToCoord(int index)
    {
        return index + (this.getBottomY() >> 4);
    }

    // For AO compatibility
    public RegistryEntry<DimensionType> getDimensionType()
    {
        return this.dimensionType;
    }

    public DimensionEffects getDimensionEffects()
    {
        return this.dimensionEffects;
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded)
    {
        boolean darkened = this.getDimensionEffects().isDarkened();

        if (!shaded)
        {
            return darkened ? 0.9F : 1.0F;
        }
        else
        {
            return switch (direction)
            {
                case DOWN -> darkened ? 0.9F : 0.5F;
                case UP -> darkened ? 0.9F : 1.0F;
                case NORTH, SOUTH -> 0.8F;
                case WEST, EAST -> 0.6F;
            };
        }
    }

    @Override
    public LightingProvider getLightingProvider()
    {
        // Returns the Fake Lighting Provider, if configured to do so
        return this.getChunkManager().getLightingProvider();
    }

    // todo --> moved to Fake Lighting Provider
    /*
    @Override
    public int getLightLevel(LightType type, BlockPos pos)
    {
        //return Configs.Visuals.RENDER_FAKE_LIGHTING_LEVEL != null ? Configs.Visuals.RENDER_FAKE_LIGHTING_LEVEL.getIntegerValue() : 15;
        return 15;
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int defaultValue)
    {
        //return Configs.Visuals.RENDER_FAKE_LIGHTING_LEVEL != null ? Configs.Visuals.RENDER_FAKE_LIGHTING_LEVEL.getIntegerValue() : 15;
        return 15;
    }
     */

    @Override
    public void updateListeners(BlockPos blockPos_1, BlockState blockState_1, BlockState blockState_2, int flags)
    {
        // NO-OP
    }

    @Override
    public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress)
    {
        // NO-OP
    }

    @Override
    public void syncGlobalEvent(int eventId, BlockPos pos, int data)
    {
        // NO-OP
    }
    
    @Override
    public void emitGameEvent(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter)
    {
        // NO-OP
    }

    @Override
    public void syncWorldEvent(@Nullable Entity entity, int eventId, BlockPos pos, int data)
    {
        // NO-OP
    }

    @Override
    public void createExplosion(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, ExplosionSourceType explosionSourceType, ParticleEffect smallParticle, ParticleEffect largeParticle, RegistryEntry<SoundEvent> soundEvent)
    {
        // NO-OP
    }

    @Override
    public DynamicRegistryManager getRegistryManager()
    {
        if (this.mc != null && this.mc.world != null)
        {
            return this.mc.world.getRegistryManager();
        }
        else if (!SchematicWorldHandler.INSTANCE.getRegistryManager().equals(DynamicRegistryManager.EMPTY))
        {
            return SchematicWorldHandler.INSTANCE.getRegistryManager();
        }
        else
        {
            return DynamicRegistryManager.EMPTY;
        }
    }

    @Override
    public BrewingRecipeRegistry getBrewingRecipeRegistry()
    {
        if (this.mc != null && this.mc.world != null)
        {
            return this.mc.world.getBrewingRecipeRegistry();
        }
        else
        {
            return BrewingRecipeRegistry.EMPTY;
        }
    }

    @Override
    public FuelRegistry getFuelRegistry()
    {
        return null;
    }

    @Override
    public FeatureSet getEnabledFeatures()
    {
        if (this.mc != null && this.mc.world != null)
        {
            return this.mc.world.getEnabledFeatures();
        }
        else
        {
            return FeatureSet.empty();
        }
    }

    @Override
    public String asString()
    {
        return "Chunks[SCH] W: "+this.getChunkManager().getDebugString()+" E: "+this.getRegularEntityCount()+" (eL: "+this.entityLookup.size()+"/"+ this.entityMap.size()+")";
    }

    @Override
    public void emitGameEvent(@Nullable Entity entity, RegistryEntry<GameEvent> event, Vec3d pos)
    {
        // NO-OP
    }

    @Override
    public void emitGameEvent(@Nullable Entity entity, RegistryEntry<GameEvent> event, BlockPos pos)
    {
        // NO-OP
    }

    @Override
    public void emitGameEvent(RegistryKey<GameEvent> event, BlockPos pos, @Nullable GameEvent.Emitter emitter)
    {
        // NO-OP
    }
}
