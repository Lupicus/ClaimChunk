package com.lupicus.cc.tileentity;

import com.lupicus.cc.block.ModBlocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.IForgeRegistry;

public class ModTileEntities
{
	public static final BlockEntityType<ClaimTileEntity> CLAIM_BLOCK = BlockEntityType.Builder.of(ClaimTileEntity::new, ModBlocks.CLAIM_BLOCK).build(null);

	public static void register(IForgeRegistry<BlockEntityType<?>> forgeRegistry)
	{
		forgeRegistry.register("claim_block", CLAIM_BLOCK);
	}
}
