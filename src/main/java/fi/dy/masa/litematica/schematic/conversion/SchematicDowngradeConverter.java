package fi.dy.masa.litematica.schematic.conversion;

import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.litematica.Litematica;

public class SchematicDowngradeConverter
{
    public static NbtCompound downgradeEntity_to_1_20_4(NbtCompound oldEntity, int minecraftDataVersion, @Nonnull DynamicRegistryManager registryManager)
    {
        NbtCompound newEntity = new NbtCompound();
        Litematica.logger.warn("downgradeEntity_to_1_20_4(): oldEntity [{}]", oldEntity.toString());

        if (oldEntity.contains("id") == false)
        {
            return oldEntity;
        }
        for (String key : oldEntity.getKeys())
        {
            switch (key)
            {
                case "x" -> newEntity.putInt("x", oldEntity.getInt("x"));
                case "y" -> newEntity.putInt("y", oldEntity.getInt("y"));
                case "z" -> newEntity.putInt("z", oldEntity.getInt("z"));
                case "id" -> newEntity.putString("id", oldEntity.getString("id"));
                case "flower_pos" -> newEntity.put("FlowerPos", processFlowerPos(oldEntity, key));
                case "hive_pos" -> newEntity.put("HivePos", processFlowerPos(oldEntity, key));
                case "attributes" -> newEntity.put("Attributes", processAttributes(oldEntity.get(key), minecraftDataVersion));
                default -> newEntity.put(key, oldEntity.get(key));
            }
        }

        Litematica.logger.warn("downgradeEntity_to_1_20_4(): newEntity [{}]", newEntity.toString());
        return newEntity;
    }

    public static NbtCompound downgradeBlockEntity_to_1_20_4(NbtCompound oldTE, int minecraftDataVersion, @Nonnull DynamicRegistryManager registryManager)
    {
        NbtCompound newTE = new NbtCompound();
        Litematica.logger.warn("downgradeBlockEntity_to_1_20_4(): oldTE: [{}]", oldTE.toString());

        if (oldTE.contains("id") == false)
        {
            oldTE.copyFrom(SchematicConversionMaps.checkForIdTag(oldTE));
        }
        for (String key : oldTE.getKeys())
        {
            switch (key)
            {
                case "x" -> newTE.putInt("x", oldTE.getInt("x"));
                case "y" -> newTE.putInt("y", oldTE.getInt("y"));
                case "z" -> newTE.putInt("z", oldTE.getInt("z"));
                case "id" -> newTE.putString("id", oldTE.getString("id"));
                case "Items" -> newTE.put("Items", processItemsTag(oldTE.getList("Items", Constants.NBT.TAG_COMPOUND), minecraftDataVersion, registryManager));
                case "patterns" -> newTE.put("Patterns", processBannerPatterns(oldTE.get(key)));
                case "profile" -> newTE.put("SkullOwner", processSkullProfile(oldTE.get(key)));
                case "flower_pos" -> newTE.put("FlowerPos", processFlowerPos(oldTE, key));
                case "bees" -> newTE.put("Bees", processBeesTag(oldTE.get(key), minecraftDataVersion, registryManager));
                case "item" -> newTE.put("item", processDecoratedPot(oldTE.get(key), minecraftDataVersion, registryManager));
                case "ticks_since_song_started" ->
                {
                    newTE.putLong("RecordStartTick", 0L);
                    newTE.putLong("TickCount", oldTE.getLong(key));
                    newTE.putByte("IsPlaying", (byte) 0);
                }
                case "RecordItem" ->
                {
                    newTE.put("RecordItem", processRecordItem(oldTE.get(key), minecraftDataVersion, registryManager));
                }
                case "Book" ->
                {
                    newTE.put("Book", processBookTag(oldTE.get(key), minecraftDataVersion, registryManager));
                }
                default -> newTE.put(key, oldTE.get(key));
            }
        }

        Litematica.logger.warn("downgradeBlockEntity_to_1_20_4(): newTE: [{}]", newTE.toString());
        return newTE;
    }

