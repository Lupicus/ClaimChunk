package com.lupicus.cc.block;

import java.util.List;

import com.lupicus.cc.config.MyConfig;
import com.lupicus.cc.item.ModItems;
import com.lupicus.cc.manager.ClaimManager;
import com.lupicus.cc.manager.ClaimManager.ClaimInfo;
import com.lupicus.cc.network.ClaimScreenPacket;
import com.lupicus.cc.network.Network;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootParameters;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.storage.IWorldInfo;

public class ClaimBlock extends Block
{
	public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

	public ClaimBlock(Properties properties) {
		super(properties);
		this.setDefaultState(this.stateContainer.getBaseState().with(ENABLED, Boolean.valueOf(true)));
	}

	@Override
	public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player,
			Hand handIn, BlockRayTraceResult hit)
	{
		if (!worldIn.isRemote)
		{
			TileEntity te = worldIn.getTileEntity(pos);
			if (te instanceof ClaimTileEntity)
			{
				ClaimTileEntity cte = (ClaimTileEntity) te;
				if (player.hasPermissionLevel(3))
					;
				else if (!player.getUniqueID().equals(cte.owner))
				{
					player.sendStatusMessage(new TranslationTextComponent("cc.message.block.not_owner"), true);
					return ActionResultType.FAIL;
				}
				if (player instanceof ServerPlayerEntity) {
					ServerPlayerEntity splayer = (ServerPlayerEntity) player;
					SUpdateTileEntityPacket supdatetileentitypacket = cte.getUpdatePacket();
					if (supdatetileentitypacket != null) {
						splayer.connection.sendPacket(supdatetileentitypacket);
					}
					Network.sendToClient(new ClaimScreenPacket(pos), splayer);
				}
				return ActionResultType.SUCCESS;
			}
		}
		return ActionResultType.CONSUME;
	}

//	@Override
//	public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving)
//	{
//		if (state.getBlock() != newState.getBlock())
//		{
//			LOGGER.warn("ClaimBlock has been replaced @ " + worldIn.func_234923_W_().func_240901_a_() + " " + pos);
//			if (hasTileEntity(state))
//				worldIn.removeTileEntity(pos);
//		}
//	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		World world = context.getWorld();
		if (world.isRemote)
			return null; // let server decide

		PlayerEntity player = context.getPlayer();
		BlockPos pos = context.getPos();
		ClaimInfo cinfo = ClaimManager.get(world, pos);
		if (!cinfo.okPerm(player))
		{
			player.sendStatusMessage(makeMsg("cc.message.claimed.chunk", cinfo), true);
			return null;
		}
		boolean doCheck = !player.hasPermissionLevel(3);
		if (doCheck && world.func_234923_W_() == World.field_234918_g_) // overworld
		{
			IWorldInfo winfo = world.getWorldInfo();
			ChunkPos scpos = new ChunkPos(new BlockPos(winfo.getSpawnX(), 0, winfo.getSpawnZ()));
			ChunkPos cpos = new ChunkPos(context.getPos());
			if (scpos.getChessboardDistance(cpos) <= MyConfig.chunksFromSpawn)
			{
				player.sendStatusMessage(new TranslationTextComponent("cc.message.spawn"), true);
				return null;
			}
		}
		if (cinfo.owner == null)
		{
			if (doCheck && ClaimManager.mapCount.getOrDefault(player.getUniqueID(), 0) >= MyConfig.claimLimit)
			{
				player.sendStatusMessage(new TranslationTextComponent("cc.message.claim_limit"), true);
				return null;
			}
		}

		return super.getStateForPlacement(context);
	}

	@SuppressWarnings("deprecation")
	@Override
	public float getExplosionResistance(BlockState state, IBlockReader world, BlockPos pos, Explosion explosion) {
		return state.get(ENABLED) ? Blocks.BEDROCK.getExplosionResistance() : getExplosionResistance();
	}

	@Override
	public boolean canEntityDestroy(BlockState state, IBlockReader world, BlockPos pos, Entity entity) {
		return !state.get(ENABLED);
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (worldIn.isRemote)
			return;
		if (!(placer instanceof PlayerEntity))
			return;
		PlayerEntity player = (PlayerEntity) placer;
		TileEntity te = worldIn.getTileEntity(pos);
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			ClaimInfo cinfo = ClaimManager.get(worldIn, pos);

			cte.owner = player.getUniqueID();
			CompoundNBT tag = stack.getTag();
			if (tag != null)
				cte.accessList = tag.getString("AccessList");
			cte.markDirty();
			if (cinfo.owner != null)
			{
				// replace old block
				BlockPos oldPos = cinfo.pos.getPos();
				BlockState oldState = worldIn.getBlockState(oldPos);
				if (oldState.getBlock() == this)
					worldIn.setBlockState(oldPos, oldState.with(ENABLED, false));
				ClaimManager.replace(worldIn, pos);
			}
			else
			{
				player.sendStatusMessage(new TranslationTextComponent("cc.message.claim"), true);
				ClaimManager.add(worldIn, pos, player);
			}
		}
	}

	@Override
	public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player,
			boolean willHarvest, FluidState fluid)
	{
		boolean flag = super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
		if (flag && !world.isRemote && state.get(ENABLED))
		{
			player.sendStatusMessage(new TranslationTextComponent("cc.message.unclaim"), true);
			ClaimManager.remove(world, pos);
		}
		return flag;
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<ItemStack> getDrops(BlockState state, net.minecraft.loot.LootContext.Builder builder)
	{
		List<ItemStack> ret = super.getDrops(state, builder);
		TileEntity entity = builder.get(LootParameters.BLOCK_ENTITY);
		if (entity instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) entity;
			if (!cte.accessList.isEmpty())
			{
				for (ItemStack e : ret)
				{
					if (e.getItem() == ModItems.CLAIM_BLOCK)
					{
						CompoundNBT tag = e.getOrCreateTag();
						tag.putString("AccessList", cte.accessList);
					}
				}
			}
		}
		return ret;
	}

	public static void enableBlock(World world, BlockPos pos, PlayerEntity player)
	{
		BlockState state = world.getBlockState(pos);
		if (!state.get(ENABLED))
		{
			ClaimInfo cinfo = ClaimManager.get(world, pos);
			if (cinfo.owner == null)
			{
				boolean doCheck = !player.hasPermissionLevel(3);
				if (doCheck && ClaimManager.mapCount.getOrDefault(player.getUniqueID(), 0) >= MyConfig.claimLimit)
				{
					player.sendStatusMessage(new TranslationTextComponent("cc.message.claim_limit"), true);
					return;
				}
				ClaimManager.add(world, pos, player);
			}
			else if (cinfo.okPerm(player))
			{
				BlockPos oldPos = cinfo.pos.getPos();
				BlockState oldState = world.getBlockState(oldPos);
				if (oldState.getBlock() == ModBlocks.CLAIM_BLOCK)
					world.setBlockState(oldPos, oldState.with(ENABLED, false));
				ClaimManager.replace(world, pos);
			}
			else
			{
				player.sendStatusMessage(makeMsg("cc.message.claimed.chunk", cinfo), true);
				return;
			}
			world.setBlockState(pos, state.with(ENABLED, true));
		}
	}

	public static TextComponent makeMsg(String trans, ClaimInfo info)
	{
		TextComponent msg = new TranslationTextComponent(trans);
		if (MyConfig.addOwner)
			msg.func_230529_a_(new StringTextComponent(": " + ClaimManager.getName(info)));
		return msg;
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		builder.add(ENABLED);
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new ClaimTileEntity();
	}
}
