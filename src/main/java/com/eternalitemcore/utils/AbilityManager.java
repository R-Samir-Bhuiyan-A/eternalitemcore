package com.eternalitemcore.utils;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

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
                
                for (Player p : loc.getWorld().getPlayers()) {
                    if (p.getLocation().distance(loc) < 30) {
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
        } else if (type.equalsIgnoreCase("GLITCH_WALK")) {
            UUID pid = player.getUniqueId();
            if (com.eternalitemcore.listeners.ActiveAbilityListener.glitchState.contains(pid)) return;

            // Read duration: prefer potion-duration (ticks, set by GUI), fall back to duration (seconds)
            int durationTicks = abilitySec.contains("potion-duration")
                    ? abilitySec.getInt("potion-duration", 100)
                    : abilitySec.getInt("duration", 5) * 20;

            // Read speed amplifier: configurable via GUI's "Edit Effect Amplifier" button (default 6 = Speed VII)
            int speedAmp = abilitySec.getInt("potion-amplifier", 6);

            // Apply speed buff using configurable amplifier
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks + 10, speedAmp, false, false));
            com.eternalitemcore.listeners.ActiveAbilityListener.glitchState.add(pid);
            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "[GLITCH] You phase into the void!");

            // Hide the player from ALL other online players (hides armor + items too)
            for (Player other : plugin.getServer().getOnlinePlayers()) {
                if (!other.getUniqueId().equals(pid)) {
                    other.hidePlayer(plugin, player);
                }
            }

            // Trail runnable — runs every 4 ticks, lasts duration + 20 extra ticks after end
            int trailDuration = durationTicks + 20;
            new org.bukkit.scheduler.BukkitRunnable() {
                int t = 0;
                public void run() {
                    if (!player.isOnline()) { this.cancel(); return; }
                    t += 4;
                    Location trailLoc = player.getLocation().add(0, 0.5, 0);
                    // Dense glitch trail visible to everyone
                    trailLoc.getWorld().spawnParticle(Particle.PORTAL,      trailLoc, 45, 0.4, 0.7, 0.4, 0.15);
                    trailLoc.getWorld().spawnParticle(Particle.CRIT_MAGIC,  trailLoc, 25, 0.3, 0.5, 0.3, 0.08);
                    trailLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, trailLoc, 15, 0.5, 0.8, 0.5, 0.05);
                    if (t >= trailDuration) this.cancel();
                }
            }.runTaskTimer(plugin, 0L, 4L);

            // Termination runnable
            new org.bukkit.scheduler.BukkitRunnable() {
                public void run() {
                    if (!player.isOnline()) return;
                    com.eternalitemcore.listeners.ActiveAbilityListener.glitchState.remove(pid);
                    player.setAllowFlight(false);
                    player.removePotionEffect(PotionEffectType.SPEED);

                    // Restore visibility to all online players
                    for (Player other : plugin.getServer().getOnlinePlayers()) {
                        if (!other.getUniqueId().equals(pid)) {
                            other.showPlayer(plugin, player);
                        }
                    }

                    // Termination debuffs
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 4, 2));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 6, 1));

                    // Loud bang to alert everyone nearby
                    player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.8f);
                    player.getLocation().getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation().add(0, 1, 0), 1);
                    player.getLocation().getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.3);

                    // Broadcast to nearby players
                    for (Player near : player.getLocation().getWorld().getPlayers()) {
                        if (near.getLocation().distance(player.getLocation()) < 40) {
                            near.sendMessage(ChatColor.DARK_PURPLE + "[GLITCH] " + ChatColor.GRAY + player.getName() + ChatColor.DARK_PURPLE + " has materialized!");
                        }
                    }
                }
            }.runTaskLater(plugin, durationTicks);

        } else if (type.equalsIgnoreCase("KILL_EFFECT") && loc != null) {
            String effectName = abilitySec.getString("effect");
            boolean hideEffects = plugin.getPlayerSettingsManager().hasEffectsHidden(player);
            
            // If the player toggled their kill effects off, don't play the effect at all for anyone
            if (hideEffects) {
                return;
            }

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
            } else if (effectName != null && effectName.equalsIgnoreCase("SOUL_FIRE_PILLAR")) {
                new org.bukkit.scheduler.BukkitRunnable() {
                    double height = 0;
                    public void run() {
                        height += 0.5;
                        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, height, 0), 10, 0.2, 0, 0.2, 0.05);
                        loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, height, 0), 2, 0.2, 0, 0.2, 0.02);
                        if (height > 5.0) this.cancel();
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            } else if (effectName != null && effectName.equalsIgnoreCase("SHADOW_ERUPTION")) {
                loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0), 100, 1.0, 1.0, 1.0, 0.1);
                loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
            } else if (effectName != null && effectName.equalsIgnoreCase("VOID_IMPLOSION")) {
                new org.bukkit.scheduler.BukkitRunnable() {
                    double radius = 5.0;
                    public void run() {
                        radius -= 0.5;
                        if (radius <= 0) {
                            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                            loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 1, 0), 1);
                            this.cancel();
                            return;
                        }
                        for (double theta = 0; theta < Math.PI * 2; theta += Math.PI / 4) {
                            for (double phi = 0; phi < Math.PI; phi += Math.PI / 4) {
                                double x = radius * Math.sin(phi) * Math.cos(theta);
                                double y = radius * Math.cos(phi) + 1.0;
                                double z = radius * Math.sin(phi) * Math.sin(theta);
                                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x, y, z), 1, 0, 0, 0, 0);
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            } else if (effectName != null && effectName.equalsIgnoreCase("FROST_NOVA")) {
                loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
                loc.getWorld().spawnParticle(Particle.SNOWBALL, loc.clone().add(0, 1, 0), 100, 1.0, 1.0, 1.0, 0.2);
                loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 1, 0), 50, 1.5, 1.5, 1.5, 0.05);
            } else if (effectName != null && effectName.equalsIgnoreCase("BLOOD_GEYSER")) {
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 1.0f, 0.5f);
                new org.bukkit.scheduler.BukkitRunnable() {
                    int ticks = 0;
                    public void run() {
                        ticks++;
                        loc.getWorld().spawnParticle(Particle.REDSTONE, loc.clone().add(0, ticks * 0.5, 0), 20, 0.3, 0.1, 0.3, new Particle.DustOptions(org.bukkit.Color.RED, 2.0f));
                        if (ticks > 10) this.cancel();
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            } else if (effectName != null && effectName.equalsIgnoreCase("VOID_SWALLOW")) {
                loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
                loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0), 150, 2.0, 2.0, 2.0, -0.5); // Negative speed pulls inward
            } else if (effectName != null && effectName.equalsIgnoreCase("ANGELIC_ASCENSION")) {
                loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
                new org.bukkit.scheduler.BukkitRunnable() {
                    int ticks = 0;
                    public void run() {
                        ticks++;
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, ticks * 0.2, 0), 5, 0.5, 0, 0.5, 0.02);
                        loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, ticks * 0.2, 0), 3, 0.5, 0, 0.5, 0.05);
                        if (ticks > 20) this.cancel();
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            } else if (effectName != null && effectName.equalsIgnoreCase("DEMONIC_SLASH")) {
                loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
                loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1, 0), 10, 2.0, 0.1, 2.0, 0);
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 100, 2.0, 0.1, 2.0, 0.1);
            } else if (effectName != null && effectName.equalsIgnoreCase("TOXIC_SPORE")) {
                loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.5f);
                loc.getWorld().spawnParticle(Particle.SLIME, loc.clone().add(0, 1, 0), 100, 1.5, 1.5, 1.5, 0);
                loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 1, 0), 30, 2.0, 2.0, 2.0, 0);
            } else if (effectName != null && effectName.equalsIgnoreCase("COSMIC_DUST")) {
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 1.0f, 1.0f);
                new org.bukkit.scheduler.BukkitRunnable() {
                    double angle = 0;
                    public void run() {
                        angle += Math.PI / 4;
                        double x = Math.cos(angle) * 1.5;
                        double z = Math.sin(angle) * 1.5;
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x, angle * 0.1, z), 2, 0, 0, 0, 0);
                        try {
                            loc.getWorld().spawnParticle(Particle.SPELL_WITCH, loc.clone().add(-x, angle * 0.1, -z), 5, 0, 0, 0, 0.02);
                        } catch (Exception e) {
                            // Ignored if unsupported
                        }
                        if (angle > Math.PI * 4) this.cancel();
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            } else if (effectName != null && effectName.equalsIgnoreCase("GOLDEN_SHOWER")) {
                loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                loc.getWorld().spawnParticle(Particle.TOTEM, loc.clone().add(0, 1, 0), 150, 1.0, 1.0, 1.0, 0.5);
                loc.getWorld().spawnParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 50, 1.0, 1.0, 1.0, 1);
            } else if (effectName != null && effectName.equalsIgnoreCase("ASHEN_WIND")) {
                loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.5f);
                new org.bukkit.scheduler.BukkitRunnable() {
                    double angle = 0;
                    public void run() {
                        angle += Math.PI / 2;
                        double x = Math.cos(angle) * 1.0;
                        double z = Math.sin(angle) * 1.0;
                        loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(x, angle * 0.2, z), 10, 0.2, 0, 0.2, 0.05);
                        loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(-x, angle * 0.2, -z), 10, 0.2, 0, 0.2, 0.05);
                        if (angle > Math.PI * 6) this.cancel();
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            } else if (effectName != null && effectName.equalsIgnoreCase("WITHER_CORRUPTION")) {
                loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
                loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 100, 1.0, 1.0, 1.0, 0.1);
                loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0), 50, 1.0, 1.0, 1.0, 0.1);
            } else if (effectName != null && effectName.equalsIgnoreCase("SONIC_PULSE")) {
                loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
                loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 1, 0), 1);
                loc.getWorld().spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 100, 1.5, 1.5, 1.5, 0.5);
            }
        }
    }
}
