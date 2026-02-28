package com.eternalitemcore.utils;

import com.eternalitemcore.EternalItemCore;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoreManager {

    private final EternalItemCore plugin;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public LoreManager(EternalItemCore plugin) {
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

            // Check for Storyline & Abilities for the CURRENT level
            ConfigurationSection levelSec = statSec.getConfigurationSection("levels." + level);
            if (levelSec != null) {
                // Add Storyline
                List<String> storyline = levelSec.getStringList("storyline");
                if (!storyline.isEmpty()) {
                    newLore.add(""); // Spacer
                    for (String line : storyline) {
                        newLore.add(color(line));
                    }
                }

                // Add Ability Display
                String abilityCoreId = levelSec.getString("ability-unlock");
                if (abilityCoreId != null) {
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
     * Replaces standard & codes and &#RRGGBB hex codes.
     */
    public String color(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
