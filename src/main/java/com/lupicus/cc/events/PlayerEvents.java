package com.lupicus.cc.events;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lupicus.cc.Main;
import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.config.MyConfig;
import com.lupicus.cc.manager.ClaimManager;
import com.lupicus.cc.manager.ClaimManager.ClaimInfo;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(bus = Bus.FORGE, modid = Main.MODID)
public class PlayerEvents
{
	private static final Logger LOGGER = LogManager.getLogger();

	/** LeftClickBlock replacement */
	public static boolean cancelBlockClick(World world, BlockPos pos, PlayerEntity player)
	{
		if (world.isRemote)
			return false;
		ClaimInfo info = ClaimManager.get(world, pos);
		if (info.okPerm(player) || player.hasPermissionLevel(3))
			return false;
		TileEntity te = world.getTileEntity(info.pos.getPos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantAccess(player))
				return false;
		}
		return true;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
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
		BlockState state = world.getBlockState(event.getPos());
		if (MyConfig.bypassBlocks.contains(state.getBlock()))
			return;
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

	/** for non living entities */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onAttack(AttackEntityEvent event)
	{
		Entity entity = event.getTarget();
		if (entity instanceof LivingEntity)
			return;
		World world = entity.world;
		if (world.isRemote)
			return;
		PlayerEntity player = event.getPlayer();
		ClaimInfo info = ClaimManager.get(world, entity.func_233580_cy_());
		if (info.okPerm(player) || player.hasPermissionLevel(3))
			return;
		TileEntity te = world.getTileEntity(info.pos.getPos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantModify(player))
				return;
		}
		if (event.isCancelable())
		{
			event.setCanceled(true);
			return;
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onAttack(LivingAttackEvent event)
	{
		LivingEntity entity = event.getEntityLiving();
		if (entity.getSoundCategory() == SoundCategory.HOSTILE || entity instanceof PlayerEntity)
			return;
		World world = entity.world;
		if (world.isRemote)
			return;
		Entity srcEntity = event.getSource().getTrueSource();
		if (!(srcEntity instanceof PlayerEntity))
			return;
		PlayerEntity player = (PlayerEntity) srcEntity;
		ClaimInfo info = ClaimManager.get(world, entity.func_233580_cy_());
		if (info.okPerm(player) || player.hasPermissionLevel(3))
			return;
		TileEntity te = world.getTileEntity(info.pos.getPos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantModify(player))
				return;
		}
		if (event.isCancelable())
		{
			event.setCanceled(true);
			return;
		}
	}

	/** player sword sweeping */
	public static boolean cancelEntityAttack(PlayerEntity player, Entity entity)
	{
		if (entity.getSoundCategory() == SoundCategory.HOSTILE || entity instanceof PlayerEntity)
			return false;
		World world = entity.world;
		if (world.isRemote)
			return false;
		ClaimInfo info = ClaimManager.get(world, entity.func_233580_cy_());
		if (info.okPerm(player) || player.hasPermissionLevel(3))
			return false;
		TileEntity te = world.getTileEntity(info.pos.getPos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantModify(player))
				return false;
		}
		return true;
	}

	/** for armor stand */
	public static boolean cancelEntityAttack(Entity entity, DamageSource source)
	{
		World world = entity.world;
		if (world.isRemote)
			return false;
		ClaimInfo info = ClaimManager.get(world, entity.func_233580_cy_());
		Entity srcEntity = source.getTrueSource();
		if (!(srcEntity instanceof PlayerEntity))
			return false;
		PlayerEntity player = (PlayerEntity) srcEntity;
		if (info.okPerm(player) || player.hasPermissionLevel(3))
			return false;
		TileEntity te = world.getTileEntity(info.pos.getPos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantModify(player))
				return false;
		}
		return true;
	}

	public static void handleExplosion(World world, PlayerEntity player, List<Entity> entityList)
	{
		if (entityList.isEmpty())
			return;
		HashMap<ChunkPos, Boolean> cfilter = new HashMap<>();
		for (Iterator<Entity> it = entityList.iterator(); it.hasNext(); )
		{
			Entity e = it.next();
			if (e.getSoundCategory() == SoundCategory.HOSTILE || e instanceof PlayerEntity)
				continue;
			BlockPos pos = e.func_233580_cy_();
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

	/** to handle armor stand but all entities come thru here first */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onInteractAt(EntityInteractSpecific event)
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
		if (MyConfig.bypassEntities.contains(event.getTarget().getType()))
			return;
		if (event.isCancelable())
		{
			event.setCanceled(true);
			return;
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onInteract(EntityInteract event)
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
		if (MyConfig.bypassEntities.contains(event.getTarget().getType()))
			return;
		if (event.isCancelable())
		{
			event.setCanceled(true);
			return;
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
