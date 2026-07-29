package com.omhvn.tools.api;

import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface SolarToolsAPI {
    boolean isCustomTool(ItemStack item);
    String getToolType(ItemStack item);
    ItemStack createTool(String type);
    ItemStack createTool(String type, String expirationMode, long durationOrUses);
    boolean isUsesMode(ItemStack item);
    int getRemainingUses(ItemStack item);
    long getRemainingTimeSeconds(ItemStack item);
    boolean consumeUse(ItemStack item);
    void giveTool(Player player, String type);
    void giveTool(Player player, String type, String expirationMode, long durationOrUses);
    Set<String> getAvailableTools();
}