    private static NbtList processItemsTag(NbtList oldItems, int minecraftDataVersion, @Nonnull DynamicRegistryManager registryManager)
    {
        NbtList newItems = new NbtList();

        for (int i = 0; i < oldItems.size(); i++)
        {
            NbtCompound itemEntry = oldItems.getCompound(i);
            NbtCompound newEntry = new NbtCompound();

            if (itemEntry.contains("id") == false)
            {
                continue;
            }
            newEntry.putString("id", itemEntry.getString("id"));
            if (itemEntry.contains("count"))
            {
                newEntry.putByte("Count", (byte) itemEntry.getInt("count"));
            }
            if (itemEntry.contains("Slot"))
            {
                newEntry.putByte("Slot", itemEntry.getByte("Slot"));
            }
            if (itemEntry.contains("components"))
            {
                newEntry.put("tag", processComponentsTag(itemEntry.getCompound("components"), itemEntry.getString("id"), minecraftDataVersion, registryManager));
            }

            newItems.add(newEntry);
        }

        return newItems;
    }

    private static NbtList processItemsTag_Nested(NbtList oldItems, int minecraftDataVersion, @Nonnull DynamicRegistryManager registryManager)
    {
        NbtList newItems = new NbtList();

        for (int i = 0; i < oldItems.size(); i++)
        {
            NbtCompound itemEntry = oldItems.getCompound(i);
            NbtCompound newEntry = new NbtCompound();

            int slotNum = itemEntry.getInt("slot");
            NbtCompound itemSlot = itemEntry.getCompound("item");

            if (itemSlot.contains("id") == false)
            {
                continue;
            }
            newEntry.putString("id", itemSlot.getString("id"));
            if (itemSlot.contains("count"))
            {
                newEntry.putByte("Count", (byte) itemSlot.getInt("count"));
            }
            newEntry.putByte("Slot", (byte) slotNum);

            if (itemSlot.contains("components"))
            {
                newEntry.put("tag", processComponentsTag(itemSlot.getCompound("components"), itemSlot.getString("id"), minecraftDataVersion, registryManager));
            }

            newItems.add(newEntry);
        }

        return newItems;
    }

    private static NbtElement processDecoratedPot(NbtElement oldPot, int minecraftDataVersion, @Nonnull DynamicRegistryManager registryManager)
    {
        NbtCompound oldNbt = (NbtCompound) oldPot;
        NbtCompound newNbt = new NbtCompound();

        for (String key : oldNbt.getKeys())
        {
            switch (key)
            {
                case "id" ->
                {
                    newNbt.putString("id", oldNbt.getString("id"));
                }
                case "count" ->
                {
                    newNbt.putByte("Count", (byte) oldNbt.getInt("count"));
                }
                case "components" ->
                {
                    newNbt.put("tag", processComponentsTag(oldNbt.getCompound("components"), oldNbt.getString("id"), minecraftDataVersion, registryManager));
                }
            }
        }

        return newNbt;
    }

