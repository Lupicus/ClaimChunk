package com.lupicus.cc.network;

import com.lupicus.cc.Main;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class Network
{
	public static final SimpleChannel INSTANCE = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(Main.MODID, "main"))
			.networkProtocolVersion(1)
			.simpleChannel();

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
