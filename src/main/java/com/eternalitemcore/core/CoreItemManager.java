package com.eternalitemcore.core;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class CoreItemManager {

    private final EternalItemCore plugin;

    public CoreItemManager(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    public ItemStack createStatCore(String coreId) {
        ConfigurationSection coreSec = plugin.getConfig().getConfigurationSection("stat-cores." + coreId);
        if (coreSec == null) return null;

        Material mat = Material.matchMaterial(coreSec.getString("material", "NETHER_STAR"));
        if (mat == null) mat = Material.NETHER_STAR;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = coreSec.getString("name");
            if (name != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            List<String> lore = coreSec.getStringList("lore");
            
            String statId = coreSec.getString("stat-type");
            if (statId != null) {
                List<String> allowedItems = plugin.getConfigManager().getAllowedItemsForStat(statId);
                if (!allowedItems.isEmpty()) {
                    lore.add("&8▬▬▬▬▬▬▬▬▬▬▬");
                    lore.add("&7Can be applied to:");
                    lore.add("&f" + String.join(", ", allowedItems));
                }
            }

            if (!lore.isEmpty()) {
                meta.setLore(lore.stream()
                        .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                        .collect(Collectors.toList()));
            }

            NamespacedKey key = new NamespacedKey(plugin, "eternal_core_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, coreId);
            item.setItemMeta(meta);
        }
        return item;
    }
}
