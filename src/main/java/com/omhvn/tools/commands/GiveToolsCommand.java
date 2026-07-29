package com.omhvn.tools.commands;

import com.omhvn.tools.SolarTool;
import com.omhvn.tools.utils.SecurityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class GiveToolsCommand implements CommandExecutor, TabCompleter {
   private final SolarTool plugin;
   private final List<String> toolTypes = Arrays.asList("drill", "treechopper", "waterbucket", "shovel", "multitool", "hoe", "rocket");

   public GiveToolsCommand(SolarTool plugin) {
      SecurityManager.checkLink(this);
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("solartools.admin")) {
         this.plugin.getMessageManager().sendMessage(sender, "no-permission");
         return true;
      } else if (args.length < 2) {
         this.plugin.getMessageManager().sendMessage(sender, "command.give.usage");
         this.plugin.getMessageManager().sendMessage(sender, "command.give.available-tools", "{tools}", String.join(", ", this.toolTypes));
         return true;
      } else {
         String toolType = args[0].toLowerCase();
         if (!this.toolTypes.contains(toolType)) {
            this.plugin.getMessageManager().sendMessage(sender, "command.give.invalid-tool", "{tools}", String.join(", ", this.toolTypes));
            return true;
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               this.plugin.getMessageManager().sendMessage(sender, "command.give.player-not-found", "{player}", args[1]);
               return true;
            } else {
               int amount = 1;
               long customDurationMinutes = -1L;
               int customUses = -1;
               if (args.length > 2) {
                  try {
                     String amountStr = args[2].replace(",", "").replace(".", "");
                     amount = Integer.parseInt(amountStr);
                     if (amount <= 0) {
                        this.plugin.getMessageManager().sendMessage(sender, "command.give.invalid-amount", "{amount}", args[2]);
                        return true;
                     }
                  } catch (NumberFormatException var13) {
                     this.plugin.getMessageManager().sendMessage(sender, "command.give.invalid-amount", "{amount}", args[2]);
                     return true;
                  }
               }

               if (args.length > 3) {
                  String arg3 = args[3];
                  if (arg3.toLowerCase().endsWith("u") || arg3.toLowerCase().startsWith("uses:")) {
                     String usesStr = arg3.toLowerCase().endsWith("u")
                           ? arg3.substring(0, arg3.length() - 1)
                           : arg3.substring(5);
                     try {
                        customUses = Integer.parseInt(usesStr.replace(",", "").replace(".", ""));
                        if (customUses <= 0) {
                           this.plugin.getMessageManager().sendMessage(sender, "command.give.invalid-uses");
                           return true;
                        }
                     } catch (NumberFormatException e) {
                        this.plugin.getMessageManager().sendMessage(sender, "command.give.invalid-uses");
                        return true;
                     }
                  } else {
                     customDurationMinutes = this.plugin.getMessageManager().parseDuration(arg3);
                     if (customDurationMinutes < 0L) {
                        this.plugin.getMessageManager().sendMessage(sender, "command.give.invalid-duration");
                        return true;
                     }
                  }
               }

               ItemStack tool = this.plugin.getToolManager().createTool(toolType);
               if (tool != null) {
                  if (customUses > 0) {
                     ItemMeta meta = tool.getItemMeta();
                     if (meta != null) {
                        meta.getPersistentDataContainer().set(
                              new NamespacedKey(plugin, "expiration_mode"), PersistentDataType.STRING, "uses");
                        meta.getPersistentDataContainer().set(
                              new NamespacedKey(plugin, "remaining_uses"), PersistentDataType.INTEGER, customUses);
                        tool.setItemMeta(meta);
                        this.plugin.getToolManager().updateExpirationDisplay(tool);
                     }
                  }
                  else if (customDurationMinutes > 0L && !target.isOp() && !target.hasPermission("solartool.bypass")) {
                     long expiresAtEpoch = Instant.now().getEpochSecond() + customDurationMinutes * 60L;
                     this.plugin.getToolManager().setExpirationEpoch(tool, expiresAtEpoch);
                  }

                  for(int i = 0; i < amount; ++i) {
                     target.getInventory().addItem(new ItemStack[]{tool.clone()});
                  }

                  this.plugin.getMessageManager().sendMessage(sender, "command.give.success-sender", "{player}", target.getName(), "{amount}", String.valueOf(amount), "{tool}", toolType);
                  this.plugin.getMessageManager().sendMessage(target, "command.give.success-receiver", "{amount}", String.valueOf(amount), "{tool}", toolType);
               }

               return true;
            }
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1) {
         return (List)this.toolTypes.stream().filter((type) -> type.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
      } else if (args.length == 2) {
         return (List)Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((name) -> name.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
      } else if (args.length == 3) {
         return (List)Arrays.asList("1", "16", "32", "64").stream().filter((s) -> s.startsWith(args[2])).collect(Collectors.toList());
      } else {
         return (List<String>)(args.length == 4
               ? Arrays.asList("7d", "3d", "1d", "12h", "6h", "1h", "30m", "500u", "100u", "50u", "uses:500", "uses:100").stream()
                     .filter((s) -> s.toLowerCase().startsWith(args[3].toLowerCase())).collect(Collectors.toList())
               : new ArrayList());
      }
   }
}
