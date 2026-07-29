package com.omhvn.tools.utils;

import com.omhvn.tools.SolarTool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlacklistManager {

    private final SolarTool plugin;
    private final Set<String> blacklistedWorlds = new HashSet<>();

    public BlacklistManager(SolarTool plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        blacklistedWorlds.clear();

        List<String> worlds = plugin.getConfig().getStringList("blacklist.worlds");
        for (String w : worlds) {
            blacklistedWorlds.add(w.toLowerCase());
        }

        plugin.getLogger().info("[SolarTool] Blacklist loaded: " + blacklistedWorlds.size() + " world(s).");
    }

    
    public boolean isWorldBlacklisted(String worldName) {
        return blacklistedWorlds.contains(worldName.toLowerCase());
    }
}
