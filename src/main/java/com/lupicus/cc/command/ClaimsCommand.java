package com.lupicus.cc.command;

import java.util.List;
import java.util.UUID;

import com.lupicus.cc.Main;
import com.lupicus.cc.manager.ClaimManager;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ClaimsCommand
{
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		dispatcher.register(Commands.literal("claims")
			.executes((ctx) -> {
				return report(ctx.getSource(), null);
			})
			.then(Commands.argument("target", StringArgumentType.string())
				.requires((source) -> {
					return source.hasPermissionLevel(3);
				})
				.suggests((ctx, builder) -> {
					return ISuggestionProvider.suggest(ClaimManager.getPlayers(), builder);
				})
				.executes((ctx) -> {
					return report(ctx.getSource(), StringArgumentType.getString(ctx, "target"));
				})
			)
		);
	}

	private static int report(CommandSource source, String string)
			throws CommandSyntaxException
	{
		UUID uuid = null;
		if (string == null)
			uuid = source.asPlayer().getUniqueID();
		else
		{
			PlayerProfileCache cache = source.getServer().getPlayerProfileCache();
			GameProfile info = cache.getGameProfileForUsername(string);
			if (info != null)
				uuid = info.getId();
			else
			{
				try {
					uuid = UUID.fromString(string);
				}
				catch (Exception e) {
				}
				if (uuid == null)
					return 0;
			}
		}
		List<GlobalPos> list = ClaimManager.getList(uuid);
		if (list.isEmpty())
		{
			String msg = (string == null) ? "noclaims.you" : "noclaims";
			source.sendFeedback(new TranslationTextComponent(Main.MODID + ".message." + msg), false);
			return 1;
		}
		StringBuilder msg = new StringBuilder();
		int count = 0;
		for (GlobalPos g : list)
		{
			String dim = g.func_239646_a_().func_240901_a_().toString();
			BlockPos pos = g.getPos();
			count++;
			msg.append(String.valueOf(count) + "= " + dim + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "\n");
		}
		msg.setLength(msg.length() - 1);
		source.sendFeedback(new StringTextComponent(msg.toString()), false);
		return 1;
	}
}
