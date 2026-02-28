package com.eternalitemcore.data;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerSettingsManager {

    private final EternalItemCore plugin;
    private final NamespacedKey effectsHiddenKey;

    public PlayerSettingsManager(EternalItemCore plugin) {
        this.plugin = plugin;
        this.effectsHiddenKey = new NamespacedKey(plugin, "hide_own_kill_effects");
    }

    public boolean hasEffectsHidden(Player player) {
        if (player == null) return false;
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        return pdc.getOrDefault(effectsHiddenKey, PersistentDataType.BYTE, (byte) 0) == 1;
    }

    public void toggleEffectsHidden(Player player) {
        if (player == null) return;
        boolean currentlyHidden = hasEffectsHidden(player);
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(effectsHiddenKey, PersistentDataType.BYTE, (byte) (currentlyHidden ? 0 : 1));
    }
}
