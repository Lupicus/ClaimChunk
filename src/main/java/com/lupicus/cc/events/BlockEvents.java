package com.lupicus.cc.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.lupicus.cc.Main;
import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.block.ModBlocks;
import com.lupicus.cc.config.MyConfig;
import com.lupicus.cc.manager.ClaimManager;
import com.lupicus.cc.manager.ClaimManager.ClaimInfo;
import com.lupicus.cc.network.ChangeBlockPacket;
import com.lupicus.cc.network.Network;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlockStructureHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IClearable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityMultiPlaceEvent;
import net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.world.BlockEvent.FarmlandTrampleEvent;
import net.minecraftforge.event.world.BlockEvent.FluidPlaceBlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.event.world.PistonEvent.PistonMoveType;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(bus = Bus.FORGE, modid = Main.MODID)
public class BlockEvents
{
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onBreakBlock(BreakEvent event)
	{
		PlayerEntity player = event.getPlayer();
		World world = player.world;
		if (world.isRemote)
			return;
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || (player.hasPermissionLevel(3) && player.isCreative()))
			return;
		TileEntity te = world.getTileEntity(info.pos.getPos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantModify(player))
				return;
		}
		player.sendStatusMessage(ClaimBlock.makeMsg("cc.message.claimed.chunk", info), true);
		if (event.isCancelable())
		{
			event.setCanceled(true);
			ServerPlayerEntity sp = (ServerPlayerEntity) player;
			if (sp.connection != null)
			{
				// fix client side view of some blocks (e.g. redstone wire)
				BlockState state = event.getState();
				if (!state.isSolid())
					Network.sendToClient(new ChangeBlockPacket(event.getPos(), state), sp);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onExplosionDetonate(ExplosionEvent.Detonate event)
	{
		World world = event.getWorld();
		List<BlockPos> list = event.getAffectedBlocks();
		HashMap<ChunkPos, Boolean> cfilter = new HashMap<>();
		LivingEntity entity = event.getExplosion().getExplosivePlacedBy();
		PlayerEntity player = null;
		if (entity instanceof MobEntity)
		{
			if (MyConfig.mobDestroy)
			{
				MobEntity mob = (MobEntity) entity;
				LivingEntity target = mob.getAttackTarget();
				if (!(target instanceof PlayerEntity))
					return;
				player = (PlayerEntity) target;
				if (MyConfig.pvpMode)
					return;
			}
		}
		else
		{
			if (entity instanceof PlayerEntity)
				player = (PlayerEntity) entity;
			if (MyConfig.pvpMode)
				return;
		}
		PlayerEvents.handleExplosion(world, player, event.getAffectedEntities());

		for (ListIterator<BlockPos> it = list.listIterator(list.size()); it.hasPrevious(); )
		{
			BlockPos pos = it.previous();
			ChunkPos cpos = new ChunkPos(pos);
			boolean flag;
			Boolean flagObj = cfilter.get(cpos);
			if (flagObj != null)
				flag = flagObj.booleanValue();
			else
			{
				ClaimInfo info = ClaimManager.get(world, pos);
				if (player != null)
				{
					flag = !info.okPerm(player);
					if (flag)
					{
						TileEntity te = world.getTileEntity(info.pos.getPos());
						if (te instanceof ClaimTileEntity)
						{
							ClaimTileEntity cte = (ClaimTileEntity) te;
							flag = !cte.grantModify(player);
						}
					}
				}
				else
					flag = (info.owner != null);
				cfilter.put(cpos, Boolean.valueOf(flag));
			}
			if (flag)
				it.remove();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onPlaceEvent(EntityPlaceEvent event)
	{
		if (event instanceof EntityMultiPlaceEvent)
			return;
		Entity entity = event.getEntity();
		if (!(entity instanceof PlayerEntity))
			return;
		PlayerEntity player = (PlayerEntity) entity;
		World world = player.world;
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || (player.hasPermissionLevel(3) && player.isCreative()))
			return;
		if (event.getPlacedBlock().getBlock() != ModBlocks.CLAIM_BLOCK)
		{
			TileEntity te = world.getTileEntity(info.pos.getPos());
			if (te instanceof ClaimTileEntity)
			{
				ClaimTileEntity cte = (ClaimTileEntity) te;
				if (cte.grantModify(player))
					return;
			}
		}
		if (event.isCancelable())
		{
			player.sendStatusMessage(ClaimBlock.makeMsg("cc.message.claimed.chunk", info), true);
			event.setCanceled(true);
			TileEntity te = world.getTileEntity(event.getPos());
			if (te != null)
				IClearable.clearObj(te);
			Utility.updateHands((ServerPlayerEntity) player);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onPlaceEvent(EntityMultiPlaceEvent event)
	{
		Entity entity = event.getEntity();
		if (!(entity instanceof PlayerEntity))
			return;
		PlayerEntity player = (PlayerEntity) entity;
		World world = player.world;
		boolean flag = false;
		ChunkPos prev = null;
		ClaimInfo info = null;
		for (BlockSnapshot s : event.getReplacedBlockSnapshots())
		{
			ChunkPos pos = new ChunkPos(s.getPos());
			if (!pos.equals(prev))
			{
				prev = pos;
				info = ClaimManager.get(world, pos);
				if (info.okPerm(player) || (player.hasPermissionLevel(3) && player.isCreative()))
					continue;
				TileEntity te = world.getTileEntity(info.pos.getPos());
				if (te instanceof ClaimTileEntity)
				{
					ClaimTileEntity cte = (ClaimTileEntity) te;
					if (cte.grantModify(player))
						continue;
				}
				flag = true;
				break;
			}
		}
		if (flag && event.isCancelable())
		{
			player.sendStatusMessage(ClaimBlock.makeMsg("cc.message.claimed.chunk", info), true);
			event.setCanceled(true);
			Utility.updateHands((ServerPlayerEntity) player);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onTrample(FarmlandTrampleEvent event)
	{
		IWorld iworld = event.getWorld();
		if (iworld.isRemote())
			return;
		Entity entity = event.getEntity();
		if (iworld instanceof World && entity instanceof PlayerEntity)
		{
			PlayerEntity player = (PlayerEntity) entity;
			World world = (World) iworld;
			ClaimInfo info = ClaimManager.get(world, event.getPos());
			if (info.okPerm(player))
				return;
			TileEntity te = world.getTileEntity(info.pos.getPos());
			if (te instanceof ClaimTileEntity)
			{
				ClaimTileEntity cte = (ClaimTileEntity) te;
				if (cte.grantModify(player))
					return;
			}
			if (event.isCancelable())
				event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onFluid(FluidPlaceBlockEvent event)
	{
		// this seems to be when fluid changes the state of a block
		IWorld iworld = event.getWorld();
		ChunkPos lpos = new ChunkPos(event.getLiquidPos());
		ChunkPos npos = new ChunkPos(event.getPos());
		if (lpos.equals(npos))
			return;
		if (iworld instanceof World)
		{
			World world = (World) iworld;
			ClaimInfo ninfo = ClaimManager.get(world, npos);
			if (ninfo.owner == null)
				return;
			ClaimInfo linfo = ClaimManager.get(world, lpos);
			if (ninfo.owner.equals(linfo.owner))
				return;
			TileEntity te = world.getTileEntity(ninfo.pos.getPos());
			if (te instanceof ClaimTileEntity)
			{
				ClaimTileEntity cte = (ClaimTileEntity) te;
				String name = (linfo.owner == null) ? "*" : ClaimManager.getName(linfo);
				if (cte.grantModify(name))
					return;
			}
			event.setNewState(event.getOriginalState());
		}
	}

	public static boolean canSpreadTo(IBlockReader iworld, BlockPos fromPos, BlockPos toPos, Direction dir)
	{
		if (dir.getAxis() == Axis.Y)
			return true;
		ChunkPos lpos = new ChunkPos(fromPos);
		ChunkPos npos = new ChunkPos(toPos);
		if (lpos.equals(npos))
			return true;
		if (iworld instanceof World)
		{
			World world = (World) iworld;
			ClaimInfo ninfo = ClaimManager.get(world, npos);
			if (ninfo.owner == null)
				return true;
			ClaimInfo linfo = ClaimManager.get(world, lpos);
			if (ninfo.owner.equals(linfo.owner))
				return true;
			TileEntity te = world.getTileEntity(ninfo.pos.getPos());
			if (te instanceof ClaimTileEntity)
			{
				ClaimTileEntity cte = (ClaimTileEntity) te;
				String name = (linfo.owner == null) ? "*" : ClaimManager.getName(linfo);
				if (cte.grantModify(name))
					return true;
			}
		}
		return false;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onPiston(PistonEvent.Pre event)
	{
		IWorld iworld = event.getWorld();
		if (iworld.isRemote())
			return;
		PistonBlockStructureHelper helper = event.getStructureHelper();
		if (helper == null)
			return;
		// when retracting the blocks might not include all of them
		@SuppressWarnings("unused")
		boolean moveFlag = helper.canMove();
//		if (!moveFlag)
//			return;
		Direction dir = event.getDirection();
		BlockPos pos = event.getPos();
		List<BlockPos> list1 = helper.getBlocksToDestroy();
		List<BlockPos> list2 = helper.getBlocksToMove();
		List<BlockPos> list3 = new ArrayList<>();
		if (event.getPistonMoveType() == PistonMoveType.EXTEND &&
			dir.getAxis() != Direction.Axis.Y)
		{
			if (list2.isEmpty())
			{
				list3.add(pos.offset(dir));
			}
			else
			{
				for (BlockPos b : list2)
				{
					list3.add(b.offset(dir));
				}
			}
		}
		else
		{
			Direction ydir = (event.getPistonMoveType() == PistonMoveType.EXTEND) ? dir : dir.getOpposite();
			for (BlockPos b : list2)
			{
				BlockPos pos2 = pos.subtract(b);
				if (pos2.getX() != 0 || pos2.getZ() != 0)
					list3.add(b.offset(ydir));
			}
		}
		World world = (World) iworld;
		ChunkPos cpos = new ChunkPos(pos);
		Set<ChunkPos> check = new HashSet<>();
		addBlocks(cpos, check, list1);
		addBlocks(cpos, check, list2);
		addBlocks(cpos, check, list3);
		if (!check.isEmpty())
		{
			boolean flag = false;
			ClaimInfo pinfo = ClaimManager.get(world, cpos);
			for (ChunkPos e : check)
			{
				ClaimInfo info = ClaimManager.get(world, e);
				if (info.owner == null)
					continue;
				if (!info.owner.equals(pinfo.owner))
				{
					TileEntity te = world.getTileEntity(info.pos.getPos());
					if (te instanceof ClaimTileEntity)
					{
						ClaimTileEntity cte = (ClaimTileEntity) te;
						String name = (pinfo.owner == null) ? "*" : ClaimManager.getName(pinfo);
						if (cte.grantModify(name))
							continue;
					}
					flag = true;
					break;
				}
			}
			if (flag && event.isCancelable())
				event.setCanceled(true);
		}
	}

	private static void addBlocks(ChunkPos pos, Set<ChunkPos> check, List<BlockPos> list)
	{
		for (BlockPos e : list)
		{
			ChunkPos test = new ChunkPos(e);
			if (!pos.equals(test))
				check.add(test);
		}
	}
}