    private static NbtCompound processComponentsTag(NbtCompound nbt, String itemId, int minecraftDataVersion, @Nonnull DynamicRegistryManager registryManager)
    {
        NbtCompound outNbt = new NbtCompound();
        NbtCompound beNbt = new NbtCompound();
        NbtCompound dispNbt = new NbtCompound();

        for (String key : nbt.getKeys())
        {
            switch (key)
            {
                case "minecraft:banner_patterns" ->
                {
                    beNbt.put("Patterns", processBannerPatterns(nbt.get(key)));
                    beNbt.putString("id", "minecraft:banner");
                }
                case "minecraft:profile" ->
                {
                    outNbt.put("SkullOwner", processSkullProfile(nbt.get(key)));
                }
                case "minecraft:container" ->
                {
                    beNbt.put("Items", processItemsTag_Nested(nbt.getList(key, Constants.NBT.TAG_COMPOUND), minecraftDataVersion, registryManager));
                    beNbt.putString("id", itemId);
                }
                case "minecraft:bundle_contents" ->
                {
                    outNbt.put("Items", processItemsTag(nbt.getList(key, Constants.NBT.TAG_COMPOUND), minecraftDataVersion, registryManager));
                }
                case "minecraft:custom_name" ->
                {
                    dispNbt.putString("Name", processCustomNameTag(nbt.getString(key), registryManager));
                }
                case "minecraft:custom_data" ->
                {
                    NbtCompound origData = (NbtCompound) nbt.get(key);

                    for (String keyData : origData.getKeys())
                    {
                        outNbt.put(keyData, origData.get(keyData));
                    }
                }
                case "minecraft:custom_model_data" ->
                {
                    outNbt.putInt("CustomModelData", nbt.getInt(key));
                }
                case "minecraft:written_book_content" ->
                {
                    NbtCompound bookNbt = nbt.getCompound(key);
                    bookNbt = processBookContent(bookNbt, minecraftDataVersion, registryManager);
                    for (String bookKey : bookNbt.getKeys())
                    {
                        outNbt.put(bookKey, bookNbt.get(bookKey));
                    }
                }
                case "minecraft:lore" ->
                {
                    dispNbt.put("Lore", nbt.get(key));
                }
                case "minecraft:bees" ->
                {
                    beNbt.put("Bees", processBeesTag(nbt.get(key), minecraftDataVersion, registryManager));
                }
                case "minecraft:block_state" ->
                {
                    outNbt.put("BlockStateTag", processBlockState(nbt.get(key)));
                }
            }
        }
        if (beNbt.isEmpty() == false)
        {
            outNbt.put("BlockEntityTag", beNbt);
        }
        if (dispNbt.isEmpty() == false)
        {
            outNbt.put("display", dispNbt);
        }

        return outNbt;
    }

    private static String processCustomNameTag(String nameTag, @Nonnull DynamicRegistryManager registryManager)
    {
        // Sometimes this is missing the 'text' designation ?
        Text customName = Text.Serialization.fromJson(nameTag, registryManager);
        String newCustomName = Text.Serialization.toJsonString(customName, registryManager);
        return newCustomName;
    }

    private static NbtElement processBlockState(NbtElement bsTag)
    {
        NbtCompound oldBS = (NbtCompound) bsTag;
        NbtCompound newBS = new NbtCompound();

        for (String key : oldBS.getKeys())
        {
            newBS.put(key, oldBS.get(key));
        }

        return bsTag;
    }

    private static NbtElement processRecordItem(NbtElement itemIn, int minecraftDataVersion, @Nonnull DynamicRegistryManager registryManager)
    {
        NbtCompound oldRecord = (NbtCompound) itemIn;
        NbtCompound recordOut = new NbtCompound();

        recordOut.putString("id", oldRecord.getString("id"));
        recordOut.putByte("Count", (byte) oldRecord.getInt("count"));

        if (oldRecord.contains("components"))
        {
            recordOut.put("tag", processComponentsTag(oldRecord.getCompound("components"), oldRecord.getString("id"), minecraftDataVersion, registryManager));
        }

        return recordOut;
    }

    private static NbtElement processBookTag(NbtElement bookNbt, int minecraftDataVersion, DynamicRegistryManager registryManager)
    {
        NbtCompound oldBook = (NbtCompound) bookNbt;
        NbtCompound newBook = new NbtCompound();

        newBook.putString("id", oldBook.getString("id"));
        newBook.putByte("Count", (byte) oldBook.getInt("count"));

        if (oldBook.contains("components"))
        {
            newBook.put("tag", processComponentsTag(oldBook.getCompound("components"), oldBook.getString("id"), minecraftDataVersion, registryManager));
        }

        return newBook;
    }

    // TODO
    private static NbtCompound processBookContent(NbtCompound bookNbt, int minecraftDataVersion, DynamicRegistryManager registryManager)
    {
        NbtCompound newBook = new NbtCompound();

        if (bookNbt.contains("author"))
        {
            newBook.putString("author", bookNbt.getString("author"));
        }
        if (bookNbt.contains("title"))
        {
            NbtCompound title = bookNbt.getCompound("title");
            newBook.putString("title", title.getString("raw"));
        }

        NbtList pages = bookNbt.getList("pages", Constants.NBT.TAG_COMPOUND);
        NbtList newPages = new NbtList();

        for (int i = 0; i < pages.size(); i++)
        {
            NbtCompound page = pages.getCompound(i);
            newPages.add(page.get("raw"));
        }
        if (newPages.isEmpty() == false)
        {
            newBook.put("pages", newPages);
        }

        return newBook;
    }

