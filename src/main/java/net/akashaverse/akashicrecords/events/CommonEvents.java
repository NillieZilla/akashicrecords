package net.akashaverse.akashicrecords.events;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.akashaverse.akashicrecords.core.mine.MineManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Handles common (both client and server) events for the mod.  In our case,
 * we hook into the server tick to drive mine regeneration and warning
 * messages.  Annotating this class with {@code @Mod.EventBusSubscriber}
 * automatically registers the methods with the Forge event bus.
 */
@EventBusSubscriber(modid = AkashicRecords.MOD_ID)
public class CommonEvents {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        // Only execute at the end of the tick to avoid interfering with other logic
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        // Iterate all levels (dimensions) and tick their mine managers
        for (ServerLevel level : server.getAllLevels()) {
            MineManager manager = MineManager.get(level);
            manager.tick(level);
        }
    }
}