package net.akashaverse.akashicrecords.core.mine;

import java.util.List;

/**
 * Represents a parsed mine type from the configuration.  Each type has a
 * unique name, a refill interval (in ticks), a warning time (in ticks),
 * and a weighted distribution of blocks to use when filling the mine.
 */
public record MineType(String name, int refillIntervalTicks, int warningTicks, List<WeightedBlock> distribution) {
}
