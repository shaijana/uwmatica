
package fi.dy.masa.litematica.interfaces.network;

import fi.dy.masa.litematica.Reference;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class UWPacketHandler {

    public static final Identifier UW_IDENTIFIER = Identifier.of(Reference.Mod_aID, Reference.Mod_aID);

    public static void init()
    {
        PayloadTypeRegistry.playC2S().register(UWPacket.PACKET_ID, UWPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(UWPacket.PACKET_ID, UWPacket.PACKET_CODEC);
        ClientPlayNetworking.registerGlobalReceiver(UWPacket.PACKET_ID, ((payload, context) -> {
        }));
      }

    private static class UWPacket implements CustomPayload {

        private static final UWPacket INSTANCE = new UWPacket();

        private static final CustomPayload.Id<UWPacket> PACKET_ID = new CustomPayload.Id<>(UW_IDENTIFIER);
        private static final PacketCodec<RegistryByteBuf, UWPacket> PACKET_CODEC = PacketCodec.unit(INSTANCE);

        @Override
        public Id<? extends CustomPayload> getId() {
            return null;
        }

    }

}

