package com.otitem;

import com.otitem.config.ConfigManager;
import com.otitem.core.CoreItemManager;
import com.otitem.data.ItemDataManager;
import com.otitem.data.PlayerSettingsManager;
import com.otitem.listeners.CoreApplyListener;
import com.otitem.listeners.StatTrackerListener;
import com.otitem.utils.AbilityManager;
import com.otitem.utils.ComboInputTracker;
import com.otitem.utils.LoreManager;
import com.otitem.commands.OTItemCommand;
import com.otitem.commands.OTItemTabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class OTItem extends JavaPlugin {

    private static OTItem instance;
    private ConfigManager configManager;
    private CoreItemManager coreItemManager;
    private ItemDataManager itemDataManager;
    private PlayerSettingsManager playerSettingsManager;
    private LoreManager loreManager;
    private AbilityManager abilityManager;
    private ComboInputTracker comboInputTracker;
    private com.otitem.gui.AdminGUIManager adminGUIManager;
    private com.otitem.api.PluginMetrics pluginMetrics;


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
        this.comboInputTracker = new ComboInputTracker(this);
        this.adminGUIManager = new com.otitem.gui.AdminGUIManager(this);
        this.pluginMetrics = new com.otitem.api.PluginMetrics(this);
        this.pluginMetrics.init();

        getCommand("otitem").setExecutor(new OTItemCommand(this));
        getCommand("otitem").setTabCompleter(new OTItemTabCompleter(this));

        // Register Listeners
        org.bukkit.plugin.PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CoreApplyListener(this), this);
        pluginManager.registerEvents(new com.otitem.listeners.AntiCraftListener(this), this);
        pluginManager.registerEvents(new StatTrackerListener(this), this);
        pluginManager.registerEvents(new com.otitem.listeners.ActiveAbilityListener(this), this);
        pluginManager.registerEvents(new com.otitem.listeners.GlitchStateListener(), this);
        pluginManager.registerEvents(new com.otitem.listeners.BowListener(this), this);
        pluginManager.registerEvents(new com.otitem.gui.AdminGUIListener(this), this);

        getServer().getScheduler().runTaskTimer(this, new com.otitem.utils.AbilityTickManager(this), 20L, 20L);

        getLogger().info("OTItem has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("OTItem has been disabled!");
    }

    public static OTItem getInstance() {
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

    public ComboInputTracker getComboInputTracker() {
        return comboInputTracker;
    }

    public PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }

    public com.otitem.gui.AdminGUIManager getAdminGUIManager() {
        return adminGUIManager;
    }
}
