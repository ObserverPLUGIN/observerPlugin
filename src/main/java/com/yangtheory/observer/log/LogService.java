package com.yangtheory.observer.log;

import com.yangtheory.observer.web.WebBridge;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LogService {
    private final JavaPlugin plugin;
    private final WebBridge webBridge;
    private final Deque<BlockLogEntry> recentLogs = new ConcurrentLinkedDeque<>();
    private final int maxStoredLogs;

    public LogService(JavaPlugin plugin, WebBridge webBridge, int maxStoredLogs) {
        this.plugin = plugin;
        this.webBridge = webBridge;
        this.maxStoredLogs = Math.max(100, maxStoredLogs);
    }

    public void logBlockAction(Player player, Block block, String action) {
        Location location = block.getLocation();
        String world = location.getWorld() == null ? "unknown" : location.getWorld().getName();

        BlockLogEntry entry = new BlockLogEntry(
                player.getName(),
                world,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                block.getType().name(),
                Instant.now().toString(),
                action
        );
        store(entry);

        plugin.getLogger().info(String.format(
                "[BlockLog] player=%s action=%s block=%s world=%s x=%d y=%d z=%d",
                entry.player(),
                entry.action(),
                entry.blockType(),
                entry.world(),
                entry.x(),
                entry.y(),
                entry.z()
        ));

        webBridge.sendBlockLog(entry);
    }

    public List<BlockLogEntry> getRecentLogs(int limit, String playerFilter) {
        int safeLimit = Math.max(1, limit);
        String normalizedFilter = normalize(playerFilter);
        List<BlockLogEntry> result = new ArrayList<>(safeLimit);

        for (BlockLogEntry entry : recentLogs) {
            if (normalizedFilter != null && !Objects.equals(normalize(entry.player()), normalizedFilter)) {
                continue;
            }
            result.add(entry);
            if (result.size() >= safeLimit) {
                break;
            }
        }

        return result;
    }

    private void store(BlockLogEntry entry) {
        recentLogs.addFirst(entry);
        while (recentLogs.size() > maxStoredLogs) {
            recentLogs.pollLast();
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
