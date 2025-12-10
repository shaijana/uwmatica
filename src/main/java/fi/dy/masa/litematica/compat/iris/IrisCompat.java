package fi.dy.masa.litematica.compat.iris;

import java.util.Objects;
import net.irisshaders.iris.api.v0.IrisApi;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;

public class IrisCompat
{
	private static final String SODIUM_ID = "sodium";
	private static final String IRIS_ID = "iris";

	private static boolean isSodiumLoaded = false;
	private static boolean isIrisLoaded = false;
	private static String sodiumVersion = "";
	private static String irisVersion = "";

	static
	{
		FabricLoader.getInstance().getAllMods().stream().toList().forEach((mc ->
		{
			ModMetadata meta = mc.getMetadata();

			if (Objects.equals(meta.getId(), SODIUM_ID))
			{
				sodiumVersion = meta.getVersion().getFriendlyString();
				isSodiumLoaded = true;
			}
			else if (Objects.equals(meta.getId(), IRIS_ID))
			{
				irisVersion = meta.getVersion().getFriendlyString();
				isIrisLoaded = true;
			}
		}));

//		Litematica.debugLog("Sodium: [{}], Iris: [{}]", isSodiumLoaded ? sodiumVersion : "N/F", isIrisLoaded ? irisVersion : "N/F");
	}

	public static boolean hasIris()
	{
		return isSodiumLoaded && isIrisLoaded;
	}

	public static boolean isShaderActive()
	{
		if (hasIris())
		{
			return IrisApi.getInstance().isShaderPackInUse();
		}

		return false;
	}

	public static boolean isShadowPassActive()
	{
		if (hasIris())
		{
			return IrisApi.getInstance().isRenderingShadowPass();
		}

		return false;
	}

	public static void registerPipelines()
	{
		if (hasIris())
		{
//
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.SOLID_MASA, IrisProgram.TERRAIN_SOLID);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.CUTOUT_MASA, IrisProgram.TERRAIN_CUTOUT);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.CUTOUT_MIPPED_MASA, IrisProgram.TERRAIN_CUTOUT);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.TRANSLUCENT_MASA, IrisProgram.TRANSLUCENT);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.TRIPWIRE_MASA, IrisProgram.TRANSLUCENT);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.WIREFRAME_MASA, IrisProgram.LINES);
//
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.SOLID_MASA_OFFSET, IrisProgram.TERRAIN_SOLID);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.CUTOUT_MASA_OFFSET, IrisProgram.TERRAIN_CUTOUT);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.CUTOUT_MIPPED_MASA_OFFSET, IrisProgram.TERRAIN_CUTOUT);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.TRANSLUCENT_MASA_OFFSET, IrisProgram.TRANSLUCENT);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.TRIPWIRE_MASA_OFFSET, IrisProgram.TRANSLUCENT);
//			IrisApi.getInstance().assignPipeline(LitematicaPipelines.WIREFRAME_MASA_OFFSET, IrisProgram.LINES);

			// todo fix Iris
//			IrisApi.getInstance().assignPipeline(RenderPipelines.SOLID_TERRAIN, IrisProgram.TERRAIN_SOLID);
//			IrisApi.getInstance().assignPipeline(RenderPipelines.CUTOUT_TERRAIN, IrisProgram.TERRAIN_CUTOUT);
//			IrisApi.getInstance().assignPipeline(RenderPipelines.TRANSLUCENT_TERRAIN, IrisProgram.TRANSLUCENT);
//			IrisApi.getInstance().assignPipeline(RenderPipelines.TRIPWIRE_TERRAIN, IrisProgram.TRANSLUCENT);
		}
	}
}
