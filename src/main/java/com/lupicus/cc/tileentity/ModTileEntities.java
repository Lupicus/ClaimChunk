package com.lupicus.cc.tileentity;

import com.lupicus.cc.block.ModBlocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.IForgeRegistry;

public class ModTileEntities
{
	public static final TileEntityType<ClaimTileEntity> CLAIM_BLOCK = create("claim_block", TileEntityType.Builder.create(ClaimTileEntity::new, ModBlocks.CLAIM_BLOCK).build(null));

	public static <T extends TileEntity> TileEntityType<T> create(String key, TileEntityType<T> type)
	{
		type.setRegistryName(key);
		return type;
	}

	public static void register(IForgeRegistry<TileEntityType<?>> forgeRegistry)
	{
		forgeRegistry.register(CLAIM_BLOCK);
	}
}
