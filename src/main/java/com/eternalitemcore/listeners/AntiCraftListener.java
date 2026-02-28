package com.eternalitemcore.listeners;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class AntiCraftListener implements Listener {

    private final EternalItemCore plugin;

    public AntiCraftListener(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    private boolean isCore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey coreIdKey = new NamespacedKey(plugin, "eternal_core_id");
        return meta.getPersistentDataContainer().has(coreIdKey, PersistentDataType.STRING);
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (isCore(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onSmith(PrepareSmithingEvent event) {
        if (isCore(event.getInventory().getInputEquipment()) || 
            isCore(event.getInventory().getInputMineral()) || 
            isCore(event.getInventory().getInputTemplate())) {
            event.setResult(null);
        }
    }
}
