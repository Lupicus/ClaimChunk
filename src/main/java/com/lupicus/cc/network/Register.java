package com.lupicus.cc.network;

public class Register
{
	public static void initPackets()
	{
	    Network.registerMessage(ClaimUpdatePacket.class,
	    		ClaimUpdatePacket::writePacketData,
	    		ClaimUpdatePacket::readPacketData,
	    		ClaimUpdatePacket::processPacket);

	    Network.registerMessage(ClaimScreenPacket.class,
	    		ClaimScreenPacket::writePacketData,
	    		ClaimScreenPacket::readPacketData,
	    		ClaimScreenPacket::processPacket);
	}
}
