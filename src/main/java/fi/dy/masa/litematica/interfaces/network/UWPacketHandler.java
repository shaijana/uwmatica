
package fi.dy.masa.litematica.interfaces.network;

import fi.dy.masa.litematica.Reference;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

public class UWPacketHandler {

    public static final Identifier UW_IDENTIFIER = new Identifier(Reference.Mod_aID, Reference.Mod_aID);

    public static void init()
    {
        ClientPlayNetworking.registerGlobalReceiver(UW_IDENTIFIER, ((minecraftClient, clientPlayNetworkHandler, packetByteBuf, packetSender) -> {
        }));
      }
}