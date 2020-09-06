package com.lupicus.cc.proxy;

import com.lupicus.cc.gui.ClaimScreen;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class ClientProxy implements IProxy
{
	@Override
	public void showClaimGui(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		TileEntity te = mc.world.getTileEntity(pos);
		if (te instanceof ClaimTileEntity)
			mc.displayGuiScreen(new ClaimScreen((ClaimTileEntity) te));
	}
}
