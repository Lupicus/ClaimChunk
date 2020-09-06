package com.lupicus.cc.item;

import com.lupicus.cc.block.ModBlocks;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Item.Properties;
import net.minecraftforge.registries.IForgeRegistry;

public class ModItems
{
	public static final Item CLAIM_BLOCK = new BlockItem(ModBlocks.CLAIM_BLOCK, new Properties().group(ItemGroup.DECORATIONS)).setRegistryName("claim_block");

	public static void register(IForgeRegistry<Item> forgeRegistry)
	{
		forgeRegistry.register(CLAIM_BLOCK);
	}
}
