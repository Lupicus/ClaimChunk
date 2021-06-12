package com.lupicus.cc.block;

import net.minecraft.block.AbstractBlock.Properties;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.registries.IForgeRegistry;

public class ModBlocks
{
	public static final Block CLAIM_BLOCK = new ClaimBlock(Properties.create(Material.IRON).hardnessAndResistance(5.0F).sound(SoundType.METAL)).setRegistryName("claim_block");

	public static void register(IForgeRegistry<Block> forgeRegistry	)
	{
		forgeRegistry.register(CLAIM_BLOCK);
	}
}
