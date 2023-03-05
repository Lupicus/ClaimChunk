package com.lupicus.cc.network;

import java.util.function.Supplier;

import com.lupicus.cc.Main;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public class ChangeBlockPacket
{
	BlockPos pos;
	BlockState state;

	public ChangeBlockPacket(BlockPos pos, BlockState state)
	{
		this.pos = pos;
		this.state = state;
	}

	public void encode(PacketBuffer buf)
	{
		buf.writeBlockPos(pos);
		buf.writeVarInt(Block.getStateId(state));
	}

	public static ChangeBlockPacket readPacketData(PacketBuffer buf)
	{
		BlockPos pos = buf.readBlockPos();
		BlockState state = Block.getStateById(buf.readVarInt());
		return new ChangeBlockPacket(pos, state);
	}

	public static void writePacketData(ChangeBlockPacket msg, PacketBuffer buf)
	{
		msg.encode(buf);
	}

	public static void processPacket(ChangeBlockPacket message, Supplier<NetworkEvent.Context> ctx)
	{
		ctx.get().enqueueWork(() -> {
			Main.proxy.changeBlock(message.pos, message.state);
		});
		ctx.get().setPacketHandled(true);
	}
}
