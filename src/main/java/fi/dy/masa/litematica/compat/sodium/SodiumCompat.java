package fi.dy.masa.litematica.compat.sodium;

import fi.dy.masa.litematica.mixin.render.IMixinGameRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

@Deprecated(forRemoval = true)
public class SodiumCompat
{
    private static boolean hasSodium;
    private static boolean wasBlockOutlineEnabled;

    public static void checkForSodium()
    {
        hasSodium = FabricLoader.getInstance().isModLoaded("sodium");
    }

    public static boolean hasSodium() { return hasSodium; }

    public static void startBlockOutlineEnabled()
    {
        wasBlockOutlineEnabled = ((IMixinGameRenderer) Minecraft.getInstance().gameRenderer).litematica_isBlockOutlineEnabled();

        if (!wasBlockOutlineEnabled())
        {
            Minecraft.getInstance().gameRenderer.setRenderBlockOutline(true);
        }
    }

    public static void endBlockOutlineEnabled()
    {
        Minecraft.getInstance().gameRenderer.setRenderBlockOutline(wasBlockOutlineEnabled());
    }

    public static boolean wasBlockOutlineEnabled() { return wasBlockOutlineEnabled; }

    static
    {
        checkForSodium();
    }
}
