package com.lupicus.cc.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;

import org.slf4j.Logger;

import com.lupicus.cc.config.MyConfig;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

public class ClaimManager
{
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final HashMap<GlobalPos, ClaimInfo> mapInfo = new HashMap<>();
	public static final HashMap<UUID, Integer> mapCount = new HashMap<>();
	public static final int VERSION = 100;
	private static File claimFile;
	private static MinecraftServer server;
	private static List<ClaimInfo> delayList = new ArrayList<>();
	public static final ClaimInfo EMPTY = new ClaimInfo(null, null) {
		@Override
		public boolean okPerm(Player player)
		{
			return true;
		}
	};

	public static boolean add(Level world, BlockPos pos, Player player)
	{
		return add(world, pos, player.getUUID());
	}

	public static boolean add(Level world, BlockPos pos, UUID owner)
	{
		GlobalPos key = GlobalPos.of(world.dimension(), new ChunkPos(pos).getWorldPosition());
		ClaimInfo info = mapInfo.get(key);
		if (info != null)
			return false;
		info = new ClaimInfo(owner, GlobalPos.of(world.dimension(), pos));
		mapInfo.put(key, info);
		save();
		int count = mapCount.getOrDefault(info.owner, 0);
		mapCount.put(info.owner, count + 1);
		return true;
	}

	public static void remove(Level world, BlockPos pos)
	{
		remove(world.dimension(), pos);
	}

	public static void remove(ResourceKey<Level> dim, BlockPos pos)
	{
		GlobalPos key = GlobalPos.of(dim, new ChunkPos(pos).getWorldPosition());
		ClaimInfo old = mapInfo.remove(key);
		if (old != null)
		{
			save();
			int count = mapCount.getOrDefault(old.owner, 0) - 1;
			if (count < 0)
				count = 0;
			mapCount.put(old.owner, count);
		}
	}

	public static boolean remove(ResourceKey<Level> dim, BlockPos pos, UUID owner)
	{
		GlobalPos key = GlobalPos.of(dim, new ChunkPos(pos).getWorldPosition());
		ClaimInfo info = mapInfo.get(key);
		if (info != null && info.owner.equals(owner))
		{
			mapInfo.remove(key);
			save();
			int count = mapCount.getOrDefault(info.owner, 0) - 1;
			if (count < 0)
				count = 0;
			mapCount.put(info.owner, count);
			return true;
		}
		return false;
	}

	public static boolean replace(Level world, BlockPos pos)
	{
		GlobalPos key = GlobalPos.of(world.dimension(), new ChunkPos(pos).getWorldPosition());
		ClaimInfo info = mapInfo.get(key);
		if (info == null)
			return false;
		info.pos = GlobalPos.of(world.dimension(), pos);
		mapInfo.put(key, info);
		save();
		return true;
	}

	public static ClaimInfo get(Level world, BlockPos pos)
	{
		GlobalPos key = GlobalPos.of(world.dimension(), new ChunkPos(pos).getWorldPosition());
		return mapInfo.getOrDefault(key, EMPTY);
	}

	public static ClaimInfo get(Level world, ChunkPos pos)
	{
		GlobalPos key = GlobalPos.of(world.dimension(), pos.getWorldPosition());
		return mapInfo.getOrDefault(key, EMPTY);
	}

	public static void check(Level world, BlockPos pos, UUID owner)
	{
		GlobalPos bpos = GlobalPos.of(world.dimension(), pos);
		if (claimFile == null)
		{
			// before file has been loaded
			delayList.add(new ClaimInfo(owner, bpos));
			return;
		}

		check(bpos, owner);
	}

	private static void check(GlobalPos bpos, UUID owner)
	{
		GlobalPos key = GlobalPos.of(bpos.dimension(), new ChunkPos(bpos.pos()).getWorldPosition());
		ClaimInfo info = mapInfo.get(key);
		if (info == null)
		{
			info = new ClaimInfo(owner, bpos);
			mapInfo.put(key, info);
			save();
			int count = mapCount.getOrDefault(info.owner, 0) + 1;
			mapCount.put(info.owner, count);
			if (count > MyConfig.claimLimit)
				warnLimit(info, count);
		}
		else
		{
			boolean update = false;
			if (!info.owner.equals(owner))
			{
				int count = mapCount.getOrDefault(info.owner, 0) - 1;
				if (count < 0)
					count = 0;
				mapCount.put(info.owner, count);
				info.owner = owner;
				count = mapCount.getOrDefault(info.owner, 0) + 1;
				mapCount.put(info.owner, count);
				if (count > MyConfig.claimLimit)
					warnLimit(info, count);
				update = true;
			}
			if (!info.pos.equals(bpos))
			{
				info.pos = bpos;
				update = true;
			}
			if (update)
				save();
		}
	}

	public static List<GlobalPos> getList(UUID uuid)
	{
		List<GlobalPos> ret = new ArrayList<>();
		for (ClaimInfo e : mapInfo.values())
		{
			if (e.owner.equals(uuid))
				ret.add(e.pos);
		}
		ret.sort(new GPComp());
		return ret;
	}

