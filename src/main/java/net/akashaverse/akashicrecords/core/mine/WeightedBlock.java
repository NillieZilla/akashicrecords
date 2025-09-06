package net.akashaverse.akashicrecords.core.mine;

/**
 * Simple record that holds a block identifier and a weight.  The block id
 * should be a valid Minecraft resource location string (e.g. "minecraft:stone").
 * Weights do not need to sum to any particular value; they will be normalised
 * when used in a weighted random selection.  We use a double for greater
 * precision when weights are specified with fractions.
 */
public record WeightedBlock(String blockId, double weight) {
    public WeightedBlock {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative: " + weight);
        }
    }
}