package com.otitem.utils;

import com.otitem.OTItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoreManager {

    private final OTItem plugin;

    public LoreManager(OTItem plugin) {
        this.plugin = plugin;
    }

    public void updateLore(ItemStack item) {
        if (!plugin.getConfigManager().isLoreUpdateEnabled()) return;
        if (item == null || !item.hasItemMeta()) return;

        List<String> enabledStats = plugin.getItemDataManager().getEnabledStats(item);
        if (enabledStats.isEmpty()) return;

        ItemMeta meta = item.getItemMeta();
        List<String> newLore = new ArrayList<>();
        
        // Base structure
        newLore.add(color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

        for (String statId : enabledStats) {
            ConfigurationSection statSec = plugin.getConfig().getConfigurationSection("stats." + statId);
            if (statSec == null) continue;

            int value = plugin.getItemDataManager().getStatValue(item, statId);
            int level = plugin.getItemDataManager().getStatLevel(item, statId);
            
            String display = statSec.getString("display", "&7" + statId);
            newLore.add(color(display + "&r &8[&7" + value + "&8] &e(Lv " + level + ")"));

            // Show abilities/kill-effects from ALL unlocked levels (not just current level)
            java.util.Set<String> shownAbilities = new java.util.HashSet<>();
            for (int i = 1; i <= level; i++) {
                ConfigurationSection lvlSec = statSec.getConfigurationSection("levels." + i);
                if (lvlSec == null) continue;

                // Show storyline only for the current level
                if (i == level) {
                    List<String> storyline = lvlSec.getStringList("storyline");
                    if (!storyline.isEmpty()) {
                        newLore.add(""); // Spacer
                        for (String line : storyline) {
                            newLore.add(color(line));
                        }
                    }
                }

                // Show every ability/kill-effect unlocked from any level up to current
                String abilityCoreId = lvlSec.getString("ability-unlock");
                if (abilityCoreId != null && shownAbilities.add(abilityCoreId)) {
                    ConfigurationSection abilitySec = plugin.getConfig().getConfigurationSection("ability-cores." + abilityCoreId);
                    if (abilitySec != null) {
                        String abilityDisplay = abilitySec.getString("display", "&eUnlocked: " + abilityCoreId);
                        newLore.add("");
                        newLore.add(color(abilityDisplay));
                    }
                }
            }
            newLore.add("&8 "); // Empty spacer between stats
        }
        
        newLore.add(color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        meta.setLore(newLore);
        item.setItemMeta(meta);
    }

    /**
     * Replaces standard & codes.
     */
    public String color(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
