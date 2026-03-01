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
                
                if (type.equalsIgnoreCase("KILL_EFFECT")) continue;
                
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
        gui.setItem(16, createGuiItem(Material.ENDER_PEARL, ChatColor.GOLD + "Edit Level Path", ChatColor.GRAY + "Configure level XP requirements", ChatColor.GRAY + "and Custom Death Messages."));
        gui.setItem(17, createGuiItem(Material.BARRIER, ChatColor.RED + "Delete Core", ChatColor.DARK_RED + "Cannot be undone."));
        
        gui.setItem(22, createGuiItem(Material.ARROW, ChatColor.RED + "Back to Cores"));

        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }
        
        player.openInventory(gui);
    }

    public void openCoreLevelMenu(Player player, String coreId) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Levels: " + coreId);
        
        // Let them edit up to Level 5 visually
        for (int i = 1; i <= 5; i++) {
            String path = "stats." + coreId + ".levels." + i;
            String reqStr = "formula";
            // Check if exact XP req is set instead of formula
            if (plugin.getConfig().contains(path + ".required-xp")) {
                reqStr = plugin.getConfig().getInt(path + ".required-xp") + " XP";
            }
            String dmStr = plugin.getConfig().getString(path + ".death-message", "None");
            
            gui.setItem(10 + (i-1), createGuiItem(Material.EXPERIENCE_BOTTLE, ChatColor.YELLOW + "Level " + i + " XP", 
                ChatColor.GRAY + "Current: " + reqStr, 
                ChatColor.YELLOW + "Click to edit required XP."));
                
            gui.setItem(19 + (i-1), createGuiItem(Material.SKELETON_SKULL, ChatColor.RED + "Level " + i + " Death Msg", 
                ChatColor.GRAY + "Current: " + dmStr,
                ChatColor.YELLOW + "Click to edit broadcast."));
                
            gui.setItem(28 + (i-1), createGuiItem(Material.BLAZE_POWDER, ChatColor.LIGHT_PURPLE + "Level " + i + " Kill Effect", 
                ChatColor.GRAY + "Current Ability ID:",
                ChatColor.GRAY + plugin.getConfig().getString(path + ".ability-unlock", "None"),
                ChatColor.YELLOW + "Click to bind Kill Effect."));
        }
        
        gui.setItem(49, createGuiItem(Material.ARROW, ChatColor.RED + "Back to Editing: " + coreId));
        
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }
        player.openInventory(gui);
    }
    
    public void openAbilitySelectorMenu(Player player, String coreId, int level) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Select Ability: " + coreId + " Lvl " + level);
        
        int slot = 0;
        if (plugin.getConfig().getConfigurationSection("ability-cores") != null) {
            for (String abilityId : plugin.getConfig().getConfigurationSection("ability-cores").getKeys(false)) {
                if (slot >= 45) break;
                
                String disp = plugin.getConfig().getString("ability-cores." + abilityId + ".display", abilityId);
                String type = plugin.getConfig().getString("ability-cores." + abilityId + ".type", "UNKNOWN");
                
                if (!type.equalsIgnoreCase("KILL_EFFECT")) continue;
                
                gui.setItem(slot++, createGuiItem(Material.ENCHANTED_BOOK, ChatColor.translateAlternateColorCodes('&', disp), 
                    ChatColor.DARK_GRAY + "ID: " + abilityId,
                    ChatColor.GRAY + "Type: " + type,
                    ChatColor.YELLOW + "Click to bind to Level " + level));
            }
        }
        
        gui.setItem(48, createGuiItem(Material.BARRIER, ChatColor.RED + "Clear Ability", ChatColor.GRAY + "Remove ability unlock from this level."));
        gui.setItem(50, createGuiItem(Material.ARROW, ChatColor.RED + "Back to Levels"));

        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }

        player.openInventory(gui);
    }
    
    public void openAbilityEditMenu(Player player, String abilityId) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Editing: " + abilityId);

        // --- Row 1: Core Stats ---
        gui.setItem(10, createGuiItem(Material.NAME_TAG, ChatColor.YELLOW + "Edit Display Name", ChatColor.GRAY + "Current: " + plugin.getConfig().getString("ability-cores." + abilityId + ".display")));
        gui.setItem(11, createGuiItem(Material.CLOCK, ChatColor.AQUA + "Edit Cooldown", ChatColor.GRAY + "Current: " + plugin.getConfig().getDouble("ability-cores." + abilityId + ".cooldown", 0.0) + "s"));
        gui.setItem(12, createGuiItem(Material.IRON_SWORD, ChatColor.RED + "Edit Damage", ChatColor.GRAY + "Current: " + plugin.getConfig().getDouble("ability-cores." + abilityId + ".damage", 0.0)));
        gui.setItem(13, createGuiItem(Material.ANVIL, ChatColor.GRAY + "Edit Durability Cost", ChatColor.GRAY + "Current: " + plugin.getConfig().getInt("ability-cores." + abilityId + ".durability-cost", 0) + " uses"));

        // --- Row 1: Keybind / Trigger ---
        String currentTrigger = plugin.getConfig().getString("ability-cores." + abilityId + ".trigger", "DEFAULT");
        gui.setItem(14, createGuiItem(Material.REPEATER, ChatColor.LIGHT_PURPLE + "Build Combo Trigger",
                ChatColor.GRAY + "Current: " + currentTrigger,
                ChatColor.YELLOW + "Click to open the visual",
                ChatColor.YELLOW + "combo sequence builder!"));

        // --- Row 2: Potion Effect editors ---
        String currentEffect = plugin.getConfig().getString("ability-cores." + abilityId + ".potion-type", "NONE");
        int currentAmp = plugin.getConfig().getInt("ability-cores." + abilityId + ".potion-amplifier", 0);
        int currentDur = plugin.getConfig().getInt("ability-cores." + abilityId + ".potion-duration", 100);

        gui.setItem(19, createGuiItem(Material.POTION, ChatColor.GREEN + "Edit Potion Effect Type",
                ChatColor.GRAY + "Current: " + currentEffect,
                ChatColor.GRAY + "e.g. SPEED, STRENGTH, SLOWNESS"));
        gui.setItem(20, createGuiItem(Material.EXPERIENCE_BOTTLE, ChatColor.GREEN + "Edit Effect Amplifier",
                ChatColor.GRAY + "Current Level: " + (currentAmp + 1),
                ChatColor.GRAY + "(0 = Level 1, 4 = Level 5)"));
        gui.setItem(21, createGuiItem(Material.COMPARATOR, ChatColor.GREEN + "Edit Effect Duration",
                ChatColor.GRAY + "Current: " + (currentDur / 20) + "s",
                ChatColor.GRAY + "Enter value in seconds"));

        // --- Row 2: Misc ---
        gui.setItem(23, createGuiItem(Material.FERMENTED_SPIDER_EYE, ChatColor.DARK_GREEN + "Edit Self-Debuffs",
                ChatColor.GRAY + "Configure effects applied",
                ChatColor.GRAY + "to caster on use."));
        gui.setItem(24, createGuiItem(Material.BARRIER, ChatColor.RED + "Delete Ability", ChatColor.DARK_RED + "Cannot be undone."));

        // --- Back ---
        gui.setItem(49, createGuiItem(Material.ARROW, ChatColor.RED + "Back to Abilities"));

        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }

        player.openInventory(gui);
    }

    /**
     * Visual Combo Builder GUI — a 27-slot chest with clickable action icons.
     * Clicking an action appends it to the player's in-progress combo sequence.
     * Save button writes it to the ability config. Clear resets the sequence.
     */
    public void openComboBuilderGUI(Player player, String abilityId, List<String> currentCombo) {
        String currentStr = currentCombo.isEmpty() ? "(empty)" : String.join(",", currentCombo);
        Inventory gui = Bukkit.createInventory(null, 27,
                ChatColor.DARK_PURPLE + "Combo Builder: " + abilityId);

        // --- Action buttons (Row 1) ---
        gui.setItem(0,  createGuiItem(Material.IRON_BOOTS,  ChatColor.AQUA   + "+ SNEAK",    ChatColor.GRAY + "Shift key"));
        gui.setItem(1,  createGuiItem(Material.FEATHER,     ChatColor.YELLOW + "+ JUMP",     ChatColor.GRAY + "Space key"));
        gui.setItem(2,  createGuiItem(Material.STICK,       ChatColor.GREEN  + "+ RIGHT_CLICK",  ChatColor.GRAY + "Right mouse"));
        gui.setItem(3,  createGuiItem(Material.IRON_AXE,    ChatColor.RED    + "+ LEFT_CLICK",   ChatColor.GRAY + "Left mouse"));
        gui.setItem(4,  createGuiItem(Material.REPEATER,    ChatColor.LIGHT_PURPLE + "+ SWAP_HANDS",   ChatColor.GRAY + "F key"));
        gui.setItem(5,  createGuiItem(Material.HOPPER,      ChatColor.GOLD   + "+ DROP_ITEM",    ChatColor.GRAY + "Q key"));

        // --- Sequence preview (Row 2 center) ---
        gui.setItem(13, createGuiItem(Material.PAPER, ChatColor.WHITE + "Current Combo",
                ChatColor.YELLOW + currentStr,
                "",
                ChatColor.GRAY + "Click action icons above to build."));

        // --- Control buttons ---
        gui.setItem(18, createGuiItem(Material.RED_CONCRETE,   ChatColor.RED   + "Clear Combo",  ChatColor.GRAY + "Resets sequence"));
        gui.setItem(26, createGuiItem(Material.GREEN_CONCRETE, ChatColor.GREEN + "Save & Apply", ChatColor.GRAY + "Writes: " + currentStr));

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
