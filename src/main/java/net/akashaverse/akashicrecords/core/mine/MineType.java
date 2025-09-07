package net.akashaverse.akashicrecords.core.mine;

import java.util.List;

/**
 * Represents a parsed mine type from the configuration.  Each type has a
 * unique name, a refill interval (in ticks), a warning time (in ticks),
 * a default weighted distribution of blocks and an optional set of layers.
 * When {@link #layers()} is non‑empty, the mine generation blends the
 * distributions of adjacent layers across the vertical axis to create a
 * transition effect.  If {@code layers} is empty or null then the
 * {@code distribution} is used uniformly throughout the mine.
 */
public record MineType(String name,
                       int refillIntervalTicks,
                       int warningTicks,
                       List<WeightedBlock> distribution,
                       List<MineLayer> layers) {

    /**
     * Returns true if this type defines at least one layer.  When layers are
     * present the mine will be generated using blended distributions per
     * vertical slice; otherwise the default distribution is used.
     */
    public boolean hasLayers() {
        return layers != null && !layers.isEmpty();
    }
}