package com.lupicus.cc.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lupicus.cc.Main;
import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.manager.ClaimManager;
import com.lupicus.cc.manager.ClaimManager.ClaimInfo;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
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
//		PlayerEntity player = event.getPlayer();
//		World world = player.world;
//		if (world.isRemote)
//			return;
//		ClaimInfo info = ClaimManager.get(world, event.getPos());
//		if (info.okPerm(player) || player.hasPermissionLevel(3))
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
		PlayerEntity player = event.getPlayer();
		World world = player.world;
		if (world.isRemote)
			return;
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || player.hasPermissionLevel(3))
			return;
		TileEntity te = world.getTileEntity(info.pos.getPos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantAccess(player))
				return;
		}
		Hand h = event.getHand();
		if (h == Hand.MAIN_HAND)
			player.sendStatusMessage(ClaimBlock.makeMsg("cc.message.block.access", info), true);
		if (event.isCancelable())
		{
			event.setCanceled(true);
			// fix client side view of the hotbar for non creative
			ItemStack itemstack = player.getHeldItem(h);
			if (!itemstack.isEmpty())
			{
				ServerPlayerEntity sp = (ServerPlayerEntity) player;
				if (sp.connection != null)
				{
					int index = 36 + ((h == Hand.MAIN_HAND) ? sp.inventory.currentItem : 9);
					sp.sendSlotContents(sp.container, index, itemstack);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onBucket(FillBucketEvent event)
	{
		RayTraceResult target = event.getTarget();
		if (target.getType() == RayTraceResult.Type.BLOCK)
		{
			PlayerEntity player = event.getPlayer();
			World world = player.world;
			if (world.isRemote)
				return;
			BlockRayTraceResult blockray = (BlockRayTraceResult) target;
			BlockPos blockpos = blockray.getPos();
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
				if (state.getFluid() != Fluids.EMPTY)
					fluid = Fluids.EMPTY;
			}
			if (fluid != Fluids.EMPTY)
			{
				boolean next = true;
				if (fluid != null)
				{
					BlockState state = world.getBlockState(blockpos);
					Block block = state.getBlock();
					if (block instanceof ILiquidContainer)
					{
						ILiquidContainer lc = (ILiquidContainer) block;
						if (lc.canContainFluid(world, blockpos, state, fluid))
							next = false;
					}
				}
				if (next)
					blockpos = blockpos.offset(blockray.getFace());
			}
			ClaimInfo info = ClaimManager.get(world, blockpos);
			boolean flag = false;
			if (info.okPerm(player) || player.hasPermissionLevel(3))
				flag = true;
			else
			{
				TileEntity te = world.getTileEntity(info.pos.getPos());
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
					LOGGER.info("Placing " + stack + " @ " + world.func_234923_W_().func_240901_a_() + " " + blockpos + " by " + player.getName().getString());
				return;
			}
			player.sendStatusMessage(ClaimBlock.makeMsg("cc.message.claimed.chunk", info), true);
			if (event.isCancelable())
				event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onRightClickItem(RightClickItem event)
	{
		ItemStack stack = event.getItemStack();
		if (stack.getItem() == Items.PAPER && event.getPlayer().isSneaking())
			ClaimBlock.clearPaper(stack, event.getPlayer());
	}
}
