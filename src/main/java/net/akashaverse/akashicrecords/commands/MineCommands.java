package net.akashaverse.akashicrecords.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.akashaverse.akashicrecords.core.mine.Mine;
import net.akashaverse.akashicrecords.configs.MineConfig;
import net.akashaverse.akashicrecords.core.mine.MineManager;
import net.akashaverse.akashicrecords.core.mine.MineType;
import net.akashaverse.akashicrecords.items.mine.SelectionWandItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;

import java.util.Map;

/**
 * Registers the "/mine" command and its subcommands.  The command provides
 * functionality to create, list, delete, rename and set spawn points for mines.
 * It also opens the mine management GUI when called without a subcommand.
 */
public final class MineCommands {
    private MineCommands() {
        // utility class; no instantiation
    }

    /**
     * Register the mine commands on the server command dispatcher.  Called by
     * the mod's event handler during the {@link RegisterCommandsEvent}.
     *
     * @param event the registration event
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("mine")
                        .requires(cs -> cs.hasPermission(2))
                        .executes(ctx -> {
                            // Open the mine management GUI on the client when no arguments are given.
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            // Open our custom menu using a SimpleMenuProvider.  This avoids the need
                            // for NetworkHooks, which is not available in NeoForge 1.21.x.
                            MenuProvider provider = new SimpleMenuProvider(
                                    (id, inventory, p) -> new net.akashaverse.akashicrecords.gui.menu.MineMainMenu(id, inventory),
                                    Component.literal("Mine Manager"));
                            player.openMenu(provider);
                            return 1;
                        })
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.serverLevel();
                                    MineManager manager = MineManager.get(level);
                                    if (manager.getMines().isEmpty()) {
                                        player.sendSystemMessage(Component.literal("No mines defined."));
                                        return 1;
                                    }
                                    for (Map.Entry<String, Mine> entry : manager.getMines().entrySet()) {
                                        String name = entry.getKey();
                                        Mine mine = entry.getValue();
                                        BlockPos spawn = mine.entrance;
                                        // Calculate a yaw that faces towards the mine's centre from the entrance.
                                        Vec3 centre = new Vec3((mine.min.getX() + mine.max.getX()) / 2.0 + 0.5,
                                                (mine.min.getY() + mine.max.getY()) / 2.0,
                                                (mine.min.getZ() + mine.max.getZ()) / 2.0 + 0.5);
                                        double dx = centre.x - (spawn.getX() + 0.5);
                                        double dz = centre.z - (spawn.getZ() + 0.5);
                                        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                                        // Create a clickable teleport link that faces the mine.
                                        String tpCommand = String.format("/tp @s %d %d %d %.1f 0.0",
                                                spawn.getX(), spawn.getY(), spawn.getZ(), yaw);
                                        // Build the clickable teleport component.  Underlines are
                                        // not directly supported on Style in 1.21, so we just
                                        // colour the text and attach the click event.  If you
                                        // wish to underline, you can add formatting codes in a
                                        // translation key instead.
                                        Component line = Component.literal("- " + name + " at " +
                                                        spawn.getX() + "," + spawn.getY() + "," + spawn.getZ() + " ")
                                                .append(Component.literal("[Teleport]")
                                                        .withStyle(Style.EMPTY.withColor(0x00FFFF)
                                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))));
                                        player.sendSystemMessage(line);
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            MineManager manager = MineManager.get(player.serverLevel());
                                            Mine mine = manager.getMine(name);
                                            if (mine == null) {
                                                player.sendSystemMessage(Component.literal("No mine named " + name));
                                                return 0;
                                            }
                                            manager.removeMine(name);
                                            player.sendSystemMessage(Component.literal("Deleted mine " + name));
                                            return 1;
                                        })))
                        .then(Commands.literal("rename")
                                .then(Commands.argument("oldName", StringArgumentType.word())
                                        .then(Commands.argument("newName", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String oldName = StringArgumentType.getString(ctx, "oldName");
                                                    String newName = StringArgumentType.getString(ctx, "newName");
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    MineManager manager = MineManager.get(player.serverLevel());
                                                    Mine mine = manager.getMine(oldName);
                                                    if (mine == null) {
                                                        player.sendSystemMessage(Component.literal("No mine named " + oldName));
                                                        return 0;
                                                    }
                                                    manager.removeMine(oldName);
                                                    manager.putMine(newName, mine);
                                                    player.sendSystemMessage(Component.literal("Renamed mine " + oldName + " to " + newName));
                                                    return 1;
                                                }))
                                ))
                        .then(Commands.literal("setspawn")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            MineManager manager = MineManager.get(player.serverLevel());
                                            Mine mine = manager.getMine(name);
                                            if (mine == null) {
                                                player.sendSystemMessage(Component.literal("No mine named " + name));
                                                return 0;
                                            }
                                            // Set spawn to one block above the player's current position
                                            BlockPos spawn = player.blockPosition().above();
                                            // Directly assign the entrance field.  The Mine class exposes
                                            // the entrance as a public field; if you added a setter,
                                            // you can call it instead.
                                            mine.entrance = spawn;
                                            manager.setDirty();
                                            player.sendSystemMessage(Component.literal("Set spawn for mine " + name + " to your position"));
                                            return 1;
                                        })))
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String mineName = StringArgumentType.getString(ctx, "name");
                                                    String typeName = StringArgumentType.getString(ctx, "type");
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    // Retrieve stored selection positions from the player's NBT data
                                                    var data = player.getPersistentData().getCompound(SelectionWandItem.TAG_NAME);
                                                    if (!data.contains("pos1") || !data.contains("pos2") || !data.contains("spawn")) {
                                                        player.sendSystemMessage(Component.literal("You must set positions using the selection wand first."));
                                                        return 0;
                                                    }
                                                    BlockPos pos1 = BlockPos.of(data.getLong("pos1"));
                                                    BlockPos pos2 = BlockPos.of(data.getLong("pos2"));
                                                    BlockPos spawn = BlockPos.of(data.getLong("spawn")).above();
                                                    MineType type = MineConfig.getType(typeName);
                                                    ServerLevel level = player.serverLevel();
                                                    MineManager manager = MineManager.get(level);
                                                    // Build the mine with the chosen type and border block (bedrock)
                                                    BlockState border = Blocks.BEDROCK.defaultBlockState();
                                                    Mine mine = new Mine(pos1, pos2, spawn,
                                                            type.refillIntervalTicks(),
                                                            type.warningTicks(),
                                                            border,
                                                            type.distribution(),
                                                            type.layers());
                                                    // Immediately regenerate the mine and register it
                                                    mine.regenerate(level);
                                                    manager.putMine(mineName, mine);
                                                    manager.setDirty();
                                                    // Clear the stored selection
                                                    data.remove("pos1");
                                                    data.remove("pos2");
                                                    data.remove("spawn");
                                                    player.getPersistentData().put(SelectionWandItem.TAG_NAME, data);
                                                    player.sendSystemMessage(Component.literal("Created mine " + mineName + " of type " + typeName));
                                                    return 1;
                                                }))
                                ))
        );
    }
}