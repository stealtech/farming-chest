package com.codegen.FarmingChest.commands;

import com.codegen.FarmingChest.FarmingChest;
import com.codegen.FarmingChest.utils.ChatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class FarmingChestCommand implements CommandExecutor, TabCompleter {
    private final FarmingChest plugin;

    public FarmingChestCommand(FarmingChest plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            Player player = (Player) sender;
            player.sendMessage(ChatUtil.format("&cPlease right-click a farming chest to open the menu."));
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "give":
                handleGiveCommand(sender, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }
        return true;
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("farmingchest.give")) {
            sender.sendMessage(ChatUtil.format("&cYou do not have permission to use this command."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatUtil.format("&cUsage: /farmingchest give <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatUtil.format("&cPlayer not found."));
            return;
        }
        this.plugin.getChestManager().giveFarmingChest(target);
        sender.sendMessage(ChatUtil.format("&aYou have given a Farming Chest to " + target.getName() + "."));
        target.sendMessage(ChatUtil.format("&aYou have received a Farming Chest."));
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("farmingchest.reload")) {
            sender.sendMessage(ChatUtil.format("&cYou do not have permission to use this command."));
        } else {
            this.plugin.reload();
            sender.sendMessage(ChatUtil.format("&aFarmingChest configuration has been reloaded."));
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatUtil.format("&8&m----------------------------------"));
        sender.sendMessage(ChatUtil.format("&6&lFarmingChest Help"));
        sender.sendMessage(ChatUtil.format(""));
        sender.sendMessage(ChatUtil.format("&e/fc give <player> &8- Gives a farming chest to a player."));
        sender.sendMessage(ChatUtil.format("&e/fc reload &8- Reloads the plugin configuration."));
        sender.sendMessage(ChatUtil.format("&8&m----------------------------------"));
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("farmingchest.give")) {
                subCommands.add("give");
            }
            if (sender.hasPermission("farmingchest.reload")) {
                subCommands.add("reload");
            }
            return (List) subCommands.stream().filter(s -> {
                return s.startsWith(args[0].toLowerCase());
            }).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("farmingchest.give")) {
            return (List) Bukkit.getOnlinePlayers().stream().map((v0) -> {
                return v0.getName();
            }).filter(name -> {
                return name.toLowerCase().startsWith(args[1].toLowerCase());
            }).collect(Collectors.toList());
        }
        return new ArrayList();
    }
}
