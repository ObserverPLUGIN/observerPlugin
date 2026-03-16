package com.yangtheory.observer.command;

import com.yangtheory.observer.observer.ObserverManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ObserveCommand implements CommandExecutor, TabCompleter {
    private final ObserverManager observerManager;

    public ObserveCommand(ObserverManager observerManager) {
        this.observerManager = observerManager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /observe <player|off>");
            return true;
        }

        if ("off".equalsIgnoreCase(args[0])) {
            boolean stopped = observerManager.stopObserving(admin.getUniqueId());
            if (stopped) {
                sender.sendMessage("Observe mode disabled.");
            } else {
                sender.sendMessage("You are not observing anyone.");
            }
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Target player is offline or not found.");
            return true;
        }

        UUID adminId = admin.getUniqueId();
        UUID targetId = target.getUniqueId();
        if (adminId.equals(targetId)) {
            sender.sendMessage("You cannot observe yourself.");
            return true;
        }

        observerManager.startObserving(adminId, targetId);
        sender.sendMessage("Now observing: " + target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();

        if ("off".startsWith(prefix)) {
            suggestions.add("off");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(name);
            }
        }

        return suggestions;
    }
}
