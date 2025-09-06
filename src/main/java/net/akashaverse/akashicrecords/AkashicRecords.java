package net.akashaverse.akashicrecords;

import net.akashaverse.akashicrecords.commands.MineCommands;
import net.akashaverse.akashicrecords.configs.MineConfig;
import net.akashaverse.akashicrecords.items.MineItems;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(AkashicRecords.MOD_ID)
public class AkashicRecords {

    public static final String MOD_ID = "akashicrecords";

    public static final Logger LOGGER = LogUtils.getLogger();

    public AkashicRecords(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        modContainer.registerConfig(ModConfig.Type.SERVER, MineConfig.SPEC);

        NeoForge.EVENT_BUS.register(this);

        // Register items and other deferred registers
        MineItems.register(modEventBus);

        // Register commands on the global event bus
        NeoForge.EVENT_BUS.addListener(MineCommands::register);

        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @EventBusSubscriber(modid = AkashicRecords.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