	public static List<GlobalPos> getNearList(UUID uuid, Player player, int limit)
	{
		List<GlobalPos> ret = new ArrayList<>();
		BlockPos loc = player.blockPosition();
		ResourceKey<Level> dim = player.level.dimension();
		PriorityQueue<GPDist> queue = new PriorityQueue<>();
		for (ClaimInfo e : mapInfo.values())
		{
			if (e.owner.equals(uuid) && e.pos.dimension() == dim)
			{
				BlockPos pos = e.pos.pos();
				double dx = (double) pos.getX() - (double) loc.getX();
				double dz = (double) pos.getZ() - (double) loc.getZ();
				queue.add(new GPDist(e.pos, dx * dx + dz * dz));
			}
		}
		int k = Math.min(queue.size(), limit);
		for (int i = 0; i < k; ++i)
		{
			ret.add(queue.poll().pos);
		}
		return ret;
	}

	public static String[] getPlayers()
	{
		String[] ret = new String[mapCount.size()];
		GameProfileCache cache = server.getProfileCache();
		int i = 0;
		for (UUID e : mapCount.keySet())
		{
			Optional<GameProfile> profile = cache.get(e);
			if (profile.isPresent())
				ret[i] = profile.get().getName();
			else
				ret[i] = e.toString();
			i++;
		}
		Arrays.sort(ret);
		return ret;
	}

	public static UUID getUUID(String name)
	{
		GameProfileCache cache = server.getProfileCache();
		for (UUID e : mapCount.keySet())
		{
			Optional<GameProfile> profile = cache.get(e);
			if (profile.isPresent())
			{
				if (name.equalsIgnoreCase(profile.get().getName()))
					return e;
			}
		}
		try {
			return UUID.fromString(name);
		}
		catch (Exception e) {
		}
		return null;
	}

	public static String getName(ClaimInfo info)
	{
		Optional<GameProfile> profile = server.getProfileCache().get(info.owner);
		return profile.isPresent() ? profile.get().getName() : "?";
	}

	private static void warnLimit(ClaimInfo info, int count)
	{
		Optional<GameProfile> profile = server.getProfileCache().get(info.owner);
		if (profile.isPresent())
			LOGGER.warn(profile.get().getName() + " exceeding limit: " + count);
		else
			LOGGER.warn(info.owner + " exceeding limit: " + count);
	}

	private static void applyDelay()
	{
		List<ClaimInfo> copy = new ArrayList<>(delayList);
		delayList.clear();
		for (ClaimInfo e : copy)
		{
			check(e.pos, e.owner);
		}
	}

