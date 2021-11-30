package com.lupicus.cc.network;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lupicus.cc.Main;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fmllegacy.network.NetworkDirection;
import net.minecraftforge.fmllegacy.network.NetworkEvent.Context;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;

public class Network
{
	private static final String PROTOCOL_VERSION = "1.0";
	public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
	    new ResourceLocation(Main.MODID, "main"),
	    () -> PROTOCOL_VERSION,
	    PROTOCOL_VERSION::equals,
	    PROTOCOL_VERSION::equals
	);
	private static int id = 0;

	public static <MSG> void registerMessage(Class<MSG> msg,
			BiConsumer<MSG, FriendlyByteBuf> encoder,
			Function<FriendlyByteBuf, MSG> decoder,
			BiConsumer<MSG, Supplier<Context>> handler)
	{
		INSTANCE.registerMessage(id++, msg, encoder, decoder, handler);
	}

	@OnlyIn(Dist.CLIENT)
	public static <MSG> void sendToServer(MSG msg)
	{
		INSTANCE.sendToServer(msg);
	}

	public static <MSG> void sendToClient(MSG msg, ServerPlayer player)
	{
		INSTANCE.sendTo(msg, player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
	}

	public static <MSG> void sendToTarget(PacketDistributor.PacketTarget target, MSG msg)
	{
		INSTANCE.send(target, msg);
	}
}
