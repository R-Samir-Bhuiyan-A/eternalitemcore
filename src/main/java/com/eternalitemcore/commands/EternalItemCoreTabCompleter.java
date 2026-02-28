package com.eternalitemcore.commands;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EternalItemCoreTabCompleter implements TabCompleter {

    private final EternalItemCore plugin;

    public EternalItemCoreTabCompleter(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("eternalitemcore.admin") && !sender.hasPermission("eternalitemcore.player")) {
            return completions;
        }

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("eternalitemcore.admin")) {
                subCommands.addAll(Arrays.asList("give", "setlevel", "addstat", "clearstats", "reload"));
            }
            if (sender.hasPermission("eternalitemcore.player")) {
                subCommands.addAll(Arrays.asList("toggleeffects", "togglebroadcast", "viewstats"));
            }
            completions.addAll(subCommands.stream()
                    .filter(c -> c.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));
        } else if (args.length == 2 && sender.hasPermission("eternalitemcore.admin") && !args[0].equalsIgnoreCase("reload")) {
            completions.addAll(plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList()));
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give")) {
                completions.addAll(plugin.getConfig().getConfigurationSection("stat-cores").getKeys(false).stream()
                        .filter(c -> c.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList()));
            } else if (sub.equals("setlevel") || sub.equals("addstat")) {
                completions.addAll(plugin.getConfig().getConfigurationSection("stats").getKeys(false).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setlevel") || sub.equals("addstat")) {
                completions.addAll(Arrays.asList("1", "10", "100", "500", "1000").stream()
                        .filter(c -> c.startsWith(args[3]))
                        .collect(Collectors.toList()));
            }
        }

        return completions;
    }
}
