package com.lupicus.cc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

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

	public static ChangeBlockPacket decode(FriendlyByteBuf buf)
	{
		BlockPos pos = buf.readBlockPos();
		BlockState state = Block.stateById(buf.readVarInt());
		return new ChangeBlockPacket(pos, state);
	}

	public static void processPacket(ChangeBlockPacket message, Context ctx)
	{
		ctx.enqueueWork(() -> {
			ClientHandler.changeBlock(message.pos, message.state);
		});
		ctx.setPacketHandled(true);
	}
}