    private static NbtElement processBannerPatterns(NbtElement oldPatterns)
    {
        NbtList newList = new NbtList();
        NbtList oldList = (NbtList) oldPatterns;

        for (int i = 0; i < oldList.size(); i++)
        {
            NbtCompound oldEntry = oldList.getCompound(i);
            NbtCompound newEntry = new NbtCompound();
            String color = oldEntry.getString("color");
            String pattern = oldEntry.getString("pattern");
            DyeColor dye = DyeColor.byName(color, DyeColor.WHITE);

            newEntry.putString("Pattern", convertBannerPattern(pattern));
            newEntry.putInt("Color", dye.getId());

            newList.add(newEntry);
        }

        return newList;
    }

    private static String convertBannerPattern(String patternId)
    {
        String key = "";
        
        switch (patternId)
        {
            case "minecraft:base" -> key = "b";
            case "minecraft:square_bottom_left" -> key = "bl";
            case "minecraft:square_bottom_right" -> key = "br";
            case "minecraft:square_top_left" -> key = "tl";
            case "minecraft:square_top_right" -> key = "tr";
            case "minecraft:stripe_bottom" -> key = "bs";
            case "minecraft:stripe_top" -> key = "ts";
            case "minecraft:stripe_left" -> key = "ls";
            case "minecraft:stripe_right" -> key = "rs";
            case "minecraft:stripe_center" -> key = "cs";
            case "minecraft:stripe_middle" -> key = "ms";
            case "minecraft:stripe_downright" -> key = "drs";
            case "minecraft:stripe_downleft" -> key = "dls";
            case "minecraft:small_stripes" -> key = "ss";
            case "minecraft:cross" -> key = "cr";
            case "minecraft:straight_cross" -> key = "sc";
            case "minecraft:triangle_bottom" -> key = "bt";
            case "minecraft:triangle_top" -> key = "tt";
            case "minecraft:triangles_bottom" -> key = "bts";
            case "minecraft:triangles_top" -> key = "tts";
            case "minecraft:diagonal_left" -> key = "ld";
            case "minecraft:diagonal_up_right" -> key = "rd";
            case "minecraft:diagonal_up_left" -> key = "lud";
            case "minecraft:diagonal_right" -> key = "rud";
            case "minecraft:circle" -> key = "mc";
            case "minecraft:rhombus" -> key = "mr";
            case "minecraft:half_vertical" -> key = "vh";
            case "minecraft:half_horizontal" -> key = "hh";
            case "minecraft:half_vertical_right" -> key = "vhr";
            case "minecraft:half_horizontal_bottom" -> key = "hhb";
            case "minecraft:border" -> key = "bo";
            case "minecraft:curly_border" -> key = "cbo";
            case "minecraft:gradient" -> key = "gra";
            case "minecraft:gradient_up" -> key = "gru";
            case "minecraft:bricks" -> key = "bri";
            case "minecraft:globe" -> key = "glb";
            case "minecraft:creeper" -> key = "cre";
            case "minecraft:skull" -> key = "sku";
            case "minecraft:flower" -> key = "flo";
            case "minecraft:mojang" -> key = "moj";
            case "minecraft:piglin" -> key = "pig";
            // Doesn't exist in 1.20.4
            //case "minecraft:flow" -> key = "flo";
            //case "minecraft:guster" -> key = "gus";
            default -> key = "b";
        }

        return key;
    }

