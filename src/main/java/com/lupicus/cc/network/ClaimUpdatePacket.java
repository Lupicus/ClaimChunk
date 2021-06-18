package com.lupicus.cc.network;

import java.util.function.Supplier;

import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.entity.player.ServerPlayerEntity;
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
	String modifyList;

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

	public ClaimUpdatePacket(BlockPos pos, String accessList, String modifyList)
	{
		cmd = 2;
		this.pos = pos;
		this.accessList = accessList;
		this.modifyList = modifyList;
	}

	public void encode(PacketBuffer buf)
	{
		buf.writeByte(cmd);
		buf.writeBlockPos(pos);
		if (cmd == 1)
			buf.writeBoolean(enabled);
		else if (cmd == 2)
		{
			buf.writeString(accessList);
			buf.writeString(modifyList);
		}
	}

	public static ClaimUpdatePacket readPacketData(PacketBuffer buf)
	{
		int cmd = buf.readByte();
		BlockPos pos = buf.readBlockPos();
		if (cmd == 1)
			return new ClaimUpdatePacket(pos, buf.readBoolean());
		else if (cmd == 2)
			return new ClaimUpdatePacket(pos, buf.readString(32767), buf.readString(32767));
		return new ClaimUpdatePacket(pos);
	}

	public static void writePacketData(ClaimUpdatePacket msg, PacketBuffer buf)
	{
		msg.encode(buf);
	}

	@SuppressWarnings("deprecation")
	public static void processPacket(ClaimUpdatePacket message, Supplier<NetworkEvent.Context> ctx)
	{
		ctx.get().enqueueWork(() -> {
			ServerPlayerEntity player = ctx.get().getSender();
			World world = player.world;
			TileEntity te = null;
			if (world.isBlockLoaded(message.pos))
				te = world.getTileEntity(message.pos);
			if (te instanceof ClaimTileEntity)
			{
				ClaimTileEntity cte = (ClaimTileEntity) te;
				if (!player.getUniqueID().equals(cte.owner) && !player.hasPermissionLevel(3))
					;  // ignore invalid packet from client
				else if (message.cmd == 1)
				{
					if (cte.owner != null)
						ClaimBlock.enableBlock(world, message.pos, player, cte.owner);
				}
				else if (message.cmd == 2)
				{
					boolean dirty = false;
					if (!cte.getAccess().equals(message.accessList))
					{
						cte.setAccess(message.accessList);
						dirty = true;
					}
					if (!cte.getModify().equals(message.modifyList))
					{
						cte.setModify(message.modifyList);
						dirty = true;
					}
					if (dirty)
						cte.markDirty();
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
