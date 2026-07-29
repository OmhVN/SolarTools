package com.omhvn.tools;

import com.omhvn.tools.commands.GiveToolsCommand;
import com.omhvn.tools.commands.ReloadCommand;
import com.omhvn.tools.commands.ToolGuiCommand;
import com.omhvn.tools.listeners.ToolListener;
import com.omhvn.tools.tool.ToolConfigManager;
import com.omhvn.tools.utils.BlacklistManager;
import com.omhvn.tools.utils.MessageManager;
import com.omhvn.tools.utils.ToolManager;
import com.omhvn.tools.utils.WorldGuardHelper;
import org.bukkit.plugin.java.JavaPlugin;

public final class SolarTool extends JavaPlugin {
    private ToolManager toolManager;
    private MessageManager messageManager;
    private BlacklistManager blacklistManager;
    private WorldGuardHelper worldGuardHelper;
    private com.omhvn.tools.database.DatabaseManager databaseManager;
    private ToolConfigManager toolConfigManager;
    private ToolGuiCommand toolGuiCommand;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.databaseManager = new com.omhvn.tools.database.DatabaseManager(this);
        this.databaseManager.load();
        this.toolConfigManager = new ToolConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.blacklistManager = new BlacklistManager(this);
        this.worldGuardHelper = new WorldGuardHelper(this);
        this.toolManager = new ToolManager(this);
        this.toolGuiCommand = new ToolGuiCommand(this);
        this.getCommand("givetools").setExecutor(new GiveToolsCommand(this));
        this.getCommand("solartool").setExecutor(new ReloadCommand(this));
        this.getCommand("tool").setExecutor(this.toolGuiCommand);
        this.getServer().getPluginManager().registerEvents(new ToolListener(this), this);

        try {
            int pluginId = 32652;
            org.bstats.bukkit.Metrics metrics = new org.bstats.bukkit.Metrics(this, pluginId);
            // Optional: Add custom charts
            metrics.addCustomChart(new org.bstats.charts.SimplePie("chart_id", () -> "My value"));
        } catch (Exception e) {
            this.getLogger().warning("Failed to initialize bStats metrics: " + e.getMessage());
        }
        
        org.bukkit.command.ConsoleCommandSender console = this.getServer().getConsoleSender();
        console.sendMessage(org.bukkit.ChatColor.GOLD + "");
        console.sendMessage(org.bukkit.ChatColor.GOLD + "   ____        _            _____           _       ");
        console.sendMessage(org.bukkit.ChatColor.GOLD + "  / ___|  ___ | | __ _ _ __|_   _|__   ___ | |___   ");
        console.sendMessage(org.bukkit.ChatColor.GOLD + "  \\___ \\ / _ \\| |/ _` | '__| | |/ _ \\ / _ \\| / __|  ");
        console.sendMessage(org.bukkit.ChatColor.GOLD + "   ___) | (_) | | (_| | |    | | (_) | (_) | \\__ \\  ");
        console.sendMessage(org.bukkit.ChatColor.GOLD + "  |____/ \\___/|_|\\__,_|_|    |_|\\___/ \\___/|_|___/  ");
        console.sendMessage(org.bukkit.ChatColor.GOLD + "");
        console.sendMessage(org.bukkit.ChatColor.YELLOW + "  Version: " + org.bukkit.ChatColor.WHITE + this.getDescription().getVersion());
        console.sendMessage(org.bukkit.ChatColor.YELLOW + "  Author:  " + org.bukkit.ChatColor.WHITE + String.join(", ", this.getDescription().getAuthors()));
        console.sendMessage(org.bukkit.ChatColor.GOLD + "");
        console.sendMessage(org.bukkit.ChatColor.GREEN + "SolarTools has been enabled!");
    }

    @Override
    public void onDisable() {
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
        this.getServer().getConsoleSender().sendMessage(org.bukkit.ChatColor.RED + "SolarTools has been disabled!");
    }

    public ToolManager getToolManager() {
        return this.toolManager;
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }

    public BlacklistManager getBlacklistManager() {
        return this.blacklistManager;
    }

    public WorldGuardHelper getWorldGuardHelper() {
        return this.worldGuardHelper;
    }

    public ToolConfigManager getToolConfigManager() {
        return this.toolConfigManager;
    }

    public ToolGuiCommand getToolGuiCommand() {
        return this.toolGuiCommand;
    }


    public void reinitializeToolManager() {
        this.toolConfigManager.reload();
        this.toolManager = new ToolManager(this);
        this.messageManager = new MessageManager(this);
        this.blacklistManager.reload();
    }
}
