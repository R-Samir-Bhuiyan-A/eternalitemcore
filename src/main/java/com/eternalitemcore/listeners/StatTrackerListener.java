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

            if (event.getEntity() instanceof Player) {
                if (plugin.getItemDataManager().hasStatEnabled(weapon, "PLAYER_KILLS")) {
                    plugin.getItemDataManager().incrementStat(killer, weapon, "PLAYER_KILLS", 1);
                    triggerKillEffects(killer, weapon, "PLAYER_KILLS");
                }
            } else {
                if (plugin.getConfig().getBoolean("settings.anti-spawner-farm-kills", true)) {
                    if (event.getEntity().hasMetadata("from_spawner")) {
                        return;
                    }
                }

                if (plugin.getItemDataManager().hasStatEnabled(weapon, "MOB_KILLS")) {
                    plugin.getItemDataManager().incrementStat(killer, weapon, "MOB_KILLS", 1);
                    triggerKillEffects(killer, weapon, "MOB_KILLS");
                }
            }
        }
    }

    private void triggerKillEffects(Player player, ItemStack weapon, String statId) {
        int level = plugin.getItemDataManager().getStatLevel(weapon, statId);
        ConfigurationSection levelSec = plugin.getConfig().getConfigurationSection("stats." + statId + ".levels." + level);
        if (levelSec != null && levelSec.contains("ability-unlock")) {
            String abilityCoreId = levelSec.getString("ability-unlock");
            ConfigurationSection abilityConfig = plugin.getConfig().getConfigurationSection("ability-cores." + abilityCoreId);
            if (abilityConfig != null && abilityConfig.getString("type", "").equalsIgnoreCase("KILL_EFFECT")) {
                plugin.getAbilityManager().triggerAbility(player, abilityCoreId, player.getLocation());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        if (tool.getType().isAir()) return;

        if (plugin.getItemDataManager().hasStatEnabled(tool, "BLOCKS_MINED")) {
            plugin.getItemDataManager().incrementStat(player, tool, "BLOCKS_MINED", 1);
        }

        if (event.getBlock().getType().name().contains("DIAMOND_ORE")) {
            if (plugin.getItemDataManager().hasStatEnabled(tool, "DIAMONDS_MINED")) {
                plugin.getItemDataManager().incrementStat(player, tool, "DIAMONDS_MINED", 1);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
                if (armorPiece != null && !armorPiece.getType().isAir()) {
                    if (plugin.getItemDataManager().hasStatEnabled(armorPiece, "DAMAGE_TAKEN")) {
                        int dr = (int) Math.ceil(event.getFinalDamage());
                        if (dr > 0) {
                            plugin.getItemDataManager().incrementStat(player, armorPiece, "DAMAGE_TAKEN", dr);
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
                    if (plugin.getItemDataManager().hasStatEnabled(shield, "DAMAGE_BLOCKED")) {
                        int dr = (int) Math.ceil(event.getDamage());
                        if (dr > 0) {
                            plugin.getItemDataManager().incrementStat(player, shield, "DAMAGE_BLOCKED", dr);
                        }
                    }
                }
            }
        }
    }
}
