package com.lupicus.cc;

import org.jetbrains.annotations.NotNull;

import com.lupicus.cc.block.ModBlocks;
import com.lupicus.cc.command.ClaimsCommand;
import com.lupicus.cc.config.MyConfig;
import com.lupicus.cc.item.ModItems;
import com.lupicus.cc.manager.ClaimManager;
import com.lupicus.cc.network.Register;
import com.lupicus.cc.proxy.ClientProxy;
import com.lupicus.cc.proxy.ServerProxy;
import com.lupicus.cc.proxy.IProxy;
import com.lupicus.cc.tileentity.ModTileEntities;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Main.MODID)
public class Main
{
	public static final String MODID = "cc";
	public static IProxy proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> ServerProxy::new);

	public Main()
	{
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
	}

	@SubscribeEvent
	public void setup(final FMLCommonSetupEvent event)
	{
        Register.initPackets();
	}

	@Mod.EventBusSubscriber(bus = Bus.MOD)
	public static class ModEvents
	{
	    @SubscribeEvent
	    public static void onRegister(final RegisterEvent event)
	    {
	    	@NotNull
			ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
	    	if (key.equals(ForgeRegistries.Keys.BLOCKS))
	    		ModBlocks.register(event.getForgeRegistry());
	    	else if (key.equals(ForgeRegistries.Keys.ITEMS))
	    		ModItems.register(event.getForgeRegistry());
	    	else if (key.equals(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES))
	    		ModTileEntities.register(event.getForgeRegistry());
	    }

	    @SubscribeEvent
	    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event)
	    {
	    	ModItems.setupTabs(event);
	    }
	}

	@Mod.EventBusSubscriber(bus = Bus.FORGE)
	public static class ForgeEvents
	{
		@SubscribeEvent
		public static void onServerAbout(ServerAboutToStartEvent event)
		{
			ClaimManager.load(event.getServer());
		}

		@SubscribeEvent
		public static void onServerStopping(ServerStoppingEvent event)
		{
			ClaimManager.clear();
		}

		@SubscribeEvent
	    public static void onCommand(RegisterCommandsEvent event)
	    {
	    	ClaimsCommand.register(event.getDispatcher());
	    }
	}
}
