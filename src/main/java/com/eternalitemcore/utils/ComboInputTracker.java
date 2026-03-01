package com.eternalitemcore.utils;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks per-player rolling input buffers and resolves them into
 * combo matches, mid-combo suppression, or single-key pass-through.
 */
public class ComboInputTracker {

    private final EternalItemCore plugin;

    // Per-player rolling input buffer
    private final Map<UUID, Deque<TimedInput>> buffers = new HashMap<>();

    public ComboInputTracker(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    public enum ResultType { FIRE_COMBO, FIRE_SINGLE, SUPPRESS }

    public record ComboResult(ResultType type, String abilityId) {}

    private record TimedInput(String action, long timestamp) {}

    /**
     * Record an action for the player and resolve what should happen.
     * @param player  The acting player
     * @param action  The action key (e.g. "SNEAK", "JUMP", "RIGHT_CLICK")
     * @return ComboResult indicating what to do
     */
    public ComboResult recordInput(Player player, String action) {
        long windowMs = plugin.getConfig().getLong("combo-window-ms", 600L);
        UUID pid = player.getUniqueId();
        buffers.putIfAbsent(pid, new ArrayDeque<>());
        Deque<TimedInput> buf = buffers.get(pid);

        long now = System.currentTimeMillis();

        // Purge inputs that have expired
        buf.removeIf(t -> (now - t.timestamp()) > windowMs);

        // Append the new input
        buf.addLast(new TimedInput(action, now));

        // Build the current sequence string
        String sequence = buf.stream()
                .map(TimedInput::action)
                .collect(Collectors.joining(","));

        // 1. Check for a full combo match
        String matchedAbility = findExactComboMatch(player, sequence);
        if (matchedAbility != null) {
            buf.clear(); // reset buffer after successful combo
            return new ComboResult(ResultType.FIRE_COMBO, matchedAbility);
        }

        // 2. Check if the current sequence is a prefix of ANY registered combo
        //    (i.e., we are mid-combo — suppress firing single abilities)
        if (sequence.contains(",") && isPrefixOfAnyCombo(player, sequence)) {
            return new ComboResult(ResultType.SUPPRESS, null);
        }

        // 3. No multi-step combo involved.
        //    If the buffer has only this single action AND it isn't a prefix of a combo,
        //    allow single-key abilities to fire.
        if (!sequence.contains(",")) {
            // Check if this single action is the start of a combo (SNEAK,SNEAK style).
            // If yes we SUPPRESS to wait for continuation. If no multi-step combo starts with it, pass through.
            if (isPrefixOfAnyCombo(player, sequence)) {
                // Could start a combo - suppress single for now, wait for next input
                return new ComboResult(ResultType.SUPPRESS, null);
            }
            return new ComboResult(ResultType.FIRE_SINGLE, null);
        }

        // 4. Multi-action sequence that matched nothing and is not a prefix.
        //    Clear the buffer (stale combo attempt) and try this action as fresh single.
        buf.clear();
        buf.addLast(new TimedInput(action, now));
        if (!isPrefixOfAnyCombo(player, action)) {
            return new ComboResult(ResultType.FIRE_SINGLE, null);
        }
        return new ComboResult(ResultType.SUPPRESS, null);
    }

    /**
     * Find an ability-core whose trigger exactly equals the given sequence.
     * Only checks multi-step combos (triggers that contain a comma).
     */
    private String findExactComboMatch(Player player, String sequence) {
        if (!sequence.contains(",")) return null; // only look for multi-step
        ConfigurationSection cores = plugin.getConfig().getConfigurationSection("ability-cores");
        if (cores == null) return null;
        for (String key : cores.getKeys(false)) {
            String trigger = cores.getString(key + ".trigger", "").toUpperCase();
            if (trigger.equals(sequence.toUpperCase())) {
                return key;
            }
        }
        return null;
    }

    /**
     * Check if the given sequence is a non-complete PREFIX of any registered multi-step combo.
     * e.g. "SNEAK" is a prefix of "SNEAK,SNEAK"
     */
    private boolean isPrefixOfAnyCombo(Player player, String sequence) {
        String upperSeq = sequence.toUpperCase();
        ConfigurationSection cores = plugin.getConfig().getConfigurationSection("ability-cores");
        if (cores == null) return false;
        for (String key : cores.getKeys(false)) {
            String trigger = cores.getString(key + ".trigger", "").toUpperCase();
            if (!trigger.contains(",")) continue; // skip single-key abilities
            if (trigger.startsWith(upperSeq) && !trigger.equals(upperSeq)) {
                return true;
            }
        }
        return false;
    }

    /** Clear the buffer for a player (e.g. on disconnect) */
    public void clearBuffer(UUID playerId) {
        buffers.remove(playerId);
    }
}
