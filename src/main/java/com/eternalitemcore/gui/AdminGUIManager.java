package com.eternalitemcore.gui;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminGUIManager {

    private final EternalItemCore plugin;

    public AdminGUIManager(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Item Mastery: Admin Panel");

        // Option 1: Edit Stat Cores
        gui.setItem(11, createGuiItem(Material.NETHER_STAR, ChatColor.RED + "Edit Stat Cores",
                ChatColor.GRAY + "Create, Modify, or Delete",
                ChatColor.GRAY + "Stat Cores and leveling paths."));

        // Option 2: Edit Ability Cores
        gui.setItem(13, createGuiItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE + "Edit Abilities",
                ChatColor.GRAY + "Change Cooldowns, Damage,",
                ChatColor.GRAY + "and active triggers."));

        // Option 3: Edit Kill Effects / Death Messages
        gui.setItem(15, createGuiItem(Material.WITHER_SKELETON_SKULL, ChatColor.DARK_GRAY + "Death Effects & Messages",
                ChatColor.GRAY + "Manage custom death messages",
                ChatColor.GRAY + "and particle execution effects."));

        // Filler glass
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    public void openStatCoresMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_RED + "Admin: Edit Stat Cores");
        
        int slot = 0;
        if (plugin.getConfig().getConfigurationSection("stat-cores") != null) {
            for (String coreId : plugin.getConfig().getConfigurationSection("stat-cores").getKeys(false)) {
                if (slot >= 45) break; 
                
                String matStr = plugin.getConfig().getString("stat-cores." + coreId + ".material", "STONE");
                Material mat = Material.matchMaterial(matStr);
                if (mat == null) mat = Material.STONE;
                
                String disp = plugin.getConfig().getString("stat-cores." + coreId + ".name", coreId);
                
                gui.setItem(slot++, createGuiItem(mat, ChatColor.translateAlternateColorCodes('&', disp), 
                    ChatColor.DARK_GRAY + "ID: " + coreId,
                    ChatColor.YELLOW + "Click to Edit"));
            }
        }
        
        // Add "Create New Core" button at the bottom
        gui.setItem(49, createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Create New Core", ChatColor.GRAY + "Generate a brand new stat core."));
        gui.setItem(45, createGuiItem(Material.ARROW, ChatColor.RED + "Back to Main Menu"));

        // Fill remaining empty slots with glass but keep bottom row nice
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }

        player.openInventory(gui);
    }

    public void openAbilitiesMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Admin: Edit Abilities");
        
        int slot = 0;
        if (plugin.getConfig().getConfigurationSection("ability-cores") != null) {
            for (String abilityId : plugin.getConfig().getConfigurationSection("ability-cores").getKeys(false)) {
                if (slot >= 45) break;
                
                String disp = plugin.getConfig().getString("ability-cores." + abilityId + ".display", abilityId);
                String type = plugin.getConfig().getString("ability-cores." + abilityId + ".type", "UNKNOWN");
                
                gui.setItem(slot++, createGuiItem(Material.ENCHANTED_BOOK, ChatColor.translateAlternateColorCodes('&', disp), 
                    ChatColor.DARK_GRAY + "ID: " + abilityId,
                    ChatColor.GRAY + "Type: " + type,
                    ChatColor.YELLOW + "Click to Edit"));
            }
        }
        
        gui.setItem(49, createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Create New Ability", ChatColor.GRAY + "Generate a brand new ability."));
        gui.setItem(45, createGuiItem(Material.ARROW, ChatColor.RED + "Back to Main Menu"));

        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }

        player.openInventory(gui);
    }
    
    public void openCoreEditMenu(Player player, String coreId) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Editing: " + coreId);
        
        gui.setItem(10, createGuiItem(Material.NAME_TAG, ChatColor.YELLOW + "Edit Display Name", ChatColor.GRAY + "Current: " + plugin.getConfig().getString("stat-cores." + coreId + ".name")));
        gui.setItem(11, createGuiItem(Material.DIAMOND, ChatColor.AQUA + "Edit Material", ChatColor.GRAY + "Current: " + plugin.getConfig().getString("stat-cores." + coreId + ".material")));
        gui.setItem(12, createGuiItem(Material.BOOK, ChatColor.GREEN + "Edit Lore Format", ChatColor.GRAY + "Click to rewrite the lore."));
        gui.setItem(14, createGuiItem(Material.COMPASS, ChatColor.LIGHT_PURPLE + "Edit Tracked Stat", ChatColor.GRAY + "Current: " + plugin.getConfig().getString("stat-cores." + coreId + ".stat-type")));
        gui.setItem(16, createGuiItem(Material.BARRIER, ChatColor.RED + "Delete Core", ChatColor.DARK_RED + "Cannot be undone."));
        
        gui.setItem(22, createGuiItem(Material.ARROW, ChatColor.RED + "Back to Cores"));

        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }
        
        player.openInventory(gui);
    }
    
    public void openAbilityEditMenu(Player player, String abilityId) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Editing: " + abilityId);
        
        gui.setItem(10, createGuiItem(Material.NAME_TAG, ChatColor.YELLOW + "Edit Display Name", ChatColor.GRAY + "Current: " + plugin.getConfig().getString("ability-cores." + abilityId + ".display")));
        gui.setItem(11, createGuiItem(Material.CLOCK, ChatColor.AQUA + "Edit Cooldown", ChatColor.GRAY + "Current: " + plugin.getConfig().getDouble("ability-cores." + abilityId + ".cooldown", 0.0) + "s"));
        gui.setItem(12, createGuiItem(Material.IRON_SWORD, ChatColor.RED + "Edit Damage", ChatColor.GRAY + "Current: " + plugin.getConfig().getDouble("ability-cores." + abilityId + ".damage", 0.0)));
        gui.setItem(14, createGuiItem(Material.LEVER, ChatColor.LIGHT_PURPLE + "Edit Keybind Trigger", ChatColor.GRAY + "Current: " + plugin.getConfig().getString("ability-cores." + abilityId + ".trigger", "DEFAULT")));
        gui.setItem(16, createGuiItem(Material.BARRIER, ChatColor.RED + "Delete Ability", ChatColor.DARK_RED + "Cannot be undone."));
        
        gui.setItem(22, createGuiItem(Material.ARROW, ChatColor.RED + "Back to Abilities"));

        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }
        
        player.openInventory(gui);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String l : lore) {
                loreList.add(l);
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
