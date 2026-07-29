package com.omhvn.tools.commands;

import com.omhvn.tools.SolarTool;
import com.omhvn.tools.utils.SecurityManager;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class ReloadCommand implements CommandExecutor, TabCompleter {
   private final SolarTool plugin;

   public ReloadCommand(SolarTool plugin) {
      SecurityManager.checkLink(this);
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("solartools.admin")) {
         this.plugin.getMessageManager().sendMessage(sender, "no-permission");
         return true;
      } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
         try {
            this.plugin.reloadConfig();
            this.plugin.getMessageManager().reloadMessages();
            this.plugin.reinitializeToolManager();
            this.plugin.getMessageManager().sendMessage(sender, "command.reload.success");
            this.plugin.getLogger().info("Plugin được tải lại bởi " + sender.getName());
         } catch (Exception e) {
            this.plugin.getMessageManager().sendMessage(sender, "command.reload.error", "{error}", e.getMessage());
            this.plugin.getLogger().severe("Lỗi khi tải lại plugin: " + e.getMessage());
            e.printStackTrace();
         }

         return true;
      } else {
         this.plugin.getMessageManager().sendMessage(sender, "command.reload.usage");
         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      return args.length == 1 ? Collections.singletonList("reload") : Collections.emptyList();
   }
}
