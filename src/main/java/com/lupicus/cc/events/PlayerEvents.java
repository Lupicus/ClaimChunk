package com.lupicus.cc.events;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;

import com.lupicus.cc.Main;
import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.config.MyConfig;
import com.lupicus.cc.manager.ClaimManager;
import com.lupicus.cc.manager.ClaimManager.ClaimInfo;
import com.lupicus.cc.network.ChangeBlockPacket;
import com.lupicus.cc.network.Network;
import com.lupicus.cc.tileentity.ClaimTileEntity;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
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
	private static final Logger LOGGER = LogUtils.getLogger();

	/** LeftClickBlock replacement */
	public static boolean cancelBlockClick(Level world, BlockPos pos, Player player)
	{
		if (world.isClientSide)
			return false;
		ClaimInfo info = ClaimManager.get(world, pos);
		if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
			return false;
		BlockEntity te = world.getBlockEntity(info.pos.pos());
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
		Player player = event.getEntity();
		Level world = player.level;
		if (world.isClientSide)
			return;
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
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
			ServerPlayer sp = (ServerPlayer) player;
			if (sp.connection != null)
			{
				// fix client side view of some blocks (e.g. door)
				if (!state.canOcclude())
					Network.sendToClient(new ChangeBlockPacket(event.getPos(), state), sp);
				// fix client side view of the hotbar for non creative
				ItemStack itemstack = player.getItemInHand(h);
				if (!itemstack.isEmpty())
				{
					int index = 36 + ((h == InteractionHand.MAIN_HAND) ? sp.getInventory().selected : 9);
					Utility.slotChanged(sp, index, itemstack);
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
		Level world = entity.level;
		if (world.isClientSide)
			return;
		Player player = event.getEntity();
		ClaimInfo info = ClaimManager.get(world, entity.blockPosition());
		if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
			return;
		BlockEntity te = world.getBlockEntity(info.pos.pos());
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
		LivingEntity entity = event.getEntity();
		if (entity.getSoundSource() == SoundSource.HOSTILE || entity instanceof Player)
			return;
		Level world = entity.level;
		if (world.isClientSide)
			return;
		Entity srcEntity = event.getSource().getEntity();
		if (!(srcEntity instanceof Player))
			return;
		Player player = (Player) srcEntity;
		ClaimInfo info = ClaimManager.get(world, entity.blockPosition());
		if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
			return;
		BlockEntity te = world.getBlockEntity(info.pos.pos());
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
	public static boolean cancelEntityAttack(Player player, Entity entity)
	{
		if (entity.getSoundSource() == SoundSource.HOSTILE || entity instanceof Player)
			return false;
		Level world = entity.level;
		if (world.isClientSide)
			return false;
		ClaimInfo info = ClaimManager.get(world, entity.blockPosition());
		if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
			return false;
		BlockEntity te = world.getBlockEntity(info.pos.pos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantModify(player))
				return false;
		}
		return true;
	}

	/** for armor stand */
	public static boolean cancelEntityHurt(Entity entity, DamageSource source)
	{
		Level world = entity.level;
		if (world.isClientSide)
			return false;
		ClaimInfo info = ClaimManager.get(world, entity.blockPosition());
		Entity srcEntity = source.getEntity();
		if (!(srcEntity instanceof Player))
			return false;
		Player player = (Player) srcEntity;
		if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
			return false;
		BlockEntity te = world.getBlockEntity(info.pos.pos());
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			if (cte.grantModify(player))
				return false;
		}
		return true;
	}

	public static void handleExplosion(Level world, Player player, List<Entity> entityList)
	{
		if (entityList.isEmpty())
			return;
		HashMap<ChunkPos, Boolean> cfilter = new HashMap<>();
		for (Iterator<Entity> it = entityList.iterator(); it.hasNext(); )
		{
			Entity e = it.next();
			if (e.getSoundSource() == SoundSource.HOSTILE || e instanceof Player)
				continue;
			BlockPos pos = e.blockPosition();
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

	/** to handle armor stand but all entities come thru here first */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onInteractAt(EntityInteractSpecific event)
	{
		Player player = event.getEntity();
		Level world = player.level;
		if (world.isClientSide)
			return;
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
			return;
		BlockEntity te = world.getBlockEntity(info.pos.pos());
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
		Player player = event.getEntity();
		Level world = player.level;
		if (world.isClientSide)
			return;
		ClaimInfo info = ClaimManager.get(world, event.getPos());
		if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
			return;
		BlockEntity te = world.getBlockEntity(info.pos.pos());
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
		HitResult target = event.getTarget();
		if (target.getType() == HitResult.Type.BLOCK)
		{
			Player player = event.getEntity();
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
			if (info.okPerm(player) || (player.hasPermissions(3) && player.isCreative()))
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
				if (fluid != Fluids.EMPTY && MyConfig.reportBucket)
					LOGGER.info("Placing " + stack + " @ " + world.dimension().location() + " " + blockpos + " by " + player.getName().getString());
				return;
			}
			player.displayClientMessage(ClaimBlock.makeMsg("cc.message.claimed.chunk", info), true);
			if (event.isCancelable())
			{
				event.setCanceled(true);
				Utility.updateHands((ServerPlayer) player);
			}
		}
	}

	@SubscribeEvent
	public static void onRightClickItem(RightClickItem event)
	{
		ItemStack stack = event.getItemStack();
		if (stack.getItem() == Items.PAPER && event.getEntity().isShiftKeyDown())
			ClaimBlock.clearPaper(stack, event.getEntity());
	}
}
