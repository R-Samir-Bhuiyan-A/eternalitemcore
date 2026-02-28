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
                    plugin.getItemDataManager().incrementStat(hand, statId, amount);
                    plugin.getLoreManager().updateLore(hand);
                    sender.sendMessage(ChatColor.GREEN + "Added " + amount + " to " + statId);
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /eicore addstat <player> <stat_id> <amount>");
                }
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
        }
        if (sender.hasPermission("eternalitemcore.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/eicore give <player> <core_id>");
            sender.sendMessage(ChatColor.YELLOW + "/eicore setlevel <player> <stat_id> <level>");
            sender.sendMessage(ChatColor.YELLOW + "/eicore addstat <player> <stat_id> <amount>");
        }
    }
}
