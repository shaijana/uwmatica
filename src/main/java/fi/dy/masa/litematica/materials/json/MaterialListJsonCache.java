package fi.dy.masa.litematica.materials.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;

import fi.dy.masa.malilib.util.game.RecipeBookUtils;
import fi.dy.masa.malilib.util.log.AnsiLogger;

/**
 * The idea with this cache is to flatten the JSON structure into a more human-digestible format,
 * and deduplicate multiple Crafting steps into one for each type of item; and then total them up.
 */
@ApiStatus.Experimental
public class MaterialListJsonCache
{
    private static final AnsiLogger LOGGER = new AnsiLogger(MaterialListJsonCache.class, true, true);
    private final List<Entry> entries;
    private final String GATHER_KEY = "GATHER";

    public MaterialListJsonCache()
    {
        this.entries = new ArrayList<>();
    }

    /**
     * Adds a new entry, but combine them if the Items match, and add to the total required.
     * @param input ()
     */
    public void putEntry(Entry input)
    {
        if (!this.entries.isEmpty())
        {
            RegistryEntry<Item> item = input.rawItem();
            final int total = input.total();
            List<Step> steps = input.steps();

            // Check for existing item matches
            for (int i = 0; i < this.entries.size(); i++)
            {
                Entry entry = this.entries.get(i);

                if (entry.rawItem().equals(item))
                {
                    // Combine steps
                    List<Step> entrySteps = entry.steps();
                    Entry newEntry = new Entry(item, (entry.total() + total), this.combineSteps(steps, entrySteps));

                    this.entries.set(i, newEntry);
                    return;
                }
            }
        }

        this.entries.add(input);
    }

    /**
     * Combines two-Step lists when the Item matches; while deduplicating by simply adding the total required.
     * @param left ()
     * @param right ()
     * @return ()
     */
    public List<Step> combineSteps(List<Step> left, List<Step> right)
    {
        List<Step> list = new ArrayList<>();
        List<Integer> ignores = new ArrayList<>();

        LOGGER.info("combineSteps: left [{}], right [{}]", left.size(), right.size());

        if (left.isEmpty() && !right.isEmpty())
        {
            return right;
        }

        for (int i = 0; i < left.size(); i++)
        {
            Step entry = left.get(i);
            RegistryEntry<Item> item = entry.stepItem();
            final int count = entry.count();
            boolean matched = false;

//            LOGGER.info("combineSteps: left[{}]: [{}]", i, item.getIdAsString());
            for (int j = 0; j < right.size(); j++)
            {
                Step otherEntry = right.get(j);

//                LOGGER.info("left[{}]: [{}] // right[{}]: [{}]", i, item.getIdAsString(), j, otherEntry.stepItem().getIdAsString());
                if (item.equals(otherEntry.stepItem()))
                {
                    Step newEntry = new Step(item, (count + otherEntry.count()), entry.type(), entry.category(), entry.networkId());
                    newEntry.debug();
                    ignores.add(j);
                    list.add(newEntry);
                    matched = true;
                }
            }

            if (!matched)
            {
                list.add(entry);
            }
        }

        for (int i = 0; i < right.size(); i++)
        {
            Step entry = right.get(i);
            LOGGER.info("ignores: // right[{}]: [{}] (Contains: {})", i, entry.stepItem().getIdAsString(), ignores.contains(i));

            if (!ignores.contains(i))
            {
                list.add(entry);
            }
        }

        LOGGER.info("combineSteps: combined [{}]", list.size());
        return list;
    }

    public boolean isEmpty() { return this.entries.isEmpty(); }

    public int size() { return this.entries.size(); }

    public List<Entry> getEntries() { return this.entries; }

    public Iterable<Entry> iterator() { return Iterables.concat(this.entries); }

    public Stream<Entry> stream() { return this.entries.stream(); }

    public void clear()
    {
        this.entries.clear();
    }

