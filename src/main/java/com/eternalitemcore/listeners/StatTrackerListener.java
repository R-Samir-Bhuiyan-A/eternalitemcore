package com.eternalitemcore.listeners;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.ChatColor;

public class StatTrackerListener implements Listener {

    private final EternalItemCore plugin;

    public StatTrackerListener(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            
            if (weapon.getType().isAir()) return;

            java.util.List<String> activeStats = plugin.getItemDataManager().getEnabledStats(weapon);
            if (activeStats.isEmpty()) return;

            for (String statId : activeStats) {
                ConfigurationSection statSec = plugin.getConfig().getConfigurationSection("stats." + statId);
                if (statSec == null) continue;

                String requiredEvent = statSec.getString("event", "");

                if (event.getEntity() instanceof Player) {
                    if (requiredEvent.equalsIgnoreCase("PLAYER_KILL")) {
                        plugin.getItemDataManager().incrementStat(killer, weapon, statId, 1);
                        triggerKillEffects(killer, weapon, statId, event.getEntity().getLocation());
                    }
                } else {
                    if (plugin.getConfig().getBoolean("settings.anti-spawner-farm-kills", true)) {
                        if (event.getEntity().hasMetadata("from_spawner")) {
                            continue; // Skip spawner farm kills
                        }
                    }

                    if (requiredEvent.equalsIgnoreCase("MOB_KILL")) {
                        plugin.getItemDataManager().incrementStat(killer, weapon, statId, 1);
                        triggerKillEffects(killer, weapon, statId, event.getEntity().getLocation());
                    }
                }
            }
        }
    }

    private void triggerKillEffects(Player player, ItemStack weapon, String statId, org.bukkit.Location loc) {
        int level = plugin.getItemDataManager().getStatLevel(weapon, statId);
        ConfigurationSection levelSec = plugin.getConfig().getConfigurationSection("stats." + statId + ".levels." + level);
        if (levelSec != null && levelSec.contains("ability-unlock")) {
            String abilityCoreId = levelSec.getString("ability-unlock");
            ConfigurationSection abilityConfig = plugin.getConfig().getConfigurationSection("ability-cores." + abilityCoreId);
            if (abilityConfig != null && abilityConfig.getString("type", "").equalsIgnoreCase("KILL_EFFECT")) {
                plugin.getAbilityManager().triggerAbility(player, abilityCoreId, loc);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        if (tool.getType().isAir()) return;

        java.util.List<String> activeStats = plugin.getItemDataManager().getEnabledStats(tool);
        if (activeStats.isEmpty()) return;

        for (String statId : activeStats) {
            String requiredEvent = plugin.getConfig().getString("stats." + statId + ".event", "");
            
            if (requiredEvent.equalsIgnoreCase("BLOCK_BREAK")) {
                plugin.getItemDataManager().incrementStat(player, tool, statId, 1);
            } else if (requiredEvent.equalsIgnoreCase("ORE_MINE") && event.getBlock().getType().name().contains("_ORE")) {
                plugin.getItemDataManager().incrementStat(player, tool, statId, 1);
            } else if (requiredEvent.equalsIgnoreCase("DIAMOND_MINE") && event.getBlock().getType().name().contains("DIAMOND_ORE")) {
                plugin.getItemDataManager().incrementStat(player, tool, statId, 1);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
                if (armorPiece != null && !armorPiece.getType().isAir()) {
                    java.util.List<String> activeStats = plugin.getItemDataManager().getEnabledStats(armorPiece);
                    for (String statId : activeStats) {
                        String requiredEvent = plugin.getConfig().getString("stats." + statId + ".event", "");
                        if (requiredEvent.equalsIgnoreCase("DAMAGE_TAKEN")) {
                            int dr = (int) Math.ceil(event.getFinalDamage());
                            if (dr > 0) {
                                plugin.getItemDataManager().incrementStat(player, armorPiece, statId, dr);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.isBlocking()) {
                ItemStack shield = player.getInventory().getItemInOffHand();
                if (shield.getType().name().equals("SHIELD")) {
                    java.util.List<String> activeStats = plugin.getItemDataManager().getEnabledStats(shield);
                    for (String statId : activeStats) {
                        String requiredEvent = plugin.getConfig().getString("stats." + statId + ".event", "");
                        if (requiredEvent.equalsIgnoreCase("DAMAGE_BLOCKED")) {
                            int dr = (int) Math.ceil(event.getDamage());
                            if (dr > 0) {
                                plugin.getItemDataManager().incrementStat(player, shield, statId, dr);
                            }
                        }
                    }
                }
            }
        }
    }
}
