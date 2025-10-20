package com.example.pickandthrow;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private final PickAndThrowPlugin plugin;
    
    public CommandHandler(PickAndThrowPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();
        
        if (args.length == 0) {
            String version = plugin.getDescription().getVersion();
            List<String> authors = plugin.getDescription().getAuthors();
            String author = authors.isEmpty() ? "Unknown" : String.join(", ", authors);
            
            sender.sendMessage(lang.getMessage("command.plugin-info", "version", version) + " §7by §f" + author);
            sender.sendMessage(lang.getMessage("command.help"));
            sender.sendMessage(lang.getMessage("command.help-reload"));
            sender.sendMessage(lang.getMessage("command.help-sethand"));
            return true;
        }
        
        String subCmd = args[0].toLowerCase();
        
        if (subCmd.equals("reload")) {
            if (!sender.hasPermission("pickandthrow.admin")) {
                sender.sendMessage(lang.getMessage("command.no-permission"));
                return true;
            }
            
            plugin.reloadConfig();
            sender.sendMessage(lang.getMessage("command.reload-success"));
            
            return true;
        }
        
        if (subCmd.equals("sethand")) {
            if (!sender.hasPermission("pickandthrow.admin")) {
                sender.sendMessage(lang.getMessage("command.no-permission"));
                return true;
            }
            
            if (!(sender instanceof Player)) {
                sender.sendMessage(lang.getMessage("command.player-only"));
                return true;
            }
            
            Player player = (Player) sender;
            ItemStack item = player.getInventory().getItemInMainHand();
            
            if (item == null || item.getType() == Material.AIR) {
                // Clear custom item, back to empty hand
                plugin.getItemManager().setItem(null);
                sender.sendMessage(lang.getMessage("command.hand-cleared"));
            } else {
                // Set custom item
                plugin.getItemManager().setItem(item);
                sender.sendMessage(lang.getMessage("command.hand-success"));
            }
            
            return true;
        }
        
        sender.sendMessage(lang.getMessage("command.unknown-command"));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("pickandthrow.admin")) {
                List<String> subCommands = Arrays.asList("reload", "sethand");
                String arg = args[0].toLowerCase();
                for (String sub : subCommands) {
                    if (sub.startsWith(arg)) {
                        completions.add(sub);
                    }
                }
            }
        }
        
        return completions;
    }
}
