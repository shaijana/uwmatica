package fi.dy.masa.litematica.network;

import fi.dy.masa.litematica.Reference;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import javax.annotation.Nonnull;

public class UWPacketHandler {

	private static final String IDENTIFIER_PATH = "main";
	private static final Identifier PACKET_IDENTIFIER = Identifier.fromNamespaceAndPath(Reference.MOD_ID, IDENTIFIER_PATH);

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(UWPacket.PACKET_ID, UWPacket.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(UWPacket.PACKET_ID, UWPacket.STREAM_CODEC);
		ClientPlayNetworking.registerGlobalReceiver(UWPacket.PACKET_ID, ((payload, context) -> {
		}));
	}

	private static class UWPacket implements CustomPacketPayload {

		private static final UWPacket INSTANCE = new UWPacket();

		private static final CustomPacketPayload.Type<UWPacket> PACKET_ID = new CustomPacketPayload.Type<>(PACKET_IDENTIFIER);
		private static final StreamCodec<FriendlyByteBuf, UWPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public @Nonnull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return PACKET_ID;
		}

	}

}
