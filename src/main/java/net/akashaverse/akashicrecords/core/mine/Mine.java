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
    public final BlockPos min;
    public final BlockPos max;
    public BlockPos entrance;
    public final List<WeightedBlock> distribution = new ArrayList<>();
    public long nextReset;
    public final int refillIntervalTicks;
    public final int warningTicks;
    public final BlockState borderBlock;

    public Mine(BlockPos pos1, BlockPos pos2, BlockPos entrance,
                int refillIntervalTicks, int warningTicks,
                BlockState borderBlock, List<WeightedBlock> distribution) {
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
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public void regenerate(ServerLevel level) {
        // build weighted list of block states using the new ResourceLocation.parse API
        SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
        for (WeightedBlock wb : distribution) {
            Block block;
            try {
                var key = ResourceLocation.parse(wb.blockId());
                block = BuiltInRegistries.BLOCK.getOptional(key).orElse(Blocks.STONE);
            } catch (Exception ex) {
                // invalid id → fallback to stone
                block = Blocks.STONE;
            }
            builder.add(block.defaultBlockState(), (int) Math.max(1, wb.weight() * 1000.0));
        }
        SimpleWeightedRandomList<BlockState> weightedList = builder.build();
        RandomSource random = level.random;

        // fill interior
        for (int x = min.getX() + 1; x < max.getX(); x++) {
            for (int y = min.getY() + 1; y < max.getY(); y++) {
                for (int z = min.getZ() + 1; z < max.getZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState state = weightedList.getRandomValue(random).orElse(Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(p, state);
                }
            }
        }

        // rebuild border
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

        // schedule next reset
        nextReset = level.getGameTime() + refillIntervalTicks;
    }
}
