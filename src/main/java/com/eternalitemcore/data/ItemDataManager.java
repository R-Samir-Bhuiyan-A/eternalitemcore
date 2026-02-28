package com.eternalitemcore.data;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemDataManager {

    private final EternalItemCore plugin;
    private final NamespacedKey enabledStatsKey;

    public ItemDataManager(EternalItemCore plugin) {
        this.plugin = plugin;
        this.enabledStatsKey = new NamespacedKey(plugin, "enabled_stats");
    }

    public List<String> getEnabledStats(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return new ArrayList<>();
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String statsString = pdc.get(enabledStatsKey, PersistentDataType.STRING);
        if (statsString == null || statsString.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(statsString.split(",")));
    }

    public boolean hasStatEnabled(ItemStack item, String statId) {
        return getEnabledStats(item).contains(statId);
    }

    public void addEnabledStat(ItemStack item, String statId) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        List<String> stats = getEnabledStats(item);
        if (!stats.contains(statId)) {
            stats.add(statId);
            meta.getPersistentDataContainer().set(enabledStatsKey, PersistentDataType.STRING, String.join(",", stats));
            
            // Initialize stat value
            NamespacedKey statValKey = new NamespacedKey(plugin, "stat_" + statId);
            meta.getPersistentDataContainer().set(statValKey, PersistentDataType.INTEGER, 0);
            
            // Initialize level
            NamespacedKey statLevelKey = new NamespacedKey(plugin, "level_" + statId);
            meta.getPersistentDataContainer().set(statLevelKey, PersistentDataType.INTEGER, 1);
            
            item.setItemMeta(meta);
        }
    }

    public int getStatValue(ItemStack item, String statId) {
        if (item == null || !item.hasItemMeta()) return 0;
        NamespacedKey statValKey = new NamespacedKey(plugin, "stat_" + statId);
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(statValKey, PersistentDataType.INTEGER, 0);
    }

    public int getRequiredXp(String statId, int currentLevel) {
        String formula = plugin.getConfig().getString("stats." + statId + ".leveling.formula", "10 * level");
        try {
            if (formula.contains("(level ^ 2)")) {
                int mult = Integer.parseInt(formula.split("\\*")[0].trim());
                return mult * (currentLevel * currentLevel);
            } else if (formula.contains("level")) {
                int mult = Integer.parseInt(formula.split("\\*")[0].trim());
                return mult * currentLevel;
            }
        } catch (Exception e) {
            // fallback
        }
        return currentLevel * 10;
    }

    public void incrementStat(org.bukkit.entity.Player player, ItemStack item, String statId, int amount) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        int current = getStatValue(item, statId);
        int currentLevel = getStatLevel(item, statId);
        int maxLevel = plugin.getConfig().getInt("stats." + statId + ".max-level", 3);
        
        int newValue = current + amount;
        NamespacedKey statValKey = new NamespacedKey(plugin, "stat_" + statId);
        meta.getPersistentDataContainer().set(statValKey, PersistentDataType.INTEGER, newValue);
        item.setItemMeta(meta);

        if (currentLevel < maxLevel) {
            int required = getRequiredXp(statId, currentLevel);
            if (newValue >= required) {
                setStatLevel(item, statId, currentLevel + 1);
                
                NamespacedKey hideBcastKey = new NamespacedKey(plugin, "hide_broadcasts");
                byte hideBcast = item.getItemMeta().getPersistentDataContainer().getOrDefault(hideBcastKey, PersistentDataType.BYTE, (byte)0);
                
                if (hideBcast == 0) {
                    String bcast = plugin.getConfig().getString("stats." + statId + ".levels." + (currentLevel + 1) + ".broadcast-message");
                    if (bcast != null && !bcast.isEmpty() && player != null) {
                        String msg = bcast.replace("%player%", player.getName());
                        plugin.getServer().broadcastMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));
                    }
                } else if (player != null) {
                    player.sendMessage(org.bukkit.ChatColor.GREEN + "Your item leveled up silently to Level " + (currentLevel + 1) + "!");
                }
            }
        }
        
        plugin.getLoreManager().updateLore(item);
    }
    
    public int getStatLevel(ItemStack item, String statId) {
        if (item == null || !item.hasItemMeta()) return 1;
        NamespacedKey statLevelKey = new NamespacedKey(plugin, "level_" + statId);
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(statLevelKey, PersistentDataType.INTEGER, 1);
    }
    
    public void setStatLevel(ItemStack item, String statId, int level) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey statLevelKey = new NamespacedKey(plugin, "level_" + statId);
        meta.getPersistentDataContainer().set(statLevelKey, PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
    }
}
