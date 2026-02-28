package com.eternalitemcore.commands;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EternalItemCoreCommand implements CommandExecutor {

    private final EternalItemCore plugin;

    public EternalItemCoreCommand(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("eternalitemcore.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("toggleeffects")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (!player.hasPermission("eternalitemcore.player")) {
                player.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }
            plugin.getPlayerSettingsManager().toggleEffectsHidden(player);
            boolean hidden = plugin.getPlayerSettingsManager().hasEffectsHidden(player);
            player.sendMessage(ChatColor.GREEN + "Your item kill effects are now " + (hidden ? ChatColor.RED + "HIDDEN" : ChatColor.GREEN + "VISIBLE") + ChatColor.GREEN + " to you.");
            return true;
        }

        if (sub.equals("viewstats")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (!player.hasPermission("eternalitemcore.player")) {
                player.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir() || !hand.hasItemMeta()) {
                player.sendMessage(ChatColor.RED + "You must hold an eternal item in your main hand.");
                return true;
            }
            java.util.List<String> stats = plugin.getItemDataManager().getEnabledStats(hand);
            if (stats.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "This item has no tracked stats.");
                return true;
            }
            player.sendMessage(ChatColor.GOLD + "=== Item Stats ===");
            for (String stat : stats) {
                int level = plugin.getItemDataManager().getStatLevel(hand, stat);
                int val = plugin.getItemDataManager().getStatValue(hand, stat);
                player.sendMessage(ChatColor.AQUA + " - " + stat + ": Level " + level + " | Value: " + val);
            }
            return true;
        }

        if (sub.equals("togglebroadcast")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (!player.hasPermission("eternalitemcore.player")) {
                player.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir() || !hand.hasItemMeta()) {
                player.sendMessage(ChatColor.RED + "You must hold an eternal item in your main hand.");
                return true;
            }
            org.bukkit.persistence.PersistentDataContainer pdc = hand.getItemMeta().getPersistentDataContainer();
            org.bukkit.NamespacedKey hideBcastKey = new org.bukkit.NamespacedKey(plugin, "hide_broadcasts");
            byte current = pdc.getOrDefault(hideBcastKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 0);
            
            org.bukkit.inventory.meta.ItemMeta meta = hand.getItemMeta();
            if (current == 0) {
                meta.getPersistentDataContainer().set(hideBcastKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                player.sendMessage(ChatColor.GREEN + "Broadcasts for this item are now " + ChatColor.RED + "HIDDEN" + ChatColor.GREEN + ".");
            } else {
                meta.getPersistentDataContainer().set(hideBcastKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 0);
                player.sendMessage(ChatColor.GREEN + "Broadcasts for this item are now " + ChatColor.AQUA + "VISIBLE" + ChatColor.GREEN + ".");
            }
            hand.setItemMeta(meta);
            return true;
        }

        if (!sender.hasPermission("eternalitemcore.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use admin commands.");
            return true;
        }
        
        switch (sub) {
            case "give":
                if (args.length == 3) {
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                    String coreId = args[2];
                    ItemStack coreItem = plugin.getCoreItemManager().createStatCore(coreId);
                    if (coreItem == null) {
                        sender.sendMessage(ChatColor.RED + "Invalid Core ID.");
                        return true;
                    }
                    target.getInventory().addItem(coreItem);
                    sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " a " + coreId);
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /eicore give <player> <core_id>");
                }
                break;
                
            case "setlevel":
                if (args.length == 4 && sender instanceof Player) {
                    Player p = plugin.getServer().getPlayer(args[1]);
                    if (p == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                    String statId = args[2].toUpperCase();
                    int level = 1;
                    try {
                        level = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Level must be a number.");
                        return true;
                    }
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (hand.getType().isAir() || !plugin.getItemDataManager().hasStatEnabled(hand, statId)) {
                        sender.sendMessage(ChatColor.RED + "Player's held item does not have stat " + statId);
                        return true;
                    }
                    plugin.getItemDataManager().setStatLevel(hand, statId, level);
                    plugin.getLoreManager().updateLore(hand);
                    sender.sendMessage(ChatColor.GREEN + "Set level of " + statId + " to " + level);
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /eicore setlevel <player> <stat_id> <level>");
                }
                break;

            case "addstat":
                if (args.length == 4 && sender instanceof Player) {
                    Player p = plugin.getServer().getPlayer(args[1]);
                    if (p == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                    String statId = args[2].toUpperCase();
                    int amount = 0;
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                        return true;
                    }
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (hand.getType().isAir() || !plugin.getItemDataManager().hasStatEnabled(hand, statId)) {
                        sender.sendMessage(ChatColor.RED + "Player's held item does not have stat " + statId);
                        return true;
                    }
                    plugin.getItemDataManager().incrementStat(p, hand, statId, amount);
                    plugin.getLoreManager().updateLore(hand);
                    sender.sendMessage(ChatColor.GREEN + "Added " + amount + " to " + statId);
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /eicore addstat <player> <stat_id> <amount>");
                }
                break;

            case "clearstats":
                if (args.length == 2 && sender instanceof Player) {
                    Player p = plugin.getServer().getPlayer(args[1]);
                    if (p == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                        return true;
                    }
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (hand.getType().isAir() || !hand.hasItemMeta()) {
                        sender.sendMessage(ChatColor.RED + "Target player is not holding a valid item.");
                        return true;
                    }
                    org.bukkit.persistence.PersistentDataContainer pdc = hand.getItemMeta().getPersistentDataContainer();
                    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "enabled_stats");
                    if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                        org.bukkit.inventory.meta.ItemMeta m = hand.getItemMeta();
                        m.getPersistentDataContainer().remove(key);
                        hand.setItemMeta(m);
                        plugin.getLoreManager().updateLore(hand);
                        sender.sendMessage(ChatColor.GREEN + "Cleared all stats from " + p.getName() + "'s held item.");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "That item has no stats to clear.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /eicore clearstats <player>");
                }
                break;

            case "reload":
                plugin.reloadConfig();
                plugin.getConfigManager().loadConfig();
                sender.sendMessage(ChatColor.GREEN + "EternalItemCore configuration reloaded successfully!");
                break;
                
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== EternalItemCore ===");
        if (sender.hasPermission("eternalitemcore.player")) {
            sender.sendMessage(ChatColor.YELLOW + "/eicore toggleeffects" + ChatColor.GRAY + " - Toggle your own kill visual effects.");
            sender.sendMessage(ChatColor.YELLOW + "/eicore viewstats" + ChatColor.GRAY + " - View exact stats on your held weapon.");
            sender.sendMessage(ChatColor.YELLOW + "/eicore togglebroadcast" + ChatColor.GRAY + " - Toggle global level-up messages for held item.");
        }
        if (sender.hasPermission("eternalitemcore.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/eicore give <player> <core_id>" + ChatColor.GRAY + " - Give a specific core to a player.");
            sender.sendMessage(ChatColor.YELLOW + "/eicore setlevel <player> <stat_id> <level>" + ChatColor.GRAY + " - Force set an item's stat level.");
            sender.sendMessage(ChatColor.YELLOW + "/eicore addstat <player> <stat_id> <amount>" + ChatColor.GRAY + " - Add raw stat value (XP) to an item.");
            sender.sendMessage(ChatColor.YELLOW + "/eicore clearstats <player>" + ChatColor.GRAY + " - Wipe all stats from a player's held item.");
            sender.sendMessage(ChatColor.YELLOW + "/eicore reload" + ChatColor.GRAY + " - Reloads config.yml from disk.");
        }
    }
}
