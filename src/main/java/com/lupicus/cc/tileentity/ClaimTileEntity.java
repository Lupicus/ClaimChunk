package com.lupicus.cc.tileentity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.manager.ClaimManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ClaimTileEntity extends BlockEntity
{
	public UUID owner;
	private String accessList = "";
	private String modifyList = "";
	private boolean enabled = true; // for loading
	private boolean accessAll;
	private boolean modifyAll;
	private Set<String> accessSet = new HashSet<String>();
	private Set<String> modifySet = new HashSet<String>();

	public ClaimTileEntity(BlockPos pos, BlockState state) {
		super(ModTileEntities.CLAIM_BLOCK, pos, state);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, Provider hp) {
		super.loadAdditional(nbt, hp);
		if (nbt.hasUUID("Owner"))
			owner = nbt.getUUID("Owner");
		setAccess(nbt.getString("AccessList"));
		setModify(nbt.getString("ModifyList"));
		enabled = getBlockState().getValue(ClaimBlock.ENABLED);
	}

	@Override
	protected void saveAdditional(CompoundTag compound, Provider hp) {
		super.saveAdditional(compound, hp);
		if (owner != null)
			compound.putUUID("Owner", owner);
		compound.putString("AccessList", accessList);
		compound.putString("ModifyList", modifyList);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (!level.isClientSide && owner != null && enabled)
			ClaimManager.check(level, worldPosition, owner);
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

	public boolean grantAccess(Player player)
	{
		if (accessAll)
			return true;
		String name = player.getName().getString();
		return accessSet.contains(name) || grantModify(name);
	}

	public boolean grantModify(Player player)
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
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(Provider hp) {
		return saveWithoutMetadata(hp);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, Provider hp) {
		if (level == null)
			return;
		super.onDataPacket(net, pkt, hp);
	}
}
