package net.akashaverse.akashicrecords.core.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MineManager extends SavedData {
    public static final String DATA_NAME = "akashic_mine_manager";
    private final Map<String, Mine> mines = new HashMap<>();

    public MineManager() {}

    public static MineManager get(ServerLevel level) {
        // The Factory takes: Supplier<T> constructor, BiFunction<CompoundTag, HolderLookup.Provider, T> loader, DataFixTypes fixers (null if none)
        SavedData.Factory<MineManager> factory =
                new SavedData.Factory<>(MineManager::new, MineManager::load, null);

        return level.getDataStorage().computeIfAbsent(factory, DATA_NAME);
    }

    public static MineManager load(CompoundTag tag, HolderLookup.Provider provider) {
        // delegate to your existing load logic (which only needs the tag)
        return load(tag);
    }

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

    public void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        for (Map.Entry<String, Mine> entry : mines.entrySet()) {
            Mine mine = entry.getValue();
            if (mine.nextReset > 0 && gameTime == mine.nextReset - mine.warningTicks) {
                warnPlayers(level, mine, mine.warningTicks / 20);
            }
            if (mine.nextReset > 0 && gameTime >= mine.nextReset) {
                // teleport players out
                for (ServerPlayer player : level.players()) {
                    if (mine.contains(player.blockPosition())) {
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

    private void warnPlayers(ServerLevel level, Mine mine, int secondsLeft) {
        Component msg = Component.literal("Mine resetting in " + secondsLeft + " seconds!");
        for (ServerPlayer player : level.players()) {
            if (mine.contains(player.blockPosition())) {
                player.sendSystemMessage(msg);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag compound, HolderLookup.Provider registries) {
        CompoundTag minesTag = new CompoundTag();
        mines.forEach((name, mine) -> {
            CompoundTag tag = new CompoundTag();
            tag.putIntArray("min", new int[]{mine.min.getX(), mine.min.getY(), mine.min.getZ()});
            tag.putIntArray("max", new int[]{mine.max.getX(), mine.max.getY(), mine.max.getZ()});
            tag.putIntArray("entrance", new int[]{mine.entrance.getX(), mine.entrance.getY(), mine.entrance.getZ()});
            tag.putLong("nextReset", mine.nextReset);
            tag.putInt("refillInterval", mine.refillIntervalTicks);
            tag.putInt("warning", mine.warningTicks);
            tag.putString("border", BuiltInRegistries.BLOCK.getKey(mine.borderBlock.getBlock()).toString());
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
                var key = ResourceLocation.parse(borderId);
                border = BuiltInRegistries.BLOCK.getOptional(key).orElse(Blocks.BEDROCK).defaultBlockState();
            } catch (Exception ex) {
                border = Blocks.BEDROCK.defaultBlockState();
            }
            ListTag distList = tag.getList("distribution", Tag.TAG_STRING);
            List<WeightedBlock> distribution = distList.stream()
                    .map(Tag::getAsString)
                    .map(str -> {
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
                    })
                    .collect(Collectors.toList());
            Mine mine = new Mine(pos1, pos2, entrance, refillInterval, warning, border, distribution);
            mine.nextReset = nextReset;
            manager.mines.put(name, mine);
        }
        return manager;
    }
}
