package com.eternalitemcore.utils;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AbilityManager {

    private final EternalItemCore plugin;

    public AbilityManager(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    public void triggerAbility(Player player, String abilityCoreId, Location loc) {
        ConfigurationSection abilitySec = plugin.getConfig().getConfigurationSection("ability-cores." + abilityCoreId);
        if (abilitySec == null) return;

        String type = abilitySec.getString("type");
        if (type == null) return;

        // Play sound if configured
        if (loc != null && abilitySec.contains("sound")) {
            try {
                Sound sound = Sound.valueOf(abilitySec.getString("sound"));
                float volume = (float) abilitySec.getDouble("volume", 1.0);
                float pitch = (float) abilitySec.getDouble("pitch", 1.0);
                
                // Play for everyone EXCEPT the player if they have it hidden
                boolean hideForSelf = plugin.getPlayerSettingsManager().hasEffectsHidden(player);
                for (Player p : loc.getWorld().getPlayers()) {
                    if (p.getLocation().distance(loc) < 30) {
                        if (p.equals(player) && hideForSelf) continue;
                        p.playSound(loc, sound, volume, pitch);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid sound named in config for ability " + abilityCoreId);
            }
        }

        if (type.equalsIgnoreCase("POTION")) {
            String effectName = abilitySec.getString("effect");
            int amplifier = abilitySec.getInt("amplifier", 0);
            
            if (effectName != null) {
                PotionEffectType effectType = PotionEffectType.getByName(effectName);
                if (effectType != null) {
                    player.addPotionEffect(new PotionEffect(effectType, 20 * 10, amplifier)); // 10 seconds duration
                }
            }
        } else if (type.equalsIgnoreCase("KILL_EFFECT") && loc != null) {
            String effectName = abilitySec.getString("effect");
            boolean hideForSelf = plugin.getPlayerSettingsManager().hasEffectsHidden(player);

            if (effectName != null && effectName.equalsIgnoreCase("LIGHTNING")) {
                boolean visualOnly = abilitySec.getBoolean("visual-only", true);
                if (visualOnly) {
                    loc.getWorld().strikeLightningEffect(loc);
                } else {
                    loc.getWorld().strikeLightning(loc);
                }
            } else if (effectName != null && effectName.equalsIgnoreCase("SHOCKWAVE")) {
                try {
                    Particle p = Particle.valueOf(abilitySec.getString("particle", "EXPLOSION_LARGE"));
                    // Cinematic expanding ring
                    new org.bukkit.scheduler.BukkitRunnable() {
                        double radius = 0;
                        public void run() {
                            radius += 0.5;
                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                                double x = (radius * Math.cos(angle));
                                double z = (radius * Math.sin(angle));
                                loc.getWorld().spawnParticle(p, loc.clone().add(x, 0.5, z), 1, 0, 0, 0, 0);
                            }
                            if (radius > 3.0) this.cancel();
                        }
                    }.runTaskTimerAsynchronously(plugin, 0L, 1L);
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid particle configured for " + abilityCoreId);
                }
            } else if (effectName != null && effectName.equalsIgnoreCase("BLOOD_SIPHON")) {
                // Cinematic particles drawing from corpse to player
                new org.bukkit.scheduler.BukkitRunnable() {
                    int ticks = 0;
                    Location start = loc.clone().add(0, 1, 0);
                    public void run() {
                        ticks++;
                        if (ticks > 10 || !player.isOnline()) {
                            // Apply heal when particles arrive
                            if (player.isOnline()) {
                                double newHealth = Math.min(player.getHealth() + 4.0, player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
                                player.setHealth(newHealth);
                                player.playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 1.0f, 1.2f);
                            }
                            this.cancel();
                            return;
                        }
                        
                        Location target = player.getLocation().add(0, 1, 0);
                        org.bukkit.util.Vector dir = target.toVector().subtract(start.toVector()).normalize().multiply(0.5);
                        start.add(dir);
                        
                        start.getWorld().spawnParticle(Particle.REDSTONE, start, 5, 0.2, 0.2, 0.2, new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }
    }
}
