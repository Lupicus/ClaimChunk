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
import java.util.PriorityQueue;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lupicus.cc.config.MyConfig;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.storage.FolderName;

public class ClaimManager
{
	public static final Logger LOGGER = LogManager.getLogger();
	public static final HashMap<GlobalPos, ClaimInfo> mapInfo = new HashMap<>();
	public static final HashMap<UUID, Integer> mapCount = new HashMap<>();
	public static final int VERSION = 100;
	private static File claimFile;
	private static MinecraftServer server;
	private static List<ClaimInfo> delayList = new ArrayList<>();
	public static final ClaimInfo EMPTY = new ClaimInfo(null, null) {
		@Override
		public boolean okPerm(PlayerEntity player)
		{
			return true;
		}
	};

	public static boolean add(World world, BlockPos pos, PlayerEntity player)
	{
		return add(world, pos, player.getUniqueID());
	}

	public static boolean add(World world, BlockPos pos, UUID owner)
	{
		GlobalPos key = GlobalPos.func_239648_a_(world.func_234923_W_(), new ChunkPos(pos).asBlockPos());
		ClaimInfo info = mapInfo.get(key);
		if (info != null)
			return false;
		info = new ClaimInfo(owner, GlobalPos.func_239648_a_(world.func_234923_W_(), pos));
		mapInfo.put(key, info);
		save();
		int count = mapCount.getOrDefault(info.owner, 0);
		mapCount.put(info.owner, count + 1);
		return true;
	}

	public static void remove(World world, BlockPos pos)
	{
		GlobalPos key = GlobalPos.func_239648_a_(world.func_234923_W_(), new ChunkPos(pos).asBlockPos());
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

	public static boolean replace(World world, BlockPos pos)
	{
		GlobalPos key = GlobalPos.func_239648_a_(world.func_234923_W_(), new ChunkPos(pos).asBlockPos());
		ClaimInfo info = mapInfo.get(key);
		if (info == null)
			return false;
		info.pos = GlobalPos.func_239648_a_(world.func_234923_W_(), pos);
		mapInfo.put(key, info);
		save();
		return true;
	}

	public static ClaimInfo get(World world, BlockPos pos)
	{
		GlobalPos key = GlobalPos.func_239648_a_(world.func_234923_W_(), new ChunkPos(pos).asBlockPos());
		return mapInfo.getOrDefault(key, EMPTY);
	}

	public static ClaimInfo get(World world, ChunkPos pos)
	{
		GlobalPos key = GlobalPos.func_239648_a_(world.func_234923_W_(), pos.asBlockPos());
		return mapInfo.getOrDefault(key, EMPTY);
	}

	public static void check(World world, BlockPos pos, UUID owner)
	{
		GlobalPos bpos = GlobalPos.func_239648_a_(world.func_234923_W_(), pos);
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
		GlobalPos key = GlobalPos.func_239648_a_(bpos.func_239646_a_(), new ChunkPos(bpos.getPos()).asBlockPos());
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

	public static List<GlobalPos> getNearList(UUID uuid, PlayerEntity player, int limit)
	{
		List<GlobalPos> ret = new ArrayList<>();
		BlockPos loc = player.func_233580_cy_();
		RegistryKey<World> dim = player.world.func_234923_W_();
		PriorityQueue<GPDist> queue = new PriorityQueue<>();
		for (ClaimInfo e : mapInfo.values())
		{
			if (e.owner.equals(uuid) && e.pos.func_239646_a_() == dim)
			{
				BlockPos pos = e.pos.getPos();
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
		PlayerProfileCache cache = server.getPlayerProfileCache();
		int i = 0;
		for (UUID e : mapCount.keySet())
		{
			GameProfile profile = cache.getProfileByUUID(e);
			if (profile != null)
				ret[i] = profile.getName();
			else
				ret[i] = e.toString();
			i++;
		}
		Arrays.sort(ret);
		return ret;
	}

	public static UUID getUUID(String name)
	{
		PlayerProfileCache cache = server.getPlayerProfileCache();
		for (UUID e : mapCount.keySet())
		{
			GameProfile profile = cache.getProfileByUUID(e);
			if (profile != null)
			{
				if (name.equalsIgnoreCase(profile.getName()))
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
		GameProfile profile = server.getPlayerProfileCache().getProfileByUUID(info.owner);
		return (profile != null) ? profile.getName() : "?";
	}

	private static void warnLimit(ClaimInfo info, int count)
	{
		GameProfile profile = server.getPlayerProfileCache().getProfileByUUID(info.owner);
		if (profile != null)
			LOGGER.warn(profile.getName() + " exceeding limit: " + count);
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
		File file1 = server.func_240776_a_(FolderName.field_237253_i_).toFile();
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
		RegistryKey<World>[] dimList = new RegistryKey[size];
		for (int i = 0; i < size; ++i)
		{
			int len = fd.readInt();
			if (len < 0)
				len = 0;
			StringBuffer buf = new StringBuffer(len);
			for (int j = 0; j < len; ++j)
				buf.append(fd.readChar());
			dimList[i] = RegistryKey.func_240903_a_(Registry.field_239699_ae_, new ResourceLocation(buf.toString()));
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
			GlobalPos key = GlobalPos.func_239648_a_(dimList[j], new ChunkPos(pos).asBlockPos());
			j = data[0];
			if (j < 0 || j >= uuidList.length)
				continue;
			ClaimInfo info = new ClaimInfo(uuidList[j], GlobalPos.func_239648_a_(key.func_239646_a_(), pos));
			mapInfo.put(key, info);
			int count = mapCount.getOrDefault(info.owner, 0);
			mapCount.put(info.owner, count + 1);
		}
	}

	private static void report()
	{
		HashSet<RegistryKey<World>> set = new HashSet<>();
		int max = 0;
		for (Integer e : mapCount.values())
			if (e.intValue() > max)
				max = e.intValue();
		for (ClaimInfo e : mapInfo.values())
			set.add(e.pos.func_239646_a_());
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
		HashMap<RegistryKey<World>, Integer> mapDim = new HashMap<>();
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
			Integer pdi = mapDim.get(e.pos.func_239646_a_());
			if (pdi == null)
			{
				di = indexDim++;
				mapDim.put(e.pos.func_239646_a_(), di);
			}
			else
				di = pdi.intValue();
			BlockPos pos = e.pos.getPos();
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
		for (Entry<RegistryKey<World>, Integer> e : mapDim.entrySet())
		{
			resList[e.getValue()] = e.getKey().func_240901_a_();
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

		public boolean okPerm(PlayerEntity player)
		{
			return (player.getUniqueID().equals(owner));
		}
	}

	public static class GPComp implements Comparator<GlobalPos>
	{
		@Override
		public int compare(GlobalPos o1, GlobalPos o2) {
			RegistryKey<World> w1 = o1.func_239646_a_();
			RegistryKey<World> w2 = o2.func_239646_a_();
			if (w1 != w2)
				return w1.func_240901_a_().compareTo(w2.func_240901_a_());
			BlockPos p1 = o1.getPos();
			BlockPos p2 = o2.getPos();
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
