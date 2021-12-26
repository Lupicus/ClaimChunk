package com.lupicus.cc;

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

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
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
		public static void onBlocksRegistry(final RegistryEvent.Register<Block> event)
		{
			ModBlocks.register(event.getRegistry());
		}

		@SubscribeEvent
		public static void onItemsRegistry(final RegistryEvent.Register<Item> event)
		{
			ModItems.register(event.getRegistry());
		}

	    @SubscribeEvent
	    public static void onTileEntitiesRegistry(final RegistryEvent.Register<BlockEntityType<?>> event)
	    {
	        ModTileEntities.register(event.getRegistry());
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
