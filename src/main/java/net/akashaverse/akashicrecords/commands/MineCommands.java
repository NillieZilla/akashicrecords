package net.akashaverse.akashicrecords.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.akashaverse.akashicrecords.configs.MineConfig;
import net.akashaverse.akashicrecords.core.mine.Mine;
import net.akashaverse.akashicrecords.core.mine.MineManager;
import net.akashaverse.akashicrecords.core.mine.MineType;
import net.akashaverse.akashicrecords.items.custom.SelectionWandItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
        dispatcher.register(
                Commands.literal("mine")
                        .requires(cs -> cs.hasPermission(2))
                        // create <name> <type>
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    CommandSourceStack source = ctx.getSource();
                                                    ServerPlayer player = source.getPlayerOrException();
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    String typeName = StringArgumentType.getString(ctx, "type");
                                                    CompoundTag tag = player.getPersistentData().getCompound(SelectionWandItem.TAG_NAME);
                                                    if (!tag.contains("pos1") || !tag.contains("pos2")) {
                                                        source.sendFailure(Component.literal("You must define two corners with the wand first."));
                                                        return 0;
                                                    }
                                                    BlockPos pos1 = BlockPos.of(tag.getLong("pos1"));
                                                    BlockPos pos2 = BlockPos.of(tag.getLong("pos2"));
                                                    // If spawn is not set, fall back to player's current position
                                                    BlockPos spawn;
                                                    if (tag.contains("spawn")) {
                                                        // Use stored spawn but move one block up to avoid placing inside the block
                                                        spawn = BlockPos.of(tag.getLong("spawn"));
                                                    } else {
                                                        // Default to the player's current position, but spawn one block above
                                                        spawn = player.blockPosition().above();
                                                    }
                                                    // Retrieve mine type from config
                                                    MineType type = MineConfig.getType(typeName);
                                                    // Always use bedrock for border; could be extended to config later
                                                    var border = net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState();
                                                    Mine mine = new Mine(pos1, pos2, spawn, type.refillIntervalTicks(), type.warningTicks(), border, type.distribution());
                                                    ServerLevel level = source.getLevel();
                                                    mine.nextReset = level.getGameTime() + type.refillIntervalTicks();
                                                    MineManager manager = MineManager.get(level);
                                                    manager.putMine(name, mine);
                                                    // Immediately generate the mine so players can use it
                                                    mine.regenerate(level);
                                                    source.sendSuccess(() -> Component.literal("Mine '" + name + "' of type '" + type.name() + "' created."), false);
                                                    // clear stored selection data
                                                    tag.remove("pos1");
                                                    tag.remove("pos2");
                                                    tag.remove("spawn");
                                                    player.getPersistentData().put(SelectionWandItem.TAG_NAME, tag);
                                                    return 1;
                                                }))
                                ))
                        // rename <old> <new>
                        .then(Commands.literal("rename")
                                .then(Commands.argument("old", StringArgumentType.word())
                                        .then(Commands.argument("new", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    CommandSourceStack source = ctx.getSource();
                                                    ServerLevel level = source.getLevel();
                                                    String oldName = StringArgumentType.getString(ctx, "old");
                                                    String newName = StringArgumentType.getString(ctx, "new");
                                                    MineManager manager = MineManager.get(level);
                                                    Mine mine = manager.getMine(oldName);
                                                    if (mine == null) {
                                                        source.sendFailure(Component.literal("Mine not found: " + oldName));
                                                        return 0;
                                                    }
                                                    manager.removeMine(oldName);
                                                    manager.putMine(newName, mine);
                                                    source.sendSuccess(() -> Component.literal("Renamed mine '" + oldName + "' to '" + newName + "'."), false);
                                                    return 1;
                                                }))))
                        // setspawn <name>
                        .then(Commands.literal("setspawn")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            ServerPlayer player = source.getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            ServerLevel level = source.getLevel();
                                            MineManager manager = MineManager.get(level);
                                            Mine mine = manager.getMine(name);
                                            if (mine == null) {
                                                source.sendFailure(Component.literal("Mine not found: " + name));
                                                return 0;
                                            }
                                            // Set spawn one block above the player's current position to avoid suffocation
                                            mine.entrance = player.blockPosition().above();
                                            manager.setDirty();
                                            source.sendSuccess(() -> Component.literal("Set spawn for mine '" + name + "' to your current position."), false);
                                            return 1;
                                        })))
                        // list
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
                                            // Build a clickable component to teleport to the mine entrance.  Clicking the
                                            // link runs a /tp command targeting the caller ("@s").  We colour it aqua
                                            // for visibility.
                                            BlockPos ent = mine.entrance;
                                            // Compute yaw so the teleport command faces the centre of the mine
                                            double destX = ent.getX() + 0.5;
                                            double destZ = ent.getZ() + 0.5;
                                            double centerX = (mine.min.getX() + mine.max.getX()) / 2.0 + 0.5;
                                            double centerZ = (mine.min.getZ() + mine.max.getZ()) / 2.0 + 0.5;
                                            double dx = centerX - destX;
                                            double dz = centerZ - destZ;
                                            float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                                            float pitch = 0.0F;
                                            String coords = ent.getX() + " " + ent.getY() + " " + ent.getZ();
                                            // Append yaw and pitch to the teleport command so the player faces the mine
                                            String command = "/tp @s " + coords + " " + yaw + " " + pitch;
                                            var clickEvent = new net.minecraft.network.chat.ClickEvent(
                                                    net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                                    command
                                            );
                                            var teleStyle = net.minecraft.network.chat.Style.EMPTY
                                                    .withColor(net.minecraft.ChatFormatting.AQUA)
                                                    .withClickEvent(clickEvent)
                                                    .withUnderlined(true);
                                            Component tele = Component.literal("[Teleport]").setStyle(teleStyle);
                                            Component line = Component.literal(" - " + name + " from " + posToString(mine.min) + " to " + posToString(mine.max) + " ")
                                                    .append(tele);
                                            source.sendSuccess(() -> line, false);
                                        });
                                    }
                                    return 1;
                                }))
                        // delete <name>
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

    /**
     * Utility method to convert a BlockPos to a user‑readable string.
     */
    private static String posToString(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}