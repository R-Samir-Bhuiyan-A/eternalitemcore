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
                    for (Player p : loc.getWorld().getPlayers()) {
                        if (p.getLocation().distance(loc) < 40) {
                            if (p.equals(player) && hideForSelf) continue;
                            // Sending lightning packet to individual players instead of global world strike would require NMS/ProtocolLib or complex packet handling.
                            // For simplicity on Bukkit API, if a player hides effects, we might just have to skip Lightning entirely for them, but Bukkit doesn't allow "fake" lightning easily without NMS.
                            // However, Paper recently added it or we can spawn a Lightning entity visually.
                            // Since true fake lightning per-player requires packets, we will use spawnParticle instead as a fallback for the "invisible to self" requirement, or just ignore LIGHTNING hide for now. 
                        }
                    }
                    loc.getWorld().strikeLightningEffect(loc); // Global for now unless using ProtocolLib
                } else {
                    loc.getWorld().strikeLightning(loc);
                }
            } else if (effectName != null && effectName.equalsIgnoreCase("PARTICLE_EXPLOSION")) {
                try {
                    Particle p = Particle.valueOf(abilitySec.getString("particle", "EXPLOSION_LARGE"));
                    int count = abilitySec.getInt("count", 20);
                    // Spawn particle for each nearby player EXCEPT the source if hidden
                    for (Player pTarget : loc.getWorld().getPlayers()) {
                        if (pTarget.getLocation().distance(loc) < 40) {
                            if (pTarget.equals(player) && hideForSelf) continue;
                            pTarget.spawnParticle(p, loc, count, 0.5, 0.5, 0.5, 0.1);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid particle configured for " + abilityCoreId);
                }
            }
        }
    }
}
