package com.lupicus.cc.proxy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface IProxy
{
	public void showClaimGui(BlockPos pos);
	public void changeBlock(BlockPos pos, BlockState state);
}
