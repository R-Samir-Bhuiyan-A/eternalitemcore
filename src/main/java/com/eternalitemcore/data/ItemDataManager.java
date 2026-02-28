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
        if (item == null || !item.hasItemMeta()) return;
        List<String> stats = getEnabledStats(item);
        if (!stats.contains(statId)) {
            stats.add(statId);
            ItemMeta meta = item.getItemMeta();
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

    public void incrementStat(ItemStack item, String statId, int amount) {
        if (item == null || !item.hasItemMeta()) return;
        int current = getStatValue(item, statId);
        ItemMeta meta = item.getItemMeta();
        NamespacedKey statValKey = new NamespacedKey(plugin, "stat_" + statId);
        meta.getPersistentDataContainer().set(statValKey, PersistentDataType.INTEGER, current + amount);
        item.setItemMeta(meta);
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
