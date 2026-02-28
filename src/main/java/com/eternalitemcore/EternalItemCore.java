package com.eternalitemcore;

import com.eternalitemcore.config.ConfigManager;
import com.eternalitemcore.core.CoreItemManager;
import com.eternalitemcore.data.ItemDataManager;
import com.eternalitemcore.data.PlayerSettingsManager;
import com.eternalitemcore.listeners.CoreApplyListener;
import com.eternalitemcore.listeners.StatTrackerListener;
import com.eternalitemcore.utils.AbilityManager;
import com.eternalitemcore.utils.LoreManager;
import com.eternalitemcore.commands.EternalItemCoreCommand;
import com.eternalitemcore.commands.EternalItemCoreTabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class EternalItemCore extends JavaPlugin {

    private static EternalItemCore instance;
    private ConfigManager configManager;
    private CoreItemManager coreItemManager;
    private ItemDataManager itemDataManager;
    private PlayerSettingsManager playerSettingsManager;
    private LoreManager loreManager;
    private AbilityManager abilityManager;


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.coreItemManager = new CoreItemManager(this);
        this.itemDataManager = new ItemDataManager(this);
        this.playerSettingsManager = new PlayerSettingsManager(this);
        this.loreManager = new LoreManager(this);
        this.abilityManager = new AbilityManager(this);

        getCommand("eternalitemcore").setExecutor(new EternalItemCoreCommand(this));
        getCommand("eternalitemcore").setTabCompleter(new EternalItemCoreTabCompleter(this));

        // Register Listeners
        org.bukkit.plugin.PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CoreApplyListener(this), this);
        pluginManager.registerEvents(new com.eternalitemcore.listeners.AntiCraftListener(this), this);
        pluginManager.registerEvents(new StatTrackerListener(this), this);
        pluginManager.registerEvents(new com.eternalitemcore.listeners.ActiveAbilityListener(this), this);

        getServer().getScheduler().runTaskTimerAsynchronously(this, new com.eternalitemcore.utils.AbilityTickManager(this), 20L, 20L);

        getLogger().info("EternalItemCore has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("EternalItemCore has been disabled!");
    }

    public static EternalItemCore getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CoreItemManager getCoreItemManager() {
        return coreItemManager;
    }

    public ItemDataManager getItemDataManager() {
        return itemDataManager;
    }

    public LoreManager getLoreManager() {
        return loreManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }
}
