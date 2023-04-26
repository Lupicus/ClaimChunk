package com.lupicus.cc.network;

import java.util.function.Supplier;

import com.lupicus.cc.Main;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

public class ChangeBlockPacket
{
	BlockPos pos;
	BlockState state;

	public ChangeBlockPacket(BlockPos pos, BlockState state)
	{
		this.pos = pos;
		this.state = state;
	}

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeBlockPos(pos);
		buf.writeVarInt(Block.getId(state));
	}

	public static ChangeBlockPacket readPacketData(FriendlyByteBuf buf)
	{
		BlockPos pos = buf.readBlockPos();
		BlockState state = Block.stateById(buf.readVarInt());
		return new ChangeBlockPacket(pos, state);
	}

	public static void writePacketData(ChangeBlockPacket msg, FriendlyByteBuf buf)
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
