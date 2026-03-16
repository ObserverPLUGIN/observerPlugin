package com.yangtheory.observer.tracker;

import com.yangtheory.observer.observer.ObserverManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTrackerService {
    private final JavaPlugin plugin;
    private final ObserverManager observerManager;
    private final long trackingIntervalMs;
    private final Map<UUID, Long> lastTrackedAt = new ConcurrentHashMap<>();

    public PlayerTrackerService(JavaPlugin plugin, ObserverManager observerManager, long trackingIntervalMs) {
        this.plugin = plugin;
        this.observerManager = observerManager;
        this.trackingIntervalMs = trackingIntervalMs;
    }

    public void handleMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || sameBlock(event.getFrom(), to)) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        long now = System.currentTimeMillis();
        Long lastTracked = lastTrackedAt.get(playerId);
        if (lastTracked != null && now - lastTracked < trackingIntervalMs) {
            return;
        }

        lastTrackedAt.put(playerId, now);

        String world = to.getWorld() == null ? "unknown" : to.getWorld().getName();
        plugin.getLogger().info(String.format(
                "[Track] %s -> %s %d,%d,%d",
                player.getName(),
                world,
                to.getBlockX(),
                to.getBlockY(),
                to.getBlockZ()
        ));

        observerManager.notifyObservers(player, to);
    }

    public void removePlayer(UUID playerId) {
        lastTrackedAt.remove(playerId);
    }

    private boolean sameBlock(Location from, Location to) {
        return from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()
                && from.getWorld() != null
                && from.getWorld().equals(to.getWorld());
    }
}
