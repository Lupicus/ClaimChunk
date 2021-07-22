package com.lupicus.cc.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lupicus.cc.Main;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Main.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
public class MyConfig
{
	private static final Logger LOGGER = LogManager.getLogger();
	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;
	static
	{
		final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
	}

	public static boolean addOwner;
	public static boolean mobDestroy;
	public static int chunksFromSpawn;
	public static int claimLimit;
	public static Set<Block> bypassBlocks;

	@SubscribeEvent
	public static void onModConfigEvent(final ModConfig.ModConfigEvent configEvent)
	{
		if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC)
		{
			bakeConfig();
		}
	}

	public static void bakeConfig()
	{
		addOwner = COMMON.addOwner.get();
		mobDestroy = COMMON.mobDestroy.get();
		chunksFromSpawn = COMMON.chunksFromSpawn.get();
		claimLimit = COMMON.claimLimit.get();
		bypassBlocks = blockSet(COMMON.bypassBlocks.get());
	}

	private static Set<Block> blockSet(List<? extends String> list)
	{
		Set<Block> ret = new HashSet<>();
		for (String name : list)
		{
			try {
				ResourceLocation res = new ResourceLocation(name);
				if (ForgeRegistries.BLOCKS.containsKey(res))
					ret.add(ForgeRegistries.BLOCKS.getValue(res));
				else
					LOGGER.warn("Unknown block: " + name);
			}
			catch (Exception e)
			{
				LOGGER.warn("Bad entry: " + name);
			}
		}
		return ret;
	}

	public static class Common
	{
		public final BooleanValue addOwner;
		public final BooleanValue mobDestroy;
		public final IntValue chunksFromSpawn;
		public final IntValue claimLimit;
		public final ConfigValue<List<? extends String>> bypassBlocks;

		public Common(ForgeConfigSpec.Builder builder)
		{
			List<String> bbDefList = Arrays.asList("minecraft:ender_chest");
			String section_trans = Main.MODID + ".config.";
			addOwner = builder
					.comment("Add owner name to some messages")
					.translation(section_trans + "add_owner")
					.define("AddOwner", true);

			mobDestroy = builder
					.comment("Mob explosions can destroy blocks based on mob target")
					.translation(section_trans + "mob_destroy")
					.define("MobDestroy", true);

			chunksFromSpawn = builder
					.comment("Chunks from world spawn")
					.translation(section_trans + "chunks_from_spawn")
					.defineInRange("ChunksFromSpawn", () -> 10, -1, 5000);

			claimLimit = builder
					.comment("Maximum claims per player")
					.translation(section_trans + "claim_limit")
					.defineInRange("ClaimLimit", () -> 4, 0, 250);

			bypassBlocks = builder
					.comment("Blocks that bypass claims on right click")
					.translation(section_trans + "bypass_blocks")
					.defineList("BypassBlocks", bbDefList, Common::isString);
		}

		public static boolean isString(Object o)
		{
			return (o instanceof String);
		}
	}
}
