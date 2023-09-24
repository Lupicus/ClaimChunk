package com.lupicus.cc.network;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.lupicus.cc.Main;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class Network
{
	public static final SimpleChannel INSTANCE = ChannelBuilder.named(new ResourceLocation(Main.MODID, "main"))
			.networkProtocolVersion(1)
			.simpleChannel();
	private static int id = 0;

	public static <MSG> void registerMessage(Class<MSG> msg,
			BiConsumer<MSG, FriendlyByteBuf> encoder,
			Function<FriendlyByteBuf, MSG> decoder,
			BiConsumer<MSG, Context> handler)
	{
		INSTANCE.messageBuilder(msg, id++).encoder(encoder).decoder(decoder).consumerNetworkThread(handler).add();
	}

	@OnlyIn(Dist.CLIENT)
	public static <MSG> void sendToServer(MSG msg)
	{
		INSTANCE.send(msg, Minecraft.getInstance().getConnection().getConnection());
	}

	public static <MSG> void sendToClient(MSG msg, ServerPlayer player)
	{
		INSTANCE.send(msg, player.connection.getConnection());
	}

	public static <MSG> void sendToTarget(PacketDistributor.PacketTarget target, MSG msg)
	{
		INSTANCE.send(msg, target);
	}
}