	public static void load(MinecraftServer server)
	{
		ClaimManager.server = server;
		File file1 = server.getWorldPath(LevelResource.ROOT).toFile();
		claimFile = new File(file1, "data/claimchunk.dat");
		if (claimFile.exists())
		{
			try {
				FileInputStream f = new FileInputStream(claimFile);
				DataInputStream fd = new DataInputStream(new BufferedInputStream(f));
				readData(fd);
				fd.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		applyDelay();
		report();
	}

	private static void readData(DataInputStream fd) throws IOException
	{
		// read version
		int version = fd.readInt();
		if (version != VERSION)
		{
			return;
		}

		// read players
		int size = fd.readInt();
		if (size <= 0)
			return;

		UUID[] uuidList = new UUID[size];
		for (int i = 0; i < size; ++i)
		{
			long lm = fd.readLong();
			long ll = fd.readLong();
			uuidList[i] = new UUID(lm, ll);
		}

		// read dim
		size = fd.readInt();
		if (size <= 0)
			return;
		@SuppressWarnings("unchecked")
		ResourceKey<Level>[] dimList = new ResourceKey[size];
		for (int i = 0; i < size; ++i)
		{
			int len = fd.readInt();
			if (len < 0)
				len = 0;
			StringBuffer buf = new StringBuffer(len);
			for (int j = 0; j < len; ++j)
				buf.append(fd.readChar());
			dimList[i] = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(buf.toString()));
		}

		// read positions
		size = fd.readInt();
		if (size <= 0)
			return;
		int[] data = new int[5];
		for (int i = 0; i < size; ++i)
		{
			// PI DI PX PY PZ
			for (int k = 0; k < 5; ++k)
				data[k] = fd.readInt();
			int j = data[1];
			if (j < 0 || j >= dimList.length)
				continue;
			BlockPos pos = new BlockPos(data[2], data[3], data[4]);
			GlobalPos key = GlobalPos.of(dimList[j], new ChunkPos(pos).getWorldPosition());
			j = data[0];
			if (j < 0 || j >= uuidList.length)
				continue;
			ClaimInfo info = new ClaimInfo(uuidList[j], GlobalPos.of(key.dimension(), pos));
			mapInfo.put(key, info);
			int count = mapCount.getOrDefault(info.owner, 0);
			mapCount.put(info.owner, count + 1);
		}
	}

	private static void report()
	{
		HashSet<ResourceKey<Level>> set = new HashSet<>();
		int max = 0;
		for (Integer e : mapCount.values())
			if (e.intValue() > max)
				max = e.intValue();
		for (ClaimInfo e : mapInfo.values())
			set.add(e.pos.dimension());
		LOGGER.info("Claims: " + mapCount.size() + " players, " + set.size() + " dims, " + mapInfo.size() + " chunks, " + max + " max chunks");
		if (max > MyConfig.claimLimit)
		{
			for (Entry<UUID, Integer> e : mapCount.entrySet())
			{
				int count = e.getValue().intValue();
				if (count > MyConfig.claimLimit)
					warnLimit(new ClaimInfo(e.getKey(), null), count);
			}
		}
	}

	public static void save()
	{
		if (claimFile == null)
			return;
		try {
			FileOutputStream f = new FileOutputStream(claimFile);
			DataOutputStream fd = new DataOutputStream(new BufferedOutputStream(f));
			writeData(fd);
			fd.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeData(DataOutputStream fd) throws IOException
	{
		HashMap<UUID, Integer> mapUUID = new HashMap<>();
		HashMap<ResourceKey<Level>, Integer> mapDim = new HashMap<>();
		int indexUUID = 0;
		int indexDim = 0;
		int index = 0;
		int size = mapInfo.size();

		fd.writeInt(VERSION);

		if (size == 0)
		{
			fd.writeInt(0);
			fd.writeInt(0);
			fd.writeInt(0);
			return;
		}

		int[] data = new int[size * 5];

		for (ClaimInfo e : mapInfo.values())
		{
			int pi;
			Integer ppi = mapUUID.get(e.owner);
			if (ppi == null)
			{
				pi = indexUUID++;
				mapUUID.put(e.owner, pi);
			}
			else
				pi = ppi.intValue();
			int di;
			Integer pdi = mapDim.get(e.pos.dimension());
			if (pdi == null)
			{
				di = indexDim++;
				mapDim.put(e.pos.dimension(), di);
			}
			else
				di = pdi.intValue();
			BlockPos pos = e.pos.pos();
			data[index] = pi;
			data[index + 1] = di;
			data[index + 2] = pos.getX();
			data[index + 3] = pos.getY();
			data[index + 4] = pos.getZ();
			index += 5;
		}

		// write player list
		size = mapUUID.size();
		UUID[] uuidList = new UUID[size];
		for (Entry<UUID, Integer> e : mapUUID.entrySet())
		{
			uuidList[e.getValue()] = e.getKey();
		}
		fd.writeInt(size);
		for (int i = 0; i < size; ++i)
		{
			fd.writeLong(uuidList[i].getMostSignificantBits());
			fd.writeLong(uuidList[i].getLeastSignificantBits());
		}

		// write dim list
		size = mapDim.size();
		ResourceLocation[] resList = new ResourceLocation[size];
		for (Entry<ResourceKey<Level>, Integer> e : mapDim.entrySet())
		{
			resList[e.getValue()] = e.getKey().location();
		}
		fd.writeInt(size);
		for (int i = 0; i < size; ++i)
		{
			String str = resList[i].toString();
			fd.writeInt(str.length());
			fd.writeChars(str);
		}

		// write positions
		size = data.length;
		fd.writeInt(size / 5);
		for (int i = 0; i < size; ++i)
			fd.writeInt(data[i]);
	}

	public static void clear()
	{
		server = null;
		claimFile = null;
		mapInfo.clear();
		mapCount.clear();
	}

	public static class ClaimInfo
	{
		public UUID owner;
		public GlobalPos pos;

		public ClaimInfo(UUID owner, GlobalPos pos)
		{
			this.owner = owner;
			this.pos = pos;
		}

		public boolean okPerm(Player player)
		{
			return (player.getUUID().equals(owner));
		}
	}

	public static class GPComp implements Comparator<GlobalPos>
	{
		@Override
		public int compare(GlobalPos o1, GlobalPos o2) {
			ResourceKey<Level> w1 = o1.dimension();
			ResourceKey<Level> w2 = o2.dimension();
			if (w1 != w2)
				return w1.location().compareTo(w2.location());
			BlockPos p1 = o1.pos();
			BlockPos p2 = o2.pos();
			int dx = p1.getX() - p2.getX();
			if (dx != 0)
				return dx;
			return p1.getZ() - p2.getZ();
		}
	}

	public static class GPDist implements Comparable<GPDist>
	{
		public GlobalPos pos;
		public double distSq;

		public GPDist(GlobalPos pos, double distSq)
		{
			this.pos = pos;
			this.distSq = distSq;
		}

		@Override
		public int compareTo(GPDist var1) {
			if (this.distSq < var1.distSq) return -1;
			else if (this.distSq > var1.distSq) return 1;
			return 0;
		}
	}
}
