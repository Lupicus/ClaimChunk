package com.lupicus.cc.tileentity;

import java.util.HashSet;
import java.util.Set;
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
	private String accessList = "";
	private String modifyList = "";
	private boolean enabled = true; // for loading
	private boolean accessAll;
	private boolean modifyAll;
	private Set<String> accessSet = new HashSet<String>();
	private Set<String> modifySet = new HashSet<String>();

	public ClaimTileEntity() {
		super(ModTileEntities.CLAIM_BLOCK);
	}

	@Override
	public void func_230337_a_(BlockState state, CompoundNBT nbt) { // read
		super.func_230337_a_(state, nbt);
		if (nbt.hasUniqueId("Owner"))
			owner = nbt.getUniqueId("Owner");
		setAccess(nbt.getString("AccessList"));
		setModify(nbt.getString("ModifyList"));
		enabled = state.get(ClaimBlock.ENABLED);
	}

	@Override
	public CompoundNBT write(CompoundNBT compound) {
		if (owner != null)
			compound.putUniqueId("Owner", owner);
		compound.putString("AccessList", accessList);
		compound.putString("ModifyList", modifyList);
		return super.write(compound);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (!world.isRemote && owner != null && enabled)
			ClaimManager.check(world, pos, owner);
	}

	public String getAccess()
	{
		return accessList;
	}

	public String getModify()
	{
		return modifyList;
	}

	public void setAccess(String list)
	{
		accessList = list;
		accessAll = false;
		accessSet.clear();
		for (String n : list.split(","))
		{
			if (n.equals("*"))
				accessAll = true;
			else
				accessSet.add(n);
		}
	}

	public void setModify(String list)
	{
		modifyList = list;
		modifyAll = false;
		modifySet.clear();
		for (String n : list.split(","))
		{
			if (n.equals("*"))
				modifyAll = true;
			else
				modifySet.add(n);
		}
	}

	public boolean grantAccess(PlayerEntity player)
	{
		if (accessAll)
			return true;
		String name = player.getName().getString();
		return accessSet.contains(name) || grantModify(name);
	}

	public boolean grantModify(PlayerEntity player)
	{
		if (modifyAll)
			return true;
		return modifySet.contains(player.getName().getString());
	}

	public boolean grantModify(String name)
	{
		if (modifyAll)
			return true;
		return modifySet.contains(name);
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
