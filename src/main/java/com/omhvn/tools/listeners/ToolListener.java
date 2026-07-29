package com.omhvn.tools.listeners;

import com.omhvn.tools.SolarTool;
import com.omhvn.tools.commands.ToolGuiCommand;
import com.omhvn.tools.utils.SchedulerUtil;
import com.omhvn.tools.utils.SecurityManager;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

public class ToolListener implements Listener {
   private final SolarTool plugin;
   private final List<Material> unbreakableBlocks;
   private final List<Material> soilBlocks;
   private final Map<UUID, BlockFace> lastClickedFace = new HashMap<>();
   private final Map<UUID, Long> rocketCooldowns = new HashMap<>();

   public ToolListener(SolarTool plugin) {
      SecurityManager.checkLink(this);
      this.plugin = plugin;
      this.unbreakableBlocks = new ArrayList<>();
      for (String blockName : plugin.getConfig().getStringList("UnBreakable")) {
         try { this.unbreakableBlocks.add(Material.valueOf(blockName)); }
         catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid material in UnBreakable config: " + blockName);
         }
      }
      this.soilBlocks = new ArrayList<>();
      for (String blockName : plugin.getConfig().getStringList("SoilBlocks")) {
         try { this.soilBlocks.add(Material.valueOf(blockName)); }
         catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid material in SoilBlocks config: " + blockName);
         }
      }
      SchedulerUtil.runTaskTimer(plugin, () -> {
         for (Player player : Bukkit.getOnlinePlayers()) {
            SchedulerUtil.runTaskAtEntity(plugin, player, () -> checkPlayerInventory(player));
         }
      }, 60L, 60L);
   }


   private int getRadius(String toolName) {
      org.bukkit.configuration.file.FileConfiguration cfg = plugin.getToolConfigManager().getConfig(toolName);
      int size = cfg != null ? cfg.getInt("size", 3) : 3;
      if (size < 3) size = 3;
      if (size > 64) size = 64;
      if (size % 2 == 0) size++;
      return (size - 1) / 2;
   }


   
   private BlockFace getTargetBlockFace(Player player) {
      BlockFace clicked = lastClickedFace.remove(player.getUniqueId());
      if (clicked != null) {
         return clicked;
      }
      RayTraceResult ray = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(), player.getEyeLocation().getDirection(),
            6.0, FluidCollisionMode.NEVER);
      if (ray != null && ray.getHitBlockFace() != null) {
         return ray.getHitBlockFace();
      }
      org.bukkit.util.Vector dir = player.getLocation().getDirection();
      double absX = Math.abs(dir.getX());
      double absY = Math.abs(dir.getY());
      double absZ = Math.abs(dir.getZ());
      if (absY > absX && absY > absZ) {
         return dir.getY() > 0 ? BlockFace.DOWN : BlockFace.UP;
      } else if (absX > absZ) {
         return dir.getX() > 0 ? BlockFace.WEST : BlockFace.EAST;
      } else {
         return dir.getZ() > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
      }
   }

    
    private List<Block> getBlocksInPlane(Block center, int radius, int depth, BlockFace face, Player player, boolean limitHorizontalAxis) {
       List<Block> blocks = new ArrayList<>();
       BlockFace playerFacing = player != null ? player.getFacing() : BlockFace.NORTH;
       for (int u = -radius; u <= radius; u++) {
          for (int v = -radius; v <= radius; v++) {
             if (limitHorizontalAxis && (face == BlockFace.UP || face == BlockFace.DOWN) && v != 0) {
                continue;
             }
             for (int d = 0; d < depth; d++) {
                int dx, dy, dz;
                switch (face) {
                   case UP -> {
                      if (limitHorizontalAxis) {
                         if (playerFacing == BlockFace.NORTH || playerFacing == BlockFace.SOUTH) {
                            dx = u; dz = 0;
                         } else {
                            dx = 0; dz = u;
                         }
                      } else {
                         dx = u; dz = v;
                      }
                      dy = -d;
                   }
                   case DOWN -> {
                      if (limitHorizontalAxis) {
                         if (playerFacing == BlockFace.NORTH || playerFacing == BlockFace.SOUTH) {
                            dx = u; dz = 0;
                         } else {
                            dx = 0; dz = u;
                         }
                      } else {
                         dx = u; dz = v;
                      }
                      dy = d;
                   }
                   case NORTH -> {
                      dx = u; dy = v; dz = d;
                   }
                   case SOUTH -> {
                      dx = u; dy = v; dz = -d;
                   }
                   case EAST -> {
                      dy = u; dz = v; dx = -d;
                   }
                   case WEST -> {
                      dy = u; dz = v; dx = d;
                   }
                   default -> { dx = u; dy = 0; dz = v; }
                }
                blocks.add(center.getRelative(dx, dy, dz));
             }
          }
       }
       return blocks;
    }


   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      SchedulerUtil.runTaskLaterAtEntity(plugin, player, () -> {
         checkPlayerInventory(player);
         for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getToolManager().isCustomTool(item)) {
               plugin.getToolManager().updateExpirationDisplay(item);
            }
         }
      }, 20L);
   }

   @EventHandler
   public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
      lastClickedFace.remove(event.getPlayer().getUniqueId());
   }

   
   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onItemPickup(EntityPickupItemEvent event) {
      if (!(event.getEntity() instanceof Player player)) return;
      ItemStack item = event.getItem().getItemStack();
      if (!plugin.getToolManager().isCustomTool(item)) return;

      plugin.getToolManager().updateExpirationDisplay(item);
      if (plugin.getToolManager().hasExpired(item)) {
         event.setCancelled(true);
         event.getItem().remove();
         plugin.getMessageManager().sendMessage(player, "tool.expired");
      }
   }

   
   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onInventoryClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player)) return;

      if (event.getView().getTitle().equals(com.omhvn.tools.commands.ToolGuiCommand.GUI_TITLE)) {
         event.setCancelled(true);
         ItemStack clicked = event.getCurrentItem();
         if (clicked == null || clicked.getType() == Material.AIR) return;
         if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

         ToolGuiCommand guiCmd = plugin.getToolGuiCommand();
         int slot = event.getRawSlot();
         UUID uuid = player.getUniqueId();

         if (slot == 20) {
            guiCmd.togglePlayerMode(uuid);
            guiCmd.updateControlButtons(event.getInventory(), player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
            return;
         }

         if (slot == 24) {
            boolean forward = event.isLeftClick();
            if ("TIME".equalsIgnoreCase(guiCmd.getPlayerMode(uuid))) {
               guiCmd.cycleTimeIndex(uuid, forward);
            } else {
               guiCmd.cycleUsesIndex(uuid, forward);
            }
            guiCmd.updateControlButtons(event.getInventory(), player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
            return;
         }

         if (!plugin.getToolManager().isCustomTool(clicked)) return;

         String toolType = plugin.getToolManager().getToolType(clicked);
         if (toolType != null) {
            ItemStack newTool = plugin.getToolManager().createTool(toolType);
            if (newTool != null) {
               String mode = guiCmd.getPlayerMode(uuid);
               ItemMeta meta = newTool.getItemMeta();
               if (meta != null) {
                  if ("USES".equalsIgnoreCase(mode)) {
                     meta.getPersistentDataContainer().set(
                           new NamespacedKey(plugin, "expiration_mode"), PersistentDataType.STRING, "uses");
                     int uIdx = guiCmd.getUsesIndex(uuid);
                     if (uIdx > 0) {
                        int count = ToolGuiCommand.USES_PRESETS_COUNT[uIdx];
                        meta.getPersistentDataContainer().set(
                              new NamespacedKey(plugin, "remaining_uses"), PersistentDataType.INTEGER, count);
                     }
                  } else {
                     meta.getPersistentDataContainer().set(
                           new NamespacedKey(plugin, "expiration_mode"), PersistentDataType.STRING, "time");
                     int tIdx = guiCmd.getTimeIndex(uuid);
                     if (tIdx > 0) {
                        long minutes = ToolGuiCommand.TIME_PRESETS_MINUTES[tIdx];
                        long expiresAtEpoch = java.time.Instant.now().getEpochSecond() + minutes * 60L;
                        meta.getPersistentDataContainer().set(
                              new NamespacedKey(plugin, "expiresat_epoch"), PersistentDataType.LONG, expiresAtEpoch);
                     }
                  }
                  newTool.setItemMeta(meta);
                  plugin.getToolManager().updateExpirationDisplay(newTool);
               }

               player.getInventory().addItem(newTool);
               player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
               plugin.getMessageManager().sendMessage(player, "tool.received");
            }
         }
         return;
      }

      ItemStack cursor = event.getCursor();
      ItemStack current = event.getCurrentItem();

      if (cursor != null && plugin.getToolManager().isCustomTool(cursor)) {
         plugin.getToolManager().updateExpirationDisplay(cursor);
         if (plugin.getToolManager().hasExpired(cursor)) {
            event.setCancelled(true);
            cursor.setAmount(0);
            plugin.getMessageManager().sendMessage(player, "tool.expired");
            return;
         }
      }

      if (current != null && plugin.getToolManager().isCustomTool(current)) {
         plugin.getToolManager().updateExpirationDisplay(current);
         if (plugin.getToolManager().hasExpired(current)) {
            event.setCancelled(true);
            current.setAmount(0);
            plugin.getMessageManager().sendMessage(player, "tool.expired");
         }
      }
   }

   @EventHandler
   public void onInventoryOpen(InventoryOpenEvent event) {
      if (!(event.getPlayer() instanceof Player player)) return;
      for (ItemStack item : player.getInventory().getContents()) {
         if (item != null && plugin.getToolManager().isCustomTool(item)) {
            plugin.getToolManager().updateExpirationDisplay(item);
         }
      }
      for (ItemStack item : event.getInventory().getContents()) {
         if (item != null && plugin.getToolManager().isCustomTool(item)) {
            plugin.getToolManager().updateExpirationDisplay(item);
         }
      }
   }

   @EventHandler
   public void onItemHold(PlayerItemHeldEvent event) {
      Player player = event.getPlayer();
      ItemStack item = player.getInventory().getItem(event.getNewSlot());
      if (item == null || !plugin.getToolManager().isCustomTool(item)) return;

      plugin.getToolManager().updateExpirationDisplay(item);
      boolean bypass = player.isOp() || player.hasPermission("solartool.bypass");

      if (!bypass && plugin.getToolManager().hasExpired(item)) {
         player.getInventory().setItem(event.getNewSlot(), null);
         plugin.getMessageManager().sendMessage(player, "tool.expired");
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      ItemStack item = player.getInventory().getItemInMainHand();
      if (!plugin.getToolManager().isCustomTool(item)) return;

      plugin.getToolManager().updateExpirationDisplay(item);
      boolean bypass = player.isOp() || player.hasPermission("solartool.bypass");

      if (!bypass && plugin.getBlacklistManager().isWorldBlacklisted(player.getWorld().getName())) {
         plugin.getMessageManager().sendMessage(player, "blacklist.world-blocked");
         event.setCancelled(true);
         return;
      }
      if (!bypass && !plugin.getWorldGuardHelper().canBuild(player, event.getBlock().getLocation())) {
         plugin.getMessageManager().sendMessage(player, "worldguard.no-build");
         event.setCancelled(true);
         return;
      }

      if (!bypass && plugin.getToolManager().hasExpired(item)) {
         player.getInventory().setItemInMainHand(null);
         String expMsg = plugin.getToolManager().isUsesMode(item) ? "tool.uses-depleted" : "tool.expired";
         plugin.getMessageManager().sendMessage(player, expMsg);
         event.setCancelled(true);
         return;
      }

      String toolType = plugin.getToolManager().getToolType(item);
      Block block = event.getBlock();
      Material blockType = block.getType();

      if (!"multitool".equals(toolType)) {
         switch (toolType) {
            case "drill"       -> spawnCustomParticles(block.getLocation().add(0.5, 0.5, 0.5), 10);
            case "treechopper" -> spawnElectricSparkParticles(block.getLocation().add(0.5, 0.5, 0.5), 10);
            case "shovel"      -> spawnCustomParticles(block.getLocation().add(0.5, 0.5, 0.5), 10);
            case "hoe"         -> spawnVillagerHappyParticles(block.getLocation().add(0.5, 0.5, 0.5), 10);
         }
         switch (toolType) {
            case "drill"       -> handleDrillBreak(player, block);
            case "treechopper" -> handleTreeChopperBreak(player, block);
            case "shovel"      -> handleShovelBreak(player, block);
         }
      } else {
         Set<Material> breakable = plugin.getToolConfigManager().getBreakableBlocks("multitool");
         boolean useConfig = !breakable.isEmpty();
         if (isBlockMinableWithPickaxe(blockType)) updateMultiToolAppearance(item, Material.NETHERITE_PICKAXE);
         else if (Tag.LOGS.isTagged(blockType) || Tag.LEAVES.isTagged(blockType)) updateMultiToolAppearance(item, Material.NETHERITE_AXE);
         else if (soilBlocks.contains(blockType)) updateMultiToolAppearance(item, Material.NETHERITE_SHOVEL);
         else if (isHoeableBlock(blockType)) updateMultiToolAppearance(item, Material.NETHERITE_HOE);

         int radius = getRadius("multitool");
         BlockFace face = getTargetBlockFace(player);
         int extraBlocksBroken = 0;
         for (Block b : getBlocksInPlane(block, radius, radius * 2 + 1, face, player, true)) {
            if (b.equals(block) || unbreakableBlocks.contains(b.getType())) continue;
            if (!bypass && !plugin.getWorldGuardHelper().canBuild(player, b.getLocation())) continue;
            boolean canBreak = useConfig ? breakable.contains(b.getType())
                  : isBlockMinableWithPickaxe(b.getType()) || Tag.LOGS.isTagged(b.getType()) || soilBlocks.contains(b.getType());
            if (canBreak) {
               b.breakNaturally(item);
               extraBlocksBroken++;
            }
         }
         if (extraBlocksBroken > 0) {
            damageTool(player, item, extraBlocksBroken);
         }
      }

      if (!bypass && plugin.getToolManager().isUsesMode(item)) {
         if (!plugin.getToolManager().consumeUse(item)) {
            player.getInventory().setItemInMainHand(null);
            plugin.getMessageManager().sendMessage(player, "tool.uses-depleted");
         }
      }
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      EquipmentSlot hand = event.getHand();
      if (hand == null) return;

      Player player = event.getPlayer();
      ItemStack item = (hand == EquipmentSlot.OFF_HAND)
            ? player.getInventory().getItemInOffHand()
            : player.getInventory().getItemInMainHand();

      if (!plugin.getToolManager().isCustomTool(item)) return;

      if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
         lastClickedFace.put(player.getUniqueId(), event.getBlockFace());
      }

      plugin.getToolManager().updateExpirationDisplay(item);
      boolean bypass = player.isOp() || player.hasPermission("solartool.bypass");

      if (!bypass && plugin.getBlacklistManager().isWorldBlacklisted(player.getWorld().getName())) {
         plugin.getMessageManager().sendMessage(player, "blacklist.world-blocked");
         event.setCancelled(true);
         return;
      }
      if (!bypass && event.getClickedBlock() != null
            && !plugin.getWorldGuardHelper().canBuild(player, event.getClickedBlock().getLocation())) {
         plugin.getMessageManager().sendMessage(player, "worldguard.no-build");
         event.setCancelled(true);
         return;
      }

      if (!bypass && plugin.getToolManager().hasExpired(item)) {
         if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(null);
         } else {
            player.getInventory().setItemInMainHand(null);
         }
         String expMsg = plugin.getToolManager().isUsesMode(item) ? "tool.uses-depleted" : "tool.expired";
         plugin.getMessageManager().sendMessage(player, expMsg);
         event.setCancelled(true);
         return;
      }

      String toolType = plugin.getToolManager().getToolType(item);

      if ("rocket".equals(toolType) && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
         handleRocketUse(event, player, item, hand);
         return;
      }

      if ("multitool".equals(toolType) && event.getClickedBlock() != null
            && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
         Material blockType = event.getClickedBlock().getType();
         if (isBlockMinableWithPickaxe(blockType)) updateMultiToolAppearance(item, Material.NETHERITE_PICKAXE);
         else if (Tag.LOGS.isTagged(blockType) || Tag.LEAVES.isTagged(blockType)) updateMultiToolAppearance(item, Material.NETHERITE_AXE);
         else if (soilBlocks.contains(blockType)) updateMultiToolAppearance(item, Material.NETHERITE_SHOVEL);
         else if (isTillableBlock(blockType)) updateMultiToolAppearance(item, Material.NETHERITE_HOE);
      }

      if ("waterbucket".equals(toolType) && event.getItem() != null && event.getItem().getType() == Material.WATER_BUCKET) {
         handleWaterBucketUse(event, player);
      } else if ("hoe".equals(toolType) && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
         handleHoeUse(event, player, event.getClickedBlock());
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onBlockFromTo(BlockFromToEvent event) {
      Location from = event.getBlock().getLocation();
      Location to = event.getToBlock().getLocation();
      if (!plugin.getWorldGuardHelper().isFlowAllowed(from, to)) {
         event.setCancelled(true);
      }
   }


   private void scheduleGroundItemExpiry(Item groundItem, ItemStack stack) {
      if (!plugin.getToolManager().isCustomTool(stack)) return;
      if (!stack.getItemMeta().getPersistentDataContainer()
               .has(new org.bukkit.NamespacedKey(plugin, "expiresat_epoch"),
                    org.bukkit.persistence.PersistentDataType.LONG)) return;

      long expiresAt = stack.getItemMeta().getPersistentDataContainer()
               .get(new org.bukkit.NamespacedKey(plugin, "expiresat_epoch"),
                    org.bukkit.persistence.PersistentDataType.LONG);
      long remaining = expiresAt - java.time.Instant.now().getEpochSecond();
      if (remaining <= 0) {
         groundItem.remove();
         return;
      }
      long delayTicks = remaining * 20L;
      SchedulerUtil.runTaskLaterAtEntity(plugin, groundItem, () -> {
         if (!groundItem.isDead()) groundItem.remove();
      }, delayTicks);
   }


   private void spawnCustomParticles(Location location, int count) {
      location.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, location, count,
            0.5, 0.5, 0.5, new Particle.DustTransition(Color.fromRGB(138, 43, 226), Color.WHITE, 1.0F));
      location.getWorld().spawnParticle(Particle.PORTAL, location, count * 2, 0.3, 0.3, 0.3, 0.1);
   }

   private void spawnElectricSparkParticles(Location location, int count) {
      location.getWorld().spawnParticle(Particle.CRIT, location, count, 0.3, 0.3, 0.3, 0.1);
      location.getWorld().spawnParticle(Particle.WITCH, location, count, 0.3, 0.3, 0.3, 0.1);
   }

   private void spawnVillagerHappyParticles(Location location, int count) {
      location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location, count, 0.5, 0.5, 0.5, 0.1);
   }


   private void handleDrillBreak(Player player, Block centerBlock) {
      if (unbreakableBlocks.contains(centerBlock.getType())) return;
      Set<Material> breakable = plugin.getToolConfigManager().getBreakableBlocks("drill");
      boolean useConfig = !breakable.isEmpty();
      if (useConfig && !breakable.contains(centerBlock.getType())) return;
      if (!useConfig && !isBlockMinableWithPickaxe(centerBlock.getType())) return;
      int radius = getRadius("drill");
      BlockFace face = getTargetBlockFace(player);
      int extraBlocksBroken = 0;
      boolean bypass = player.isOp() || player.hasPermission("solartool.bypass");
      for (Block block : getBlocksInPlane(centerBlock, radius, radius * 2 + 1, face, player, true)) {
         if (block.equals(centerBlock) || unbreakableBlocks.contains(block.getType())) continue;
         if (!bypass && !plugin.getWorldGuardHelper().canBuild(player, block.getLocation())) continue;
         boolean canBreak = useConfig ? breakable.contains(block.getType()) : isBlockMinableWithPickaxe(block.getType());
         if (canBreak) {
            block.breakNaturally(player.getInventory().getItemInMainHand());
            spawnCustomParticles(block.getLocation().add(0.5, 0.5, 0.5), 3);
            extraBlocksBroken++;
         }
      }
      if (extraBlocksBroken > 0) {
         damageTool(player, player.getInventory().getItemInMainHand(), extraBlocksBroken);
      }
   }

   private void handleTreeChopperBreak(Player player, Block block) {
      Set<Material> breakable = plugin.getToolConfigManager().getBreakableBlocks("treechopper");
      boolean useConfig = !breakable.isEmpty();
      boolean isLog = useConfig ? breakable.contains(block.getType()) : Tag.LOGS.isTagged(block.getType());
      if (!isLog) return;
      Set<Block> treeBlocks = new HashSet<>();
      findConnectedLogs(block, treeBlocks, new HashSet<>(), 0, breakable, useConfig);
      int extraBlocksBroken = 0;
      boolean bypass = player.isOp() || player.hasPermission("solartool.bypass");
      for (Block treeBlock : treeBlocks) {
         if (treeBlock.equals(block)) continue;
         if (!bypass && !plugin.getWorldGuardHelper().canBuild(player, treeBlock.getLocation())) continue;
         treeBlock.breakNaturally(player.getInventory().getItemInMainHand());
         spawnElectricSparkParticles(treeBlock.getLocation().add(0.5, 0.5, 0.5), 5);
         extraBlocksBroken++;
      }
      if (extraBlocksBroken > 0) {
         damageTool(player, player.getInventory().getItemInMainHand(), extraBlocksBroken);
      }
   }

   private void handleShovelBreak(Player player, Block centerBlock) {
      Set<Material> breakable = plugin.getToolConfigManager().getBreakableBlocks("shovel");
      boolean useConfig = !breakable.isEmpty();
      boolean isSoil = useConfig ? breakable.contains(centerBlock.getType()) : soilBlocks.contains(centerBlock.getType());
      if (!isSoil) return;
      int radius = getRadius("shovel");
      BlockFace face = getTargetBlockFace(player);
      int extraBlocksBroken = 0;
      boolean bypass = player.isOp() || player.hasPermission("solartool.bypass");
      for (Block block : getBlocksInPlane(centerBlock, radius, radius * 2 + 1, face, player, true)) {
         if (block.equals(centerBlock)) continue;
         if (!bypass && !plugin.getWorldGuardHelper().canBuild(player, block.getLocation())) continue;
         boolean canBreak = useConfig ? breakable.contains(block.getType()) : soilBlocks.contains(block.getType());
         if (canBreak) {
            block.breakNaturally(player.getInventory().getItemInMainHand());
            spawnCustomParticles(block.getLocation().add(0.5, 0.5, 0.5), 3);
            extraBlocksBroken++;
         }
      }
      if (extraBlocksBroken > 0) {
         damageTool(player, player.getInventory().getItemInMainHand(), extraBlocksBroken);
      }
   }

   private void handleHoeUse(PlayerInteractEvent event, Player player, Block centerBlock) {
      event.setCancelled(true);
      Set<Material> breakable = plugin.getToolConfigManager().getBreakableBlocks("hoe");
      boolean useConfig = !breakable.isEmpty();
      boolean isTillable = useConfig ? breakable.contains(centerBlock.getType()) : isTillableBlock(centerBlock.getType());
      if (!isTillable) return;
      int radius = getRadius("hoe");
      int blocksTilled = 0;
      boolean bypass = player.isOp() || player.hasPermission("solartool.bypass");
      for (Block block : getBlocksInPlane(centerBlock, radius, 1, BlockFace.UP, player, false)) {
         if (!bypass && !plugin.getWorldGuardHelper().canBuild(player, block.getLocation())) continue;
         boolean canTill = useConfig ? breakable.contains(block.getType()) : isTillableBlock(block.getType());
         if (canTill) {
            block.setType(Material.FARMLAND);
            block.getWorld().playSound(block.getLocation(), Sound.ITEM_HOE_TILL, 1.0F, 1.0F);
            spawnVillagerHappyParticles(block.getLocation().add(0.5, 1.0, 0.5), 5);
            blocksTilled++;
         }
      }
      if (blocksTilled > 0) {
         damageTool(player, player.getInventory().getItemInMainHand(), blocksTilled);
         boolean bypass2 = player.isOp() || player.hasPermission("solartool.bypass");
         ItemStack hoeItem = player.getInventory().getItemInMainHand();
         if (!bypass2 && plugin.getToolManager().isUsesMode(hoeItem)) {
            if (!plugin.getToolManager().consumeUse(hoeItem)) {
               player.getInventory().setItemInMainHand(null);
               plugin.getMessageManager().sendMessage(player, "tool.uses-depleted");
            }
         }
      }
   }

   private void handleWaterBucketUse(PlayerInteractEvent event, Player player) {
      event.setCancelled(true);
      double reach = (player.getGameMode() == org.bukkit.GameMode.CREATIVE) ? 6.0 : 5.0;
      RayTraceResult ray = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(), player.getEyeLocation().getDirection(), reach, FluidCollisionMode.NEVER);
      if (ray == null || ray.getHitBlock() == null) {
         return;
      }

      Block hitBlock = ray.getHitBlock();
      Block targetBlock;
      if (hitBlock.getBlockData() instanceof org.bukkit.block.data.Waterlogged w && !w.isWaterlogged()) {
         targetBlock = hitBlock;
      } else if (hitBlock.isReplaceable()) {
         targetBlock = hitBlock;
      } else {
         targetBlock = hitBlock.getRelative(ray.getHitBlockFace());
      }
      Location loc = targetBlock.getLocation();

      boolean bypass = player.isOp() || player.hasPermission("solartool.bypass");
      if (!bypass && !plugin.getWorldGuardHelper().canBuild(player, loc)) {
         plugin.getMessageManager().sendMessage(player, "worldguard.no-build");
         return;
      }

      if (player.getWorld().isUltraWarm() || player.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) {
         player.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5F, 2.6F);
         player.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0.5, 0.5, 0.5), 8, 0.25, 0.25, 0.25, 0.05);

         ItemStack bucketItem = player.getInventory().getItemInMainHand();
         if (!bypass && plugin.getToolManager().isUsesMode(bucketItem)) {
            if (!plugin.getToolManager().consumeUse(bucketItem)) {
               player.getInventory().setItemInMainHand(null);
               plugin.getMessageManager().sendMessage(player, "tool.uses-depleted");
            }
         }
         return;
      }

      if (targetBlock.getBlockData() instanceof org.bukkit.block.data.Waterlogged waterlogged) {
         if (!waterlogged.isWaterlogged()) {
            waterlogged.setWaterlogged(true);
            targetBlock.setBlockData(waterlogged);
         } else {
            return;
         }
      } else if (targetBlock.isReplaceable()) {
         targetBlock.setType(Material.WATER);
      } else {
         return;
      }

      player.getWorld().playSound(loc, Sound.ITEM_BUCKET_EMPTY, 1.0F, 1.0F);

      ItemStack bucketItem = player.getInventory().getItemInMainHand();
      if (!bypass && plugin.getToolManager().isUsesMode(bucketItem)) {
         if (!plugin.getToolManager().consumeUse(bucketItem)) {
            player.getInventory().setItemInMainHand(null);
            plugin.getMessageManager().sendMessage(player, "tool.uses-depleted");
         }
      }
   }

   private void handleRocketUse(PlayerInteractEvent event, Player player, ItemStack item, EquipmentSlot hand) {
      boolean bypass = player.isOp() || player.hasPermission("solartool.bypass");

      if (player.isGliding()) {
         event.setCancelled(true);

         org.bukkit.configuration.file.FileConfiguration cfg = plugin.getToolConfigManager().getConfig("rocket");
         long cooldownMs = cfg != null ? cfg.getLong("cooldown-ms", 250) : 250;
         if (cooldownMs > 0 && !bypass) {
            long last = rocketCooldowns.getOrDefault(player.getUniqueId(), 0L);
            long now = System.currentTimeMillis();
            if (now - last < cooldownMs) {
               return;
            }
            rocketCooldowns.put(player.getUniqueId(), now);
         }

         player.boostElytra(item);

         if (!bypass && plugin.getToolManager().isUsesMode(item)) {
            if (!plugin.getToolManager().consumeUse(item)) {
               if (hand == EquipmentSlot.OFF_HAND) {
                  player.getInventory().setItemInOffHand(null);
               } else {
                  player.getInventory().setItemInMainHand(null);
               }
               plugin.getMessageManager().sendMessage(player, "tool.uses-depleted");
            }
         }
      } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
         event.setCancelled(true);

         org.bukkit.configuration.file.FileConfiguration cfg = plugin.getToolConfigManager().getConfig("rocket");
         long cooldownMs = cfg != null ? cfg.getLong("cooldown-ms", 250) : 250;
         if (cooldownMs > 0 && !bypass) {
            long last = rocketCooldowns.getOrDefault(player.getUniqueId(), 0L);
            long now = System.currentTimeMillis();
            if (now - last < cooldownMs) {
               return;
            }
            rocketCooldowns.put(player.getUniqueId(), now);
         }

         Location spawnLoc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0.0, 0.5);
         Firework firework = spawnLoc.getWorld().spawn(spawnLoc, Firework.class);
         if (item.getItemMeta() instanceof FireworkMeta fm) {
            firework.setFireworkMeta(fm);
         } else {
            FireworkMeta fm = firework.getFireworkMeta();
            int power = cfg != null ? cfg.getInt("power", 2) : 2;
            fm.setPower(power);
            firework.setFireworkMeta(fm);
         }

         if (!bypass && plugin.getToolManager().isUsesMode(item)) {
            if (!plugin.getToolManager().consumeUse(item)) {
               if (hand == EquipmentSlot.OFF_HAND) {
                  player.getInventory().setItemInOffHand(null);
               } else {
                  player.getInventory().setItemInMainHand(null);
               }
               plugin.getMessageManager().sendMessage(player, "tool.uses-depleted");
            }
         }
      }
   }


   private void findConnectedLogs(Block block, Set<Block> logs, Set<Location> visited, int depth,
                                   Set<Material> breakable, boolean useConfig) {
      if (depth > 500 || logs.size() > 200 || visited.contains(block.getLocation())) return;
      visited.add(block.getLocation());
      boolean isLog = useConfig ? breakable.contains(block.getType()) : Tag.LOGS.isTagged(block.getType());
      if (!isLog) return;
      logs.add(block);
      for (BlockFace face : BlockFace.values())
         if (face.isCartesian())
            findConnectedLogs(block.getRelative(face), logs, visited, depth + 1, breakable, useConfig);
   }

   private boolean isBlockMinableWithPickaxe(Material m) {
      String n = m.name();
      return n.contains("STONE") || n.contains("ORE") || n.contains("DEEPSLATE")
            || n.contains("IRON") || n.contains("GOLD") || n.contains("DIAMOND");
   }

   private boolean isTillableBlock(Material m) {
      return m == Material.GRASS_BLOCK || m == Material.DIRT || m == Material.COARSE_DIRT;
   }

   private boolean isHoeableBlock(Material m) { return isTillableBlock(m); }

   private void updateMultiToolAppearance(ItemStack item, Material material) {
      if (item.getType() != material) item.setType(material);
   }

   private void damageTool(Player player, ItemStack item, int amount) {
      if (item == null || item.getType() == Material.AIR || amount <= 0) return;
      if (item.getType().getMaxDurability() <= 0) return;
      ItemMeta meta = item.getItemMeta();
      if (meta instanceof Damageable damageable) {
         int unbreakingLevel = item.getEnchantmentLevel(Enchantment.UNBREAKING);
         int finalDamage = 0;
         for (int i = 0; i < amount; i++) {
            if (unbreakingLevel > 0) {
               if (java.util.concurrent.ThreadLocalRandom.current().nextInt(unbreakingLevel + 1) == 0) {
                  finalDamage++;
               }
            } else {
               finalDamage++;
            }
         }
         if (finalDamage > 0) {
            int newDamage = damageable.getDamage() + finalDamage;
            if (newDamage >= item.getType().getMaxDurability()) {
               player.getInventory().setItemInMainHand(null);
               player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
            } else {
               damageable.setDamage(newDamage);
               item.setItemMeta(meta);
            }
         }
      }
   }


   private void checkPlayerInventory(Player player) {
      boolean bypass = player.isOp() || player.hasPermission("solartool.bypass");
      for (int i = 0; i < player.getInventory().getSize(); i++) {
         ItemStack item = player.getInventory().getItem(i);
         if (item == null || !plugin.getToolManager().isCustomTool(item)) continue;

         if (!bypass && plugin.getToolManager().hasExpired(item)) {
            player.getInventory().setItem(i, null);
            plugin.getMessageManager().sendMessage(player, "tool.expired");
            continue;
         }

         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            org.bukkit.NamespacedKey initKey = new org.bukkit.NamespacedKey(plugin, "initialized");
            org.bukkit.NamespacedKey expiresKey = new org.bukkit.NamespacedKey(plugin, "expiresat_epoch");
            boolean needsInit = !meta.getPersistentDataContainer().has(initKey, org.bukkit.persistence.PersistentDataType.BYTE);
            boolean needsDerive = !bypass && !meta.getPersistentDataContainer().has(expiresKey, org.bukkit.persistence.PersistentDataType.LONG);
            
            if (needsInit) {
               plugin.getToolManager().initializeNewTool(item);
            }
            if (needsDerive) {
               plugin.getToolManager().startDerivation(item, player);
            }
         }
      }
   }
}
