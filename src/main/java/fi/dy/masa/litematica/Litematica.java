package fi.dy.masa.litematica;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.fabricmc.api.ModInitializer;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.world.fallback.SchematicBiomeTypes;
import fi.dy.masa.litematica.world.fallback.SchematicDimensionTypes;

public class Litematica implements ModInitializer
{
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public void onInitialize()
    {
        // Required to be here
        SchematicDimensionTypes.registerFallbackDimensions();
        SchematicBiomeTypes.registerFallbackBiomes();
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }

    public static void debugLog(String msg, Object... args)
    {
        if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
        {
            Litematica.logger.info(msg, args);
        }
    }
}
