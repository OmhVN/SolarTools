package com.omhvn.tools.utils;

import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class SchedulerUtil {
   private static final boolean IS_FOLIA;

   public static boolean isFolia() {
      return IS_FOLIA;
   }

   public static void runTask(Plugin plugin, Runnable task) {
      if (IS_FOLIA) {
         Bukkit.getGlobalRegionScheduler().run(plugin, (scheduledTask) -> task.run());
      } else {
         Bukkit.getScheduler().runTask(plugin, task);
      }

   }

   public static void runTaskLater(Plugin plugin, Runnable task, long delay) {
      if (IS_FOLIA) {
         Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (scheduledTask) -> task.run(), delay);
      } else {
         Bukkit.getScheduler().runTaskLater(plugin, task, delay);
      }

   }

   public static void runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
      if (IS_FOLIA) {
         Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (scheduledTask) -> task.run(), delay, period);
      } else {
         Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
      }

   }

   public static void runTaskAsync(Plugin plugin, Runnable task) {
      if (IS_FOLIA) {
         Bukkit.getAsyncScheduler().runNow(plugin, (scheduledTask) -> task.run());
      } else {
         Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
      }

   }

   public static void runTaskLaterAsync(Plugin plugin, Runnable task, long delay) {
      if (IS_FOLIA) {
         Bukkit.getAsyncScheduler().runDelayed(plugin, (scheduledTask) -> task.run(), delay * 50L, TimeUnit.MILLISECONDS);
      } else {
         Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
      }

   }

   public static void runTaskAtEntity(Plugin plugin, Entity entity, Runnable task) {
      if (IS_FOLIA) {
         entity.getScheduler().run(plugin, (scheduledTask) -> task.run(), (Runnable)null);
      } else {
         Bukkit.getScheduler().runTask(plugin, task);
      }

   }

   public static void runTaskLaterAtEntity(Plugin plugin, Entity entity, Runnable task, long delay) {
      if (IS_FOLIA) {
         entity.getScheduler().runDelayed(plugin, (scheduledTask) -> task.run(), (Runnable)null, delay);
      } else {
         Bukkit.getScheduler().runTaskLater(plugin, task, delay);
      }

   }

   public static void runTaskAtLocation(Plugin plugin, Location location, Runnable task) {
      if (IS_FOLIA) {
         Bukkit.getRegionScheduler().run(plugin, location, (scheduledTask) -> task.run());
      } else {
         Bukkit.getScheduler().runTask(plugin, task);
      }

   }

   public static void runTaskLaterAtLocation(Plugin plugin, Location location, Runnable task, long delay) {
      if (IS_FOLIA) {
         Bukkit.getRegionScheduler().runDelayed(plugin, location, (scheduledTask) -> task.run(), delay);
      } else {
         Bukkit.getScheduler().runTaskLater(plugin, task, delay);
      }

   }

   static {
      boolean folia;
      try {
         Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         folia = true;
      } catch (ClassNotFoundException var2) {
         folia = false;
      }

      IS_FOLIA = folia;
   }
}
