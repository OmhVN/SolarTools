package com.omhvn.tools.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class SolarToolExpireEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String toolType;
    private final ItemStack item;
    private final String reason;

    public SolarToolExpireEvent(Player player, String toolType, ItemStack item, String reason) {
        this.player = player;
        this.toolType = toolType;
        this.item = item;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public String getToolType() {
        return toolType;
    }

    public ItemStack getItem() {
        return item;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
