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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.level.BlockEvent.EntityMultiPlaceEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.level.BlockEvent.FarmlandTrampleEvent;
import net.minecraftforge.event.level.BlockEvent.FluidPlaceBlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.level.PistonEvent.PistonMoveType;
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
		Player player = event.getPlayer();
		Level world = player.level();
		if (world.isClientSide)
			return;
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
			return;
		BlockEntity te = world.getBlockEntity(info.pos.pos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantModify(player))
				return;
		}
		player.displayClientMessage(ClaimBlock.makeMsg("cc.message.claimed.chunk", info), true);
		if (event.isCancelable())
		{
			event.setCanceled(true);
			ServerPlayer sp = (ServerPlayer) player;
			if (sp.connection != null)
			{
				// fix client side view of some blocks (e.g. redstone wire)
				BlockState state = event.getState();
				if (!state.canOcclude())
					Network.sendToClient(new ChangeBlockPacket(event.getPos(), state), sp);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onExplosionDetonate(ExplosionEvent.Detonate event)
	{
		Level world = event.getLevel();
		List<BlockPos> list = event.getAffectedBlocks();
		HashMap<ChunkPos, Boolean> cfilter = new HashMap<>();
		LivingEntity entity = event.getExplosion().getIndirectSourceEntity();
		Player player = null;
		if (entity instanceof Mob)
		{
			if (MyConfig.mobDestroy)
			{
				Mob mob = (Mob) entity;
				LivingEntity target = mob.getTarget();
				if (!(target instanceof Player))
					return;
				player = (Player) target;
				if (MyConfig.pvpMode)
					return;
			}
		}
		else
		{
			if (entity instanceof Player)
				player = (Player) entity;
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
						BlockEntity te = world.getBlockEntity(info.pos.pos());
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
		if (!(entity instanceof Player))
			return;
		Player player = (Player) entity;
		Level world = player.level();
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
			return;
		if (event.getPlacedBlock().getBlock() != ModBlocks.CLAIM_BLOCK)
		{
			BlockEntity te = world.getBlockEntity(info.pos.pos());
			if (te instanceof ClaimTileEntity)
			{
				ClaimTileEntity cte = (ClaimTileEntity) te;
				if (cte.grantModify(player))
					return;
			}
		}
		if (event.isCancelable())
		{
			player.displayClientMessage(ClaimBlock.makeMsg("cc.message.claimed.chunk", info), true);
			event.setCanceled(true);
			BlockEntity te = world.getBlockEntity(event.getPos());
			if (te != null)
				Clearable.tryClear(te);
			Utility.updateHands((ServerPlayer) player);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onPlaceEvent(EntityMultiPlaceEvent event)
	{
		Entity entity = event.getEntity();
		if (!(entity instanceof Player))
			return;
		Player player = (Player) entity;
		Level world = player.level();
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
				if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
					continue;
				BlockEntity te = world.getBlockEntity(info.pos.pos());
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
			player.displayClientMessage(ClaimBlock.makeMsg("cc.message.claimed.chunk", info), true);
			event.setCanceled(true);
			Utility.updateHands((ServerPlayer) player);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onTrample(FarmlandTrampleEvent event)
	{
		LevelAccessor iworld = event.getLevel();
		if (iworld.isClientSide())
			return;
		Entity entity = event.getEntity();
		if (iworld instanceof Level && entity instanceof Player)
		{
			Player player = (Player) entity;
			Level world = (Level) iworld;
			ClaimInfo info = ClaimManager.get(world, event.getPos());
			if (info.okPerm(player))
				return;
			BlockEntity te = world.getBlockEntity(info.pos.pos());
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
		LevelAccessor iworld = event.getLevel();
		ChunkPos lpos = new ChunkPos(event.getLiquidPos());
		ChunkPos npos = new ChunkPos(event.getPos());
		if (lpos.equals(npos))
			return;
		if (iworld instanceof Level)
		{
			Level world = (Level) iworld;
			ClaimInfo ninfo = ClaimManager.get(world, npos);
			if (ninfo.owner == null)
				return;
			ClaimInfo linfo = ClaimManager.get(world, lpos);
			if (ninfo.owner.equals(linfo.owner))
				return;
			BlockEntity te = world.getBlockEntity(ninfo.pos.pos());
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

	public static boolean canSpreadTo(BlockGetter iworld, BlockPos fromPos, BlockPos toPos, Direction dir)
	{
		if (dir.getAxis() == Axis.Y)
			return true;
		ChunkPos lpos = new ChunkPos(fromPos);
		ChunkPos npos = new ChunkPos(toPos);
		if (lpos.equals(npos))
			return true;
		if (iworld instanceof Level)
		{
			Level world = (Level) iworld;
			ClaimInfo ninfo = ClaimManager.get(world, npos);
			if (ninfo.owner == null)
				return true;
			ClaimInfo linfo = ClaimManager.get(world, lpos);
			if (ninfo.owner.equals(linfo.owner))
				return true;
			BlockEntity te = world.getBlockEntity(ninfo.pos.pos());
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

	public static boolean canBreakPot(Level world, BlockHitResult hitResult, Projectile pEntity)
	{
		Entity entity = pEntity.getOwner();
		Player player = null;
		if (entity instanceof Mob)
		{
			if (MyConfig.mobDestroy)
			{
				Mob mob = (Mob) entity;
				LivingEntity target = mob.getTarget();
				if (!(target instanceof Player))
					return true;
				player = (Player) target;
			}
		}
		else
		{
			if (entity instanceof Player)
				player = (Player) entity;
		}
		boolean flag;
		ClaimInfo info = ClaimManager.get(world, hitResult.getBlockPos());
		if (player != null)
		{
			flag = info.okPerm(player) || (player.hasPermissions(3) && player.isCreative());
			if (!flag)
			{
				BlockEntity te = world.getBlockEntity(info.pos.pos());
				if (te instanceof ClaimTileEntity)
				{
					ClaimTileEntity cte = (ClaimTileEntity) te;
					flag = cte.grantModify(player);
				}
			}
		}
		else
			flag = (info.owner == null);
		return flag;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onPiston(PistonEvent.Pre event)
	{
		LevelAccessor iworld = event.getLevel();
		if (iworld.isClientSide())
			return;
		PistonStructureResolver helper = event.getStructureHelper();
		if (helper == null)
			return;
		// when retracting the blocks might not include all of them
		@SuppressWarnings("unused")
		boolean moveFlag = helper.resolve();
//		if (!moveFlag)
//			return;
		Direction dir = event.getDirection();
		BlockPos pos = event.getPos();
		List<BlockPos> list1 = helper.getToDestroy();
		List<BlockPos> list2 = helper.getToPush();
		List<BlockPos> list3 = new ArrayList<>();
		if (event.getPistonMoveType() == PistonMoveType.EXTEND &&
			dir.getAxis() != Direction.Axis.Y)
		{
			if (list2.isEmpty())
			{
				list3.add(pos.relative(dir));
			}
			else
			{
				for (BlockPos b : list2)
				{
					list3.add(b.relative(dir));
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
					list3.add(b.relative(ydir));
			}
		}
		Level world = (Level) iworld;
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
					BlockEntity te = world.getBlockEntity(info.pos.pos());
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
