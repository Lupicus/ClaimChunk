package com.lupicus.cc.config;

import org.apache.commons.lang3.tuple.Pair;

import com.lupicus.cc.Main;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod.EventBusSubscriber(modid = Main.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
public class MyConfig
{
	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;
	static
	{
		final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
	}

	public static int chunksFromSpawn;
	public static int claimLimit;

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
		chunksFromSpawn = COMMON.chunksFromSpawn.get();
		claimLimit = COMMON.claimLimit.get();
	}

	public static class Common
	{
		public final IntValue chunksFromSpawn;
		public final IntValue claimLimit;

		public Common(ForgeConfigSpec.Builder builder)
		{
			String section_trans = Main.MODID + ".config.";
			chunksFromSpawn = builder
					.comment("Chunks from world spawn")
					.translation(section_trans + "chunks_from_spawn")
					.defineInRange("ChunksFromSpawn", () -> 10, 1, 5000);

			claimLimit = builder
					.comment("Maximum claims per player")
					.translation(section_trans + "claim_limit")
					.defineInRange("ClaimLimit", () -> 4, 1, 11);
		}
	}
}