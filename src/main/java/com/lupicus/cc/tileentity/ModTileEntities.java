package com.lupicus.cc.tileentity;

import com.lupicus.cc.block.ModBlocks;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.IForgeRegistry;

public class ModTileEntities
{
	public static final BlockEntityType<ClaimTileEntity> CLAIM_BLOCK = create("claim_block", BlockEntityType.Builder.of(ClaimTileEntity::new, ModBlocks.CLAIM_BLOCK).build(null));

	public static <T extends BlockEntity> BlockEntityType<T> create(String key, BlockEntityType<T> type)
	{
		type.setRegistryName(key);
		return type;
	}

	public static void register(IForgeRegistry<BlockEntityType<?>> forgeRegistry)
	{
		forgeRegistry.register(CLAIM_BLOCK);
	}
}
