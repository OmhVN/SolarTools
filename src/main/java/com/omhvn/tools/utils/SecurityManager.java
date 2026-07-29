package com.omhvn.tools.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class SecurityManager {
   private final Plugin plugin;

   public SecurityManager(Plugin plugin) {
      this.plugin = plugin;
   }

   // Obfuscated repackage prefix set by ProGuard (-repackageclasses in proguard.pro).
   // Internal classes are moved here after obfuscation and must be treated as trusted.
   private static final String OBFUSCATED_PREFIX =
         "decodenodecode.solartools.nothingtoseehere.stoptryingtofigurethisout" +
         ".youarewastingyourtime.seriouslythereisnothinghidden.keepgoingforward" +
         ".dontlookback.justignorethissegment.anothermeaninglesspartadded";

   public static void checkLink(Object caller) {
      if (caller != null) {
         String callerName = caller.getClass().getName();
         if (!callerName.startsWith("com.omhvn.tools") && !callerName.startsWith(OBFUSCATED_PREFIX)) {
            Bukkit.getLogger().warning("[SolarTool] Unauthorized caller detected: " + callerName);
         }

      }
   }

   public void validate() {
   }
}
