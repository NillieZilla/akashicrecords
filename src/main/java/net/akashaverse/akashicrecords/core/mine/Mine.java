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

import java.util.ArrayList;
import java.util.List;

public class Mine {
    /** minimum corner of the cuboid (inclusive) */
    public final BlockPos min;
    /** maximum corner of the cuboid (inclusive) */
    public final BlockPos max;
    /** entrance used to teleport players out before regeneration */
    public BlockPos entrance;
    /** weighted distribution of blocks inside the mine */
    public final List<WeightedBlock> distribution = new ArrayList<>();
    /** game time (ticks) when the next reset will occur */
    public long nextReset;
    /** number of ticks between resets */
    public final int refillIntervalTicks;
    /** number of ticks before reset to warn players */
    public final int warningTicks;
    /** block used for the border surrounding the mine */
    public final BlockState borderBlock;

    public Mine(BlockPos pos1, BlockPos pos2, BlockPos entrance, int refillIntervalTicks, int warningTicks,
                BlockState borderBlock, List<WeightedBlock> distribution) {
        // normalise the region so min contains the lowest coordinates and max the highest
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
        this.nextReset = 0L;
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
     * Rebuilds the border and fills the interior with randomly selected blocks
     * according to {@link #distribution}.  After regeneration the next reset
     * time is scheduled based on {@link #refillIntervalTicks}.  Modded blocks
     * are supported because we resolve block identifiers via the global block
     * registry.  If a block id cannot be resolved it falls back to stone.
     */
    public void regenerate(ServerLevel level) {
        // Build a weighted random list of block states.  We multiply the weight by 1000
        // to convert from double weights to integer weights (required by the builder).
        SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
        for (WeightedBlock wb : distribution) {
            ResourceLocation key = new ResourceLocation(wb.blockId());
            Block block = BuiltInRegistries.BLOCK.getOptional(key).orElse(Blocks.STONE);
            builder.add(block.defaultBlockState(), (int) Math.max(1, wb.weight() * 1000.0));
        }
        SimpleWeightedRandomList<BlockState> weightedList = builder.build();
        RandomSource random = level.random;

        // Fill the interior (excluding border)
        for (int x = min.getX() + 1; x < max.getX(); x++) {
            for (int y = min.getY() + 1; y < max.getY(); y++) {
                for (int z = min.getZ() + 1; z < max.getZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState state = weightedList.getRandomValue(random).orElse(Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(p, state);
                }
            }
        }
        // Build the border (including the shell around the interior)
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    boolean onBorder = x == min.getX() || x == max.getX()
                            || y == min.getY() || y == max.getY()
                            || z == min.getZ() || z == max.getZ();
                    if (onBorder) {
                        level.setBlockAndUpdate(new BlockPos(x, y, z), borderBlock);
                    }
                }
            }
        }
        this.nextReset = level.getGameTime() + refillIntervalTicks;
    }
}
