package com.eternalitemcore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Intercepts all events during the GLITCH_WALK active ability state:
 *  - Prevents the player from receiving any damage (invulnerability)
 *  - Prevents the player from dealing any damage (pacifism)
 *  - Prevents the player from chatting or running commands (muted)
 *  - Prevents the player from interacting with blocks/items (locked)
 */
public class GlitchStateListener implements Listener {

    // --- Invulnerability ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGlitchTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (ActiveAbilityListener.glitchState.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // --- Pacifism (can't hit anyone) ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGlitchAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (ActiveAbilityListener.glitchState.contains(attacker.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.DARK_PURPLE + "[GLITCH] You are intangible — you cannot attack while phased!");
        }
    }

    // --- Muted (no chat) ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGlitchChat(AsyncPlayerChatEvent event) {
        if (ActiveAbilityListener.glitchState.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "[GLITCH] You cannot speak while phased!");
        }
    }

    // --- Muted (no commands) ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGlitchCommand(PlayerCommandPreprocessEvent event) {
        if (ActiveAbilityListener.glitchState.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "[GLITCH] Commands are disabled while phased!");
        }
    }

    // --- Locked (no interact) ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGlitchInteract(PlayerInteractEvent event) {
        if (ActiveAbilityListener.glitchState.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
