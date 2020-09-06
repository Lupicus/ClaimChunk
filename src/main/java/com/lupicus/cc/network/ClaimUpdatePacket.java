package com.lupicus.cc.network;

import java.util.function.Supplier;

import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

public class ClaimUpdatePacket
{
	int cmd;
	boolean enabled;
	BlockPos pos;
	String accessList;

	public ClaimUpdatePacket(BlockPos pos)
	{
		cmd = 0;
		this.pos = pos;
	}

	public ClaimUpdatePacket(BlockPos pos, boolean enabled)
	{
		cmd = 1;
		this.pos = pos;
		this.enabled = enabled;
	}

	public ClaimUpdatePacket(BlockPos pos, String accessList) {
		cmd = 2;
		this.pos = pos;
		this.accessList = accessList;
	}

	public void encode(PacketBuffer buf)
	{
		buf.writeByte(cmd);
		buf.writeBlockPos(pos);
		if (cmd == 1)
			buf.writeBoolean(enabled);
		else if (cmd == 2)
			buf.writeString(accessList);
	}

	public static ClaimUpdatePacket readPacketData(PacketBuffer buf)
	{
		int cmd = buf.readByte();
		BlockPos pos = buf.readBlockPos();
		if (cmd == 1)
			return new ClaimUpdatePacket(pos, buf.readBoolean());
		else if (cmd == 2)
			return new ClaimUpdatePacket(pos, buf.readString(32767));
		return new ClaimUpdatePacket(pos);
	}

	public static void writePacketData(ClaimUpdatePacket msg, PacketBuffer buf)
	{
		msg.encode(buf);
	}

	public static void processPacket(ClaimUpdatePacket message, Supplier<NetworkEvent.Context> ctx)
	{
		ctx.get().enqueueWork(() -> {
			World world = ctx.get().getSender().world;
			TileEntity te = world.getTileEntity(message.pos);
			if (te instanceof ClaimTileEntity)
			{
				ClaimTileEntity cte = (ClaimTileEntity) te;
				if (message.cmd == 1)
				{
					ClaimBlock.enableBlock(world, message.pos, ctx.get().getSender());
				}
				else if (message.cmd == 2)
				{
					if (!cte.accessList.equals(message.accessList))
					{
						cte.accessList = message.accessList;
						cte.markDirty();
					}
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
