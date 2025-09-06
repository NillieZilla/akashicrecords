package net.akashaverse.akashicrecords.configs;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.akashaverse.akashicrecords.core.mine.MineType;
import net.akashaverse.akashicrecords.core.mine.WeightedBlock;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Defines server‑side configuration options for the AutoRefillingMine mod.  Using
 * NeoForge's ModConfigSpec automatically generates a config file (common.toml)
 * that server owners can edit.  Values defined here are read at runtime via
 * their respective getters.
 */
/**
 * Configuration loader for AutoRefillingMine.  Instead of storing all mine
 * definitions in a single config entry, this class reads each mine type
 * definition from its own TOML file under {@code config/AkashicRecords/Mine/}.
 * The file name (without extension) becomes the type name.  Each file may
 * define the following keys:
 *
 * <pre>
 * intervalMinutes = 30        # refill interval in minutes
 * warningSeconds  = 60        # warning time in seconds
 * blocks = ["minecraft:stone=70", "minecraft:diamond_ore=0.5"]
 * </pre>
 *
 * Weights may be fractional.  Unknown or malformed values are ignored or
 * replaced with sensible defaults.
 */
public class MineConfig {
    // An empty config spec – we still register a spec so NeoForge creates a
    // config directory entry for our mod, but all mine definitions live in
    // separate files.  Additional global settings could be added here later.
    public static final ModConfigSpec SPEC = new ModConfigSpec.Builder().build();

    /**
     * Cached map of type names to definitions loaded from the file system.
     */
    private static final Map<String, MineType> FILE_TYPES = new HashMap<>();
    private static boolean loaded = false;

    private MineConfig() {
    }

    /**
     * Retrieve a mine type by name.  This method lazily loads and parses
     * TOML files from the config directory on first invocation.  If no
     * matching type is found, or the directory does not exist, a built‑in
     * default type is returned.
     *
     * @param typeName the name of the type requested
     * @return a {@link MineType}
     */
    public static MineType getType(String typeName) {
        if (!loaded) {
            loadMineTypesFromFiles();
        }
        MineType type = FILE_TYPES.get(typeName.toLowerCase(Locale.ROOT));
        if (type != null) {
            return type;
        }
        // default fallback
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
        loaded = true;
        FILE_TYPES.clear();
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path mineDir = configDir.resolve("AkashicRecords/Mine");
            if (!Files.exists(mineDir)) {
                Files.createDirectories(mineDir);
            }
            // ensure a default file exists
            Path defaultFile = mineDir.resolve("default.toml");
            if (!Files.exists(defaultFile)) {
                List<String> defaultLines = List.of(
                        "# Default mine type for AutoRefillingMine",
                        "# intervalMinutes and warningSeconds control the timing",
                        "intervalMinutes = 2",
                        "warningSeconds = 60",
                        "blocks = [\"minecraft:stone=70\", \"minecraft:coal_ore=10\", \"minecraft:iron_ore=8\", \"minecraft:copper_ore=5\", \"minecraft:diamond_ore=1\"]"
                );
                Files.write(defaultFile, defaultLines);
            }
            // iterate over .toml files
            try (var stream = Files.list(mineDir)) {
                stream.filter(path -> path.toString().endsWith(".toml")).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String typeName = fileName.substring(0, fileName.length() - 5);
                    try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().autosave().build()) {
                        config.load();
                        int intervalMinutes = config.getOrElse("intervalMinutes", 30);
                        int warningSeconds = config.getOrElse("warningSeconds", 60);
                        @SuppressWarnings("unchecked")
                        List<String> blocks = config.get("blocks");
                        List<WeightedBlock> weights = new ArrayList<>();
                        if (blocks != null) {
                            for (String entry : blocks) {
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
                        MineType type = new MineType(typeName, intervalTicks, warningTicks, weights);
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
        return new MineType("default", 30 * 20 * 60, 60 * 20, list);
    }
}