    public Pair<Step, List<Step>> buildStepsBase(MaterialListJsonBase base, List<Step> lastSteps)
    {
        RegistryEntry<Item> resultItem = base.getInput();
        final int total = base.getCount();

        List<MaterialListJsonBase> requirements = new ArrayList<>();
        Step furnaceStep;
        Step stonecutterStep;
        Step recipeStep;
        Step finalStep = null;

//        Step baseStep = new Step(resultItem, total, RecipeBookUtils.Type.UNKNOWN, "RESULT", -1);
//        LOGGER.debug("buildStepsEntryEach: (Basic) from resultItem [{}]", resultItem.getIdAsString());
//        baseStep.debug();
//        lastSteps.addFirst(baseStep);

        LOGGER.debug("buildStepsBase: resultItem: [{}], count [{}], lastSteps [{}]", resultItem.getIdAsString(), total, lastSteps.size());

        if (base.getMaterialsFurnace() != null)
        {
            Pair<Step, List<MaterialListJsonBase>> pair = this.buildStepsEntryEach(resultItem, base.getMaterialsFurnace(), RecipeBookUtils.Type.FURNACE);

            if (!pair.getRight().isEmpty())
            {
                requirements.addAll(pair.getRight());
            }

            furnaceStep = pair.getLeft();
            lastSteps.addFirst(furnaceStep);
        }
        if (base.getMaterialsStonecutter() != null)
        {
            Pair<Step, List<MaterialListJsonBase>> pair = this.buildStepsEntryEach(resultItem, base.getMaterialsStonecutter(), RecipeBookUtils.Type.STONECUTTER);

            if (!pair.getRight().isEmpty())
            {
                requirements.addAll(pair.getRight());
            }

            stonecutterStep = pair.getLeft();
            lastSteps.addFirst(stonecutterStep);
        }
        if (base.getMaterialsCrafting() != null)
        {
            Pair<Step, List<MaterialListJsonBase>> pair = this.buildStepsEntryEach(resultItem, base.getMaterialsCrafting(), RecipeBookUtils.Type.SHAPELESS);

            if (!pair.getRight().isEmpty())
            {
                requirements.addAll(pair.getRight());
            }

            recipeStep = pair.getLeft();
            lastSteps.addFirst(recipeStep);
        }
        if (base.getMaterialsRemaining() != null)
        {
            // Final Step
            Pair<Step, List<MaterialListJsonBase>> pair = this.buildStepsEntryEach(resultItem, base.getMaterialsRemaining(), RecipeBookUtils.Type.UNKNOWN);

            if (!pair.getRight().isEmpty())
            {
                requirements.addAll(pair.getRight());
            }

            finalStep = pair.getLeft();
            lastSteps.addFirst(finalStep);
        }

        if (!requirements.isEmpty())
        {
            List<Step> list = new ArrayList<>();

            for (MaterialListJsonBase baseEach : requirements)
            {
                LOGGER.debug("buildStepsBase: buildStepsBaseEach (Requirements) PRE steps size [{}]", lastSteps.size());
                Pair<Step, List<Step>> pair = this.buildStepsBaseEach(baseEach, lastSteps);

                if (pair.getRight() != null && !pair.getRight().isEmpty())
                {
                    list = this.combineSteps(list, pair.getRight());
                }

                LOGGER.debug("buildStepsBase: buildStepsBaseEach (Requirements) POST steps size [{}], list size [{}]", lastSteps.size(), list.size());

                if (pair.getLeft() != null)
                {
                    // Final Step
                    list.add(pair.getLeft());
                    lastSteps = this.combineSteps(list, lastSteps);
                    Entry entryOut = new Entry(resultItem, total, lastSteps);
                    LOGGER.debug("buildStepsBase: Entry (Requirements) -->");
                    entryOut.debug();
                    this.putEntry(entryOut);
                    return Pair.of(null, List.of());
                }
            }
        }
        else if (finalStep != null)
        {
            Entry entryOut = new Entry(resultItem, total, lastSteps);
            LOGGER.debug("buildStepsBase: Entry (No-Requirements) -->");
            entryOut.debug();
            this.putEntry(entryOut);
            return Pair.of(null, List.of());
        }

        LOGGER.debug("buildStepsBase: No-Entry (Default) steps size [{}] -->", lastSteps.size());
        return Pair.of(null, List.of());
    }

