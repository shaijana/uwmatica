package fi.dy.masa.litematica.handler;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class AllowedFunctionsHandler {

	public final static ImmutableList<IConfigBase> ALLOWED_GENERIC_CONFIGS = ImmutableList.of(
			Configs.Generic.PLACEMENT_REPLACE_BEHAVIOR,
			Configs.Generic.PLACEMENT_RESTRICTION_WARN,
			Configs.Generic.CUSTOM_SCHEMATIC_BASE_DIRECTORY_ENABLED,
			Configs.Generic.CUSTOM_SCHEMATIC_BASE_DIRECTORY,
			Configs.Generic.BETTER_RENDER_ORDER,
			Configs.Generic.DEBUG_LOGGING,
			Configs.Generic.DATAFIXER_DEFAULT_SCHEMA,
			Configs.Generic.EXECUTE_REQUIRE_TOOL,
			Configs.Generic.FIX_CHEST_MIRROR,
			Configs.Generic.FIX_RAIL_ROTATION,
			Configs.Generic.FIX_STAIRS_MIRROR,
			Configs.Generic.GENERATE_LOWERCASE_NAMES,
			Configs.Generic.ITEM_USE_PACKET_CHECK_BYPASS,
			Configs.Generic.LAYER_MODE_DYNAMIC,
			Configs.Generic.LOAD_ENTIRE_SCHEMATICS,
			Configs.Generic.MATERIAL_LIST_IGNORE_STATE,
			Configs.Generic.RENDER_MATERIALS_IN_GUI,
			Configs.Generic.RENDER_THREAD_NO_TIMEOUT,
			Configs.Generic.SIGN_TEXT_PASTE,
			Configs.Generic.TOOL_ITEM,
			Configs.Generic.TOOL_ITEM_ENABLED
	);

	public static final List<IHotkey> ALLOWED_CONFIG_HOTKEYS = ImmutableList.of(
			Configs.Generic.ENTITY_DATA_SYNC
	);

	public final static ImmutableList<IConfigBase> ALLOWED_VISUALS_CONFIGS = ImmutableList.of(
			Configs.Visuals.ENABLE_RENDERING,
			Configs.Visuals.ENABLE_SCHEMATIC_RENDERING,
			Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS,
			Configs.Visuals.ENABLE_SCHEMATIC_FLUIDS,
			Configs.Visuals.ENABLE_SCHEMATIC_FAKE_LIGHTING,
			Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY,
			Configs.Visuals.IGNORE_EXISTING_FLUIDS,
			Configs.Visuals.IGNORE_EXISTING_BLOCKS,
			Configs.Visuals.IGNORABLE_EXISTING_BLOCKS,
			Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES,
			Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT,
			Configs.Visuals.RENDER_ENABLE_TRANSLUCENT_RESORTING,
			Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS,
			Configs.Visuals.RENDER_ERROR_MARKER_CONNECTIONS,
			Configs.Visuals.RENDER_ERROR_MARKER_SIDES,
			Configs.Visuals.RENDER_FAKE_LIGHTING_LEVEL,
			Configs.Visuals.RENDER_PLACEMENT_BOX_SIDES,
			Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX,
			Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX_SIDES,
			Configs.Visuals.RENDER_SCHEMATIC_ENTITIES,
			Configs.Visuals.RENDER_SCHEMATIC_TILE_ENTITIES,
			Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES,
			Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES,
			Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES,
			Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE,
			Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES,
			Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH,
			Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_DIFF_BLOCK,
			Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_EXTRA,
			Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_MISSING,
			Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK,
			Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_STATE,

			Configs.Visuals.GHOST_BLOCK_ALPHA,
			Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA,
			Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH,
			Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH
	);

	public final static ImmutableList<IConfigBase> ALLOWED_COLORS_CONFIGS = ImmutableList.of(
			Configs.Colors.MATERIAL_LIST_HUD_ITEM_COUNTS,
			Configs.Colors.SCHEMATIC_OVERLAY_COLOR_DIFF_BLOCK,
			Configs.Colors.SCHEMATIC_OVERLAY_COLOR_EXTRA,
			Configs.Colors.SCHEMATIC_OVERLAY_COLOR_MISSING,
			Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK,
			Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_STATE
	);

	public static final List<ConfigHotkey> ALLOWED_HOTKEYS = ImmutableList.of(
			Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE,
			Hotkeys.INVERT_OVERLAY_RENDER_STATE,
			Hotkeys.LAYER_MODE_NEXT,
			Hotkeys.LAYER_MODE_PREVIOUS,
			Hotkeys.LAYER_NEXT,
			Hotkeys.LAYER_PREVIOUS,
			Hotkeys.LAYER_SET_HERE,
			Hotkeys.OPEN_GUI_LOADED_SCHEMATICS,
			Hotkeys.OPEN_GUI_MAIN_MENU,
			Hotkeys.OPEN_GUI_MATERIAL_LIST,
			Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS,
			Hotkeys.OPEN_GUI_SCHEMATIC_PLACEMENTS,
			Hotkeys.OPEN_GUI_SCHEMATIC_VERIFIER,
			Hotkeys.OPEN_GUI_SETTINGS,
			Hotkeys.OPERATION_MODE_CHANGE_MODIFIER,
			Hotkeys.RENDER_INFO_OVERLAY,
			Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS,
			Hotkeys.RERENDER_SCHEMATIC,
			Hotkeys.SCHEMATIC_PLACEMENT_ROTATION,
			Hotkeys.SCHEMATIC_PLACEMENT_MIRROR,
			Hotkeys.TOGGLE_ALL_RENDERING,
			Hotkeys.TOGGLE_AREA_SELECTION_RENDERING,
			Hotkeys.TOGGLE_INFO_OVERLAY_RENDERING,
			Hotkeys.TOGGLE_OVERLAY_RENDERING,
			Hotkeys.TOGGLE_OVERLAY_OUTLINE_RENDERING,
			Hotkeys.TOGGLE_OVERLAY_SIDE_RENDERING,
			Hotkeys.TOGGLE_PLACEMENT_BOXES_RENDERING,
			Hotkeys.TOGGLE_PLACEMENT_RESTRICTION,
			Hotkeys.TOGGLE_SCHEMATIC_BLOCK_RENDERING,
			Hotkeys.TOGGLE_SCHEMATIC_RENDERING,
			Hotkeys.TOGGLE_SIGN_TEXT_PASTE,
			Hotkeys.TOGGLE_TRANSLUCENT_RENDERING,
			Hotkeys.TOGGLE_VERIFIER_OVERLAY_RENDERING,
			Hotkeys.TOOL_ENABLED_TOGGLE,
			Hotkeys.TOOL_SELECT_ELEMENTS,
			Hotkeys.UNLOAD_CURRENT_SCHEMATIC
	);

	private static final HashSet<Block> forbiddenBlocks = new HashSet<>(Arrays.asList(
			Blocks.DIAMOND_ORE,
			Blocks.IRON_ORE,
			Blocks.LAPIS_ORE,
			Blocks.COAL_ORE,
			Blocks.EMERALD_ORE,
			Blocks.GOLD_ORE,
			Blocks.NETHER_GOLD_ORE,
			Blocks.REDSTONE_ORE,
			Blocks.NETHER_QUARTZ_ORE,
			Blocks.DEEPSLATE_IRON_ORE,
			Blocks.COPPER_ORE,
			Blocks.DEEPSLATE_COPPER_ORE,
			Blocks.DEEPSLATE_GOLD_ORE,
			Blocks.DEEPSLATE_REDSTONE_ORE,
			Blocks.DEEPSLATE_EMERALD_ORE,
			Blocks.DEEPSLATE_LAPIS_ORE,
			Blocks.DEEPSLATE_DIAMOND_ORE,
			Blocks.ANCIENT_DEBRIS,
			Blocks.AMETHYST_BLOCK,
			Blocks.BUDDING_AMETHYST,
			Blocks.END_PORTAL_FRAME,
			Blocks.END_PORTAL,
			Blocks.SPAWNER,
			Blocks.SMALL_AMETHYST_BUD,
			Blocks.MEDIUM_AMETHYST_BUD,
			Blocks.LARGE_AMETHYST_BUD,
			Blocks.AMETHYST_CLUSTER,
			Blocks.POINTED_DRIPSTONE
	));

	public static boolean isAllowed(Block block) {
		return !forbiddenBlocks.contains(block);
	}

}
