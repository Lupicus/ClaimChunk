package com.lupicus.cc.block;

import net.minecraft.block.AbstractBlock.Properties;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.IForgeRegistry;

public class ModBlocks
{
	public static final Block CLAIM_BLOCK = new ClaimBlock(Properties.create(Material.IRON).hardnessAndResistance(5.0F).sound(SoundType.METAL)).setRegistryName("claim_block");

	public static void register(IForgeRegistry<Block> forgeRegistry	)
	{
		forgeRegistry.register(CLAIM_BLOCK);
	}

	@OnlyIn(Dist.CLIENT)
	public static void setRenderLayer()
	{
//		RenderTypeLookup.setRenderLayer(REDSTONE_PIPE_BLOCK, RenderType.getCutout());
	}

	@OnlyIn(Dist.CLIENT)
	public static void register(BlockColors blockColors)
	{
//		blockColors.register((blockstate, lightreader, pos, index) -> {
//			return REDSTONE_MAGIC_PYLON_MINOR.colorMultiplier(blockstate.get(REDSTONE_MAGIC_PYLON_MINOR.POWER));
//		}, REDSTONE_MAGIC_PYLON_MINOR);
//
//		blockColors.addColorState(REDSTONE_MAGIC_PYLON_MINOR.POWER, REDSTONE_MAGIC_PYLON_MINOR);
	}
}
