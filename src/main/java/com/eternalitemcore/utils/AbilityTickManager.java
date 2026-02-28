package com.eternalitemcore.utils;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class AbilityTickManager implements Runnable {

    private final EternalItemCore plugin;

    public AbilityTickManager(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Check main hand
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            checkAndApplyPassive(player, mainHand);
            
            // Check off hand
            ItemStack offHand = player.getInventory().getItemInOffHand();
            checkAndApplyPassive(player, offHand);
            
            // Check armor
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                checkAndApplyPassive(player, armor);
            }
        }
    }

    private void checkAndApplyPassive(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        List<String> enabledStats = plugin.getItemDataManager().getEnabledStats(item);
        if (enabledStats.isEmpty()) return;

        for (String statId : enabledStats) {
            int currentLevel = plugin.getItemDataManager().getStatLevel(item, statId);
            
            // Check all levels up to the current level to see if passive abilities are unlocked
            for (int i = 1; i <= currentLevel; i++) {
                ConfigurationSection levelSec = plugin.getConfig().getConfigurationSection("stats." + statId + ".levels." + i);
                if (levelSec != null && levelSec.contains("ability-unlock")) {
                    String abilityCoreId = levelSec.getString("ability-unlock");
                    ConfigurationSection abilitySec = plugin.getConfig().getConfigurationSection("ability-cores." + abilityCoreId);
                    
                    if (abilitySec != null && "PASSIVE".equalsIgnoreCase(abilitySec.getString("type"))) {
                        String effectName = abilitySec.getString("effect");
                        int amplifier = abilitySec.getInt("amplifier", 0);
                        
                        if (effectName != null) {
                            PotionEffectType effectType = PotionEffectType.getByName(effectName);
                            if (effectType != null) {
                                // Apply potion effect for slightly more than 1 second to prevent flashing
                                player.addPotionEffect(new PotionEffect(effectType, 40, amplifier, true, false, true));
                            }
                        }
                    }
                }
            }
        }
    }
}
