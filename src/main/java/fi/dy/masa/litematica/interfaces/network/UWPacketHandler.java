
package fi.dy.masa.litematica.interfaces.network;

import fi.dy.masa.litematica.Reference;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class UWPacketHandler {

    public static final Identifier UW_IDENTIFIER = Identifier.of(Reference.Mod_aID, Reference.Mod_aID);

    public static void init()
    {

        ClientPlayNetworking.registerGlobalReceiver(UWPacket.PACKET_ID, ((payload, context) -> {
        }));
      }

    private static class UWPacket implements CustomPayload {

        private static final CustomPayload.Id<UWPacket> PACKET_ID = new CustomPayload.Id<>(UW_IDENTIFIER);

        @Override
        public Id<? extends CustomPayload> getId() {
            return null;
        }

    }

}

