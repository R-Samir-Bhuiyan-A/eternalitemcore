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
                // Player Kill (Anti-exploit check could be added here, e.g., cooldowns per player UUID)
                if (plugin.getItemDataManager().hasStatEnabled(weapon, "PLAYER_KILLS")) {
                    plugin.getItemDataManager().incrementStat(weapon, "PLAYER_KILLS", 1);
                    checkLevelUp(killer, weapon, "PLAYER_KILLS");
                }
            } else {
                // Mob Kill Anti-Spawner Exploit
                if (plugin.getConfig().getBoolean("settings.anti-spawner-farm-kills", true)) {
                    if (event.getEntity().hasMetadata("from_spawner")) {
                        return; // Ignore spawner mobs
                    }
                }

                if (plugin.getItemDataManager().hasStatEnabled(weapon, "MOB_KILLS")) {
                    plugin.getItemDataManager().incrementStat(weapon, "MOB_KILLS", 1);
                    checkLevelUp(killer, weapon, "MOB_KILLS");
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        if (tool.getType().isAir()) return;

        if (plugin.getItemDataManager().hasStatEnabled(tool, "BLOCKS_MINED")) {
            plugin.getItemDataManager().incrementStat(tool, "BLOCKS_MINED", 1);
            checkLevelUp(player, tool, "BLOCKS_MINED");
        }

        if (event.getBlock().getType().name().contains("DIAMOND_ORE")) {
            if (plugin.getItemDataManager().hasStatEnabled(tool, "DIAMONDS_MINED")) {
                plugin.getItemDataManager().incrementStat(tool, "DIAMONDS_MINED", 1);
                checkLevelUp(player, tool, "DIAMONDS_MINED");
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
                            plugin.getItemDataManager().incrementStat(armorPiece, "DAMAGE_TAKEN", dr);
                            checkLevelUp(player, armorPiece, "DAMAGE_TAKEN");
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
                            plugin.getItemDataManager().incrementStat(shield, "DAMAGE_BLOCKED", dr);
                            checkLevelUp(player, shield, "DAMAGE_BLOCKED");
                        }
                    }
                }
            }
        }
    }

    private void checkLevelUp(Player player, ItemStack item, String statId) {
        int currentValue = plugin.getItemDataManager().getStatValue(item, statId);
        int currentLevel = plugin.getItemDataManager().getStatLevel(item, statId);

        ConfigurationSection statConfig = plugin.getConfig().getConfigurationSection("stats." + statId);
        if (statConfig == null) return;

        int maxLevel = statConfig.getInt("max-level", 10);
        if (currentLevel >= maxLevel) {
            // Check for abilities triggers on current level
            applyAbilitiesForLevel(player, currentLevel, statConfig);
            return;
        }

        // Simple Leveling logic based on config values
        int requiredForNextLevel = 0;
        if (statConfig.contains("leveling.formula")) {
            // Basic simplistic parse or fallback
            // Realistically we'd use EvalEx or JS engine, but we will hardcode a basic formula representation for MVP
            requiredForNextLevel = 100 * (int) Math.pow(currentLevel, 1.5);
        } else {
            // If explicit levels
            ConfigurationSection nextLevelSec = statConfig.getConfigurationSection("levels." + (currentLevel + 1));
            requiredForNextLevel = 100 * currentLevel; // Fallback
        }

        if (currentValue >= requiredForNextLevel) {
            plugin.getItemDataManager().setStatLevel(item, statId, currentLevel + 1);
            
            // Send Title and Sound
            String display = statConfig.getString("display", statId);
            String title = ChatColor.translateAlternateColorCodes('&', "&a&lLEVEL UP!");
            String subtitle = ChatColor.translateAlternateColorCodes('&', display + " &fhas reached level &e" + (currentLevel + 1));
            player.sendTitle(title, subtitle, 10, 70, 20);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.sendMessage("§8[§6EternalItemCore§8] §aYour item leveled up in " + display + " &ato level " + (currentLevel + 1) + "!");
        }

        // Apply abilities for current level (like potions or kill effects)
        applyAbilitiesForLevel(player, currentLevel + (currentValue >= requiredForNextLevel ? 1 : 0), statConfig);
        plugin.getLoreManager().updateLore(item);
    }

    private void applyAbilitiesForLevel(Player player, int level, ConfigurationSection statConfig) {
        ConfigurationSection levelConfig = statConfig.getConfigurationSection("levels." + level);
        if (levelConfig != null && levelConfig.contains("ability-unlock")) {
            String abilityCoreId = levelConfig.getString("ability-unlock");
            plugin.getAbilityManager().triggerAbility(player, abilityCoreId, player.getLocation());
            
            ConfigurationSection abilityConfig = plugin.getConfig().getConfigurationSection("ability-cores." + abilityCoreId);
            if (abilityConfig != null) {
                String unlockName = abilityConfig.getString("display", abilityCoreId);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[§6EternalItemCore§8] &eAbility Unlocked: " + unlockName));
            }
        }
    }
}
