package net.akashaverse.akashicrecords.core.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Represents a single auto‑refilling mine.  A mine is defined by two corners
 * (min and max positions), a teleportation entrance, a block distribution,
 * optional layered distributions, and timing information.  The {@link #regenerate(ServerLevel)}
 * method will re‑populate the interior of the region with randomly selected
 * blocks according to either the default distribution or a blended distribution
 * derived from layers.  The border is built once on the first generation and
 * then preserved on subsequent regenerations.
 */
public class Mine {
    /** minimum corner of the cuboid (inclusive) */
    public final BlockPos min;
    /** maximum corner of the cuboid (inclusive) */
    public final BlockPos max;
    /** entrance used to teleport players out before regeneration */
    public BlockPos entrance;
    /** weighted distribution of blocks inside the mine */
    public final List<WeightedBlock> distribution = new ArrayList<>();
    /** optional layered distributions used for vertical mixing */
    public final List<MineLayer> layers;
    /** game time (ticks) when the next reset will occur */
    public long nextReset;
    /** number of ticks between resets */
    public final int refillIntervalTicks;
    /** number of ticks before reset to warn players */
    public final int warningTicks;
    /** block used for the border surrounding the mine */
    public final BlockState borderBlock;
    /**
     * Whether the border has already been built.  On first generation
     * (when this is false), the border will be created around the region.
     * Subsequent regenerations will leave the border in place and only
     * refill the interior.
     */
    public boolean borderBuilt;

    /**
     * Construct a mine with a single distribution (no layering).
     */
    public Mine(BlockPos pos1, BlockPos pos2, BlockPos entrance, int refillIntervalTicks, int warningTicks,
                BlockState borderBlock, List<WeightedBlock> distribution) {
        this(pos1, pos2, entrance, refillIntervalTicks, warningTicks, borderBlock, distribution, null);
    }

    /**
     * Construct a mine that may use layered distributions.  If {@code layers} is null or empty,
     * the default distribution will be used for all vertical positions.
     */
    public Mine(BlockPos pos1, BlockPos pos2, BlockPos entrance, int refillIntervalTicks, int warningTicks,
                BlockState borderBlock, List<WeightedBlock> distribution, List<MineLayer> layers) {
        this.min = new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
        this.max = new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
        this.entrance = entrance;
        this.refillIntervalTicks = refillIntervalTicks;
        this.warningTicks = warningTicks;
        this.borderBlock = borderBlock;
        if (distribution != null) {
            this.distribution.addAll(distribution);
        }
        this.layers = (layers == null || layers.isEmpty()) ? null : List.copyOf(layers);
        this.nextReset = 0L;
        this.borderBuilt = false;
    }

    /**
     * Returns true if the given position lies within this mine region (inclusive).
     */
    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /**
     * Rebuilds the border (if not yet built) and fills the interior with randomly selected blocks
     * according to either the default distribution or a blended layered distribution.  After
     * regeneration the next reset time is scheduled.
     */
    public void regenerate(ServerLevel level) {
        RandomSource random = level.random;

        Map<Integer, SimpleWeightedRandomList<BlockState>> layerDistributions = null;
        if (layers != null && !layers.isEmpty()) {
            layerDistributions = new HashMap<>();
            int layerCount = layers.size();
            for (int y = min.getY() + 1; y <= max.getY(); y++) {
                SimpleWeightedRandomList.Builder<BlockState> builder = getBlockStateBuilder(y, layerCount);
                layerDistributions.put(y, builder.build());
            }
        }

        for (int x = min.getX() + 1; x < max.getX(); x++) {
            for (int y = min.getY() + 1; y <= max.getY(); y++) {
                for (int z = min.getZ() + 1; z < max.getZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    SimpleWeightedRandomList<BlockState> weightedList;
                    if (layerDistributions != null) {
                        weightedList = layerDistributions.get(y);
                    } else {
                        weightedList = null;
                    }
                    BlockState state;
                    if (weightedList != null) {
                        state = weightedList.getRandomValue(random).orElse(Blocks.STONE.defaultBlockState());
                    } else {
                        if (distribution.isEmpty()) {
                            state = Blocks.STONE.defaultBlockState();
                        } else {
                            SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
                            for (WeightedBlock wb : distribution) {
                                Block block;
                                try {
                                    var key = net.minecraft.resources.ResourceLocation.parse(wb.blockId());
                                    block = BuiltInRegistries.BLOCK.getOptional(key).orElse(Blocks.STONE);
                                } catch (Exception ex) {
                                    block = Blocks.STONE;
                                }
                                builder.add(block.defaultBlockState(), (int) Math.max(1, wb.weight() * 1000.0));
                            }
                            weightedList = builder.build();
                            layerDistributions = new HashMap<>();
                            for (int yy = min.getY() + 1; yy <= max.getY(); yy++) {
                                layerDistributions.put(yy, weightedList);
                            }
                            state = weightedList.getRandomValue(random).orElse(Blocks.STONE.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(p, state);
                }
            }
        }
        if (!borderBuilt) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int y = min.getY(); y <= max.getY(); y++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        boolean onBorder = x == min.getX() || x == max.getX()
                                || y == min.getY()
                                || z == min.getZ() || z == max.getZ();
                        if (onBorder) {
                            level.setBlockAndUpdate(new BlockPos(x, y, z), borderBlock);
                        }
                    }
                }
            }
            borderBuilt = true;
        }

        this.nextReset = level.getGameTime() + refillIntervalTicks;
    }

    private SimpleWeightedRandomList.@NotNull Builder<BlockState> getBlockStateBuilder(int y, int layerCount) {
        double position = (double) (y - (min.getY() + 1)) / Math.max(1.0, max.getY() - (min.getY() + 1));
        double scaled = position * (layerCount - 1);
        int idx = (int) Math.floor(scaled);
        double t = scaled - idx;
        MineLayer layer1 = layers.get(Math.min(idx, layerCount - 1));
        MineLayer layer2 = layers.get(Math.min(idx + 1, layerCount - 1));
        Map<String, Double> combined = new HashMap<>();
        BiConsumer<WeightedBlock, Double> accumulate = (wb, weightFactor) -> combined.merge(wb.blockId(), wb.weight() * weightFactor, Double::sum);
        for (WeightedBlock wb : layer1.distribution()) {
            accumulate.accept(wb, 1.0 - t);
        }
        for (WeightedBlock wb : layer2.distribution()) {
            accumulate.accept(wb, t);
        }
        SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
        combined.forEach((id, weight) -> {
            Block block;
            try {
                var key = ResourceLocation.parse(id);
                block = BuiltInRegistries.BLOCK.getOptional(key).orElse(Blocks.STONE);
            } catch (Exception ex) {
                block = Blocks.STONE;
            }
            builder.add(block.defaultBlockState(), (int) Math.max(1, weight * 1000.0));
        });
        return builder;
    }
}