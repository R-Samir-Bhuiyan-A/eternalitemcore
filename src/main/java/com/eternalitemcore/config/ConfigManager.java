package com.eternalitemcore.config;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final EternalItemCore plugin;
    private boolean updateLore;

    // Stat Core Types maps
    private final Map<String, String> statCores = new HashMap<>(); // Core ID -> Stat ID
    // Allowed Stats per Item Type (String from enum name)
    private final Map<String, List<String>> allowedStats = new HashMap<>(); 

    public ConfigManager(EternalItemCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.updateLore = config.getBoolean("settings.lore-update", true);

        ConfigurationSection statCoresSection = config.getConfigurationSection("stat-cores");
        if (statCoresSection != null) {
            for (String key : statCoresSection.getKeys(false)) {
                statCores.put(key, statCoresSection.getString(key + ".stat-type"));
            }
        }

        ConfigurationSection allowedStatsSection = config.getConfigurationSection("allowed-stats");
        if (allowedStatsSection != null) {
            for (String key : allowedStatsSection.getKeys(false)) {
                allowedStats.put(key, allowedStatsSection.getStringList(key));
            }
        }
    }

    public boolean isLoreUpdateEnabled() {
        return updateLore;
    }

    public Map<String, String> getStatCores() {
        return statCores;
    }

    public List<String> getAllowedStats(String itemType) {
        return allowedStats.getOrDefault(itemType, List.of());
    }

    public List<String> getAllowedItemsForStat(String statId) {
        List<String> items = new java.util.ArrayList<>();
        for (Map.Entry<String, List<String>> entry : allowedStats.entrySet()) {
            if (entry.getValue().contains(statId)) {
                items.add(entry.getKey());
            }
        }
        return items;
    }
}
