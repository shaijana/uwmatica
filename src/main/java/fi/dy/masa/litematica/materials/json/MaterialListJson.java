package fi.dy.masa.litematica.materials.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.serialization.JsonOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;

@ApiStatus.Experimental
public class MaterialListJson
{
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<MaterialListJsonBase> data;

    public MaterialListJson()
    {
        this.data = new ArrayList<>();
    }

    public List<MaterialListJsonBase> getMaterials()
    {
        return this.data;
    }

    public boolean readMaterialListAll(MaterialListBase materialList, MaterialListJsonCache cache)
    {
        ImmutableList<MaterialListEntry> materials = materialList.getMaterialsAll();

        if (materials.isEmpty())
        {
            return false;
        }

        this.data.clear();

        materials.forEach(
                (entry) ->
                {
                    MaterialListJsonBase base = new MaterialListJsonBase(entry.getStack().getRegistryEntry(), (entry.getStack().getCount() * entry.getCountTotal()), null);
                    this.data.add(base);
                    cache.buildStepsBase(base, new ArrayList<>());
                }
        );

        cache.simplifyEntrySteps();

        return true;
    }

    public boolean readMaterialListMissingOnly(MaterialListBase materialList, MaterialListJsonCache cache)
    {
        List<MaterialListEntry> materials = materialList.getMaterialsMissingOnly(false);

        if (materials.isEmpty())
        {
            return false;
        }

        this.data.clear();

        materials.forEach(
                (entry) ->
                {
                    MaterialListJsonBase base = new MaterialListJsonBase(entry.getStack().getRegistryEntry(), (entry.getStack().getCount() * entry.getCountTotal()), null);
                    this.data.add(base);
                    cache.buildStepsBase(base, new ArrayList<>());
                }
        );

        cache.simplifyEntrySteps();

        return true;
    }

    public boolean writeJson(Path file, MinecraftClient mc)
    {
        if (this.data.isEmpty() || mc.world == null)
        {
            return false;
        }

        if (Files.exists(file))
        {
            try
            {
                Files.delete(file);
            }
            catch (IOException err)
            {
                Litematica.LOGGER.error("MaterialListJson#toJson(): Exception deleting file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
                return false;
            }
        }

        try
        {
            Files.writeString(file, GSON.toJson(this.toJson(mc.world.getRegistryManager())));
            Litematica.LOGGER.info("MaterialListJson#toJson(): Exported Materials file '{}' successfully.", file.toAbsolutePath().toString());
            return true;
        }
        catch (IOException err)
        {
            Litematica.LOGGER.error("MaterialListJson#toJson(): Exception writing file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
            return false;
        }
    }

    public boolean writeCacheJson(MaterialListJsonCache cache, Path file, MinecraftClient mc)
    {
        if (cache.isEmpty() || mc.world == null)
        {
            return false;
        }

        if (Files.exists(file))
        {
            try
            {
                Files.delete(file);
            }
            catch (IOException err)
            {
                Litematica.LOGGER.error("MaterialListJson#writeCacheJson(): Exception deleting file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
                return false;
            }
        }

        try
        {
            Files.writeString(file, GSON.toJson(cache.toJson(mc.world.getRegistryManager().getOps(JsonOps.INSTANCE))));
            Litematica.LOGGER.info("MaterialListJson#writeCacheJson(): Exported Materials Cache file '{}' successfully.", file.toAbsolutePath().toString());
            return true;
        }
        catch (IOException err)
        {
            Litematica.LOGGER.error("MaterialListJson#writeCacheJson(): Exception writing file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
            return false;
        }
    }

    public JsonElement toJson(DynamicRegistryManager registry)
    {
        RegistryOps<?> ops = registry.getOps(JsonOps.INSTANCE);
        JsonArray arr = new JsonArray();

        this.data.forEach(
                (entry) ->
                        arr.add(entry.toJson(ops))
        );

        return arr;
    }

    public void clear()
    {
        this.data.clear();
    }
}
