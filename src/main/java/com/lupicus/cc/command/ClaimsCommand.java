package com.lupicus.cc.command;

import java.util.List;
import java.util.UUID;

import com.lupicus.cc.Main;
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
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ClaimsCommand
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("claims")
			.executes((ctx) -> {
				return report(ctx.getSource(), (String) null, -1);
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
					return SharedSuggestionProvider.suggest(ClaimManager.getPlayers(), builder);
				})
				.executes((ctx) -> {
					return report(ctx.getSource(), StringArgumentType.getString(ctx, "target"), -1);
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
			)
		);
	}

	private static int report(CommandSourceStack source, String string, int limit)
			throws CommandSyntaxException
	{
		UUID uuid = null;
		if (string == null)
			uuid = source.getPlayerOrException().getUUID();
		else
			uuid = ClaimManager.getUUID(string);
		List<GlobalPos> list = null;
		if (limit < 0)
			list = ClaimManager.getList(uuid);
		else
			list = ClaimManager.getNearList(uuid, source.getPlayerOrException(), limit);
		if (list.isEmpty())
		{
			String msg = (string == null) ? "noclaims.you" : "noclaims";
			source.sendSuccess(new TranslatableComponent(Main.MODID + ".message." + msg), false);
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
		source.sendSuccess(new TextComponent(msg.toString()), false);
		return 1;
	}

	private static int destroy(CommandSourceStack source, String string)
			throws CommandSyntaxException
	{
		UUID uuid = null;
		if (string != null)
			uuid = ClaimManager.getUUID(string);
		List<GlobalPos> list = ClaimManager.getList(uuid);
		if (list.isEmpty())
		{
			source.sendSuccess(new TranslatableComponent(Main.MODID + ".message.noclaims"), false);
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
			ClaimManager.remove(world, pos);
		}
		return 1;
	}
}
