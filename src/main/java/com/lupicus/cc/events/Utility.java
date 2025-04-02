package com.lupicus.cc.events;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

public class Utility
{
	/**
	 * fix client side view of the hotbar for non creative
	 */
	public static void updateHands(ServerPlayer player)
	{
		if (player.connection == null)
			return;
		ItemStack itemstack = player.getInventory().getSelectedItem();
		if (!itemstack.isEmpty())
			slotChanged(player, 36 + player.getInventory().getSelectedSlot(), itemstack);
		itemstack = player.getInventory().getItem(Inventory.SLOT_OFFHAND);
		if (!itemstack.isEmpty())
			slotChanged(player, 45, itemstack);
	}

	public static void slotChanged(ServerPlayer player, int index, ItemStack itemstack)
	{
		InventoryMenu menu = player.inventoryMenu;
		player.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), index, itemstack));
	}
}
