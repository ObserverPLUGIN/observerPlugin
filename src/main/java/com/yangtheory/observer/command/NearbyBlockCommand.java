package com.yangtheory.observer.command;

import com.yangtheory.observer.nearby.NearbyBlockMatch;
import com.yangtheory.observer.nearby.NearbyBlockService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NearbyBlockCommand implements CommandExecutor, TabCompleter {
    private final NearbyBlockService nearbyBlockService;

    public NearbyBlockCommand(NearbyBlockService nearbyBlockService) {
        this.nearbyBlockService = nearbyBlockService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /nearblock <check|reload> [player]");
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(subCommand)) {
            if (!sender.hasPermission("observer.nearblock.reload")) {
                sender.sendMessage("You do not have permission to reload near block config.");
                return true;
            }

            nearbyBlockService.reload();
            sender.sendMessage("Nearby block config reloaded.");
            return true;
        }

        if (!"check".equals(subCommand)) {
            sender.sendMessage("Usage: /nearblock <check|reload> [player]");
            return true;
        }

        if (!sender.hasPermission("observer.nearblock.check")) {
            sender.sendMessage("You do not have permission to check nearby blocks.");
            return true;
        }

        Player target = resolveTarget(sender, args);
        if (target == null) {
            return true;
        }

        if (!nearbyBlockService.isEnabled()) {
            sender.sendMessage("Nearby block detection is disabled in config.");
            return true;
        }

        NearbyBlockMatch match = nearbyBlockService.checkNow(target);
        if (match == null) {
            sender.sendMessage("No configured blocks found near " + target.getName() + ".");
            return true;
        }

        Location location = match.block().getLocation();
        sender.sendMessage(String.format(
                "Found %s near %s at %s %d,%d,%d within radius %d.",
                match.material().name(),
                target.getName(),
                location.getWorld() == null ? "unknown" : location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                nearbyBlockService.getRadius()
        ));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (String subCommand : List.of("check", "reload")) {
                if (subCommand.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    suggestions.add(subCommand);
                }
            }
            return suggestions;
        }

        if (args.length == 2 && "check".equalsIgnoreCase(args[0])) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    private Player resolveTarget(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Target player is offline or not found.");
            }
            return target;
        }

        if (sender instanceof Player player) {
            return player;
        }

        sender.sendMessage("Console must specify a player: /nearblock check <player>");
        return null;
    }
}
