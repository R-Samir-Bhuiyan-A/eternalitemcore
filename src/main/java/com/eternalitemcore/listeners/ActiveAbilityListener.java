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
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ActiveAbilityListener implements Listener {

    private final EternalItemCore plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    // Per-player category GCDs (ATTACK / DEFENSE / MISC)
    private final Map<UUID, Map<String, Long>> categoryGcds = new HashMap<>();
    // Players currently in Glitch State
    public static final Set<UUID> glitchState = new HashSet<>();
    // Track ground state per player for reliable JUMP detection
    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();

    public ActiveAbilityListener(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    private String getAbilityTrigger(ConfigurationSection ability) {
        if (ability.contains("trigger")) return ability.getString("trigger").toUpperCase();
        String type = ability.getString("type", "").toUpperCase();
        if ("ACTIVE".equals(type)) return "RIGHT_CLICK";
        if ("SNEAK_ACTIVE".equals(type)) return "SNEAK";
        if ("BOW_ACTIVE".equals(type)) return "BOW_SHOOT";
        return "NONE";
    }

    /** Single-item trigger matching — only for FIRE_SINGLE results from the tracker. */
    private boolean processSingleTrigger(Player player, ItemStack item, String triggerAction) {
        if (item == null || item.getType().isAir()) return false;
        List<String> enabledStats = plugin.getItemDataManager().getEnabledStats(item);
        if (enabledStats.isEmpty()) return false;
        boolean triggeredAny = false;
        for (String statId : enabledStats) {
            int currentLevel = plugin.getItemDataManager().getStatLevel(item, statId);
            for (int i = 1; i <= currentLevel; i++) {
                ConfigurationSection levelSec = plugin.getConfig().getConfigurationSection("stats." + statId + ".levels." + i);
                if (levelSec != null && levelSec.contains("ability-unlock")) {
                    String abilityCoreId = levelSec.getString("ability-unlock");
                    ConfigurationSection abilitySec = plugin.getConfig().getConfigurationSection("ability-cores." + abilityCoreId);
                    if (abilitySec != null) {
                        String reqTrigger = getAbilityTrigger(abilitySec);
                        // Skip multi-step combo triggers in single-key mode
                        if (reqTrigger.contains(",")) continue;
                        if (reqTrigger.equals(triggerAction)) {
                            String effectName = abilitySec.getString("effect", abilitySec.getString("type"));
                            int cooldownSeconds = abilitySec.getInt("cooldown", 10);
                            if (effectName != null) {
                                double damage = abilitySec.getDouble("damage", 25.0);
                                int duraCost = abilitySec.getInt("durability-cost", 0);
                                if (invokeActiveAbility(player, item, abilityCoreId, effectName, cooldownSeconds, damage, duraCost)) {
                                    triggeredAny = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return triggeredAny;
    }

    /**
     * Central dispatch: routes an action through the ComboInputTracker.
     * FIRE_COMBO  → find and invoke the matched combo ability directly
     * SUPPRESS    → mid-combo, do nothing
     * FIRE_SINGLE → fall through to per-item single-key matching
     */
    private boolean processAllTriggers(Player player, String action) {
        com.eternalitemcore.utils.ComboInputTracker.ComboResult result =
                plugin.getComboInputTracker().recordInput(player, action);

        switch (result.type()) {
            case FIRE_COMBO -> {
                // Find and fire only the matched combo ability
                return fireComboAbility(player, result.abilityId());
            }
            case SUPPRESS -> {
                return false; // mid-combo, suppress all
            }
            case FIRE_SINGLE -> {
                // Normal per-item trigger matching
                boolean triggered = false;
                if (processSingleTrigger(player, player.getInventory().getItemInMainHand(), action)) triggered = true;
                for (ItemStack armor : player.getInventory().getArmorContents()) {
                    if (armor != null && !armor.getType().isAir()) {
                        if (processSingleTrigger(player, armor, action)) triggered = true;
                    }
                }
                return triggered;
            }
        }
        return false;
    }

    /**
     * Fires a specific ability ID directly — used when the combo tracker identifies an exact match.
     * Scans all equipped items to find one that has the matched ability bound.
     */
    private boolean fireComboAbility(Player player, String abilityId) {
        // Check hotbar + armor for this ability binding
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        java.util.List<ItemStack> allItems = new java.util.ArrayList<>();
        allItems.add(inv.getItemInMainHand());
        java.util.Collections.addAll(allItems, inv.getArmorContents());

        for (ItemStack item : allItems) {
            if (item == null || item.getType().isAir()) continue;
            List<String> enabledStats = plugin.getItemDataManager().getEnabledStats(item);
            for (String statId : enabledStats) {
                int currentLevel = plugin.getItemDataManager().getStatLevel(item, statId);
                for (int i = 1; i <= currentLevel; i++) {
                    ConfigurationSection levelSec = plugin.getConfig().getConfigurationSection("stats." + statId + ".levels." + i);
                    if (levelSec == null || !levelSec.contains("ability-unlock")) continue;
                    if (!abilityId.equals(levelSec.getString("ability-unlock"))) continue;
                    // Found the item holding this ability
                    ConfigurationSection abilitySec = plugin.getConfig().getConfigurationSection("ability-cores." + abilityId);
                    if (abilitySec == null) continue;
                    String effectName = abilitySec.getString("effect", abilitySec.getString("type"));
                    int cooldown = abilitySec.getInt("cooldown", 10);
                    double damage = abilitySec.getDouble("damage", 0);
                    int duraCost = abilitySec.getInt("durability-cost", 0);
                    if (effectName != null) {
                        return invokeActiveAbility(player, item, abilityId, effectName, cooldown, damage, duraCost);
                    }
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String action = "";
        
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) action = "RIGHT_CLICK";
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) action = "LEFT_CLICK";
        
        if (action.isEmpty()) return;
        
        if (processAllTriggers(player, action)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        processAllTriggers(event.getPlayer(), "SNEAK");
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onJump(org.bukkit.event.player.PlayerMoveEvent event) {
        Player p = event.getPlayer();
        boolean onGround = p.isOnGround();
        boolean prevOnGround = wasOnGround.getOrDefault(p.getUniqueId(), true);
        wasOnGround.put(p.getUniqueId(), onGround);
        // Fire JUMP exactly once: when transitioning from grounded to airborne with upward movement
        if (prevOnGround && !onGround && event.getTo() != null && event.getTo().getY() > event.getFrom().getY()) {
            processAllTriggers(p, "JUMP");
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (processAllTriggers(event.getPlayer(), "SWAP_HANDS")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (processAllTriggers(event.getPlayer(), "DROP_ITEM")) {
            event.setCancelled(true);
        }
    }

    private boolean invokeActiveAbility(Player player, ItemStack itemUsed, String abilityId, String effectName, int cooldownSeconds, double damage, int duraCost) {
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
        
        // Categorized GCD check
        ConfigurationSection abilitySec2 = plugin.getConfig().getConfigurationSection("ability-cores." + abilityId);
        String category = (abilitySec2 != null) ? abilitySec2.getString("category", "MISC").toUpperCase() : "MISC";
        categoryGcds.putIfAbsent(pId, new HashMap<>());
        long lastCatUse = categoryGcds.get(pId).getOrDefault(category, 0L);
        double gcdSecs = plugin.getConfig().getDouble("global-cooldowns." + category.toLowerCase(), 0.0);
        if (gcdSecs > 0 && (now - lastCatUse) < (gcdSecs * 1000L)) {
            long remGcd = (long)(gcdSecs * 1000L - (now - lastCatUse)) / 1000L;
            net.md_5.bungee.api.ChatMessageType barType = net.md_5.bungee.api.ChatMessageType.ACTION_BAR;
            player.spigot().sendMessage(barType, new net.md_5.bungee.api.chat.TextComponent(ChatColor.GOLD + "[" + category + " GCD] " + remGcd + "s remaining"));
            return false;
        }
        // Apply GCD for category
        if (gcdSecs > 0) categoryGcds.get(pId).put(category, now);
        
        // Durability Check
        if (duraCost > 0) {
            org.bukkit.inventory.meta.Damageable meta = (org.bukkit.inventory.meta.Damageable) itemUsed.getItemMeta();
            if (meta == null) return false; // Not damageable
            
            int maxDura = itemUsed.getType().getMaxDurability();
            if (maxDura == 0 || (meta.getDamage() + duraCost) >= maxDura) {
                player.sendMessage(ChatColor.RED + "This item is too damaged to handle the strain of that ability!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                return false;
            }
            // Consume Durability
            meta.setDamage(meta.getDamage() + duraCost);
            itemUsed.setItemMeta(meta);
        }
        
        ConfigurationSection abilitySec = plugin.getConfig().getConfigurationSection("ability-cores." + abilityId);
        if (abilitySec != null) {
            if (abilitySec.contains("self-effects")) {
                for (String effectKey : abilitySec.getConfigurationSection("self-effects").getKeys(false)) {
                    String typeStr = abilitySec.getString("self-effects." + effectKey + ".type");
                    int dur = abilitySec.getInt("self-effects." + effectKey + ".duration", 100);
                    int amp = abilitySec.getInt("self-effects." + effectKey + ".amplifier", 0);
                    if (typeStr != null) {
                        PotionEffectType pType = PotionEffectType.getByName(typeStr);
                        if (pType != null) {
                            player.addPotionEffect(new PotionEffect(pType, dur, amp));
                        }
                    }
                }
            }
            if (abilitySec.contains("remove-effects")) {
                for (String typeStr : abilitySec.getStringList("remove-effects")) {
                    PotionEffectType pType = PotionEffectType.getByName(typeStr);
                    if (pType != null && player.hasPotionEffect(pType)) {
                        player.removePotionEffect(pType);
                    }
                }
            }
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
        } else if (effectName.equalsIgnoreCase("EARTHQUAKE_CLEAVE")) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, player.getLocation(), 2);
            for (org.bukkit.entity.Entity entity : player.getNearbyEntities(5, 3, 5)) {
                if (entity instanceof org.bukkit.entity.Damageable damageable && entity != player) {
                    damageable.damage(damage, player);
                    entity.setVelocity(new org.bukkit.util.Vector(0, 1.2, 0)); // Launch enemies
                }
            }
            success = true;
        } else if (effectName.equalsIgnoreCase("SONIC_BOOM")) {
            // Warden Sonic Boom logic
            org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 15);
            org.bukkit.Location strikeLoc = (result != null && result.getHitBlock() != null) 
                ? result.getHitBlock().getLocation() 
                : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(15));
            
            player.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, player.getEyeLocation().add(player.getEyeLocation().getDirection()), 1);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 1.0f);
            
            // Give self blindness
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0)); // 3 seconds
            
            for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(strikeLoc, 3, 3, 3)) {
                if (entity instanceof org.bukkit.entity.Damageable damageable && entity != player) {
                    damageable.damage(damage, player);
                    Vector kb = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(2.0);
                    entity.setVelocity(kb.setY(0.5));
                }
            }
            success = true;
        } else if (effectName.equalsIgnoreCase("GRAND_HARVEST")) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, player.getLocation(), 30, 3, 1, 3);
            
            int radius = 5;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        org.bukkit.block.Block block = player.getLocation().getBlock().getRelative(x, y, z);
                        if (block.getBlockData() instanceof org.bukkit.block.data.Ageable crop) {
                            crop.setAge(crop.getMaximumAge());
                            block.setBlockData(crop);
                            block.getWorld().spawnParticle(org.bukkit.Particle.COMPOSTER, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3, 0);
                        }
                    }
                }
            }
            success = true;
        } else if (effectName.equalsIgnoreCase("GLITCH_WALK")) {
            // Route to AbilityManager for the complex Glitch State logic
            plugin.getAbilityManager().triggerAbility(player, abilityId, null);
            // triggerAbility handles the state internally; mark as success so cooldown applies
            success = true;
        }
        
        if (success) {
            cooldowns.get(pId).put(abilityId, now);
        }
        
        return success;
    }
}
