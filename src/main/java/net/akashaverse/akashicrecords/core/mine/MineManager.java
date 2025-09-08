package net.akashaverse.akashicrecords.core.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MineManager extends SavedData {
    public static final String DATA_NAME = "akashic_mine_manager";

    public static final String TAG_HIDE_MINE_MESSAGES = "ak_hide_mine_messages";

    private final Map<String, Mine> mines = new HashMap<>();

    public MineManager() {}

    public static MineManager get(ServerLevel level) {
        SavedData.Factory<MineManager> factory =
                new SavedData.Factory<>(MineManager::new, MineManager::load, null);
        return level.getDataStorage().computeIfAbsent(factory, DATA_NAME);
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
                java.util.List<ServerPlayer> affected = new java.util.ArrayList<>();
                for (ServerPlayer p : level.players()) {
                    if (mine.contains(p.blockPosition())) {
                        affected.add(p);

                        double destX = mine.entrance.getX() + 0.5;
                        double destY = mine.entrance.getY();
                        double destZ = mine.entrance.getZ() + 0.5;
                        double centerX = (mine.min.getX() + mine.max.getX()) / 2.0 + 0.5;
                        double centerZ = (mine.min.getZ() + mine.max.getZ()) / 2.0 + 0.5;
                        double dx = centerX - destX;
                        double dz = centerZ - destZ;
                        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                        float pitch = 0.0F;
                        p.teleportTo(level, destX, destY, destZ, yaw, pitch);
                    }
                }

                mine.regenerate(level);

                Component resetMsg = Component.literal("Mine '" + entry.getKey() + "' has been reset.");
                for (ServerPlayer p : affected) {
                    if (!p.getPersistentData().getBoolean(TAG_HIDE_MINE_MESSAGES)) {
                        p.sendSystemMessage(resetMsg);
                    }
                }

                setDirty();
            }
        }
    }

    private void warnPlayers(ServerLevel level, Mine mine, int secondsLeft) {
        Component msg = Component.literal("Mine resetting in " + secondsLeft + " seconds!");
        for (ServerPlayer p : level.players()) {
            if (mine.contains(p.blockPosition())
                    && !p.getPersistentData().getBoolean(TAG_HIDE_MINE_MESSAGES)) {
                p.sendSystemMessage(msg);
            }
        }
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag compound, HolderLookup.@NotNull Provider provider) {
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
            tag.putBoolean("borderBuilt", mine.borderBuilt);
            minesTag.put(name, tag);
        });
        compound.put("mines", minesTag);
        return compound;
    }

    public static MineManager load(CompoundTag compound, HolderLookup.Provider provider) {
        return load(compound);
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
            List<WeightedBlock> distribution = distList.stream().map(Tag::getAsString).map(str -> {
                String[] parts = str.split("\\|");
                String id = parts.length > 0 ? parts[0] : "minecraft:stone";
                double weight = 1.0;
                if (parts.length > 1) {
                    try { weight = Double.parseDouble(parts[1]); } catch (NumberFormatException ignored) {}
                }
                return new WeightedBlock(id, weight);
            }).collect(Collectors.toList());
            Mine mine = new Mine(pos1, pos2, entrance, refillInterval, warning, border, distribution);
            mine.nextReset = nextReset;
            if (tag.contains("borderBuilt")) {
                mine.borderBuilt = tag.getBoolean("borderBuilt");
            }
            manager.mines.put(name, mine);
        }
        return manager;
    }
}
