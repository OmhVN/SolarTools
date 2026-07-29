package com.omhvn.tools.api;

import com.omhvn.tools.SolarTool;
import com.omhvn.tools.tool.ToolConfigManager;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class SolarToolsAPIImpl implements SolarToolsAPI {
    private final SolarTool plugin;

    public SolarToolsAPIImpl(SolarTool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isCustomTool(ItemStack item) {
        return plugin.getToolManager().isCustomTool(item);
    }

    @Override
    public String getToolType(ItemStack item) {
        return plugin.getToolManager().getToolType(item);
    }

    @Override
    public ItemStack createTool(String type) {
        return plugin.getToolManager().createTool(type);
    }

    @Override
    public ItemStack createTool(String type, String expirationMode, long durationOrUses) {
        ItemStack tool = plugin.getToolManager().createTool(type);
        if (tool == null) return null;
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return tool;

        if ("uses".equalsIgnoreCase(expirationMode)) {
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "expiration_mode"), PersistentDataType.STRING, "uses");
            if (durationOrUses > 0) {
                meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "remaining_uses"), PersistentDataType.INTEGER, (int) durationOrUses);
            }
        } else {
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "expiration_mode"), PersistentDataType.STRING, "time");
            if (durationOrUses > 0) {
                long expiresAtEpoch = Instant.now().getEpochSecond() + durationOrUses * 60L;
                meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "expiresat_epoch"), PersistentDataType.LONG, expiresAtEpoch);
            }
        }
        tool.setItemMeta(meta);
        plugin.getToolManager().updateExpirationDisplay(tool);
        return tool;
    }

    @Override
    public boolean isUsesMode(ItemStack item) {
        return plugin.getToolManager().isUsesMode(item);
    }

    @Override
    public int getRemainingUses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1;
        Integer uses = meta.getPersistentDataContainer().get(
            new NamespacedKey(plugin, "remaining_uses"), PersistentDataType.INTEGER);
        return uses != null ? uses : -1;
    }

    @Override
    public long getRemainingTimeSeconds(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1;
        Long expiresAt = meta.getPersistentDataContainer().get(
            new NamespacedKey(plugin, "expiresat_epoch"), PersistentDataType.LONG);
        if (expiresAt == null) return -1;
        long diff = expiresAt - Instant.now().getEpochSecond();
        return Math.max(0, diff);
    }

    @Override
    public boolean consumeUse(ItemStack item) {
        return plugin.getToolManager().consumeUse(item);
    }

    @Override
    public void giveTool(Player player, String type) {
        ItemStack tool = createTool(type);
        if (tool != null && player != null && player.isOnline()) {
            player.getInventory().addItem(tool);
        }
    }

    @Override
    public void giveTool(Player player, String type, String expirationMode, long durationOrUses) {
        ItemStack tool = createTool(type, expirationMode, durationOrUses);
        if (tool != null && player != null && player.isOnline()) {
            player.getInventory().addItem(tool);
        }
    }

    @Override
    public Set<String> getAvailableTools() {
        return new HashSet<>(ToolConfigManager.ALL_TOOLS);
    }
}
