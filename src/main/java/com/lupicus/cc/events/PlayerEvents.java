package com.lupicus.cc.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lupicus.cc.Main;
import com.lupicus.cc.manager.ClaimManager;
import com.lupicus.cc.manager.ClaimManager.ClaimInfo;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
//import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
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
		player.sendStatusMessage(new TranslationTextComponent("cc.message.block.access"), true);
		if (event.isCancelable())
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onBucket(FillBucketEvent event)
	{
		PlayerEntity player = event.getPlayer();
		World world = player.world;
		if (world.isRemote)
			return;
		ItemStack stack = event.getEmptyBucket();
		RayTraceResult target = event.getTarget();
		if (target.getType() == RayTraceResult.Type.BLOCK)
		{
			BlockRayTraceResult blockray = (BlockRayTraceResult) target;
			BlockPos blockpos = blockray.getPos().offset(blockray.getFace());
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
					if (cte.grantAccess(player))
						flag = true;
				}
			}
			if (flag)
			{
				if (stack.getItem() != Items.BUCKET)
					LOGGER.info("Placing " + stack + " @ " + world.func_234923_W_().func_240901_a_() + " " + blockpos + " by " + player.getDisplayName().getString());
				return;
			}
			player.sendStatusMessage(new TranslationTextComponent("cc.message.claimed.chunk"), true);
			if (event.isCancelable())
				event.setCanceled(true);
		}
	}
}
