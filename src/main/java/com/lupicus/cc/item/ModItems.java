package com.lupicus.cc.item;

import com.lupicus.cc.block.ModBlocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.IForgeRegistry;

public class ModItems
{
	public static final Item CLAIM_BLOCK = new BlockItem(ModBlocks.CLAIM_BLOCK, new Properties());

	public static void register(IForgeRegistry<Item> forgeRegistry)
	{
		forgeRegistry.register("claim_block", CLAIM_BLOCK);
	}

	public static void setupTabs(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
		{
			event.accept(CLAIM_BLOCK);
		}
	}
}
