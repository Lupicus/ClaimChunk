package com.lupicus.cc.item;

import java.util.function.BiFunction;

import com.lupicus.cc.block.ModBlocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.IForgeRegistry;

public class ModItems
{
	public static final Item CLAIM_BLOCK = register(ModBlocks.CLAIM_BLOCK, BlockItem::new, new Properties());

	public static void register(IForgeRegistry<Item> forgeRegistry)
	{
	}

	private static Item register(Block block, BiFunction<Block, Properties, Item> func, Properties prop)
	{
		return Items.registerBlock(block, func, prop);
	}

	public static void setupTabs(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
		{
			event.accept(CLAIM_BLOCK);
		}
	}
}
