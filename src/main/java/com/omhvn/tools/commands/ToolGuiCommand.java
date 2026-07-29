package com.omhvn.tools.commands;

import com.omhvn.tools.SolarTool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ToolGuiCommand implements CommandExecutor {
    private final SolarTool plugin;
    public static final String GUI_TITLE = "§8ѕᴏʟᴀʀ ᴛᴏᴏʟѕ";

    // Preset options in Small Caps (non-bold)
    public static final String[] TIME_PRESETS_LORE = {"ᴄᴏɴғɪɢ ᴅᴇғᴀᴜʟᴛ", "1ʜ", "6ʜ", "12ʜ", "1ᴅ", "3ᴅ", "7ᴅ", "30ᴅ"};
    public static final long[] TIME_PRESETS_MINUTES = {-1, 60, 360, 720, 1440, 4320, 10080, 43200};

    public static final String[] USES_PRESETS_LORE = {"ᴄᴏɴғɪɢ ᴅᴇғᴀᴜʟᴛ", "50ᴜ", "100ᴜ", "250ᴜ", "500ᴜ", "1000ᴜ", "2500ᴜ", "5000ᴜ"};
    public static final int[] USES_PRESETS_COUNT = {-1, 50, 100, 250, 500, 1000, 2500, 5000};

    private final Map<UUID, String> selectedMode = new HashMap<>();
    private final Map<UUID, Integer> timeIndex = new HashMap<>();
    private final Map<UUID, Integer> usesIndex = new HashMap<>();

    public ToolGuiCommand(SolarTool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be executed by players.");
            return true;
        }

        if (!player.hasPermission("solartools.admin")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        openGui(player);
        return true;
    }

    public String getPlayerMode(UUID uuid) {
        return selectedMode.getOrDefault(uuid, "TIME");
    }

    public void togglePlayerMode(UUID uuid) {
        String current = getPlayerMode(uuid);
        selectedMode.put(uuid, "TIME".equalsIgnoreCase(current) ? "USES" : "TIME");
    }

    public int getTimeIndex(UUID uuid) {
        return timeIndex.getOrDefault(uuid, 0);
    }

    public void cycleTimeIndex(UUID uuid, boolean forward) {
        int idx = getTimeIndex(uuid);
        if (forward) {
            idx = (idx + 1) % TIME_PRESETS_LORE.length;
        } else {
            idx = (idx - 1 + TIME_PRESETS_LORE.length) % TIME_PRESETS_LORE.length;
        }
        timeIndex.put(uuid, idx);
    }

    public int getUsesIndex(UUID uuid) {
        return usesIndex.getOrDefault(uuid, 0);
    }

    public void cycleUsesIndex(UUID uuid, boolean forward) {
        int idx = getUsesIndex(uuid);
        if (forward) {
            idx = (idx + 1) % USES_PRESETS_LORE.length;
        } else {
            idx = (idx - 1 + USES_PRESETS_LORE.length) % USES_PRESETS_LORE.length;
        }
        usesIndex.put(uuid, idx);
    }

    public void openGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Fill background with gray glass panes
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Add tools to the middle row (slots 10 to 16)
        inv.setItem(10, plugin.getToolManager().createTool("drill"));
        inv.setItem(11, plugin.getToolManager().createTool("treechopper"));
        inv.setItem(12, plugin.getToolManager().createTool("shovel"));
        inv.setItem(13, plugin.getToolManager().createTool("hoe"));
        inv.setItem(14, plugin.getToolManager().createTool("waterbucket"));
        inv.setItem(15, plugin.getToolManager().createTool("multitool"));
        inv.setItem(16, plugin.getToolManager().createTool("rocket"));

        updateControlButtons(inv, player);

        player.openInventory(inv);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
    }

    public void updateControlButtons(Inventory inv, Player player) {
        UUID uuid = player.getUniqueId();
        String mode = getPlayerMode(uuid);

        // Slot 20: Mode toggle button
        ItemStack modeBtn = new ItemStack("TIME".equalsIgnoreCase(mode) ? Material.CLOCK : Material.REPEATER);
        ItemMeta modeMeta = modeBtn.getItemMeta();
        if (modeMeta != null) {
            modeMeta.setDisplayName("§eᴇxᴘɪʀᴀᴛɪᴏɴ ᴍᴏᴅᴇ: " + ("TIME".equalsIgnoreCase(mode) ? "§aᴛɪᴍᴇ" : "§bᴜѕᴇѕ"));
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to toggle mode between TIME and USES.");
            lore.add("§fCurrent Mode: " + ("TIME".equalsIgnoreCase(mode) ? "§aᴛɪᴍᴇ" : "§bᴜѕᴇѕ"));
            modeMeta.setLore(lore);
            modeBtn.setItemMeta(modeMeta);
        }
        inv.setItem(20, modeBtn);

        // Slot 24: Value selector button
        ItemStack valBtn = new ItemStack(Material.EMERALD);
        ItemMeta valMeta = valBtn.getItemMeta();
        if (valMeta != null) {
            if ("TIME".equalsIgnoreCase(mode)) {
                int tIdx = getTimeIndex(uuid);
                String valStr = TIME_PRESETS_LORE[tIdx];
                valMeta.setDisplayName("§eᴇxᴘɪʀᴀᴛɪᴏɴ ᴠᴀʟᴜᴇ: §a" + valStr);
                List<String> lore = new ArrayList<>();
                lore.add("§7Left-click: Next option | Right-click: Previous");
                lore.add("§fSelected Duration: §a" + valStr);
                valMeta.setLore(lore);
            } else {
                int uIdx = getUsesIndex(uuid);
                String valStr = USES_PRESETS_LORE[uIdx];
                valMeta.setDisplayName("§eᴇxᴘɪʀᴀᴛɪᴏɴ ᴠᴀʟᴜᴇ: §b" + valStr);
                List<String> lore = new ArrayList<>();
                lore.add("§7Left-click: Next option | Right-click: Previous");
                lore.add("§fSelected Uses: §b" + valStr);
                valMeta.setLore(lore);
            }
            valBtn.setItemMeta(valMeta);
        }
        inv.setItem(24, valBtn);
    }
}
