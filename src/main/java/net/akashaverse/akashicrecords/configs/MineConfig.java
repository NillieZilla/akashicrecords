package net.akashaverse.akashicrecords.configs;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.akashaverse.akashicrecords.core.mine.MineLayer;
import net.akashaverse.akashicrecords.core.mine.MineType;
import net.akashaverse.akashicrecords.core.mine.WeightedBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class MineConfig {
    public static final ModConfigSpec SPEC = new ModConfigSpec.Builder().build();

    private static final Map<String, MineType> FILE_TYPES = new HashMap<>();

    private MineConfig() {}

    public static MineType getType(String typeName) {
        loadMineTypesFromFiles();
        String key = typeName == null ? "" : typeName.toLowerCase(Locale.ROOT);
        MineType type = FILE_TYPES.get(key);
        if (type != null) return type;
        return fallbackType();
    }

    private static void loadMineTypesFromFiles() {
        FILE_TYPES.clear();
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path mineDir = configDir.resolve("AkashicRecords/Mine");
            if (!Files.exists(mineDir)) Files.createDirectories(mineDir);

            Path defaultFile = mineDir.resolve("default.toml");
            if (!Files.exists(defaultFile)) {
                Files.write(defaultFile, buildDefaultToml());
            }

            try (var stream = Files.list(mineDir)) {
                stream.filter(p -> p.toString().endsWith(".toml")).forEach(path -> {
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
                                        try { weight = Double.parseDouble(kv[1]); } catch (NumberFormatException ignored) {}
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
                                    if (entry instanceof Map<?, ?> map) {
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
                    try { weight = Double.parseDouble(kv[1]); } catch (NumberFormatException ignored) {}
                }
                layerWeights.add(new WeightedBlock(id, weight));
            }
        }
        return layerWeights;
    }

    private static List<String> buildDefaultToml() {
        List<String> surfaceOres = new ArrayList<>(List.of(
                "minecraft:coal_ore=8",
                "minecraft:copper_ore=6",
                "minecraft:iron_ore=5",
                "minecraft:gold_ore=2",
                "minecraft:emerald_ore=0.2",
                "minecraft:lapis_ore=1"
        ));
        List<String> deepslateOres = new ArrayList<>(List.of(
                "minecraft:deepslate_coal_ore=5",
                "minecraft:deepslate_copper_ore=4",
                "minecraft:deepslate_iron_ore=6",
                "minecraft:deepslate_gold_ore=2",
                "minecraft:deepslate_emerald_ore=0.2",
                "minecraft:deepslate_lapis_ore=1",
                "minecraft:deepslate_diamond_ore=0.6",
                "minecraft:ancient_debris=0.0"
        ));

        BuiltInRegistries.BLOCK.entrySet().forEach(entry -> {
            ResourceLocation id = entry.getKey().location();
            String s = id.toString();
            if (!s.startsWith("minecraft:") && s.contains("ore")) {
                if (s.contains("deepslate")) {
                    deepslateOres.add(s + "=0");
                } else {
                    surfaceOres.add(s + "=0");
                }
            }
        });

        return getStrings(surfaceOres, deepslateOres);
    }

    private static @NotNull List<String> getStrings(List<String> surfaceOres, List<String> deepslateOres) {
        List<String> lines = new ArrayList<>();
        lines.add("# Default mine type for AkashicRecords");
        lines.add("# intervalMinutes and warningSeconds control timing");
        lines.add("intervalMinutes = 30");
        lines.add("warningSeconds = 60");
        lines.add("");
        lines.add("# Optional top-level fallback if no layers are defined");
        lines.add("blocks = [");
        lines.add("  \"minecraft:stone=70\",");
        lines.add("  \"minecraft:andesite=6\",");
        lines.add("  \"minecraft:granite=6\",");
        lines.add("  \"minecraft:diorite=6\",");
        lines.add("  \"minecraft:dirt=6\",");
        lines.add("  \"minecraft:gravel=3\"");
        lines.add("]");
        lines.add("");
        lines.add("# Layered distributions. Layers are blended by depth across the mine height.");
        lines.add("[[layers]]");
        lines.add("blocks = [");
        lines.add("  \"minecraft:stone=60\",");
        lines.add("  \"minecraft:andesite=8\",");
        lines.add("  \"minecraft:granite=8\",");
        lines.add("  \"minecraft:diorite=8\",");
        lines.add("  \"minecraft:dirt=8\",");
        lines.add("  \"minecraft:gravel=4\",");
        for (int i = 0; i < surfaceOres.size(); i++) {
            String e = surfaceOres.get(i);
            lines.add("  \"" + e + "\"" + (i < surfaceOres.size() - 1 ? "," : ""));
        }
        lines.add("]");
        lines.add("");
        lines.add("[[layers]]");
        lines.add("blocks = [");
        lines.add("  \"minecraft:deepslate=70\",");
        lines.add("  \"minecraft:tuff=10\",");
        lines.add("  \"minecraft:dripstone_block=5\",");
        for (int i = 0; i < deepslateOres.size(); i++) {
            String e = deepslateOres.get(i);
            lines.add("  \"" + e + "\"" + (i < deepslateOres.size() - 1 ? "," : ""));
        }
        lines.add("]");
        return lines;
    }

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
