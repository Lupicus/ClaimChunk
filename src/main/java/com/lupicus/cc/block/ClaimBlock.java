package com.lupicus.cc.block;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.lupicus.cc.config.MyConfig;
import com.lupicus.cc.item.ModItems;
import com.lupicus.cc.manager.ClaimManager;
import com.lupicus.cc.manager.ClaimManager.ClaimInfo;
import com.lupicus.cc.network.ClaimScreenPacket;
import com.lupicus.cc.network.Network;
import com.lupicus.cc.tileentity.ClaimTileEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClaimBlock extends Block implements EntityBlock
{
	public static final MapCodec<ClaimBlock> CODEC = simpleCodec(ClaimBlock::new);
	public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
	private static final String DATA_TAG = "ClaimData";
	private static final String ACCESS_LIST = "AccessList";
	private static final String MODIFY_LIST = "ModifyList";
	private static final VoxelShape SHAPE = Stream.of(
			Block.box(0, 0, 0, 16, 1, 16),
			Block.box(4, 1, 4, 12, 2, 12),
			Block.box(5, 2, 5, 11, 3, 11),
			Block.box(6, 3, 6, 10, 8, 10),
			Block.box(3, 8, 3, 13, 12, 13)
			).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

	@Override
	public MapCodec<ClaimBlock> codec() {
		return CODEC;
	}

	public ClaimBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(getStateDefinition().any().setValue(ENABLED, Boolean.valueOf(true)));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level worldIn,
			BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit)
	{
		if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND)
		{
			BlockEntity te = worldIn.getBlockEntity(pos);
			if (te instanceof ClaimTileEntity)
			{
				ClaimTileEntity cte = (ClaimTileEntity) te;
				if (player.hasPermissions(3) && player.isCreative())
					;
				else if (!player.getUUID().equals(cte.owner))
				{
					player.displayClientMessage(Component.translatable("cc.message.block.not_owner"), true);
					return InteractionResult.FAIL;
				}
				if (stack.getItem() == Items.PAPER)
				{
					CustomData data = stack.get(DataComponents.CUSTOM_DATA);
					if (data != null && data.contains(DATA_TAG))
					{
						@SuppressWarnings("deprecation")
						CompoundTag tag = data.getUnsafe().getCompound(DATA_TAG);
						cte.setAccess(tag.getString(ACCESS_LIST));
						cte.setModify(tag.getString(MODIFY_LIST));
						cte.setChanged();
						return InteractionResult.SUCCESS;
					}
					else if (stack.getComponentsPatch().isEmpty())
					{
						ItemStack newstack = (stack.getCount() > 1) ? stack.split(1) : stack;
						CustomData.update(DataComponents.CUSTOM_DATA, newstack,
								t -> {
									CompoundTag tag = new CompoundTag();
									t.put(DATA_TAG, tag);
									tag.putString(ACCESS_LIST, cte.getAccess());
									tag.putString(MODIFY_LIST, cte.getModify());
									});
						setLore(newstack);
						if (stack != newstack && !player.addItem(newstack))
							player.drop(newstack, false);
						return InteractionResult.SUCCESS;
					}
				}
				else
					return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player player,
			BlockHitResult hit)
	{
		if (player instanceof ServerPlayer)
		{
			ServerPlayer splayer = (ServerPlayer) player;
			BlockEntity te = worldIn.getBlockEntity(pos);
			if (te instanceof ClaimTileEntity)
			{
				ClientboundBlockEntityDataPacket supdatetileentitypacket = ((ClaimTileEntity) te).getUpdatePacket();
				if (supdatetileentitypacket != null) {
					splayer.connection.send(supdatetileentitypacket);
				}
				Network.sendToClient(new ClaimScreenPacket(pos), splayer);
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
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
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		Level world = context.getLevel();
		if (world.isClientSide)
			return null; // let server decide

		Player player = context.getPlayer();
		if (player == null || world.getServer().getPlayerList().getPlayer(player.getUUID()) == null)
			return null; // FakePlayer
		BlockPos pos = context.getClickedPos();
		ClaimInfo cinfo = ClaimManager.get(world, pos);
		if (!cinfo.okPerm(player))
		{
			player.displayClientMessage(makeMsg("cc.message.claimed.chunk", cinfo), true);
			return null;
		}
		if (!(player.hasPermissions(3) && player.isCreative()))
		{
			if (MyConfig.checkDim(world))
				return null;
			if (world.dimension() == Level.OVERWORLD)
			{
				LevelData winfo = world.getLevelData();
				ChunkPos scpos = new ChunkPos(winfo.getSpawnPos());
				ChunkPos cpos = new ChunkPos(pos);
				if (scpos.getChessboardDistance(cpos) <= MyConfig.chunksFromSpawn)
				{
					player.displayClientMessage(Component.translatable("cc.message.spawn"), true);
					return null;
				}
			}
			if (cinfo.owner == null)
			{
				if (ClaimManager.mapCount.getOrDefault(player.getUUID(), 0) >= MyConfig.claimLimit)
				{
					player.displayClientMessage(Component.translatable("cc.message.claim_limit"), true);
					return null;
				}
			}
		}
		return super.getStateForPlacement(context);
	}

	@SuppressWarnings("deprecation")
	@Override
	public float getExplosionResistance(BlockState state, BlockGetter world, BlockPos pos, Explosion explosion)
	{
		boolean flag = state.getValue(ENABLED);
		if (flag && MyConfig.pvpMode)
		{
			LivingEntity entity = explosion.getIndirectSourceEntity();
			if (entity instanceof Mob)
			{
				if (MyConfig.mobDestroy)
				{
					Mob mob = (Mob) entity;
					if (mob.getTarget() instanceof Player)
						flag = false;
				}
			}
			else
				flag = false;
		}
		return flag ? Blocks.BEDROCK.getExplosionResistance() : getExplosionResistance();
	}

	@Override
	public void wasExploded(ServerLevel worldIn, BlockPos pos, Explosion explosionIn) {
		ClaimInfo cinfo = ClaimManager.get(worldIn, pos);
		if (cinfo.owner != null && pos.equals(cinfo.pos.pos()))
			ClaimManager.remove(worldIn, pos);
	}

	@Override
	public boolean canEntityDestroy(BlockState state, BlockGetter world, BlockPos pos, Entity entity) {
		return !state.getValue(ENABLED);
	}

	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (worldIn.isClientSide)
			return;
		if (!(placer instanceof Player))
			return;
		Player player = (Player) placer;
		BlockEntity te = worldIn.getBlockEntity(pos);
		if (te instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) te;
			ClaimInfo cinfo = ClaimManager.get(worldIn, pos);

			cte.owner = player.getUUID();
			CustomData data = stack.get(DataComponents.CUSTOM_DATA);
			if (data != null && !data.isEmpty())
			{
				@SuppressWarnings("deprecation")
				CompoundTag tag = data.getUnsafe();
				cte.setAccess(tag.getString(ACCESS_LIST));
				cte.setModify(tag.getString(MODIFY_LIST));
			}
			cte.setChanged();
			if (cinfo.owner != null)
			{
				// replace old block
				BlockPos oldPos = cinfo.pos.pos();
				BlockState oldState = worldIn.getBlockState(oldPos);
				if (oldState.getBlock() == this)
					worldIn.setBlockAndUpdate(oldPos, oldState.setValue(ENABLED, false));
				ClaimManager.replace(worldIn, pos);
			}
			else
			{
				player.displayClientMessage(Component.translatable("cc.message.claim"), true);
				ClaimManager.add(worldIn, pos, player);
			}
		}
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player,
			boolean willHarvest, FluidState fluid)
	{
		boolean claimed = state.getValue(ENABLED);
		if (claimed)
		{
			if (world.isClientSide)
				return false;
			ClaimInfo cinfo = ClaimManager.get(world, pos);
			if (cinfo.owner == null || !pos.equals(cinfo.pos.pos()))
				claimed = false;
			else if (!cinfo.okPerm(player))
			{
				player.displayClientMessage(Component.translatable("cc.message.block.not_owner"), true);
				return false;
			}
		}
		boolean flag = super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
		if (flag && claimed)
		{
			player.displayClientMessage(Component.translatable("cc.message.unclaim"), true);
			ClaimManager.remove(world, pos);
		}
		return flag;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder)
	{
		List<ItemStack> ret = super.getDrops(state, builder);
		BlockEntity entity = builder.getParameter(LootContextParams.BLOCK_ENTITY);
		if (entity instanceof ClaimTileEntity)
		{
			ClaimTileEntity cte = (ClaimTileEntity) entity;
			if (!cte.getAccess().isEmpty() || !cte.getModify().isEmpty())
			{
				for (ItemStack e : ret)
				{
					if (e.getItem() == ModItems.CLAIM_BLOCK)
					{
						CustomData.update(DataComponents.CUSTOM_DATA, e,
								t -> {
									t.putString(ACCESS_LIST, cte.getAccess());
									t.putString(MODIFY_LIST, cte.getModify());
									});
					}
				}
			}
		}
		return ret;
	}

	public static void enableBlock(Level world, BlockPos pos, Player player, UUID owner)
	{
		BlockState state = world.getBlockState(pos);
		if (!state.getValue(ENABLED))
		{
			ClaimInfo cinfo = ClaimManager.get(world, pos);
			if (cinfo.owner == null)
			{
				boolean opFlag = player.hasPermissions(3) && player.isCreative();
				if (!opFlag && ClaimManager.mapCount.getOrDefault(owner, 0) >= MyConfig.claimLimit)
				{
					player.displayClientMessage(Component.translatable("cc.message.claim_limit"), true);
					return;
				}
				ClaimManager.add(world, pos, owner);
			}
			else if (cinfo.owner.equals(owner))
			{
				BlockPos oldPos = cinfo.pos.pos();
				BlockState oldState = world.getBlockState(oldPos);
				if (oldState.getBlock() == ModBlocks.CLAIM_BLOCK)
					world.setBlockAndUpdate(oldPos, oldState.setValue(ENABLED, false));
				ClaimManager.replace(world, pos);
			}
			else
			{
				player.displayClientMessage(makeMsg("cc.message.claimed.chunk", cinfo), true);
				return;
			}
			world.setBlockAndUpdate(pos, state.setValue(ENABLED, true));
		}
	}

	public static void clearPaper(ItemStack stack, Player player)
	{
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data != null && data.contains(DATA_TAG))
		{
			ItemStack newstack = (stack.getCount() > 1) ? stack.split(1) : stack;
			CustomData.update(DataComponents.CUSTOM_DATA, newstack,
					t -> {
						t.remove(DATA_TAG);
						});
			newstack.set(DataComponents.LORE, ItemLore.EMPTY);
			if (stack != newstack && !player.addItem(newstack))
				player.drop(newstack, false);
		}
	}

	private static void setLore(ItemStack stack)
	{
		List<Component> list = new ArrayList<>();
		list.add(Component.translatable("cc.lore.paper"));
		stack.set(DataComponents.LORE, new ItemLore(list));
	}

	public static MutableComponent makeMsg(String trans, ClaimInfo info)
	{
		MutableComponent msg = Component.translatable(trans);
		if (MyConfig.addOwner)
			msg.append(": " + ClaimManager.getName(info));
		return msg;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ENABLED);
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ClaimTileEntity(pos, state);
	}
}
