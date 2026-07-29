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

/**
 * Loads and caches per-tool YAML configuration files from
 * {@code plugins/SolarTool/tool/<toolname>.yml}.
 *
 * <p>Each file is created from an embedded default (in
 * {@code src/main/resources/tool/<toolname>.yml}) when it does not yet
 * exist on disk, so server admins always get a fully-commented template.
 *
 * <p>Supported tool names (lower-case): drill, treechopper, shovel,
 * hoe, waterbucket, multitool.
 */
public class ToolConfigManager {

    /** All tool names recognised by this plugin. */
    public static final List<String> ALL_TOOLS = List.of(
            "drill", "treechopper", "shovel", "hoe", "waterbucket", "multitool", "rocket");

    private final SolarTool plugin;

    /** toolName → parsed FileConfiguration */
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    /** toolName → cached Set<Material> of breakable blocks */
    private final Map<String, Set<Material>> breakableCache = new HashMap<>();

    public ToolConfigManager(SolarTool plugin) {
        this.plugin = plugin;
        reload();
    }

    // ── public API ───────────────────────────────────────────────────────────

    /**
     * (Re-)loads every tool config from disk, saving defaults first if the
     * file is missing.
     */
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

            // Merge any keys present in the embedded default but missing from
            // the user's file (forward-compatibility).
            InputStream defaultStream = plugin.getResource("tool/" + toolName + ".yml");
            if (defaultStream != null) {
                FileConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                cfg.setDefaults(defaults);
            }

            configs.put(toolName, cfg);
        }
    }

    /**
     * Returns the {@link FileConfiguration} for the given tool, or
     * {@code null} if the tool name is unknown.
     */
    public FileConfiguration getConfig(String toolName) {
        return configs.get(toolName.toLowerCase());
    }

    /**
     * Returns the set of {@link Material}s that the given tool is allowed
     * to break as part of its area effect.
     *
     * <p>The list is read from {@code breakable-blocks} in the tool's YAML.
     * If the key is absent or empty the set is empty (no area-break
     * restriction — callers should fall back to their own logic).
     *
     * <p>Results are cached; call {@link #reload()} to clear the cache.
     */
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

    /**
     * Returns {@code true} if {@code breakable-blocks} is defined (even if
     * empty) in the tool's config, indicating the admin wants to restrict
     * breakable blocks explicitly.
     */
    public boolean hasBreakableOverride(String toolName) {
        FileConfiguration cfg = configs.get(toolName.toLowerCase());
        return cfg != null && cfg.contains("breakable-blocks");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private File getToolDir() {
        return new File(plugin.getDataFolder(), "tool");
    }

    private void saveDefaultIfAbsent(File file, String toolName) {
        if (file.exists()) return;

        String resourcePath = "tool/" + toolName + ".yml";
        InputStream stream = plugin.getResource(resourcePath);
        if (stream == null) {
            // No embedded default — create an empty file so the dir is populated.
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

        // Use Bukkit's saveResource so the embedded file (with comments) lands on disk.
        plugin.saveResource(resourcePath, false);
    }
}
