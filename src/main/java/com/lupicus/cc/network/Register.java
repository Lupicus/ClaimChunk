package com.lupicus.cc.network;

import net.minecraft.network.codec.StreamCodec;

public class Register
{
	public static void initPackets()
	{
		Network.INSTANCE.play()
			.serverbound()
				.add(ClaimUpdatePacket.class, StreamCodec.ofMember(ClaimUpdatePacket::encode, ClaimUpdatePacket::decode), ClaimUpdatePacket::processPacket)
			.clientbound()
				.add(ClaimScreenPacket.class, StreamCodec.ofMember(ClaimScreenPacket::encode, ClaimScreenPacket::decode), ClaimScreenPacket::processPacket)
				.add(ChangeBlockPacket.class, StreamCodec.ofMember(ChangeBlockPacket::encode, ChangeBlockPacket::decode), ChangeBlockPacket::processPacket)
			.build();
	}
}
