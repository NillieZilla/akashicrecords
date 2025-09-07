package net.akashaverse.akashicrecords.core.mine;

import java.util.List;

/**
 * Represents a single layer within a multi‑layered mine.  Each layer defines
 * its own weighted distribution of blocks.  When generating the mine the
 * distributions of adjacent layers are blended across the vertical axis to
 * create a smooth transition between them.
 */
public record MineLayer(List<WeightedBlock> distribution) {
    /**
     * Create a layer with the provided block distribution.  The list
     * should not be null; if it is, an empty list will be used instead.
     */
    public MineLayer {
        if (distribution == null) {
            throw new IllegalArgumentException("Distribution cannot be null for a layer");
        }
    }
}