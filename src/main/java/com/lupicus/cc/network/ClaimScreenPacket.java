package com.lupicus.cc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

public class ClaimScreenPacket
{
	int cmd;
	BlockPos pos;

	public ClaimScreenPacket(BlockPos pos)
	{
		cmd = 1;
		this.pos = pos;
	}

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeByte(cmd);
		buf.writeBlockPos(pos);
	}

	public static ClaimScreenPacket readPacketData(FriendlyByteBuf buf)
	{
		@SuppressWarnings("unused")
		int cmd = buf.readByte();
		BlockPos pos = buf.readBlockPos();
		return new ClaimScreenPacket(pos);
	}

	public static void writePacketData(ClaimScreenPacket msg, FriendlyByteBuf buf)
	{
		msg.encode(buf);
	}

	public static void processPacket(ClaimScreenPacket message, Context ctx)
	{
		ctx.enqueueWork(() -> {
			ClientHandler.showClaimGui(message.pos);
		});
		ctx.setPacketHandled(true);
	}
}
