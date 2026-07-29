package com.omhvn.tools.utils;

import com.omhvn.tools.SolarTool;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class MessageManager {
   private final SolarTool plugin;
   private FileConfiguration messages;
   private File messagesFile;
   private static final Pattern HEX_PATTERN = Pattern.compile("&x(&[0-9A-Fa-f]){6}");

   public MessageManager(SolarTool plugin) {
      this.plugin = plugin;
      this.loadMessages();
   }

   public void loadMessages() {
      this.messagesFile = new File(this.plugin.getDataFolder(), "messages.yml");
      if (!this.messagesFile.exists()) {
         this.plugin.saveResource("messages.yml", false);
      }

      this.messages = YamlConfiguration.loadConfiguration(this.messagesFile);
      InputStream defConfigStream = this.plugin.getResource("messages.yml");
      if (defConfigStream != null) {
         YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
         this.messages.setDefaults(defConfig);
      }

   }

   public void reloadMessages() {
      this.loadMessages();
   }

   public String getMessage(String path) {
      String message = this.messages.getString(path);
      return message == null ? "&cMessage not found: " + path : this.translateColorCodes(message);
   }

   public String getMessage(String path, String... replacements) {
      String message = this.getMessage(path);

      for(int i = 0; i + 1 < replacements.length; i += 2) {
         message = message.replace(replacements[i], replacements[i + 1]);
      }

      return message;
   }

   public void sendMessage(CommandSender sender, String path) {
      sender.sendMessage(this.getMessage(path));
   }

   public void sendMessage(CommandSender sender, String path, String... replacements) {
      sender.sendMessage(this.getMessage(path, replacements));
   }

   public void sendPrefixedMessage(CommandSender sender, String path) {
      String var10001 = this.getMessage("prefix");
      sender.sendMessage(var10001 + this.getMessage(path));
   }

   public void sendPrefixedMessage(CommandSender sender, String path, String... replacements) {
      String var10001 = this.getMessage("prefix");
      sender.sendMessage(var10001 + this.getMessage(path, replacements));
   }

   public String translateColorCodes(String message) {
      if (message == null) {
         return "";
      } else {
         Matcher matcher = HEX_PATTERN.matcher(message);
         StringBuffer buffer = new StringBuffer();

         while(matcher.find()) {
            String hexCode = matcher.group().replace("&x", "").replace("&", "");
            StringBuilder hex = new StringBuilder("§x");

            for(char c : hexCode.toCharArray()) {
               hex.append("§").append(c);
            }

            matcher.appendReplacement(buffer, hex.toString());
         }

         matcher.appendTail(buffer);
         return ChatColor.translateAlternateColorCodes('&', buffer.toString());
      }
   }

   public long parseDuration(String input) {
      if (input != null && !input.isEmpty()) {
         long totalMinutes = 0L;
         Matcher matcher = Pattern.compile("(\\d+)([dhms])").matcher(input.toLowerCase());

         while(matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            switch (matcher.group(2)) {
               case "d":
                  totalMinutes += (long)value * 1440L;
                  break;
               case "h":
                  totalMinutes += (long)value * 60L;
                  break;
               case "m":
                  totalMinutes += (long)value;
                  break;
               case "s":
                  totalMinutes += (long)Math.max(1, value / 60);
            }
         }

         return totalMinutes > 0L ? totalMinutes : -1L;
      } else {
         return -1L;
      }
   }

   public String formatDuration(long seconds) {
      if (seconds <= 0L) {
         return this.getMessage("time.broken");
      } else {
         long days = seconds / 86400L;
         long hours = seconds % 86400L / 3600L;
         long minutes = seconds % 3600L / 60L;
         long secs = seconds % 60L;
         StringBuilder sb = new StringBuilder();
         if (days > 0L) {
            sb.append(days).append(this.getMessage("time.days")).append(" ");
         }

         if (hours > 0L) {
            sb.append(hours).append(this.getMessage("time.hours")).append(" ");
         }

         if (minutes > 0L) {
            sb.append(minutes).append(this.getMessage("time.minutes")).append(" ");
         }

         if (secs > 0L || sb.length() == 0) {
            sb.append(secs).append(this.getMessage("time.seconds"));
         }

         return sb.toString().trim();
      }
   }
}
