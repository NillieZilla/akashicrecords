package net.akashaverse.akashicrecords.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.akashaverse.akashicrecords.core.mine.MineManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.server.level.ServerLevel;

/**
 * Registers commands for interacting with the AutoRefillingMine mod.  These
 * commands allow administrators to list and remove mines.  More commands
 * (e.g. adjusting distribution or intervals) could be added following this
 * pattern.
 */
public class MineCommands {
    public static void register(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        // Root command `/mine`
        dispatcher.register(
                Commands.literal("mine")
                        .requires(cs -> cs.hasPermission(2))
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel level = source.getLevel();
                                    MineManager manager = MineManager.get(level);
                                    if (manager.getMines().isEmpty()) {
                                        source.sendSuccess(() -> Component.literal("No mines defined."), false);
                                    } else {
                                        source.sendSuccess(() -> Component.literal("Mines:"), false);
                                        manager.getMines().forEach((name, mine) -> {
                                            source.sendSuccess(() -> Component.literal(" - " + name + " at " + mine.min + " to " + mine.max), false);
                                        });
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            ServerLevel level = source.getLevel();
                                            MineManager manager = MineManager.get(level);
                                            if (manager.getMines().containsKey(name)) {
                                                manager.removeMine(name);
                                                source.sendSuccess(() -> Component.literal("Removed mine " + name), false);
                                            } else {
                                                source.sendFailure(Component.literal("Mine not found: " + name));
                                            }
                                            return 1;
                                        })))
        );
    }
}
