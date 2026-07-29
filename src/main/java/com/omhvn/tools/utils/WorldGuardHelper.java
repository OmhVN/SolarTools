package com.omhvn.tools.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldGuardHelper {

    private final boolean enabled;

    public WorldGuardHelper(Plugin plugin) {
        boolean wgFound = false;
        try {
            Plugin wg = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            if (wg != null && wg.isEnabled()) {
                
                Class.forName("com.sk89q.worldguard.WorldGuard");
                wgFound = true;
                plugin.getLogger().info("[SolarTool] WorldGuard detected – region protection enabled.");
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("[SolarTool] WorldGuard found but incompatible version – region checks disabled.");
        }
        this.enabled = wgFound;
    }

    public boolean isEnabled() {
        return enabled;
    }

    
    public boolean canBuild(Player player, Location location) {
        if (!enabled) return true;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
            com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            return query.testBuild(weLoc, localPlayer);
        } catch (Exception e) {
            
            return true;
        }
    }

    public boolean isFlowAllowed(Location from, Location to) {
        if (!enabled) return true;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(BukkitAdapter.adapt(from.getWorld()));
            if (manager == null) return true;

            BlockVector3 fromVec = BlockVector3.at(from.getX(), from.getY(), from.getZ());
            BlockVector3 toVec = BlockVector3.at(to.getX(), to.getY(), to.getZ());

            ApplicableRegionSet fromSet = manager.getApplicableRegions(fromVec);
            ApplicableRegionSet toSet = manager.getApplicableRegions(toVec);

            if (toSet.size() == 0) return true;

            if (fromSet.size() == 0) return false;

            for (ProtectedRegion toReg : toSet) {
                if (!fromSet.getRegions().contains(toReg)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}

