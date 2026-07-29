package com.omhvn.tools.tool;

import com.omhvn.tools.SolarTool;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;


public class ToolConfigManager {

    
    public static final List<String> ALL_TOOLS = List.of(
            "drill", "treechopper", "shovel", "hoe", "waterbucket", "multitool", "rocket");

    private final SolarTool plugin;

    
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    
    private final Map<String, Set<Material>> breakableCache = new HashMap<>();

    public ToolConfigManager(SolarTool plugin) {
        this.plugin = plugin;
        reload();
    }


    
    public void reload() {
        configs.clear();
        breakableCache.clear();

        File toolDir = getToolDir();
        if (!toolDir.exists() && !toolDir.mkdirs()) {
            plugin.getLogger().warning("[ToolConfigManager] Could not create tool/ directory.");
        }

        for (String toolName : ALL_TOOLS) {
            File file = new File(toolDir, toolName + ".yml");
            saveDefaultIfAbsent(file, toolName);
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            InputStream defaultStream = plugin.getResource("tool/" + toolName + ".yml");
            if (defaultStream != null) {
                FileConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                cfg.setDefaults(defaults);
            }

            configs.put(toolName, cfg);
        }
    }

    
    public FileConfiguration getConfig(String toolName) {
        return configs.get(toolName.toLowerCase());
    }

    
    public Set<Material> getBreakableBlocks(String toolName) {
        String key = toolName.toLowerCase();
        return breakableCache.computeIfAbsent(key, k -> {
            FileConfiguration cfg = configs.get(k);
            if (cfg == null) return Collections.emptySet();

            List<String> names = cfg.getStringList("breakable-blocks");
            if (names.isEmpty()) return Collections.emptySet();

            Set<Material> result = EnumSet.noneOf(Material.class);
            for (String name : names) {
                try {
                    result.add(Material.valueOf(name.trim().toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning(
                            "[ToolConfigManager] Unknown material '" + name
                            + "' in tool/" + k + ".yml breakable-blocks — skipped.");
                }
            }
            return Collections.unmodifiableSet(result);
        });
    }

    
    public boolean hasBreakableOverride(String toolName) {
        FileConfiguration cfg = configs.get(toolName.toLowerCase());
        return cfg != null && cfg.contains("breakable-blocks");
    }


    private File getToolDir() {
        return new File(plugin.getDataFolder(), "tool");
    }

    private void saveDefaultIfAbsent(File file, String toolName) {
        if (file.exists()) return;

        String resourcePath = "tool/" + toolName + ".yml";
        InputStream stream = plugin.getResource(resourcePath);
        if (stream == null) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("[ToolConfigManager] Could not create " + file.getName());
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING,
                        "[ToolConfigManager] Could not create " + file.getName(), e);
            }
            return;
        }

        plugin.saveResource(resourcePath, false);
    }
}
