package com.lupicus.cc.tileentity;

import java.util.Set;

import com.lupicus.cc.block.ModBlocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.IForgeRegistry;

public class ModTileEntities
{
	public static final BlockEntityType<ClaimTileEntity> CLAIM_BLOCK = new BlockEntityType<>(ClaimTileEntity::new, Set.of(ModBlocks.CLAIM_BLOCK));

	public static void register(IForgeRegistry<BlockEntityType<?>> forgeRegistry)
	{
		forgeRegistry.register("claim_block", CLAIM_BLOCK);
	}
}
