package com.lupicus.cc.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.lupicus.cc.Main;
import com.lupicus.cc.manager.ClaimManager;
import com.lupicus.cc.manager.ClaimManager.ClaimInfo;

import net.minecraft.block.PistonBlockStructureHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.event.world.PistonEvent.PistonMoveType;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityMultiPlaceEvent;
import net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.world.BlockEvent.FarmlandTrampleEvent;
import net.minecraftforge.event.world.BlockEvent.FluidPlaceBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(bus = Bus.FORGE, modid = Main.MODID)
public class BlockEvents
{
	@SubscribeEvent
	public static void onBreakBlock(BreakEvent event)
	{
		PlayerEntity player = event.getPlayer();
		World world = player.world;
		if (world.isRemote)
			return;
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || player.hasPermissionLevel(3))
			return;
		player.sendStatusMessage(new TranslationTextComponent("cc.message.claimed.chunk"), true);
		if (event.isCancelable())
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onExplosionDetonate(ExplosionEvent.Detonate event)
	{
		World world = event.getWorld();
		List<BlockPos> list = event.getAffectedBlocks();
		HashSet<BlockPos> filter = new HashSet<>();
		HashMap<ChunkPos, Boolean> cfilter = new HashMap<>();
		LivingEntity entity = event.getExplosion().getExplosivePlacedBy();
		PlayerEntity player = null;
		if (entity instanceof MobEntity)
		{
			MobEntity mob = (MobEntity) entity;
			LivingEntity target = mob.getAttackTarget();
			if (!(target instanceof PlayerEntity))
				return;
			player = (PlayerEntity) target;
		}
		else if (entity instanceof PlayerEntity)
			player = (PlayerEntity) entity;

		for (BlockPos pos : list)
		{
			ChunkPos pos2 = new ChunkPos(pos);
			boolean flag;
			if (!cfilter.containsKey(pos2))
			{
				ClaimInfo info = ClaimManager.get(world, pos);
				if (player != null)
					flag = !info.okPerm(player);
				else
					flag = (info.owner != null);
				cfilter.put(pos2, flag);
			}
			else
				flag = cfilter.get(pos2);
			if (flag)
				filter.add(pos);
		}
		if (!filter.isEmpty())
			list.removeIf(b -> filter.contains(b));
	}

	@SubscribeEvent
	public static void onPlaceEvent(EntityPlaceEvent event)
	{
		Entity entity = event.getEntity();
		if (!(entity instanceof PlayerEntity))
			return;
		PlayerEntity player = (PlayerEntity) entity;
		World world = player.world;
		if (world.isRemote)
			return;
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || player.hasPermissionLevel(3))
			return;
		player.sendStatusMessage(new TranslationTextComponent("cc.message.claimed.chunk"), true);
		if (event.isCancelable())
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onPlaceEvent(EntityMultiPlaceEvent event)
	{
		Entity entity = event.getEntity();
		if (!(entity instanceof PlayerEntity))
			return;
		PlayerEntity player = (PlayerEntity) entity;
		World world = player.world;
		if (world.isRemote)
			return;
		boolean flag = false;
		ChunkPos prev = null;
		for (BlockSnapshot s : event.getReplacedBlockSnapshots())
		{
			ChunkPos pos = new ChunkPos(s.getPos());
			if (!pos.equals(prev))
			{
				prev = pos;
				ClaimInfo info = ClaimManager.get(world, pos);
				if (info.okPerm(player) || player.hasPermissionLevel(3))
					continue;
				flag = true;
				break;
			}
		}
		if (flag && event.isCancelable())
		{
			player.sendStatusMessage(new TranslationTextComponent("cc.message.claimed.chunk"), true);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onTrample(FarmlandTrampleEvent event)
	{
		IWorld iworld = event.getWorld();
		if (iworld.isRemote())
			return;
		Entity entity = event.getEntity();
		if (iworld instanceof World && entity instanceof PlayerEntity)
		{
			World world = (World) iworld;
			ClaimInfo info = ClaimManager.get(world, event.getPos());
			if (info.okPerm((PlayerEntity) entity))
				return;
			if (event.isCancelable())
				event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onFluid(FluidPlaceBlockEvent event)
	{
		// this seems to be when fluid changes the state of a block
		IWorld iworld = event.getWorld();
		if (iworld.isRemote())
			return;
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
			event.setNewState(event.getOriginalState());
		}
	}

	@SubscribeEvent
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
				if (pinfo.owner == null || !pinfo.owner.equals(info.owner))
				{
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
