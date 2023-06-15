package com.lupicus.cc.command;

import java.util.List;
import java.util.UUID;

import com.lupicus.cc.Main;
import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.block.ModBlocks;
import com.lupicus.cc.manager.ClaimManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ClaimsCommand
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("claims")
			.executes((ctx) -> {
				return report(ctx.getSource(), (String) null, 0);
			})
			.then(Commands.argument("limit", IntegerArgumentType.integer(1))
				.executes((ctx) -> {
					return report(ctx.getSource(), (String) null, IntegerArgumentType.getInteger(ctx, "limit"));
				})
			)
			.then(Commands.argument("target", StringArgumentType.string())
				.requires((source) -> {
					return source.hasPermission(3);
				})
				.suggests((ctx, builder) -> {
					String[] list;
					if (isCreative(ctx.getSource()))
						list = ClaimManager.getPlayers();
					else
						list = new String[0];
					return SharedSuggestionProvider.suggest(list, builder);
				})
				.executes((ctx) -> {
					return report(ctx.getSource(), StringArgumentType.getString(ctx, "target"), 0);
				})
				.then(Commands.argument("limit", IntegerArgumentType.integer(1))
					.executes((ctx) -> {
						return report(ctx.getSource(), StringArgumentType.getString(ctx, "target"), IntegerArgumentType.getInteger(ctx, "limit"));
					})
				)
				.then(Commands.literal("remove")
					.executes((ctx) -> {
						return destroy(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
					})
				)
				.then(Commands.literal("check")
					.executes((ctx) -> {
						return check(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
					})
				)
			)
		);
	}

	private static boolean isCreative(CommandSourceStack source)
	{
		Entity e = source.getEntity();
		boolean creative;
		if (e == null)
			creative = true;
		else if (e instanceof ServerPlayer)
			creative = ((ServerPlayer) e).isCreative();
		else
			creative = false;
		return creative;
	}

	private static boolean opCheck(CommandSourceStack source)
	{
		if (isCreative(source))
			return false;
		source.sendFailure(Component.literal("Must be in Creative Mode to perform this command."));
		return true;
	}

	private static int report(CommandSourceStack source, String string, int limit)
			throws CommandSyntaxException
	{
		if (string != null && opCheck(source))
			return 0;
		UUID uuid = null;
		if (string == null)
			uuid = source.getPlayerOrException().getUUID();
		else
			uuid = ClaimManager.getUUID(string);
		List<GlobalPos> list = null;
		if (limit <= 0)
			list = ClaimManager.getList(uuid);
		else
			list = ClaimManager.getNearList(uuid, source.getPlayerOrException(), limit);
		if (list.isEmpty())
		{
			String msg = (string == null) ? "noclaims.you" : "noclaims";
			source.sendSuccess(() -> Component.translatable(Main.MODID + ".message." + msg), false);
			return 0;
		}
		StringBuilder msg = new StringBuilder();
		int count = 0;
		for (GlobalPos g : list)
		{
			String dim = g.dimension().location().toString();
			BlockPos pos = g.pos();
			count++;
			msg.append(String.valueOf(count) + "= " + dim + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "\n");
		}
		msg.setLength(msg.length() - 1);
		source.sendSuccess(() -> Component.literal(msg.toString()), false);
		return 1;
	}

	private static int destroy(CommandSourceStack source, String string)
			throws CommandSyntaxException
	{
		if (opCheck(source))
			return 0;
		UUID uuid = ClaimManager.getUUID(string);
		List<GlobalPos> list = ClaimManager.getList(uuid);
		if (list.isEmpty())
		{
			source.sendSuccess(() -> Component.translatable(Main.MODID + ".message.noclaims"), false);
			return 0;
		}
		for (GlobalPos g : list)
		{
			ResourceKey<Level> dim = g.dimension();
			BlockPos pos = g.pos();
			ServerLevel world = source.getServer().getLevel(dim);
			if (world != null)
			{
				BlockState state = world.getBlockState(pos);
				if (state.getBlock() == ModBlocks.CLAIM_BLOCK)
					world.destroyBlock(pos, false);
			}
			ClaimManager.remove(dim, pos);
		}
		return 1;
	}

	private static int check(CommandSourceStack source, String string)
			throws CommandSyntaxException
	{
		if (opCheck(source))
			return 0;
		UUID uuid = ClaimManager.getUUID(string);
		List<GlobalPos> list = ClaimManager.getList(uuid);
		if (list.isEmpty())
		{
			source.sendSuccess(() -> Component.translatable(Main.MODID + ".message.noclaims"), false);
			return 0;
		}
		int count = 0;
		for (GlobalPos g : list)
		{
			ResourceKey<Level> dim = g.dimension();
			BlockPos pos = g.pos();
			ServerLevel world = source.getServer().getLevel(dim);
			boolean flag = false;
			if (world != null)
			{
				BlockState state = world.getBlockState(pos);
				if (state.getBlock() != ModBlocks.CLAIM_BLOCK || !state.getValue(ClaimBlock.ENABLED))
					flag = true;
			}
			else
				flag = true;
			if (flag && ClaimManager.remove(dim, pos, uuid))
				count++;
		}
		String msg = "Removed " + count + " bad claim chunks";
		source.sendSuccess(() -> Component.literal(msg), false);
		return 1;
	}
}
