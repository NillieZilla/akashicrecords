package net.akashaverse.akashicrecords;

import net.akashaverse.akashicrecords.commands.MineCommands;
import net.akashaverse.akashicrecords.configs.MineConfig;
import net.akashaverse.akashicrecords.gui.ModMenuTypes;
import net.akashaverse.akashicrecords.items.MineItems;
import net.akashaverse.akashicrecords.items.ModCreativeModeTabs;
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

/**
 * Main mod entry point for the AkashicRecords mod.  This class is marked with
 * {@link Mod} so NeoForge recognises it as a mod and invokes the constructor
 * during loading.  In the constructor we register our configs, items, creative
 * tabs, commands and menu types.  Additional initialisation hooks for common
 * and client setup are provided but currently empty.
 */
@Mod(AkashicRecords.MOD_ID)
public class AkashicRecords {

    /** The mod id defined in {@code mods.toml}. */
    public static final String MOD_ID = "akashicrecords";

    /** Logger instance for debug output. */
    public static final Logger LOGGER = LogUtils.getLogger();

    public AkashicRecords(IEventBus modEventBus, ModContainer modContainer) {
        // Register common setup callback
        modEventBus.addListener(this::commonSetup);

        // Register server configuration; use SERVER scope so values load on both
        // integrated and dedicated servers.  Config files live under
        // config/AkashicRecords.
        modContainer.registerConfig(ModConfig.Type.SERVER, MineConfig.SPEC);

        // Register this instance with the global event bus for any annotated
        // @SubscribeEvent methods below
        NeoForge.EVENT_BUS.register(this);

        // Register deferred registers: items, creative tabs and menu types
        MineItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        // Register commands on the global event bus
        NeoForge.EVENT_BUS.addListener(MineCommands::register);

        // Register creative tab entries callback
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Perform any common initialisation here (network handlers, capabilities, etc.)
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Populate custom creative tabs if needed
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Hook for when the dedicated server is starting; nothing needed at the moment
    }

    /**
     * Client‑only event hooks.  These are only loaded on the physical client
     * because they deal with rendering and other client logic.  NeoForge will
     * automatically register this nested class on the mod bus.
     */
    @EventBusSubscriber(modid = AkashicRecords.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            // Client setup callback (e.g. register entity renderers).  Screen
            // registration for menus is handled by {@link ModMenuScreens}.
        }
    }
}