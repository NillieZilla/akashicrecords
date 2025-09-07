package net.akashaverse.akashicrecords.configs;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.akashaverse.akashicrecords.core.mine.MineLayer;
import net.akashaverse.akashicrecords.core.mine.MineType;
import net.akashaverse.akashicrecords.core.mine.WeightedBlock;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Configuration loader for AkashicRecords mines.  Instead of storing all mine
 * definitions in a single config entry, this class reads each mine type
 * definition from its own TOML file under {@code config/AkashicRecords/Mine/}.
 * The file name (without extension) becomes the type name.  Each file may
 * define the following keys:
 *
 * <pre>
 * intervalMinutes = 30        # refill interval in minutes
 * warningSeconds  = 60        # warning time in seconds
 * blocks = ["minecraft:stone=70", "minecraft:diamond_ore=0.5"]
 *
 * # Optional layered definitions.  Each entry in the [[layers]] array defines
 * # a vertical slice of the mine with its own distribution.  Layers are
 * # blended linearly based on depth.  If no layers are defined the
 * # top‑level blocks list is used for all depths.
 * [[layers]]
 * blocks = ["minecraft:stone=70", "minecraft:coal_ore=10"]
 * [[layers]]
 * blocks = ["minecraft:deepslate=60", "minecraft:diamond_ore=5"]
 * </pre>
 *
 * Unknown or malformed values are ignored or replaced with sensible defaults.
 */
public class MineConfig {
    public static final ModConfigSpec SPEC = new ModConfigSpec.Builder().build();

    /**
     * Cached map of type names to definitions loaded from the file system.
     */
    private static final Map<String, MineType> FILE_TYPES = new HashMap<>();

    private MineConfig() {
    }

    /**
     * Retrieve a mine type by name.  This method reloads and parses
     * TOML files from the config directory on each invocation.  If no
     * matching type is found, a built‑in default type is returned.
     *
     * @param typeName the name of the type requested
     * @return a {@link MineType}
     */
    public static MineType getType(String typeName) {
        loadMineTypesFromFiles();
        String key = typeName == null ? "" : typeName.toLowerCase(Locale.ROOT);
        MineType type = FILE_TYPES.get(key);
        if (type != null) {
            return type;
        }
        return fallbackType();
    }

    /**
     * Load all mine type definitions from {@code config/AkashicRecords/Mine}.
     * If the directory does not exist, it will be created and a default
     * {@code default.toml} will be written.  Files must have a .toml
     * extension.  Each file is parsed independently; failures are logged to
     * stderr but do not halt execution.
     */
    private static void loadMineTypesFromFiles() {
        FILE_TYPES.clear();
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path mineDir = configDir.resolve("AkashicRecords/Mine");
            if (!Files.exists(mineDir)) {
                Files.createDirectories(mineDir);
            }
            Path defaultFile = mineDir.resolve("default.toml");
            if (!Files.exists(defaultFile)) {
                List<String> defaultLines = List.of(
                        "# Default mine type for AkashicRecords",
                        "# intervalMinutes and warningSeconds control the timing",
                        "intervalMinutes = 30",
                        "warningSeconds = 60",
                        "blocks = [\"minecraft:stone=70\", \"minecraft:coal_ore=10\", \"minecraft:iron_ore=8\", \"minecraft:copper_ore=5\", \"minecraft:diamond_ore=1\", \"minecraft:air=6\"]"
                );
                Files.write(defaultFile, defaultLines);
            }
            try (var stream = Files.list(mineDir)) {
                stream.filter(path -> path.toString().endsWith(".toml")).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String typeName = fileName.substring(0, fileName.length() - 5);
                    try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().autosave().build()) {
                        config.load();
                        int intervalMinutes = config.getOrElse("intervalMinutes", 30);
                        int warningSeconds = config.getOrElse("warningSeconds", 60);
                        List<WeightedBlock> topDistribution = new ArrayList<>();
                        if (config.contains("blocks")) {
                            List<String> blocks = config.get("blocks");
                            if (blocks != null) {
                                for (String entry : blocks) {
                                    String[] kv = entry.split("=");
                                    String id = kv.length > 0 ? kv[0].trim() : "minecraft:stone";
                                    double weight = 1.0;
                                    if (kv.length > 1) {
                                        try {
                                            weight = Double.parseDouble(kv[1]);
                                        } catch (NumberFormatException ignored) {
                                        }
                                    }
                                    topDistribution.add(new WeightedBlock(id, weight));
                                }
                            }
                        }
                        List<MineLayer> layerList = new ArrayList<>();
                        if (config.contains("layers")) {
                            Object layersObj = config.get("layers");
                            if (layersObj instanceof List<?> layersRaw) {
                                for (Object entry : layersRaw) {
                                    if (entry instanceof Map<?,?> map) {
                                        Object blockListObj = map.get("blocks");
                                        if (blockListObj instanceof List<?> layerBlocks) {
                                            List<WeightedBlock> layerWeights = getWeightedBlocks(layerBlocks);
                                            layerList.add(new MineLayer(layerWeights));
                                        }
                                    }
                                }
                            }
                        }
                        int intervalTicks = intervalMinutes * 20 * 60;
                        int warningTicks = warningSeconds * 20;
                        List<MineLayer> layers = layerList.isEmpty() ? List.of() : List.copyOf(layerList);
                        MineType type = new MineType(typeName, intervalTicks, warningTicks, topDistribution, layers);
                        FILE_TYPES.put(typeName.toLowerCase(Locale.ROOT), type);
                    } catch (Exception e) {
                        System.err.println("Failed to load mine type from " + path + ": " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error loading mine type configs: " + e.getMessage());
        }
    }

    private static @NotNull List<WeightedBlock> getWeightedBlocks(List<?> layerBlocks) {
        List<WeightedBlock> layerWeights = new ArrayList<>();
        for (Object obj : layerBlocks) {
            if (obj instanceof String str) {
                String[] kv = str.split("=");
                String id = kv.length > 0 ? kv[0].trim() : "minecraft:stone";
                double weight = 1.0;
                if (kv.length > 1) {
                    try {
                        weight = Double.parseDouble(kv[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                layerWeights.add(new WeightedBlock(id, weight));
            }
        }
        return layerWeights;
    }

    /**
     * A built‑in fallback type used when no files can be read or a
     * requested type is missing.  Uses a 30‑minute interval, a 60‑second
     * warning, and a simple stone/coal/iron distribution.
     */
    private static MineType fallbackType() {
        List<WeightedBlock> list = new ArrayList<>();
        list.add(new WeightedBlock("minecraft:stone", 70.0));
        list.add(new WeightedBlock("minecraft:coal_ore", 10.0));
        list.add(new WeightedBlock("minecraft:iron_ore", 8.0));
        list.add(new WeightedBlock("minecraft:copper_ore", 5.0));
        list.add(new WeightedBlock("minecraft:diamond_ore", 1.0));
        list.add(new WeightedBlock("minecraft:air", 6.0));
        return new MineType("default", 30 * 20 * 60, 60 * 20, list, List.of());
    }
}