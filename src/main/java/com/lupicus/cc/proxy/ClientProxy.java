package com.lupicus.cc.proxy;

import com.lupicus.cc.gui.ClaimScreen;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ClientProxy implements IProxy
{
	@Override
	public void showClaimGui(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		BlockEntity te = mc.level.getBlockEntity(pos);
		if (te instanceof ClaimTileEntity)
			mc.setScreen(new ClaimScreen((ClaimTileEntity) te));
	}

	@Override
	public void changeBlock(BlockPos pos, BlockState state) {
		Minecraft mc = Minecraft.getInstance();
		mc.level.setBlock(pos, state, 3);
	}
}
