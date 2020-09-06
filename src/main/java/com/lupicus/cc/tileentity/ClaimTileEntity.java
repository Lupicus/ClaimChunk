package com.lupicus.cc.tileentity;

import java.util.UUID;

import javax.annotation.Nullable;

import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.manager.ClaimManager;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;

public class ClaimTileEntity extends TileEntity
{
	public UUID owner;
	public String accessList = "";
	private boolean enabled = true; // for loading

	public ClaimTileEntity() {
		super(ModTileEntities.CLAIM_BLOCK);
	}

	@Override
	public void func_230337_a_(BlockState state, CompoundNBT nbt) { // read
		super.func_230337_a_(state, nbt);
		if (nbt.hasUniqueId("Owner"))
			owner = nbt.getUniqueId("Owner");
		accessList = nbt.getString("AccessList");
		enabled = state.get(ClaimBlock.ENABLED);
	}

	@Override
	public CompoundNBT write(CompoundNBT compound) {
		if (owner != null)
			compound.putUniqueId("Owner", owner);
		compound.putString("AccessList", accessList);
		return super.write(compound);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (!world.isRemote && owner != null && enabled)
			ClaimManager.check(world, pos, owner);
	}

	public boolean grantAccess(PlayerEntity player)
	{
		String name = player.getName().getString();
		for (String n : accessList.split(","))
		{
			if (n.equals("*"))
				return true;
			if (name.equals(n))
				return true;
		}
		return false;
	}

	@Override
	@Nullable
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(this.pos, 0, getUpdateTag());
	}

	@Override
	public CompoundNBT getUpdateTag() {
		return write(new CompoundNBT());
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		if (world == null)
			return;
		func_230337_a_(world.getBlockState(pkt.getPos()), pkt.getNbtCompound());
	}
}
