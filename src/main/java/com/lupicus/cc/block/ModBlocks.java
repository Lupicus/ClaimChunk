package com.lupicus.cc.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.IForgeRegistry;

public class ModBlocks
{
	public static final Block CLAIM_BLOCK = new ClaimBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK));

	public static void register(IForgeRegistry<Block> forgeRegistry	)
	{
		forgeRegistry.register("claim_block", CLAIM_BLOCK);
	}
}
