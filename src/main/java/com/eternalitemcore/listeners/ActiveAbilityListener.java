package com.eternalitemcore.listeners;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ActiveAbilityListener implements Listener {

    private final EternalItemCore plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public ActiveAbilityListener(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType().isAir()) return;

        List<String> enabledStats = plugin.getItemDataManager().getEnabledStats(item);
        if (enabledStats.isEmpty()) return;

        boolean triggeredAbility = false;

        for (String statId : enabledStats) {
            int currentLevel = plugin.getItemDataManager().getStatLevel(item, statId);
            
            // Check all levels up to the current level to see if passive abilities are unlocked
            for (int i = 1; i <= currentLevel; i++) {
                ConfigurationSection levelSec = plugin.getConfig().getConfigurationSection("stats." + statId + ".levels." + i);
                if (levelSec != null && levelSec.contains("ability-unlock")) {
                    String abilityCoreId = levelSec.getString("ability-unlock");
                    ConfigurationSection abilitySec = plugin.getConfig().getConfigurationSection("ability-cores." + abilityCoreId);
                    
                    if (abilitySec != null && "ACTIVE".equalsIgnoreCase(abilitySec.getString("type"))) {
                        String effectName = abilitySec.getString("effect");
                        int cooldownSeconds = abilitySec.getInt("cooldown", 10);
                        
                        if (effectName != null) {
                            double damage = abilitySec.getDouble("damage", 25.0);
                            if (invokeActiveAbility(player, abilityCoreId, effectName, cooldownSeconds, damage)) {
                                triggeredAbility = true;
                            }
                        }
                    }
                }
            }
        }
        
        if (triggeredAbility) {
            event.setCancelled(true);
        }
    }

    private boolean invokeActiveAbility(Player player, String abilityId, String effectName, int cooldownSeconds, double damage) {
        UUID pId = player.getUniqueId();
        cooldowns.putIfAbsent(pId, new HashMap<>());
        
        long lastUse = cooldowns.get(pId).getOrDefault(abilityId, 0L);
        long now = System.currentTimeMillis();
        long diff = now - lastUse;
        
        if (diff < (cooldownSeconds * 1000L)) {
            long remaining = (cooldownSeconds * 1000L - diff) / 1000L;
            // To prevent spam, only show cooldown message occasionally or use Action Bar
            net.md_5.bungee.api.ChatMessageType type = net.md_5.bungee.api.ChatMessageType.ACTION_BAR;
            player.spigot().sendMessage(type, new net.md_5.bungee.api.chat.TextComponent(ChatColor.RED + "Ability on Cooldown: " + remaining + "s"));
            return false;
        }
        
        boolean success = false;
        
        if (effectName.equalsIgnoreCase("LIGHTNING_STRIKE")) {
            org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 30);
            Location strikeLoc = (result != null && result.getHitBlock() != null) 
                ? result.getHitBlock().getLocation() 
                : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(30));
            strikeLoc.getWorld().strikeLightning(strikeLoc);
            // Apply heavy independent AoE damage
            for (org.bukkit.entity.Entity entity : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 4, 4, 4)) {
                if (entity instanceof org.bukkit.entity.Damageable damageable && entity != player) {
                    damageable.damage(damage, player);
                }
            }
            success = true;
        } else if (effectName.equalsIgnoreCase("DASH")) {
            player.setVelocity(player.getLocation().getDirection().multiply(1.5).setY(0.5));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
            success = true;
        } else if (effectName.equalsIgnoreCase("BLADE_DANCE")) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
            player.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 10, 2, 0.5, 2, 0);
            for (org.bukkit.entity.Entity entity : player.getNearbyEntities(6, 3, 6)) {
                if (entity instanceof org.bukkit.entity.Damageable damageable && entity != player) {
                    damageable.damage(damage, player); // Configurable dynamic damage
                }
            }
            success = true;
        }
        
        if (success) {
            cooldowns.get(pId).put(abilityId, now);
        }
        
        return success;
    }
}