    public Pair<Step, List<Step>> buildStepsBaseEach(MaterialListJsonBase base, List<Step> lastSteps)
    {
        return this.buildStepsBase(base, lastSteps);
    }

    public Pair<Step, List<MaterialListJsonBase>> buildStepsEntryEach(RegistryEntry<Item> resultItem, MaterialListJsonEntry materials, RecipeBookUtils.Type typeIn)
    {
        RegistryEntry<Item> stepItem = materials.getInputItem();
        final int stepCount = materials.getTotal();

        LOGGER.debug("buildStepsEntryEach: resultItem: [{}], typeIn [{}]", resultItem.getIdAsString(), typeIn.name());

        if (materials.hasOutput())
        {
            NetworkRecipeId stepNetworkId = materials.getPrimaryId();
//            HashMap<NetworkRecipeId, List<Ingredient>> stepIngs = materials.getRecipeRequirements();
            HashMap<NetworkRecipeId, RecipeBookCategory> stepCats = materials.getRecipeCategory();
            HashMap<NetworkRecipeId, RecipeBookUtils.Type> stepTypes = materials.getRecipeTypes();

//            List<MaterialListJsonBase> requirements = materials.getRequirements();
//            List<Ingredient> ings = stepIngs.get(stepNetworkId);
            RecipeBookCategory category = stepCats.get(stepNetworkId);
            RecipeBookUtils.Type type = stepTypes.get(stepNetworkId);

            Step stepOut = new Step(stepItem, stepCount, type, RecipeBookUtils.getRecipeCategoryId(category), stepNetworkId.index());
            LOGGER.debug("buildStepsEntryEach: (Output) from resultItem [{}]", resultItem.getIdAsString());
            stepOut.debug();

            return Pair.of(
                    stepOut,
                    materials.getRequirements()
            );
        }

        Step stepOut = new Step(stepItem, stepCount, typeIn, GATHER_KEY, -1);
        LOGGER.debug("buildStepsEntryEach: (Basic) from resultItem [{}]", resultItem.getIdAsString());
        stepOut.debug();

        return Pair.of(
                stepOut,
                List.of()
        );
    }

    public void simplifyEntrySteps()
    {
        List<Step> otherSteps = new ArrayList<>();

        for (int i = 0; i < this.entries.size(); i++)
        {
            Entry entry = this.entries.get(i);
            List<Step> entrySteps = entry.steps();
            int entryCount = entry.total();
            LOGGER.debug("simplifyEntrySteps(): Entry[{}/{}]: steps [{}], otherSteps [{}]", i, entry.rawItem.getIdAsString(), entry.steps().size(), otherSteps.size());

            if (i == 0)
            {
                LOGGER.debug("simplifyEntrySteps(): Entry[{}/{}]: --> UPDATE OTHER STEPS", i, entry.rawItem.getIdAsString());
                otherSteps.addAll(entrySteps);
            }

            boolean updated = false;
            boolean found = false;
            List<Step> updatedSteps = new ArrayList<>();
            int updatedCount = entryCount;
            RegistryEntry<Item> prevStep = null;

            for (Step step : entrySteps)
            {
                if (step.stepItem().equals(entry.rawItem()) && Objects.equals(step.category(), GATHER_KEY))
                {
                    if (step.count() >= entryCount)
                    {
                        if (step.count() > entryCount)
                        {
                            updatedCount = step.count();
                            updated = true;
                        }

                        if (!found)
                        {
                            updatedSteps.add(step);
                            found = true;
                        }
                        else
                        {
                            // Ignore it
                            LOGGER.debug("simplifyEntrySteps(): Entry[{}/{}]: --> IGNORE STEP [Already matched/found]", i, entry.rawItem.getIdAsString());
                            updated = true;
                        }
                    }
                    else
                    {
                        // Ignore it
                        LOGGER.debug("simplifyEntrySteps(): Entry[{}/{}]: --> IGNORE STEP [{} < {}]", i, entry.rawItem.getIdAsString(), step.count(), entryCount);
                        updated = true;
                    }
                }
                else
                {
                    if (prevStep != null && prevStep.equals(step.stepItem()))
                    {
                        LOGGER.debug("simplifyEntrySteps(): Entry[{}/{}]: --> IGNORE STEP [Equals Previous Item]", i, entry.rawItem.getIdAsString());
                        updated = true;
                    }
                    else
                    {
                        updatedSteps.add(step);
                    }
                }

                prevStep = step.stepItem();
            }

            if (updated)
            {
                LOGGER.debug("simplifyEntrySteps(): Entry[{}/{}]: --> SIMPLIFY STEPS [{} -> {}]", i, entry.rawItem.getIdAsString(), entrySteps.size(), updatedSteps.size());
                Entry newEntry = new Entry(entry.rawItem(), updatedCount, updatedSteps);
                this.entries.set(i, newEntry);
                entrySteps.clear();
                entrySteps.addAll(updatedSteps);

                if (i == 0)
                {
                    otherSteps.clear();
                    otherSteps.addAll(updatedSteps);
                }
            }

            if (i == 0)
            {
                continue;
            }

            if (!otherSteps.isEmpty() && this.compareSteps(entrySteps, otherSteps))
            {
                LOGGER.debug("simplifyEntrySteps(): Entry[{}/{}]: --> REPLACE STEPS", i, entry.rawItem.getIdAsString());
                this.entries.set(i, new Entry(entry.rawItem(), entry.total(), List.of()));
            }
            else
            {
                LOGGER.debug("simplifyEntrySteps(): Entry[{}/{}]: --> NEXT", i, entry.rawItem.getIdAsString());
                otherSteps.clear();
                otherSteps.addAll(entrySteps);
            }
        }
    }

