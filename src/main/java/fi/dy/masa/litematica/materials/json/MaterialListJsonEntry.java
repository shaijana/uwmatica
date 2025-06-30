package fi.dy.masa.litematica.materials.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.math.MathHelper;

import fi.dy.masa.malilib.util.game.RecipeBookUtils;
import fi.dy.masa.malilib.util.log.AnsiLogger;
import fi.dy.masa.litematica.Litematica;

@ApiStatus.Experimental
public class MaterialListJsonEntry
{
    private static final AnsiLogger LOGGER = new AnsiLogger(MaterialListJsonEntry.class, true, true);
    private final List<MaterialListJsonBase> requirements;
    private final RegistryEntry<Item> inputItem;
    private final int total;
    private final Type type;
    private boolean hasOutput = false;
    private NetworkRecipeId primaryId;
    private HashMap<NetworkRecipeId, List<Ingredient>> recipeRequirements;
    private HashMap<NetworkRecipeId, RecipeBookCategory> recipeCategory;
    private HashMap<NetworkRecipeId, RecipeBookUtils.Type> recipeTypes;

    private MaterialListJsonEntry(RegistryEntry<Item> inputItem, int total, Type type)
    {
        this.requirements = new ArrayList<>();
        this.inputItem = inputItem;
        this.total = total;
        this.type = type;
    }

    public static @Nullable MaterialListJsonEntry build(RegistryEntry<Item> input, final int total, List<RecipeBookUtils.Type> types, @Nullable RegistryEntry<Item> prevItem)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (input == null || mc.world == null) return null;
        if (types.isEmpty())
        {
            // No types to match (Remaining logic)
            return new MaterialListJsonEntry(input, total, Type.EMPTY);
        }

