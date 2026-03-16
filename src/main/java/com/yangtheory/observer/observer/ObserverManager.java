package com.yangtheory.observer.observer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ObserverManager {
    private final Map<UUID, UUID> observerTargets = new ConcurrentHashMap<>();

    public void startObserving(UUID adminId, UUID targetId) {
        observerTargets.put(adminId, targetId);
    }

    public boolean stopObserving(UUID adminId) {
        return observerTargets.remove(adminId) != null;
    }

    public List<UUID> stopObservingTarget(UUID targetId) {
        List<UUID> affectedAdmins = new ArrayList<>();

        for (Map.Entry<UUID, UUID> entry : observerTargets.entrySet()) {
            UUID adminId = entry.getKey();
            UUID observedTargetId = entry.getValue();
            if (!observedTargetId.equals(targetId)) {
                continue;
            }
            if (observerTargets.remove(adminId, observedTargetId)) {
                affectedAdmins.add(adminId);
            }
        }

        return affectedAdmins;
    }

    public void notifyObservers(Player target, Location location) {
        String world = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        String message = String.format(
                "[Observe] %s -> %s %d,%d,%d",
                target.getName(),
                world,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );

        for (Map.Entry<UUID, UUID> entry : observerTargets.entrySet()) {
            if (!entry.getValue().equals(target.getUniqueId())) {
                continue;
            }

            Player admin = Bukkit.getPlayer(entry.getKey());
            if (admin == null || !admin.isOnline()) {
                continue;
            }

            admin.sendMessage(message);
        }
    }
}
