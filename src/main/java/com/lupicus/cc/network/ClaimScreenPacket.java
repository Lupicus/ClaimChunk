package com.lupicus.cc.network;

import java.util.function.Supplier;

import com.lupicus.cc.Main;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public class ClaimScreenPacket
{
	int cmd;
	BlockPos pos;

	public ClaimScreenPacket(BlockPos pos)
	{
		cmd = 1;
		this.pos = pos;
	}

	public void encode(PacketBuffer buf)
	{
		buf.writeByte(cmd);
		buf.writeBlockPos(pos);
	}

	public static ClaimScreenPacket readPacketData(PacketBuffer buf)
	{
		@SuppressWarnings("unused")
		int cmd = buf.readByte();
		BlockPos pos = buf.readBlockPos();
		return new ClaimScreenPacket(pos);
	}

	public static void writePacketData(ClaimScreenPacket msg, PacketBuffer buf)
	{
		msg.encode(buf);
	}

	public static void processPacket(ClaimScreenPacket message, Supplier<NetworkEvent.Context> ctx)
	{
		ctx.get().enqueueWork(() -> {
			Main.proxy.showClaimGui(message.pos);
		});
		ctx.get().setPacketHandled(true);
	}
}