        ItemStack shadow = new ItemStack(input);
        List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup = RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, types);

        if (lookup.isEmpty())
        {
            // No recipes found.
            return new MaterialListJsonEntry(input, total, Type.LAST);
        }

        final int lookupCount = lookup.size();
        final Type outType = lookupCount > 1 ? Type.MULTI : Type.ONE;

        Litematica.LOGGER.warn("MaterialListJsonEntry#build(): Found [{}] recipe(s) for item [{}]", lookupCount, input.getIdAsString());

        MaterialListJsonEntry result = new MaterialListJsonEntry(input, total, outType);
        result.recipeRequirements = new HashMap<>();
        result.recipeCategory = new HashMap<>();
        result.recipeTypes = new HashMap<>();
        result.hasOutput = true;

        // Only report the first entry (It's just redundant work, but we flagged it as MULTI)
        Pair<NetworkRecipeId, RecipeDisplayEntry> pair = lookup.getFirst();
        NetworkRecipeId id = pair.getLeft();
        RecipeDisplayEntry entry = pair.getRight();
        RecipeBookCategory category = entry.category();
        RecipeBookUtils.Type type = RecipeBookUtils.Type.fromRecipeDisplay(entry.display());

        ContextParameterMap map = RecipeBookUtils.getMap(mc);
        List<ItemStack> resultStacks = entry.getStacks(map);
        ItemStack resultStack = resultStacks.getFirst();
        final int resultCount = resultStack.getCount();

        // Stacks was already verified
        if (entry.craftingRequirements().isPresent())
        {
            List<Ingredient> ingredients = entry.craftingRequirements().get();

            result.recipeRequirements.put(id, ingredients);
            result.recipeCategory.put(id, category);
            result.recipeTypes.put(id, type);
            result.primaryId = id;

            HashMap<RegistryEntry<Item>, Integer> ded = new HashMap<>();

            for (Ingredient ing : ingredients)
            {
                SlotDisplay display = ing.toDisplay();
                ItemStack displayStack = display.getFirst(map);
                RegistryEntry<Item> itemEntry = displayStack.getRegistryEntry();

                if (prevItem != null && prevItem == itemEntry)
                {
                    // Stop loops.
                    Litematica.LOGGER.warn("MaterialListJsonEntry#build(): ingredient matches previous item [{}] ... Skipping", prevItem.getIdAsString());
                    continue;
                }

                // FIXME --> Correct math ?
                LOGGER.warn("build(): ResultStack: [{}] // Result Count: [{}]", resultStack.toString(), resultCount);
                final float adjusted = ((float) total / resultCount);
                final int floor = MathHelper.floor(adjusted);
                final int diff = total - floor;
                final int adjustedTotal = resultCount > 1 ? diff : total;

                LOGGER.warn("build(): adjusted: [{}], floor: [{}], diff: [{}] // AdjustedTotal: [{}]", adjusted, floor, diff, adjustedTotal);

                if (ded.containsKey(itemEntry))
                {
                    final int count = ded.get(itemEntry) + adjustedTotal;
                    ded.put(itemEntry, count);
                }
                else
                {
                    ded.put(itemEntry, adjustedTotal);
                }
            }

            Litematica.LOGGER.warn("MaterialListJsonEntry#build(): Found [{}] sub-materials(s) for item [{}]", ded.size(), input.getIdAsString());

            ded.forEach(
                    (key, count) ->
                            result.requirements.add(new MaterialListJsonBase(key, count, input))
            );
        }

        return result;
    }

    public Type getType() { return this.type; }

    public RegistryEntry<Item> getInputItem() { return this.inputItem; }

    public List<MaterialListJsonBase> getRequirements() { return this.requirements; }

    public NetworkRecipeId getPrimaryId() { return this.primaryId; }

    public HashMap<NetworkRecipeId, List<Ingredient>> getRecipeRequirements()
    {
        return this.recipeRequirements;
    }

    public HashMap<NetworkRecipeId, RecipeBookCategory> getRecipeCategory()
    {
        return this.recipeCategory;
    }

    public HashMap<NetworkRecipeId, RecipeBookUtils.Type> getRecipeTypes()
    {
        return this.recipeTypes;
    }

    public boolean hasOutput()
    {
        return this.hasOutput;
    }

    public int getTotal()
    {
        return this.total;
    }

    public enum Type
    {
        LAST,
        EMPTY,
        ONE,
        MULTI
    }

    public JsonElement toJson(RegistryOps<?> ops)
    {
        JsonObject obj = new JsonObject();

        obj.add("Item", new JsonPrimitive(this.getInputItem().getIdAsString()));
        obj.add("Count", new JsonPrimitive(this.getTotal()));
        obj.add("Type", new JsonPrimitive(this.type.name()));

        if (this.hasOutput())
        {
            obj.add("PrimaryId", new JsonPrimitive(this.getPrimaryId().index()));
            obj.add("Recipes", this.lookupResultsToJson(ops));
        }

        return obj;
    }

    private JsonArray lookupResultsToJson(RegistryOps<?> ops)
    {
        JsonArray arr = new JsonArray();

        for (NetworkRecipeId id : this.recipeRequirements.keySet())
        {
            List<Ingredient> requires = this.recipeRequirements.get(id);
            RecipeBookCategory category = this.recipeCategory.get(id);
            RecipeBookUtils.Type type = this.recipeTypes.get(id);
            JsonObject obj = new JsonObject();

            obj.add("NetworkId", new JsonPrimitive(id.index()));
            obj.add("Category", new JsonPrimitive(RecipeBookUtils.getRecipeCategoryId(category)));
            obj.add("Type", new JsonPrimitive(type.name()));

            JsonArray itemArr = new JsonArray();
            for (Ingredient ing : requires)
            {
                itemArr.add((JsonElement) Ingredient.CODEC.encodeStart(ops, ing).getPartialOrThrow());
            }
            obj.add("Ingredients", itemArr);

            JsonArray outputArr = new JsonArray();
            for (MaterialListJsonBase jsonEntry : this.requirements)
            {
                outputArr.add(jsonEntry.toJson(ops));
            }
            obj.add("Requirements", outputArr);

            arr.add(obj);
        }

        return arr;
    }
}
