package com.lupicus.cc.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lupicus.cc.Main;

import net.minecraft.block.Block;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

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
	public static boolean pvpMode;
	public static int chunksFromSpawn;
	public static int claimLimit;
	public static Set<Block> bypassBlocks;
	public static Set<String> excludeDimSet;
	public static Set<String> includeDimSet;
	public static boolean allDims;

	public static boolean checkDim(World world)
	{
		String key = world.func_234923_W_().func_240901_a_().toString();
		if (!(allDims || includeDimSet.contains(key)) ||
			excludeDimSet.contains(key))
			return true;
		return false;
	}

	@SubscribeEvent
	public static void onModConfigEvent(final ModConfigEvent configEvent)
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
		pvpMode = COMMON.pvpMode.get();
		chunksFromSpawn = COMMON.chunksFromSpawn.get();
		claimLimit = COMMON.claimLimit.get();
		bypassBlocks = blockSet(COMMON.bypassBlocks.get());
		String[] temp = extract(COMMON.includeDims.get());
		allDims = hasAll(temp);
		if (allDims)
			temp = new String[0];
		includeDimSet = stringSet(temp);
		excludeDimSet = stringSet(extract(COMMON.excludeDims.get()));
	}

	private static Set<String> stringSet(String[] values)
	{
		HashSet<String> ret = new HashSet<>();
		for (String name : values)
		{
			ret.add(name);
		}
		return ret;
	}

	private static Set<Block> blockSet(List<? extends String> list)
	{
		Set<Block> ret = new HashSet<>();
		IForgeRegistry<Block> reg = ForgeRegistries.BLOCKS;
		for (String name : list)
		{
			int i = name.indexOf(":*");
			if (i > 0)
			{
				expandMod(reg, ret, name.substring(0, i), name.substring(i + 2));
				continue;
			}
			try {
				ResourceLocation res = new ResourceLocation(name);
				if (reg.containsKey(res))
					ret.add(reg.getValue(res));
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

	private static void expandMod(IForgeRegistry<Block> reg, Set<Block> set, String name, String minus)
	{
		if (!ModList.get().isLoaded(name))
		{
			LOGGER.warn("Unknown mod entry in BypassBlocks: " + name);
			return;
		}

		Set<String> minusSet = new HashSet<>();
		for (String n : minus.split("-"))
		{
			String nt = n.trim();
			if (!nt.isEmpty())
				minusSet.add(nt);
		}
		for (Entry<RegistryKey<Block>, Block> entry : reg.getEntries())
		{
			ResourceLocation key = entry.getKey().func_240901_a_();
			if (name.equals(key.getNamespace()) && !minusSet.contains(key.getPath()))
			{
				Block block = entry.getValue();
				set.add(block);
			}
		}
	}

	private static boolean hasAll(String[] values)
	{
		for (String name : values)
		{
			if (name.equals("*"))
				return true;
		}
		return false;
	}

	private static String[] extract(List<? extends String> value)
	{
		return value.toArray(new String[value.size()]);
	}

	public static class Common
	{
		public final BooleanValue addOwner;
		public final BooleanValue mobDestroy;
		public final BooleanValue pvpMode;
		public final IntValue chunksFromSpawn;
		public final IntValue claimLimit;
		public final ConfigValue<List<? extends String>> bypassBlocks;
		public final ConfigValue<List<? extends String>> includeDims;
		public final ConfigValue<List<? extends String>> excludeDims;

		public Common(ForgeConfigSpec.Builder builder)
		{
			List<String> bbDefList = Arrays.asList("minecraft:ender_chest");
			List<String> idDefList = Arrays.asList("*");
			List<String> edDefList = Arrays.asList("");
			String section_trans = Main.MODID + ".config.";

			addOwner = builder
					.comment("Add owner name to some messages")
					.translation(section_trans + "add_owner")
					.define("AddOwner", true);

			mobDestroy = builder
					.comment("Mob explosions can destroy blocks based on mob target")
					.translation(section_trans + "mob_destroy")
					.define("MobDestroy", true);

			pvpMode = builder
					.comment("Explosions caused by any player can destroy blocks")
					.translation(section_trans + "player_destroy")
					.define("PvpMode", false);

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

			includeDims = builder
					.comment("Include dimensions")
					.translation(section_trans + "include_dims")
					.defineList("IncludeDims", idDefList, Common::isString);

			excludeDims = builder
					.comment("Exclude dimensions")
					.translation(section_trans + "exclude_dims")
					.defineList("ExcludeDims", edDefList, Common::isString);
		}

		public static boolean isString(Object o)
		{
			return (o instanceof String);
		}
	}
}
