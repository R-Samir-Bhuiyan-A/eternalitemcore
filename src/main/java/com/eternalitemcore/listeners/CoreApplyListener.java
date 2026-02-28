package com.eternalitemcore.listeners;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class CoreApplyListener implements Listener {

    private final EternalItemCore plugin;

    public CoreApplyListener(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (mainHand.getType().isAir() || offHand.getType().isAir()) {
            return;
        }

        ItemMeta mainMeta = mainHand.getItemMeta();
        if (mainMeta == null) return;

        NamespacedKey coreIdKey = new NamespacedKey(plugin, "eternal_core_id");
        if (!mainMeta.getPersistentDataContainer().has(coreIdKey, PersistentDataType.STRING)) {
            return;
        }

        String coreId = mainMeta.getPersistentDataContainer().get(coreIdKey, PersistentDataType.STRING);
        String statId = plugin.getConfigManager().getStatCores().get(coreId);

        if (statId == null) {
            return; // Not a valid stat core
        }

        // Check if offhand item can receive this stat
        String itemType = getGenericItemType(offHand);
        List<String> allowedStats = plugin.getConfigManager().getAllowedStats(itemType);

        if (!allowedStats.contains(statId)) {
            player.sendMessage(ChatColor.RED + "This stat cannot be applied to this item type (" + itemType + ").");
            return;
        }

        if (plugin.getItemDataManager().hasStatEnabled(offHand, statId)) {
            player.sendMessage(ChatColor.RED + "This item already has this stat tracked!");
            return;
        }

        List<String> enabledStats = plugin.getItemDataManager().getEnabledStats(offHand);
        int maxStats = plugin.getConfig().getInt("settings.max-stats-per-item", 3);
        if (enabledStats.size() >= maxStats) {
            player.sendMessage(ChatColor.RED + "This item has reached the maximum number of cores (" + maxStats + ")!");
            return;
        }

        List<String> conflicts = plugin.getConfig().getStringList("stats." + statId + ".conflicts-with");
        for (String enabled : enabledStats) {
            if (conflicts.contains(enabled)) {
                player.sendMessage(ChatColor.RED + "This core conflicts with the " + enabled + " core already on this item!");
                return;
            }
        }

        // Apply stat
        plugin.getItemDataManager().addEnabledStat(offHand, statId);
        
        mainHand.setAmount(mainHand.getAmount() - 1);
        player.sendMessage(ChatColor.GREEN + "Successfully applied " + statId + " tracking to your item!");
        
        giveLoreBook(player, statId);
        
        plugin.getLoreManager().updateLore(offHand);

        player.getInventory().setItemInMainHand(mainHand);
        player.getInventory().setItemInOffHand(offHand);
        player.updateInventory();

        // Cancel to prevent placing blocks if the core is a block
        event.setCancelled(true);
    }

    private String getGenericItemType(ItemStack item) {
        String name = item.getType().name();
        if (name.endsWith("_SWORD")) return "SWORD";
        if (name.endsWith("_PICKAXE")) return "PICKAXE";
        if (name.endsWith("_AXE")) return "AXE";
        if (name.endsWith("_SPADE") || name.endsWith("_SHOVEL")) return "SHOVEL";
        if (name.endsWith("_HELMET")) return "HELMET";
        if (name.endsWith("_CHESTPLATE")) return "CHESTPLATE";
        if (name.endsWith("_LEGGINGS")) return "LEGGINGS";
        if (name.endsWith("_BOOTS")) return "BOOTS";
        if (name.equals("SHIELD")) return "SHIELD";
        return name;
    }

    private void giveLoreBook(Player player, String statId) {
        org.bukkit.configuration.ConfigurationSection statConfig = plugin.getConfig().getConfigurationSection("stats." + statId);
        if (statConfig == null) return;
        
        String display = statConfig.getString("display", statId);
        org.bukkit.inventory.ItemStack book = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        
        if (meta != null) {
            meta.setTitle(ChatColor.translateAlternateColorCodes('&', "&6Tome of " + display));
            meta.setAuthor("Eternal Item Core");
            
            int maxLevel = statConfig.getInt("max-level", 5);
            for (int i = 1; i <= maxLevel; i++) {
                org.bukkit.configuration.ConfigurationSection lvlConfig = statConfig.getConfigurationSection("levels." + i);
                if (lvlConfig != null) {
                    StringBuilder page = new StringBuilder();
                    page.append(ChatColor.translateAlternateColorCodes('&', "&0&lLevel " + i + "\n\n"));
                    
                    List<String> story = lvlConfig.getStringList("storyline");
                    for (String line : story) {
                        page.append(ChatColor.translateAlternateColorCodes('&', line)).append("\n");
                    }
                    
                    if (lvlConfig.contains("ability-unlock")) {
                        String abilityId = lvlConfig.getString("ability-unlock");
                        String abilityName = plugin.getConfig().getString("ability-cores." + abilityId + ".display", abilityId);
                        page.append("\n\n");
                        page.append(ChatColor.translateAlternateColorCodes('&', "&8&lUnlocks:\n"));
                        page.append(ChatColor.translateAlternateColorCodes('&', abilityName));
                    }
                    
                    if (i < maxLevel) {
                        int reqXp = plugin.getItemDataManager().getRequiredXp(statId, i);
                        String eventStr = statConfig.getString("event", "Trigger").replace("_", " ");
                        page.append("\n\n");
                        page.append(ChatColor.translateAlternateColorCodes('&', "&8&lTo Reach Level " + (i + 1) + ":\n"));
                        page.append(ChatColor.translateAlternateColorCodes('&', "&7" + reqXp + " " + eventStr));
                    }
                    
                    meta.addPage(page.toString());
                }
            }
            
            book.setItemMeta(meta);
            player.getInventory().addItem(book);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&oA mysterious tome appears in your inventory..."));
        }
    }
}
