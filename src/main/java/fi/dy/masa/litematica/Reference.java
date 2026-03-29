package fi.dy.masa.litematica;

import net.minecraft.SharedConstants;

import fi.dy.masa.malilib.MaLiLibReference;
import fi.dy.masa.malilib.util.StringUtils;

public class Reference
{
    public static final String MOD_ID = "uwmatica"; //Shaijana
    public static final String MOD_NAME = "UWmatica"; //Shaijana
    public static final String MOD_VERSION = StringUtils.getModVersionString(MOD_ID);
    public static final String MC_VERSION = SharedConstants.getCurrentVersion().id();
    public static final String MOD_TYPE = "fabric";
    public static final String MOD_STRING = MOD_ID+"-"+MOD_TYPE+"-"+MC_VERSION+"-"+MOD_VERSION;
    public static final boolean LOCAL_DEBUG = false;
    public static final boolean DEBUG_MODE = isDebug();

    private static boolean isDebug()
    {
        return LOCAL_DEBUG || MaLiLibReference.DEBUG_MODE || MaLiLibReference.RUNNING_IN_IDE;
    }
}
