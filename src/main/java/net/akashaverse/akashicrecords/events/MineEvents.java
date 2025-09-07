package net.akashaverse.akashicrecords.events;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.akashaverse.akashicrecords.core.mine.MineManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

@EventBusSubscriber(modid = AkashicRecords.MOD_ID)
public class MineEvents {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            MineManager manager = MineManager.get(level);
            manager.tick(level);
        }
    }
}
