package com.lupicus.cc.proxy;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public interface IProxy
{
	public void showClaimGui(BlockPos pos);
	public void changeBlock(BlockPos pos, BlockState state);
}
