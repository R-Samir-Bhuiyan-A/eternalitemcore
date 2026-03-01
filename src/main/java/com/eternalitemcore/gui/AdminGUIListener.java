package com.eternalitemcore.gui;

import com.eternalitemcore.EternalItemCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminGUIListener implements Listener {

    private final EternalItemCore plugin;
    private final Map<UUID, String> chatPrompts = new HashMap<>();

    public AdminGUIListener(EternalItemCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("Item Mastery: Admin Panel")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            
            if (name.equals("Edit Stat Cores")) {
                plugin.getAdminGUIManager().openStatCoresMenu(player);
            } else if (name.equals("Edit Abilities")) {
                plugin.getAdminGUIManager().openAbilitiesMenu(player);
            }
        } 
        else if (event.getView().getTitle().contains("Admin: Edit Stat Cores")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            if (name.equals("Back to Main Menu")) {
                plugin.getAdminGUIManager().openMainMenu(player);
            } else if (name.equals("Create New Core")) {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), "CREATE_CORE");
                player.sendMessage(ChatColor.YELLOW + "Please type the new Core ID (e.g. MY_CUSTOM_CORE) in chat:");
            } else if (event.getCurrentItem().getItemMeta().getLore() != null) {
                // Parse ID from lore
                String loreLine = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0));
                if (loreLine.startsWith("ID: ")) {
                    String coreId = loreLine.substring(4);
                    plugin.getAdminGUIManager().openCoreEditMenu(player, coreId);
                }
            }
        }
        else if (event.getView().getTitle().contains("Admin: Edit Abilities")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            if (name.equals("Back to Main Menu")) {
                plugin.getAdminGUIManager().openMainMenu(player);
            } else if (name.equals("Create New Ability")) {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), "CREATE_ABILITY");
                player.sendMessage(ChatColor.YELLOW + "Please type the new Ability ID in chat:");
            } else if (event.getCurrentItem().getItemMeta().getLore() != null) {
                String loreLine = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0));
                if (loreLine.startsWith("ID: ")) {
                    String abilityId = loreLine.substring(4);
                    plugin.getAdminGUIManager().openAbilityEditMenu(player, abilityId);
                }
            }
        }
        else if (event.getView().getTitle().contains("Editing: ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            String title = ChatColor.stripColor(event.getView().getTitle());
            String id = title.replace("Editing: ", "");
            String action = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            
            if (action.equals("Back to Cores")) {
                plugin.getAdminGUIManager().openStatCoresMenu(player);
            } else if (action.equals("Back to Abilities")) {
                plugin.getAdminGUIManager().openAbilitiesMenu(player);
            } else if (action.equals("Edit Display Name")) {
                player.closeInventory();
                boolean isCore = plugin.getConfig().contains("stat-cores." + id);
                chatPrompts.put(player.getUniqueId(), "EDIT_NAME:" + (isCore ? "stat-cores" : "ability-cores") + ":" + id);
                player.sendMessage(ChatColor.YELLOW + "Type the new Display Name in chat (You can use & color codes).");
                player.sendMessage(ChatColor.GRAY + "[Type 'cancel' anytime to abort]");
            } else if (action.equals("Edit Material")) {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), "EDIT_MAT:stat-cores:" + id);
                player.sendMessage(ChatColor.YELLOW + "Type the new Material enum in chat (e.g. DIAMOND_SWORD).");
                player.sendMessage(ChatColor.GRAY + "[Type 'cancel' anytime to abort]");
            } else if (action.equals("Edit Cooldown")) {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), "EDIT_COOLDOWN:ability-cores:" + id);
                player.sendMessage(ChatColor.YELLOW + "Type the new Cooldown in seconds (e.g. 5.5).");
                player.sendMessage(ChatColor.GRAY + "[Type 'cancel' anytime to abort]");
            } else if (action.equals("Edit Damage")) {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), "EDIT_DAMAGE:ability-cores:" + id);
                player.sendMessage(ChatColor.YELLOW + "Type the new Damage value (e.g. 10.0).");
                player.sendMessage(ChatColor.GRAY + "[Type 'cancel' anytime to abort]");
            } else if (action.equals("Edit Durability Cost")) {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), "EDIT_DURABILITY:ability-cores:" + id);
                player.sendMessage(ChatColor.YELLOW + "Type the Durability Cost (e.g. 5) (or 0 to disable).");
                player.sendMessage(ChatColor.GRAY + "[Type 'cancel' anytime to abort]");
            } else if (action.equals("Edit Keybind Trigger")) {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), "EDIT_TRIGGER:ability-cores:" + id);
                player.sendMessage(ChatColor.YELLOW + "Type the new Trigger (RIGHT_CLICK, LEFT_CLICK, SNEAK, SWAP_HANDS, DROP_ITEM, BOW_SHOOT).");
                player.sendMessage(ChatColor.GRAY + "[Type 'cancel' anytime to abort]");
            } else if (action.equals("Edit Self-Debuffs")) {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), "EDIT_DEBUFF:ability-cores:" + id);
                player.sendMessage(ChatColor.YELLOW + "Type Debuff and Duration (e.g. SLOWNESS:3), or type 'clear'.");
                player.sendMessage(ChatColor.GRAY + "[Type 'cancel' anytime to abort]");
            } else if (action.equals("Edit Level Path")) {
                plugin.getAdminGUIManager().openCoreLevelMenu(player, id);
            }
        }
        else if (event.getView().getTitle().startsWith(ChatColor.GOLD + "Levels: ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            String title = ChatColor.stripColor(event.getView().getTitle());
            String coreId = title.replace("Levels: ", "");
            String action = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            if (action.startsWith("Back to Editing")) {
                plugin.getAdminGUIManager().openCoreEditMenu(player, coreId);
            } else if (action.contains("XP")) {
                int levelNum = Integer.parseInt(action.replaceAll("[^0-9]", ""));
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), "EDIT_XP:" + coreId + ":" + levelNum);
                player.sendMessage(ChatColor.YELLOW + "Type the new REQUIRED XP number for Level " + levelNum + " (or type 'formula' to use default math).");
                player.sendMessage(ChatColor.GRAY + "[Type 'cancel' anytime to abort]");
            } else if (action.contains("Death Msg")) {
                int levelNum = Integer.parseInt(action.replaceAll("[^0-9]", ""));
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), "EDIT_DEATH_MSG:" + coreId + ":" + levelNum);
                player.sendMessage(ChatColor.YELLOW + "Type the Custom Death Broadcast (use %killer% and %victim%) (or 'clear' to remove).");
                player.sendMessage(ChatColor.GRAY + "[Type 'cancel' anytime to abort]");
            } else if (action.contains("Kill Effect")) {
                int levelNum = Integer.parseInt(action.replaceAll("[^0-9]", ""));
                plugin.getAdminGUIManager().openAbilitySelectorMenu(player, coreId, levelNum);
            }
        }
        else if (event.getView().getTitle().startsWith(ChatColor.DARK_PURPLE + "Select Ability: ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            String title = ChatColor.stripColor(event.getView().getTitle());
            String[] parts = title.replace("Select Ability: ", "").split(" Lvl ");
            if (parts.length != 2) return;
            
            String coreId = parts[0];
            int levelNum = Integer.parseInt(parts[1]);
            String action = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            if (action.startsWith("Back to Levels")) {
                plugin.getAdminGUIManager().openCoreLevelMenu(player, coreId);
            } else if (action.equals("Clear Ability")) {
                plugin.getConfig().set("stats." + coreId + ".levels." + levelNum + ".ability-unlock", null);
                plugin.saveConfig();
                plugin.getConfigManager().loadConfig();
                player.sendMessage(ChatColor.GREEN + "Ability cleared from Level " + levelNum + "!");
                plugin.getAdminGUIManager().openCoreLevelMenu(player, coreId);
            } else {
                List<String> lore = event.getCurrentItem().getItemMeta().getLore();
                if (lore != null && !lore.isEmpty()) {
                    String loreLine = ChatColor.stripColor(lore.get(0));
                    if (loreLine.startsWith("ID: ")) {
                        String abilityId = loreLine.substring(4);
                        plugin.getConfig().set("stats." + coreId + ".levels." + levelNum + ".ability-unlock", abilityId);
                        plugin.saveConfig();
                        plugin.getConfigManager().loadConfig();
                        player.sendMessage(ChatColor.GREEN + "Bound " + abilityId + " to Level " + levelNum + "!");
                        plugin.getAdminGUIManager().openCoreLevelMenu(player, coreId);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (chatPrompts.containsKey(player.getUniqueId())) {
            event.setCancelled(true); // Stop it from broadcasting to the server
            String input = event.getMessage();
            String context = chatPrompts.remove(player.getUniqueId());
            
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Action cancelled.");
                return;
            }

            // Must jump back to main thread to edit config and open GUI
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (context.startsWith("CREATE_CORE")) {
                    String id = input.toUpperCase().replace(" ", "_");
                    plugin.getConfig().set("stat-cores." + id + ".material", "STONE");
                    plugin.getConfig().set("stat-cores." + id + ".name", "&f" + input);
                    plugin.saveConfig();
                    plugin.getAdminGUIManager().openCoreEditMenu(player, id);
                } else if (context.startsWith("CREATE_ABILITY")) {
                    String id = input.toUpperCase().replace(" ", "_");
                    plugin.getConfig().set("ability-cores." + id + ".type", "ACTIVE");
                    plugin.getConfig().set("ability-cores." + id + ".display", "&f" + input);
                    plugin.saveConfig();
                    plugin.getAdminGUIManager().openAbilityEditMenu(player, id);
                } else {
                    String[] parts = context.split(":");
                    String action = parts[0];
                    String section = parts[1];
                    String id = parts[2];
                    String path = section + "." + id;
                    
                    if (action.equals("EDIT_NAME")) {
                        plugin.getConfig().set(path + (section.equals("stat-cores") ? ".name" : ".display"), input);
                    } else if (action.equals("EDIT_MAT")) {
                        plugin.getConfig().set(path + ".material", input.toUpperCase());
                    } else if (action.equals("EDIT_COOLDOWN")) {
                        try { plugin.getConfig().set(path + ".cooldown", Double.parseDouble(input)); } catch(Exception e){}
                    } else if (action.equals("EDIT_DAMAGE")) {
                        try { plugin.getConfig().set(path + ".damage", Double.parseDouble(input)); } catch(Exception e){}
                    } else if (action.equals("EDIT_DURABILITY")) {
                        try { plugin.getConfig().set(path + ".durability-cost", Integer.parseInt(input)); } catch(Exception e){}
                    } else if (action.equals("EDIT_TRIGGER")) {
                        plugin.getConfig().set(path + ".trigger", input.toUpperCase());
                    } else if (action.equals("EDIT_DEBUFF")) {
                        if (input.equalsIgnoreCase("clear")) {
                            plugin.getConfig().set(path + ".self-effects", null);
                        } else {
                            String[] debugParts = input.split(":");
                            if (debugParts.length == 2) {
                                String efKey = "DEBUFF_" + System.currentTimeMillis();
                                plugin.getConfig().set(path + ".self-effects." + efKey + ".type", debugParts[0].toUpperCase());
                                try {
                                    plugin.getConfig().set(path + ".self-effects." + efKey + ".duration", Integer.parseInt(debugParts[1]) * 20); // ticks
                                } catch (Exception ignored) {}
                                plugin.getConfig().set(path + ".self-effects." + efKey + ".amplifier", 1);
                            }
                        }
                    } else if (action.equals("EDIT_XP")) {
                        String core = section;
                        int lvl = Integer.parseInt(id);
                        if (input.equalsIgnoreCase("formula")) {
                            plugin.getConfig().set("stats." + core + ".levels." + lvl + ".required-xp", null);
                        } else {
                            try { plugin.getConfig().set("stats." + core + ".levels." + lvl + ".required-xp", Integer.parseInt(input)); } catch(Exception e){}
                        }
                    } else if (action.equals("EDIT_DEATH_MSG")) {
                        String core = section;
                        int lvl = Integer.parseInt(id);
                        if (input.equalsIgnoreCase("clear")) {
                            plugin.getConfig().set("stats." + core + ".levels." + lvl + ".death-message", null);
                        } else {
                            plugin.getConfig().set("stats." + core + ".levels." + lvl + ".death-message", input);
                        }
                    } else if (action.equals("EDIT_KILL_EFFECT")) {
                        String core = section;
                        int lvl = Integer.parseInt(id);
                        if (input.equalsIgnoreCase("clear")) {
                            plugin.getConfig().set("stats." + core + ".levels." + lvl + ".ability-unlock", null);
                        } else {
                            plugin.getConfig().set("stats." + core + ".levels." + lvl + ".ability-unlock", input.toUpperCase());
                        }
                    }
                    
                    plugin.saveConfig();
                    plugin.getConfigManager().loadConfig(); // Refresh memory
                    
                    if (action.startsWith("EDIT_XP") || action.startsWith("EDIT_DEATH_MSG") || action.startsWith("EDIT_KILL_EFFECT")) {
                        plugin.getAdminGUIManager().openCoreLevelMenu(player, section);
                    } else if (section.equals("stat-cores")) {
                        plugin.getAdminGUIManager().openCoreEditMenu(player, id);
                    } else {
                        plugin.getAdminGUIManager().openAbilityEditMenu(player, id);
                    }
                }
                player.sendMessage(ChatColor.GREEN + "Configuration updated!");
            });
        }
    }
}
