package net.akashaverse.akashicrecords.configs;

import net.neoforged.neoforge.common.ModConfigSpec;
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
     * Default refill interval for newly created mines, in minutes.  A value of
     * 30 means that mines will refill every 30 minutes unless overridden by
     * commands or per‑mine configuration.  Minimum value is 1 minute.
     */
    public static final ModConfigSpec.IntValue DEFAULT_REFILL_INTERVAL_MINUTES = BUILDER
            .comment("Default mine refill interval in minutes")
            .defineInRange("refillInterval", 30, 1, 1440);

    /**
     * Warning time before refill, in seconds.  Players inside a mine will
     * receive a chat warning this many seconds before the mine resets.
     */
    public static final ModConfigSpec.IntValue WARNING_TIME_SECONDS = BUILDER
            .comment("Seconds before refill to send warning")
            .defineInRange("warningTime", 60, 0, 3600);

    /**
     * Default block distribution list.  Each entry is formatted
     * "namespace:block=weight".  For example: "minecraft:stone=70".  The sum of
     * weights is arbitrary; the weights are normalised at runtime.
     */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DEFAULT_DISTRIBUTION = BUILDER
            .comment("Default block distribution for new mines, format modid:block=weight")
            .defineList(
                    "defaultBlocks",
                    List.of(
                            "minecraft:stone=70",
                            "minecraft:coal_ore=10",
                            "minecraft:iron_ore=8",
                            "minecraft:copper_ore=5",
                            "minecraft:diamond_ore=1",
                            "minecraft:air=6"
                    ),
                    obj -> obj instanceof String
            );

    public static final ModConfigSpec SPEC = BUILDER.build();
}
