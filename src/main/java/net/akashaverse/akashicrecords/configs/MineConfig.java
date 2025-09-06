package net.akashaverse.akashicrecords.configs;

import net.akashaverse.akashicrecords.core.mine.MineType;
import net.akashaverse.akashicrecords.core.mine.WeightedBlock;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines server‑side configuration options for the AutoRefillingMine mod.  Using
 * NeoForge's ModConfigSpec automatically generates a config file (common.toml)
 * that server owners can edit.  Values defined here are read at runtime via
 * their respective getters.
 */
public class MineConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * List of mine type definitions.  Each string must have exactly four
     * semicolon‑separated parts: (1) a type name, (2) refill interval in
     * minutes, (3) warning time in seconds, and (4) a comma‑separated list
     * of block weights.  The weights may be fractional; they are normalised
     * at runtime.  For example:
     *
     * <pre>
     *   "default;30;60;minecraft:stone=70,minecraft:coal_ore=10,minecraft:iron_ore=8"
     * </pre>
     */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MINE_TYPE_DEFINITIONS = BUILDER
            .comment("Definitions for each mine type: name;intervalMinutes;warningSeconds;blocks")
            .defineList(
                    "mineTypes",
                    List.of(
                            "default;30;60;minecraft:stone=70,minecraft:coal_ore=10,minecraft:iron_ore=8,minecraft:copper_ore=5,minecraft:diamond_ore=1,minecraft:air=6"
                    ),
                    obj -> obj instanceof String
            );

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MineConfig() {
    }

    /**
     * Parse and return a {@link MineType} corresponding to the given type name.
     * If the type is not found or the definition is malformed, this method
     * returns the first defined type as a fallback.  The parsing logic is
     * deliberately forgiving: numbers that fail to parse will fall back to
     * sensible defaults and unknown block ids are ignored.
     *
     * @param typeName the name of the requested mine type
     * @return a parsed {@link MineType}
     */
    public static MineType getType(String typeName) {
        List<? extends String> defs = MINE_TYPE_DEFINITIONS.get();
        // if config not yet loaded, return a built‑in default type
        if (defs == null || defs.isEmpty()) {
            return fallbackType();
        }
        MineType fallback = null;
        for (String def : defs) {
            MineType parsed = parseDefinition(def);
            if (fallback == null) {
                fallback = parsed;
            }
            if (parsed.name().equalsIgnoreCase(typeName)) {
                return parsed;
            }
        }
        return fallback != null ? fallback : fallbackType();
    }

    /**
     * Parse a single definition string into a {@link MineType}.  If the
     * definition is malformed (wrong number of parts), this method returns
     * a reasonable default based on the given name.
     */
    private static MineType parseDefinition(String def) {
        String[] parts = def.split(";", 4);
        String name = parts.length > 0 ? parts[0] : "default";
        int intervalMinutes = 30;
        int warningSeconds = 60;
        List<WeightedBlock> weights = new ArrayList<>();
        if (parts.length > 1) {
            try {
                intervalMinutes = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
                intervalMinutes = 30;
            }
        }
        if (parts.length > 2) {
            try {
                warningSeconds = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
                warningSeconds = 60;
            }
        }
        if (parts.length > 3) {
            String blocksPart = parts[3];
            String[] blockEntries = blocksPart.split(",");
            for (String entry : blockEntries) {
                String[] kv = entry.split("=");
                String id = kv.length > 0 ? kv[0].trim() : "minecraft:stone";
                double weight = 1.0;
                if (kv.length > 1) {
                    try {
                        weight = Double.parseDouble(kv[1]);
                    } catch (NumberFormatException ignored) {
                        weight = 1.0;
                    }
                }
                weights.add(new WeightedBlock(id, weight));
            }
        }
        int intervalTicks = intervalMinutes * 20 * 60;
        int warningTicks = warningSeconds * 20;
        return new MineType(name, intervalTicks, warningTicks, weights);
    }

    /**
     * A built‑in fallback type used when the config cannot be read.  Uses a
     * 30‑minute interval, a 60‑second warning, and a simple stone/coal/iron
     * distribution.
     */
    private static MineType fallbackType() {
        List<WeightedBlock> list = new ArrayList<>();
        list.add(new WeightedBlock("minecraft:stone", 70.0));
        list.add(new WeightedBlock("minecraft:coal_ore", 10.0));
        list.add(new WeightedBlock("minecraft:iron_ore", 8.0));
        list.add(new WeightedBlock("minecraft:copper_ore", 5.0));
        list.add(new WeightedBlock("minecraft:diamond_ore", 1.0));
        list.add(new WeightedBlock("minecraft:air", 6.0));
        return new MineType("default", 30 * 20 * 60, 60 * 20, list);
    }
}