    private static NbtElement processSkullProfile(NbtElement oldProfile)
    {
        NbtCompound profile = (NbtCompound) oldProfile;
        NbtCompound newProfile = new NbtCompound();
        String name = profile.getString("name");
        UUID uuid = profile.getUuid("id");

        newProfile.putString("Name", name);
        newProfile.putUuid("Id", uuid);

        NbtList properties = profile.getList("properties", Constants.NBT.TAG_COMPOUND);
        NbtCompound newProperties = new NbtCompound();

        for (int i = 0; i < properties.size(); i++)
        {
            NbtCompound property = properties.getCompound(i);
            String propName = property.getString("name");
            String propValue = property.getString("value");

            switch (propName)
            {
                case "textures" ->
                {
                    NbtList textures = new NbtList();
                    NbtCompound value = new NbtCompound();
                    value.putString("Value", propValue);
                    textures.add(value);
                    newProperties.put("textures", textures);
                }
            }
        }

        newProfile.put("Properties", newProperties);

        return newProfile;
    }

    private static NbtElement processFlowerPos(NbtCompound oldNbt, String key)
    {
        NbtCompound flowerOut = new NbtCompound();
        BlockPos flowerPos = NbtHelper.toBlockPos(oldNbt, key).orElse(null);

        if (flowerPos != null)
        {
            flowerOut.putInt("X", flowerPos.getX());
            flowerOut.putInt("Y", flowerPos.getY());
            flowerOut.putInt("Z", flowerPos.getZ());
        }

        return flowerOut;
    }

    private static NbtElement processBeesTag(NbtElement beesTag, int minecraftDataVersion, @Nonnull DynamicRegistryManager registryManager)
    {
        NbtList oldBees = (NbtList) beesTag;
        NbtList newBees = new NbtList();

        for (int i = 0; i < oldBees.size(); i++)
        {
            NbtCompound oldEntry = oldBees.getCompound(i);
            NbtCompound newEntry = new NbtCompound();

            newEntry.putInt("TicksInHive", oldEntry.getInt("ticks_in_hive"));
            newEntry.putInt("MinOccupationTicks", oldEntry.getInt("min_ticks_in_hive"));
            newEntry.put("EntityData", downgradeEntity_to_1_20_4(oldEntry.getCompound("entity_data"), minecraftDataVersion, registryManager));

            newBees.add(newEntry);
        }

        return newBees;
    }

    private static NbtElement processAttributes(NbtElement attrib, int minecraftDataVersion)
    {
        NbtList oldAttr = (NbtList) attrib;
        NbtList newAttr = new NbtList();

        for (int i = 0; i < oldAttr.size(); i++)
        {
            NbtCompound attrEntry = oldAttr.getCompound(i);
            NbtCompound newEntry = new NbtCompound();

            newEntry.putString("Name", attrEntry.getString("id"));
            newEntry.putDouble("Base", attrEntry.getDouble("base"));

            NbtList listEntry = attrEntry.getList("modifiers", Constants.NBT.TAG_COMPOUND);
            NbtList newMods = new NbtList();

            for (int y = 0; y < listEntry.size(); y++)
            {
                NbtCompound modEntry = listEntry.getCompound(y);
                NbtCompound newMod = new NbtCompound();

                newMod.putDouble("Amount", modEntry.getDouble("amount"));
                newMod.putString("Name", modiferIdToName(modEntry.getString("id")));
                newMod.putInt("Operation", modifierOperationToInt(modEntry.getString("operation")));
                newMods.add(newMod);
            }
            if (newMods.isEmpty() == false)
            {
                newEntry.put("Modifiers", newMods);
            }

            newAttr.add(newEntry);
        }

        return newAttr;
    }

    private static String modiferIdToName(String idIn)
    {
        switch (idIn)
        {
            case "minecraft:random_spawn_bonus" ->
            {
                return "Random spawn bonus";
            }
            default ->
            {
                return "";
            }
        }
    }

    private static int modifierOperationToInt(String op)
    {
        switch (op)
        {
            case "add_value" ->
            {
                return 0;
            }
            case "add_multiplied_base" ->
            {
                return 1;
            }
            case "add_multiplied_total" ->
            {
                return 2;
            }
            default ->
            {
                return 0;
            }
        }
    }
}
