package com.lupicus.cc.proxy;

import com.lupicus.cc.gui.ClaimScreen;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ClientProxy implements IProxy
{
	@Override
	public void showClaimGui(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		BlockEntity te = mc.level.getBlockEntity(pos);
		if (te instanceof ClaimTileEntity)
			mc.setScreen(new ClaimScreen((ClaimTileEntity) te));
	}
}
