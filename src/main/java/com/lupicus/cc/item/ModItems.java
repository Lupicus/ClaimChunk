package com.lupicus.cc.item;

import com.lupicus.cc.block.ModBlocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.registries.IForgeRegistry;

public class ModItems
{
	public static final Item CLAIM_BLOCK = new BlockItem(ModBlocks.CLAIM_BLOCK, new Properties().tab(CreativeModeTab.TAB_DECORATIONS));

	public static void register(IForgeRegistry<Item> forgeRegistry)
	{
		forgeRegistry.register("claim_block", CLAIM_BLOCK);
	}
}
