package com.eternalitemcore.listeners;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BowListener implements Listener {

    private final EternalItemCore plugin;
    
    // Player UUID -> (Ability ID -> Array[Remaining Charges, Last Reload Time])
    private final Map<UUID, Map<String, long[]>> abilityCharges = new HashMap<>();

    public BowListener(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (bow == null || bow.getType().isAir()) return;

        List<String> enabledStats = plugin.getItemDataManager().getEnabledStats(bow);
        if (enabledStats.isEmpty()) return;

        for (String statId : enabledStats) {
            int currentLevel = plugin.getItemDataManager().getStatLevel(bow, statId);
            for (int i = 1; i <= currentLevel; i++) {
                ConfigurationSection levelSec = plugin.getConfig().getConfigurationSection("stats." + statId + ".levels." + i);
                if (levelSec != null && levelSec.contains("ability-unlock")) {
                    String abilityId = levelSec.getString("ability-unlock");
                    ConfigurationSection abilitySec = plugin.getConfig().getConfigurationSection("ability-cores." + abilityId);
                    
                    if (abilitySec != null && "BOW_ACTIVE".equalsIgnoreCase(abilitySec.getString("type"))) {
                        String effectName = abilitySec.getString("effect");
                        if (effectName != null) {
                            int maxCharges = abilitySec.getInt("max-charges", 1);
                            int cooldown = abilitySec.getInt("cooldown", 10);
                            double damage = abilitySec.getDouble("damage", 10.0);
                            
                            if (tryConsumeCharge(player, abilityId, maxCharges, cooldown)) {
                                executeBowAbility(player, event.getProjectile(), effectName, damage);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean tryConsumeCharge(Player player, String abilityId, int maxCharges, int cooldownSeconds) {
        UUID pId = player.getUniqueId();
        abilityCharges.putIfAbsent(pId, new HashMap<>());
        
        long[] chargeData = abilityCharges.get(pId).getOrDefault(abilityId, new long[]{maxCharges, 0L});
        long charges = chargeData[0];
        long lastUse = chargeData[1];
        long now = System.currentTimeMillis();
        
        // Check if cooldown has passed to restock
        if (charges <= 0) {
            long diff = now - lastUse;
            if (diff >= (cooldownSeconds * 1000L)) {
                charges = maxCharges; // Restocked
            } else {
                long remaining = (cooldownSeconds * 1000L - diff) / 1000L;
                net.md_5.bungee.api.ChatMessageType type = net.md_5.bungee.api.ChatMessageType.ACTION_BAR;
                player.spigot().sendMessage(type, new net.md_5.bungee.api.chat.TextComponent(ChatColor.RED + "Bow Ability Reloading: " + remaining + "s"));
                return false;
            }
        }

        // Consume 1 charge
        charges--;
        if (charges == 0) {
            // Start the cooldown timer precisely when the last charge is spent
            lastUse = now;
        }

        abilityCharges.get(pId).put(abilityId, new long[]{charges, lastUse});
        
        net.md_5.bungee.api.ChatMessageType type = net.md_5.bungee.api.ChatMessageType.ACTION_BAR;
        player.spigot().sendMessage(type, new net.md_5.bungee.api.chat.TextComponent(ChatColor.AQUA + "Charges Left: " + charges + "/" + maxCharges));
        
        return true;
    }

    private void executeBowAbility(Player player, org.bukkit.entity.Entity originalProjectile, String effectName, double configDamage) {
        if (effectName.equalsIgnoreCase("EXPLOSIVE_SHOT")) {
            // Tag the arrow so we know it should explode on hit
            originalProjectile.setMetadata("eicore_explosive", new FixedMetadataValue(plugin, configDamage));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);
        } else if (effectName.equalsIgnoreCase("VOLLEY")) {
            Vector dir = originalProjectile.getVelocity();
            for (int i = 0; i < 4; i++) {
                Arrow extra = player.launchProjectile(Arrow.class);
                extra.setVelocity(dir.clone().add(new Vector(
                    (Math.random() - 0.5) * 0.5,
                    (Math.random() - 0.5) * 0.5,
                    (Math.random() - 0.5) * 0.5
                )));
                extra.setShooter(player);
                extra.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.5f);
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (proj.hasMetadata("eicore_explosive")) {
            double damage = proj.getMetadata("eicore_explosive").get(0).asDouble();
            
            // Create explosion that damages but DOES NOT break blocks
            proj.getWorld().createExplosion(proj.getLocation(), 3.0f, false, false, proj.getShooter() instanceof org.bukkit.entity.Entity ? (org.bukkit.entity.Entity) proj.getShooter() : null);
            
            // Apply bonus exact damage
            if (proj.getShooter() instanceof Player player) {
                for (org.bukkit.entity.Entity ent : proj.getWorld().getNearbyEntities(proj.getLocation(), 4, 4, 4)) {
                    if (ent instanceof org.bukkit.entity.Damageable d && ent != player) {
                        d.damage(damage, player);
                    }
                }
            }
            proj.remove();
        }
    }
}
