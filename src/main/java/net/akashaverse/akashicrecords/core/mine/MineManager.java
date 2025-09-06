package net.akashaverse.akashicrecords.core.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MineManager extends SavedData {
    public static final String DATA_NAME = "akashic_mine_manager";

    private final Map<String, Mine> mines = new HashMap<>();

    public MineManager() {
    }

    /**
     * Retrieve the MineManager for the given level.  If none exists, a new one
     * will be created.  NeoForge automatically handles saving when
     * {@link SavedData#setDirty()} is called.
     */
    public static MineManager get(ServerLevel level) {
        // Use the new SavedData.Factory API introduced in NeoForge 1.21.  It
        // accepts a supplier to create a new manager, a loader that accepts
        // both NBT and a HolderLookup.Provider, and a DataFixTypes parameter
        // (null since we have no datafixer).  See load(CompoundTag, HolderLookup.Provider).
        SavedData.Factory<MineManager> factory =
                new SavedData.Factory<>(MineManager::new, MineManager::load, null);
        return level.getDataStorage().computeIfAbsent(factory, DATA_NAME);
    }

    /**
     * Add a new mine to the manager.  If a mine with the same name exists,
     * it will be replaced.  Remember to call {@link #setDirty()} after making
     * changes so the world knows to save the data.
     */
    public void putMine(String name, Mine mine) {
        mines.put(name, mine);
        setDirty();
    }

    public Mine getMine(String name) {
        return mines.get(name);
    }

    public void removeMine(String name) {
        mines.remove(name);
        setDirty();
    }

    public Map<String, Mine> getMines() {
        return mines;
    }

    /**
     * Called each server tick.  Checks each mine’s timers and performs
     * warnings or regeneration.  Players inside a mine about to regenerate
     * are teleported to the mine entrance when regeneration occurs.
     */
    public void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        for (Map.Entry<String, Mine> entry : mines.entrySet()) {
            Mine mine = entry.getValue();
            // send warning
            if (mine.nextReset > 0 && gameTime == mine.nextReset - mine.warningTicks) {
                warnPlayers(level, mine, mine.warningTicks / 20);
            }
            // regeneration time
            if (mine.nextReset > 0 && gameTime >= mine.nextReset) {
                // teleport players out
                for (ServerPlayer player : level.players()) {
                    if (mine.contains(player.blockPosition())) {
                        // teleport to entrance; use centre of block
                        player.teleportTo(
                                level,
                                mine.entrance.getX() + 0.5,
                                mine.entrance.getY(),
                                mine.entrance.getZ() + 0.5,
                                player.getYRot(),
                                player.getXRot()
                        );
                    }
                }
                mine.regenerate(level);
                setDirty();
            }
        }
    }

    /**
     * Send a warning message to all players currently inside the given mine.
     */
    private void warnPlayers(ServerLevel level, Mine mine, int secondsLeft) {
        Component msg = Component.literal("Mine resetting in " + secondsLeft + " seconds!");
        for (ServerPlayer player : level.players()) {
            if (mine.contains(player.blockPosition())) {
                player.sendSystemMessage(msg);
            }
        }
    }

    /**
     * Serialise this manager to NBT.  Each mine is stored as a compound tag
     * keyed by its name.  The data stored for each mine includes the region
     * boundaries, entrance, timing, border block, and distribution list.
     */
    @Override
    public CompoundTag save(CompoundTag compound, HolderLookup.Provider lookup) {
        CompoundTag minesTag = new CompoundTag();
        mines.forEach((name, mine) -> {
            CompoundTag tag = new CompoundTag();
            tag.putIntArray("min", new int[]{mine.min.getX(), mine.min.getY(), mine.min.getZ()});
            tag.putIntArray("max", new int[]{mine.max.getX(), mine.max.getY(), mine.max.getZ()});
            tag.putIntArray("entrance", new int[]{mine.entrance.getX(), mine.entrance.getY(), mine.entrance.getZ()});
            tag.putLong("nextReset", mine.nextReset);
            tag.putInt("refillInterval", mine.refillIntervalTicks);
            tag.putInt("warning", mine.warningTicks);
            tag.putString("border", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(mine.borderBlock.getBlock()).toString());
            ListTag list = new ListTag();
            for (WeightedBlock wb : mine.distribution) {
                list.add(StringTag.valueOf(wb.blockId() + "|" + wb.weight()));
            }
            tag.put("distribution", list);
            minesTag.put(name, tag);
        });
        compound.put("mines", minesTag);
        return compound;
    }

    /**
     * Deserialise a MineManager from NBT.  This overload matches the
     * signature expected by {@link SavedData.Factory}.  We ignore the
     * {@code provider} parameter because we do not need registry lookups
     * beyond parsing block IDs.
     *
     * @param compound the tag from which to load
     * @param provider registry lookup provider (unused)
     * @return a new MineManager loaded from the given NBT
     */
    public static MineManager load(CompoundTag compound, net.minecraft.core.HolderLookup.Provider provider) {
        return load(compound);
    }

    /**
     * Legacy loader used internally by the overload that accepts a
     * {@link net.minecraft.core.HolderLookup.Provider}.  Parses the
     * mines map from NBT into a new manager.
     */
    public static MineManager load(CompoundTag compound) {
        MineManager manager = new MineManager();
        CompoundTag minesTag = compound.getCompound("mines");
        for (String name : minesTag.getAllKeys()) {
            CompoundTag tag = minesTag.getCompound(name);
            int[] minArr = tag.getIntArray("min");
            int[] maxArr = tag.getIntArray("max");
            int[] entArr = tag.getIntArray("entrance");
            BlockPos pos1 = new BlockPos(minArr[0], minArr[1], minArr[2]);
            BlockPos pos2 = new BlockPos(maxArr[0], maxArr[1], maxArr[2]);
            BlockPos entrance = new BlockPos(entArr[0], entArr[1], entArr[2]);
            long nextReset = tag.getLong("nextReset");
            int refillInterval = tag.getInt("refillInterval");
            int warning = tag.getInt("warning");
            String borderId = tag.getString("border");
            BlockState border;
            try {
                var key = net.minecraft.resources.ResourceLocation.parse(borderId);
                border = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(key).orElse(Blocks.BEDROCK).defaultBlockState();
            } catch (Exception ex) {
                border = Blocks.BEDROCK.defaultBlockState();
            }
            ListTag distList = tag.getList("distribution", Tag.TAG_STRING);
            List<WeightedBlock> distribution = distList.stream().map(Tag::getAsString).map(str -> {
                String[] parts = str.split("\\|");
                String id = parts.length > 0 ? parts[0] : "minecraft:stone";
                double weight = 1.0;
                if (parts.length > 1) {
                    try {
                        weight = Double.parseDouble(parts[1]);
                    } catch (NumberFormatException ignored) {
                        weight = 1.0;
                    }
                }
                return new WeightedBlock(id, weight);
            }).collect(Collectors.toList());
            Mine mine = new Mine(pos1, pos2, entrance, refillInterval, warning, border, distribution);
            mine.nextReset = nextReset;
            manager.mines.put(name, mine);
        }
        return manager;
    }
}
