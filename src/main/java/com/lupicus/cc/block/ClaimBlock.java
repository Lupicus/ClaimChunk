package com.lupicus.cc.block;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
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
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.storage.IWorldInfo;
import net.minecraftforge.common.util.FakePlayer;

public class ClaimBlock extends Block
{
	public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
	private static final String DATA_TAG = "ClaimData";
	private static final String ACCESS_LIST = "AccessList";
	private static final String MODIFY_LIST = "ModifyList";
	private static final VoxelShape SHAPE = Stream.of(
			Block.makeCuboidShape(0, 0, 0, 16, 1, 16),
			Block.makeCuboidShape(4, 1, 4, 12, 2, 12),
			Block.makeCuboidShape(5, 2, 5, 11, 3, 11),
			Block.makeCuboidShape(6, 3, 6, 10, 8, 10),
			Block.makeCuboidShape(3, 8, 3, 13, 12, 13)
			).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, IBooleanFunction.OR)).get();

	public ClaimBlock(Properties properties) {
		super(properties);
		this.setDefaultState(this.stateContainer.getBaseState().with(ENABLED, Boolean.valueOf(true)));
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return SHAPE;
	}

	@Override
	public PushReaction getPushReaction(BlockState state) {
		return PushReaction.BLOCK;
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
				if (player.hasPermissionLevel(3) && player.isCreative())
					;
				else if (!player.getUniqueID().equals(cte.owner))
				{
					player.sendStatusMessage(new TranslationTextComponent("cc.message.block.not_owner"), true);
					return ActionResultType.FAIL;
				}
				ItemStack stack = player.getHeldItem(handIn);
				Item item = stack.getItem();
				if (item == Items.PAPER)
				{
					CompoundNBT tag = stack.getChildTag(DATA_TAG);
					if (tag != null)
					{
						cte.setAccess(tag.getString(ACCESS_LIST));
						cte.setModify(tag.getString(MODIFY_LIST));
						cte.markDirty();
					}
					else if (!stack.hasTag())
					{
						ItemStack newstack = (stack.getCount() > 1) ? stack.split(1) : stack;
						tag = newstack.getOrCreateChildTag(DATA_TAG);
						tag.putString(ACCESS_LIST, cte.getAccess());
						tag.putString(MODIFY_LIST, cte.getModify());
						setLore(newstack);
						if (stack != newstack && !player.addItemStackToInventory(newstack))
							player.dropItem(newstack, false);
					}
				}
				else if (player instanceof ServerPlayerEntity) {
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
		if (player instanceof FakePlayer)
			return null;
		BlockPos pos = context.getPos();
		ClaimInfo cinfo = ClaimManager.get(world, pos);
		if (!cinfo.okPerm(player))
		{
			player.sendStatusMessage(makeMsg("cc.message.claimed.chunk", cinfo), true);
			return null;
		}
		if (!(player.hasPermissionLevel(3) && player.isCreative()))
		{
			if (MyConfig.checkDim(world))
				return null;
			if (world.func_234923_W_() == World.field_234918_g_) // overworld
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
				if (ClaimManager.mapCount.getOrDefault(player.getUniqueID(), 0) >= MyConfig.claimLimit)
				{
					player.sendStatusMessage(new TranslationTextComponent("cc.message.claim_limit"), true);
					return null;
				}
			}
		}
		return super.getStateForPlacement(context);
	}

	@SuppressWarnings("deprecation")
	@Override
	public float getExplosionResistance(BlockState state, IBlockReader world, BlockPos pos, Explosion explosion)
	{
		boolean flag = state.get(ENABLED);
		if (flag && MyConfig.pvpMode)
		{
			LivingEntity entity = explosion.getExplosivePlacedBy();
			if (entity instanceof MobEntity)
			{
				if (MyConfig.mobDestroy)
				{
					MobEntity mob = (MobEntity) entity;
					if (mob.getAttackTarget() instanceof PlayerEntity)
						flag = false;
				}
			}
			else
				flag = false;
		}
		return flag ? Blocks.BEDROCK.getExplosionResistance() : getExplosionResistance();
	}

	@Override
	public void onExplosionDestroy(World worldIn, BlockPos pos, Explosion explosionIn) {
		ClaimInfo cinfo = ClaimManager.get(worldIn, pos);
		if (cinfo.owner != null && pos.equals(cinfo.pos.getPos()))
			ClaimManager.remove(worldIn, pos);
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
			{
				cte.setAccess(tag.getString(ACCESS_LIST));
				cte.setModify(tag.getString(MODIFY_LIST));
			}
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
		boolean claimed = state.get(ENABLED);
		if (claimed)
		{
			if (world.isRemote)
				return false;
			ClaimInfo cinfo = ClaimManager.get(world, pos);
			if (cinfo.owner == null || !pos.equals(cinfo.pos.getPos()))
				claimed = false;
			else if (!cinfo.okPerm(player))
			{
				player.sendStatusMessage(new TranslationTextComponent("cc.message.block.not_owner"), true);
				return false;
			}
		}
		boolean flag = super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
		if (flag && claimed)
		{
			player.sendStatusMessage(new TranslationTextComponent("cc.message.unclaim"), true);
			ClaimManager.remove(world, pos);
		}
		return flag;
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder)
	{
		List<ItemStack> ret = super.getDrops(state, builder);
		TileEntity entity = builder.get(LootParameters.BLOCK_ENTITY);
		if (entity instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) entity;
			if (!cte.getAccess().isEmpty() || !cte.getModify().isEmpty())
			{
				for (ItemStack e : ret)
				{
					if (e.getItem() == ModItems.CLAIM_BLOCK)
					{
						CompoundNBT tag = e.getOrCreateTag();
						tag.putString(ACCESS_LIST, cte.getAccess());
						tag.putString(MODIFY_LIST, cte.getModify());
					}
				}
			}
		}
		return ret;
	}

	public static void enableBlock(World world, BlockPos pos, PlayerEntity player, UUID owner)
	{
		BlockState state = world.getBlockState(pos);
		if (!state.get(ENABLED))
		{
			ClaimInfo cinfo = ClaimManager.get(world, pos);
			if (cinfo.owner == null)
			{
				boolean opFlag = player.hasPermissionLevel(3) && player.isCreative();
				if (!opFlag && ClaimManager.mapCount.getOrDefault(owner, 0) >= MyConfig.claimLimit)
				{
					player.sendStatusMessage(new TranslationTextComponent("cc.message.claim_limit"), true);
					return;
				}
				ClaimManager.add(world, pos, owner);
			}
			else if (cinfo.owner.equals(owner))
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

	public static void clearPaper(ItemStack stack, PlayerEntity player)
	{
		CompoundNBT tag = stack.getChildTag(DATA_TAG);
		if (tag != null)
		{
			ItemStack newstack = (stack.getCount() > 1) ? stack.split(1) : stack;
			newstack.setTag(null);
			if (stack != newstack && !player.addItemStackToInventory(newstack))
				player.dropItem(newstack, false);
		}
	}

	private static void setLore(ItemStack stack)
	{
		CompoundNBT tag = stack.getOrCreateChildTag("display");
		ListNBT list = new ListNBT();
		list.add(StringNBT.valueOf(ITextComponent.Serializer.toJson(new TranslationTextComponent("cc.lore.paper"))));
		tag.put("Lore", list);
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
