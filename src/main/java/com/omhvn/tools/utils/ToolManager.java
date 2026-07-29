package com.omhvn.tools.utils;

import com.omhvn.tools.SolarTool;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ToolManager {
   private final SolarTool plugin;
   private final NamespacedKey toolTypeKey;
   private final NamespacedKey expirationTimeKey;
   private final NamespacedKey expirationModeKey;
   private final NamespacedKey remainingUsesKey;
   private static final Pattern HEX_PATTERN = Pattern.compile("&x(&[0-9A-Fa-f]){6}");

   public ToolManager(SolarTool plugin) {
      SecurityManager.checkLink(this);
      this.plugin = plugin;
      this.toolTypeKey      = new NamespacedKey(plugin, "tooltype");
      this.expirationTimeKey = new NamespacedKey(plugin, "expiresat_epoch");
      this.expirationModeKey = new NamespacedKey(plugin, "expiration_mode");
      this.remainingUsesKey  = new NamespacedKey(plugin, "remaining_uses");
   }

   public String translateColorCodes(String message) {
      Matcher matcher = HEX_PATTERN.matcher(message);
      StringBuffer buffer = new StringBuffer();
      while (matcher.find()) {
         String hexCode = matcher.group().replace("&x", "").replace("&", "");
         StringBuilder hex = new StringBuilder("§x");
         for (char c : hexCode.toCharArray()) hex.append("§").append(c);
         matcher.appendReplacement(buffer, hex.toString());
      }
      matcher.appendTail(buffer);
      return ChatColor.translateAlternateColorCodes('&', buffer.toString());
   }

   private String normalizeToolName(String type) {
       if (type == null) return "drill";
       String lower = type.toLowerCase();
       if (lower.equals("bucket")) return "waterbucket";
       if (lower.equals("firework") || lower.equals("rocket")) return "rocket";
       return lower;
    }

   private Enchantment getEnchantmentFromName(String name) {
      return switch (name.toLowerCase()) {
         case "efficiency"        -> Enchantment.EFFICIENCY;
         case "unbreaking"        -> Enchantment.UNBREAKING;
         case "mending"           -> Enchantment.MENDING;
         case "fortune"           -> Enchantment.FORTUNE;
         case "silk_touch"        -> Enchantment.SILK_TOUCH;
         case "aqua_affinity"     -> Enchantment.AQUA_AFFINITY;
         case "sharpness"         -> Enchantment.SHARPNESS;
         case "protection"        -> Enchantment.PROTECTION;
         case "looting"           -> Enchantment.LOOTING;
         case "power"             -> Enchantment.POWER;
         case "punch"             -> Enchantment.PUNCH;
         case "flame"             -> Enchantment.FLAME;
         case "infinity"          -> Enchantment.INFINITY;
         case "knockback"         -> Enchantment.KNOCKBACK;
         case "fire_aspect"       -> Enchantment.FIRE_ASPECT;
         case "smite"             -> Enchantment.SMITE;
         case "bane_of_arthropods"-> Enchantment.BANE_OF_ARTHROPODS;
         default -> {
            try { yield Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase())); }
            catch (Exception e) { yield null; }
         }
      };
   }

   public void applyEnchantmentsToItem(ItemStack item, String toolType) {
       ItemMeta meta = item.getItemMeta();
       if (meta == null) return;
       String name = normalizeToolName(toolType);
       FileConfiguration cfg = plugin.getToolConfigManager().getConfig(name);
       if (cfg == null) return;
       ConfigurationSection enchantSection = cfg.getConfigurationSection("enchantments");
       if (enchantSection != null) {
          for (String enchantName : enchantSection.getKeys(false)) {
             Enchantment enchant = getEnchantmentFromName(enchantName);
             if (enchant != null) {
                meta.addEnchant(enchant, enchantSection.getInt(enchantName), true);
             }
          }
       }
       item.setItemMeta(meta);
    }

   public boolean isCustomTool(ItemStack item) {
      if (item == null || item.getType() == Material.AIR) return false;
      ItemMeta meta = item.getItemMeta();
      return meta != null && meta.getPersistentDataContainer().has(toolTypeKey, PersistentDataType.STRING);
   }

   public void initializeNewTool(ItemStack item) {
      if (!isCustomTool(item)) return;
      ItemMeta meta = item.getItemMeta();
      meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "initialized"), PersistentDataType.BYTE, (byte) 1);
      item.setItemMeta(meta);
      updateExpirationDisplay(item);
   }


   
   public String getExpirationMode(ItemStack item) {
      if (!isCustomTool(item)) return "time";
      ItemMeta meta = item.getItemMeta();
      if (meta.getPersistentDataContainer().has(expirationModeKey, PersistentDataType.STRING)) {
         return meta.getPersistentDataContainer().get(expirationModeKey, PersistentDataType.STRING);
      }
      String type = meta.getPersistentDataContainer().get(toolTypeKey, PersistentDataType.STRING);
      String name = normalizeToolName(type);
      FileConfiguration cfg = plugin.getToolConfigManager().getConfig(name);
      return cfg != null ? cfg.getString("expiration-mode", "time") : "time";
   }

   public boolean isUsesMode(ItemStack item) {
      return "uses".equalsIgnoreCase(getExpirationMode(item));
   }


   public void startDerivation(ItemStack item, Player player) {
      if (!isCustomTool(item)) return;
      boolean isBypass = player.isOp() || player.hasPermission("solartool.bypass");
      if (isBypass) { updateExpirationDisplay(item); return; }

      ItemMeta meta = item.getItemMeta();
      String mode = getExpirationMode(item);

      if ("time".equalsIgnoreCase(mode)) {
         if (!meta.getPersistentDataContainer().has(expirationTimeKey, PersistentDataType.LONG)) {
            String type = meta.getPersistentDataContainer().get(toolTypeKey, PersistentDataType.STRING);
            String name = normalizeToolName(type);
            FileConfiguration cfg = plugin.getToolConfigManager().getConfig(name);
            int durationMinutes = cfg != null ? cfg.getInt("duration", 60) : 60;
            long expiresAtEpoch = Instant.now().getEpochSecond() + (long) durationMinutes * 60L;
            meta.getPersistentDataContainer().set(expirationTimeKey, PersistentDataType.LONG, expiresAtEpoch);
            item.setItemMeta(meta);
         }
      }
      updateExpirationDisplay(item);
   }

   public void setExpirationEpoch(ItemStack item, long epochSeconds) {
      if (!isCustomTool(item)) return;
      ItemMeta meta = item.getItemMeta();
      meta.getPersistentDataContainer().set(expirationTimeKey, PersistentDataType.LONG, epochSeconds);
      item.setItemMeta(meta);
      updateExpirationDisplay(item);
   }

   public String getToolType(ItemStack item) {
      if (!isCustomTool(item)) return null;
      return item.getItemMeta().getPersistentDataContainer().get(toolTypeKey, PersistentDataType.STRING);
   }


   
   public boolean consumeUse(ItemStack item) {
      if (!isCustomTool(item)) return true;
      if (!isUsesMode(item)) return true;

      ItemMeta meta = item.getItemMeta();
      if (!meta.getPersistentDataContainer().has(remainingUsesKey, PersistentDataType.INTEGER)) return true;

      int remaining = meta.getPersistentDataContainer().get(remainingUsesKey, PersistentDataType.INTEGER);
      remaining = Math.max(0, remaining - 1);
      meta.getPersistentDataContainer().set(remainingUsesKey, PersistentDataType.INTEGER, remaining);
      item.setItemMeta(meta);
      updateExpirationDisplay(item);
      return remaining > 0;
   }

   public int getRemainingUses(ItemStack item) {
      if (!isCustomTool(item)) return -1;
      ItemMeta meta = item.getItemMeta();
      if (!meta.getPersistentDataContainer().has(remainingUsesKey, PersistentDataType.INTEGER)) return -1;
      return meta.getPersistentDataContainer().get(remainingUsesKey, PersistentDataType.INTEGER);
   }


   public boolean hasExpired(ItemStack item) {
      if (!isCustomTool(item)) return false;
      ItemMeta meta = item.getItemMeta();
      String mode = getExpirationMode(item);

      if ("uses".equalsIgnoreCase(mode)) {
         if (meta.getPersistentDataContainer().has(remainingUsesKey, PersistentDataType.INTEGER)) {
            int remaining = meta.getPersistentDataContainer().get(remainingUsesKey, PersistentDataType.INTEGER);
            return remaining <= 0;
         }
         return false;
      } else {
         if (!meta.getPersistentDataContainer().has(expirationTimeKey, PersistentDataType.LONG)) return false;
         long expiresAt = meta.getPersistentDataContainer().get(expirationTimeKey, PersistentDataType.LONG);
         return Instant.now().getEpochSecond() > expiresAt;
      }
   }

   public void tickExpiration(ItemStack item) {
      if (!isCustomTool(item)) return;
      updateExpirationDisplay(item);
   }


   public void updateExpirationDisplay(ItemStack item) {
      if (!isCustomTool(item)) return;
      ItemMeta meta = item.getItemMeta();
      try {
         List<String> lore = meta.getLore();
         if (lore == null) lore = new ArrayList<>();
         MessageManager msg = plugin.getMessageManager();
         String mode = getExpirationMode(item);

         if ("uses".equalsIgnoreCase(mode)) {
            String usesText = msg.getMessage("lore.uses-remaining-text");
            String usesPrefix = msg.getMessage("lore.self-destruct-prefix");
            int remaining;
            if (meta.getPersistentDataContainer().has(remainingUsesKey, PersistentDataType.INTEGER)) {
               remaining = meta.getPersistentDataContainer().get(remainingUsesKey, PersistentDataType.INTEGER);
            } else {
               String type = meta.getPersistentDataContainer().get(toolTypeKey, PersistentDataType.STRING);
               String name = normalizeToolName(type);
               FileConfiguration cfg = plugin.getToolConfigManager().getConfig(name);
               remaining = cfg != null ? cfg.getInt("max-uses", 500) : 500;
            }
            String newLoreLine = translateColorCodes(usesPrefix + usesText + " " + remaining);
            String strippedTarget = ChatColor.stripColor(usesText);

            String selfDestructText = msg.getMessage("lore.self-destruct-text");
            String strippedTimeTarget = ChatColor.stripColor(selfDestructText);

            boolean found = false;
            for (int i = 0; i < lore.size(); i++) {
               String stripped = ChatColor.stripColor(lore.get(i));
               if (stripped.contains(strippedTimeTarget)) {
                  lore.set(i, newLoreLine);
                  found = true;
                  break;
               }
               if (stripped.contains(strippedTarget)) {
                  if (lore.get(i).equals(newLoreLine)) return;
                  lore.set(i, newLoreLine);
                  found = true;
                  break;
               }
            }
            if (!found) lore.add(newLoreLine);
         } else {
            String selfDestructText = msg.getMessage("lore.self-destruct-text");
            String timerPrefix      = msg.getMessage("lore.self-destruct-prefix");
            long remainingSeconds;
            if (!meta.getPersistentDataContainer().has(expirationTimeKey, PersistentDataType.LONG)) {
               String type = meta.getPersistentDataContainer().get(toolTypeKey, PersistentDataType.STRING);
               String name = normalizeToolName(type);
               FileConfiguration cfg = plugin.getToolConfigManager().getConfig(name);
               int durationMinutes = cfg != null ? cfg.getInt("duration", 60) : 60;
               remainingSeconds = (long) durationMinutes * 60L;
            } else {
               long expiresAt = meta.getPersistentDataContainer().get(expirationTimeKey, PersistentDataType.LONG);
               remainingSeconds = Math.max(0L, expiresAt - Instant.now().getEpochSecond());
            }
            String formattedTime = msg.formatDuration(remainingSeconds);
            String newLoreLine = translateColorCodes(timerPrefix + selfDestructText + " " + formattedTime);
            String strippedTarget = ChatColor.stripColor(selfDestructText);
            boolean found = false;
            for (int i = 0; i < lore.size(); i++) {
               if (ChatColor.stripColor(lore.get(i)).contains(strippedTarget)) {
                  if (lore.get(i).equals(newLoreLine)) return;
                  lore.set(i, newLoreLine);
                  found = true;
                  break;
               }
            }
            if (!found) lore.add(newLoreLine);
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
      } catch (Exception ignored) {}
   }


   public ItemStack createTool(String type) {
      String name = normalizeToolName(type);
      FileConfiguration cfg = plugin.getToolConfigManager().getConfig(name);
      if (cfg == null) {
         plugin.getLogger().warning("Tool configuration not found for: " + name);
         return null;
      }
      Material material = switch (name) {
         case "drill", "multitool" -> Material.NETHERITE_PICKAXE;
         case "treechopper"        -> Material.NETHERITE_AXE;
         case "shovel"             -> Material.NETHERITE_SHOVEL;
         case "hoe"                -> Material.NETHERITE_HOE;
         case "waterbucket"        -> Material.WATER_BUCKET;
         case "rocket"             -> Material.FIREWORK_ROCKET;
         default                   -> Material.STONE;
      };
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) return item;
      if (meta instanceof org.bukkit.inventory.meta.FireworkMeta fireworkMeta) {
         int power = cfg.getInt("power", 2);
         fireworkMeta.setPower(power);
      }
      meta.setDisplayName(translateColorCodes(cfg.getString("display-name", type)));
      String enchantText = cfg.getString("enchantment-text", "");
      List<String> lore = new ArrayList<>();
      if (!enchantText.isEmpty()) {
         for (String line : translateColorCodes(enchantText).split("\\\\n")) lore.add(line);
      }
      meta.getPersistentDataContainer().set(toolTypeKey, PersistentDataType.STRING, name);

      String mode = cfg.getString("expiration-mode", "time");
      meta.getPersistentDataContainer().set(expirationModeKey, PersistentDataType.STRING, mode);

      if ("uses".equalsIgnoreCase(mode)) {
         int maxUses = cfg.getInt("max-uses", 500);
         meta.getPersistentDataContainer().set(remainingUsesKey, PersistentDataType.INTEGER, maxUses);
      }

      meta.setLore(lore);
      item.setItemMeta(meta);
      applyEnchantmentsToItem(item, name);
      updateExpirationDisplay(item);
      return item;
   }
}
