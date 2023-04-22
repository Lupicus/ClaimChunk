package com.lupicus.cc.network;

import java.util.function.Supplier;

import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

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

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeByte(cmd);
		buf.writeBlockPos(pos);
		if (cmd == 1)
			buf.writeBoolean(enabled);
		else if (cmd == 2)
		{
			buf.writeUtf(accessList);
			buf.writeUtf(modifyList);
		}
	}

	public static ClaimUpdatePacket readPacketData(FriendlyByteBuf buf)
	{
		int cmd = buf.readByte();
		BlockPos pos = buf.readBlockPos();
		if (cmd == 1)
			return new ClaimUpdatePacket(pos, buf.readBoolean());
		else if (cmd == 2)
			return new ClaimUpdatePacket(pos, buf.readUtf(32767), buf.readUtf(32767));
		return new ClaimUpdatePacket(pos);
	}

	public static void writePacketData(ClaimUpdatePacket msg, FriendlyByteBuf buf)
	{
		msg.encode(buf);
	}

	@SuppressWarnings("deprecation")
	public static void processPacket(ClaimUpdatePacket message, Supplier<NetworkEvent.Context> ctx)
	{
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			Level world = player.level;
			BlockEntity te = null;
			if (world.hasChunkAt(message.pos))
				te = world.getBlockEntity(message.pos);
			if (te instanceof ClaimTileEntity)
			{
				ClaimTileEntity cte = (ClaimTileEntity) te;
				if (!(player.getUUID().equals(cte.owner) || (player.hasPermissions(3) && player.isCreative())))
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
						cte.setChanged();
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
