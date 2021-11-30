package com.lupicus.cc.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lupicus.cc.Main;
import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.config.MyConfig;
import com.lupicus.cc.manager.ClaimManager;
import com.lupicus.cc.manager.ClaimManager.ClaimInfo;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.FillBucketEvent;
//import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(bus = Bus.FORGE, modid = Main.MODID)
public class PlayerEvents
{
	private static final Logger LOGGER = LogManager.getLogger();

//	@SubscribeEvent
//	public static void onLeftClick(LeftClickBlock event)
//	{
//		Player player = event.getPlayer();
//		Level world = player.level;
//		if (world.isClientSide)
//			return;
//		ClaimInfo info = ClaimManager.get(world, event.getPos());
//		if (info.okPerm(player) || player.hasPermissions(3))
//			return;
//		if (event.isCancelable())
//		{
//			event.setCanceled(true);
//			return;
//		}
//	}

	@SubscribeEvent
	public static void onRightClick(RightClickBlock event)
	{
		Player player = event.getPlayer();
		Level world = player.level;
		if (world.isClientSide)
			return;
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || player.hasPermissions(3))
			return;
		BlockEntity te = world.getBlockEntity(info.pos.pos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantAccess(player))
				return;
		}
		BlockState state = world.getBlockState(event.getPos());
		if (MyConfig.bypassBlocks.contains(state.getBlock()))
			return;
		InteractionHand h = event.getHand();
		if (h == InteractionHand.MAIN_HAND)
			player.displayClientMessage(ClaimBlock.makeMsg("cc.message.block.access", info), true);
		if (event.isCancelable())
		{
			event.setCanceled(true);
			// fix client side view of the hotbar for non creative
			ItemStack itemstack = player.getItemInHand(h);
			if (!itemstack.isEmpty())
			{
				ServerPlayer sp = (ServerPlayer) player;
				if (sp.connection != null)
				{
					int index = 36 + ((h == InteractionHand.MAIN_HAND) ? sp.getInventory().selected : 9);
					InventoryMenu menu = sp.inventoryMenu;
					sp.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), index, itemstack));
				}
			}
		}
	}

	@SubscribeEvent
	public static void onBucket(FillBucketEvent event)
	{
		HitResult target = event.getTarget();
		if (target.getType() == HitResult.Type.BLOCK)
		{
			Player player = event.getPlayer();
			Level world = player.level;
			if (world.isClientSide)
				return;
			BlockHitResult blockray = (BlockHitResult) target;
			BlockPos blockpos = blockray.getBlockPos();
			Fluid fluid = null;
			ItemStack stack = event.getEmptyBucket();
			Item item = stack.getItem();
			if (item instanceof BucketItem)
			{
				BucketItem bucket = (BucketItem) item;
				fluid = bucket.getFluid();
			}
			else
			{
				// not a bucket (not sure this will happen), so guess
				FluidState state = world.getFluidState(blockpos);
				if (state.getType() != Fluids.EMPTY)
					fluid = Fluids.EMPTY;
			}
			if (fluid != Fluids.EMPTY)
			{
				boolean next = true;
				if (fluid != null)
				{
					BlockState state = world.getBlockState(blockpos);
					Block block = state.getBlock();
					if (block instanceof LiquidBlockContainer)
					{
						LiquidBlockContainer lc = (LiquidBlockContainer) block;
						if (lc.canPlaceLiquid(world, blockpos, state, fluid))
							next = false;
					}
				}
				if (next)
					blockpos = blockpos.relative(blockray.getDirection());
			}
			ClaimInfo info = ClaimManager.get(world, blockpos);
			boolean flag = false;
			if (info.okPerm(player) || player.hasPermissions(3))
				flag = true;
			else
			{
				BlockEntity te = world.getBlockEntity(info.pos.pos());
				if (te instanceof ClaimTileEntity)
				{
					ClaimTileEntity cte = (ClaimTileEntity) te;
					if (cte.grantModify(player))
						flag = true;
				}
			}
			if (flag)
			{
				if (fluid != Fluids.EMPTY)
					LOGGER.info("Placing " + stack + " @ " + world.dimension().location() + " " + blockpos + " by " + player.getName().getString());
				return;
			}
			player.displayClientMessage(ClaimBlock.makeMsg("cc.message.claimed.chunk", info), true);
			if (event.isCancelable())
				event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onRightClickItem(RightClickItem event)
	{
		ItemStack stack = event.getItemStack();
		if (stack.getItem() == Items.PAPER && event.getPlayer().isShiftKeyDown())
			ClaimBlock.clearPaper(stack, event.getPlayer());
	}
}
