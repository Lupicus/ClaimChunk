package com.lupicus.cc.events;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

public class Utility
{
	/**
	 * fix client side view of the hotbar for non creative
	 */
	public static void updateHands(ServerPlayerEntity player)
	{
		if (player.connection == null)
			return;
		ItemStack itemstack = player.inventory.getCurrentItem();
		if (!itemstack.isEmpty())
			player.sendSlotContents(player.container, 36 + player.inventory.currentItem, itemstack);
		itemstack = player.inventory.offHandInventory.get(0);
		if (!itemstack.isEmpty())
			player.sendSlotContents(player.container, 45, itemstack);
	}
}
