package com.yangtheory.observer.command;

import com.yangtheory.observer.log.BlockLogEntry;
import com.yangtheory.observer.log.LogService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlockLogCommand implements CommandExecutor, TabCompleter {
    private static final int DEFAULT_COUNT = 10;

    private final LogService logService;
    private final int maxQueryResults;

    public BlockLogCommand(LogService logService, int maxQueryResults) {
        this.logService = logService;
        this.maxQueryResults = Math.max(1, maxQueryResults);
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission("observer.logs.view")) {
            sender.sendMessage("You do not have permission to view logs.");
            return true;
        }

        if (args.length > 2) {
            sender.sendMessage("Usage: /blocklog [count] [player]");
            return true;
        }

        int requestedCount = DEFAULT_COUNT;
        if (args.length >= 1 && !args[0].isBlank()) {
            try {
                requestedCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                sender.sendMessage("Count must be a number.");
                return true;
            }
        }

        if (requestedCount <= 0) {
            sender.sendMessage("Count must be greater than 0.");
            return true;
        }

        int count = Math.min(requestedCount, maxQueryResults);
        String playerFilter = args.length == 2 ? args[1] : null;
        List<BlockLogEntry> logs = logService.getRecentLogs(count, playerFilter);

        if (logs.isEmpty()) {
            sender.sendMessage("No block logs found.");
            return true;
        }

        sender.sendMessage(String.format(
                "Recent block logs (%d/%d)%s:",
                logs.size(),
                count,
                playerFilter == null || playerFilter.isBlank() ? "" : " for " + playerFilter
        ));
        for (BlockLogEntry entry : logs) {
            sender.sendMessage(formatEntry(entry));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            for (String value : List.of("10", "20", "50")) {
                if (value.startsWith(prefix)) {
                    suggestions.add(value);
                }
            }
            return suggestions;
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    suggestions.add(name);
                }
            }
            return suggestions;
        }

        return List.of();
    }

    private String formatEntry(BlockLogEntry entry) {
        return String.format(
                "[%s] %s %s %s %s %d,%d,%d",
                shortTimestamp(entry.timestamp()),
                entry.player(),
                entry.action(),
                entry.blockType(),
                entry.world(),
                entry.x(),
                entry.y(),
                entry.z()
        );
    }

    private String shortTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return "unknown-time";
        }
        if (timestamp.length() >= 19) {
            return timestamp.substring(11, 19);
        }
        return timestamp;
    }
}