    private boolean compareSteps(List<Step> left, List<Step> right)
    {
        if (left.size() != right.size())
        {
            return false;
        }

        int lCount = 0;

        for (Step entry : left)
        {
            if (right.contains(entry))
            {
                lCount++;
            }
        }

        int rCount = 0;

        for (Step entry : right)
        {
            if (left.contains(entry))
            {
                rCount++;
            }
        }

        return lCount == rCount;
    }

    public record Step(RegistryEntry<Item> stepItem, Integer count, RecipeBookUtils.Type type, String category, Integer networkId)
    {
        public static final Codec<Step> CODEC = RecordCodecBuilder.create(
                inst -> inst.group(
                        Item.ENTRY_CODEC.fieldOf("StepItem").forGetter(get -> get.stepItem),
                        PrimitiveCodec.INT.fieldOf("Count").forGetter(get -> get.count),
                        RecipeBookUtils.Type.CODEC.fieldOf("RecipeType").forGetter(get -> get.type),
                        PrimitiveCodec.STRING.fieldOf("RecipeCategory").forGetter(get -> get.category),
                        PrimitiveCodec.INT.fieldOf("RecipeId").forGetter(get -> get.networkId)
                ).apply(inst, Step::new)
        );

        public void debug()
        {
            LOGGER.debug("Step(): item: [{}], count: [{}], type: [{}], category: [{}], id: [{}]",
                         this.stepItem().getIdAsString(), this.count(),
                         this.type().name(), this.category(), this.networkId());
        }
    }

    public record Entry(RegistryEntry<Item> rawItem, Integer total, List<Step> steps)
    {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
                inst -> inst.group(
                        Item.ENTRY_CODEC.fieldOf("RawItem").forGetter(get -> get.rawItem),
                        PrimitiveCodec.INT.fieldOf("Total").forGetter(get -> get.total),
                        Codec.list(Step.CODEC).fieldOf("Steps").forGetter(get -> get.steps)
                ).apply(inst, Entry::new)
        );

        public void debug()
        {
            LOGGER.debug("Entry(): item: [{}], total: [{}], STEPS -->",
                         this.rawItem().getIdAsString(), this.total());

            for (Step step : this.steps())
            {
                step.debug();
            }
        }
    }

    public JsonElement toJson(RegistryOps<?> ops)
    {
        JsonArray arr = new JsonArray();

        if (!this.isEmpty())
        {
            this.entries.forEach(
                    (entry) ->
                            arr.add((JsonElement) Entry.CODEC.encodeStart(ops, entry).getPartialOrThrow())
            );
        }

        return arr;
    }
}
