package com.yangtheory.observer.listener;

import com.yangtheory.observer.nearby.NearbyBlockService;
import com.yangtheory.observer.observer.ObserverManager;
import com.yangtheory.observer.snapshot.NearbySnapshotService;
import com.yangtheory.observer.tracker.PlayerTrackerService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;

public class PlayerQuitCleanupListener implements Listener {
    private final ObserverManager observerManager;
    private final PlayerTrackerService trackerService;
    private final NearbyBlockService nearbyBlockService;
    private final NearbySnapshotService nearbySnapshotService;

    public PlayerQuitCleanupListener(
            ObserverManager observerManager,
            PlayerTrackerService trackerService,
            NearbyBlockService nearbyBlockService,
            NearbySnapshotService nearbySnapshotService
    ) {
        this.observerManager = observerManager;
        this.trackerService = trackerService;
        this.nearbyBlockService = nearbyBlockService;
        this.nearbySnapshotService = nearbySnapshotService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player quittingPlayer = event.getPlayer();
        UUID quittingPlayerId = quittingPlayer.getUniqueId();

        trackerService.removePlayer(quittingPlayerId);
        nearbyBlockService.clearPlayer(quittingPlayerId);
        nearbySnapshotService.clearPlayer(quittingPlayerId);
        observerManager.stopObserving(quittingPlayerId);

        List<UUID> affectedAdmins = observerManager.stopObservingTarget(quittingPlayerId);
        if (affectedAdmins.isEmpty()) {
            return;
        }

        for (UUID adminId : affectedAdmins) {
            Player admin = Bukkit.getPlayer(adminId);
            if (admin == null || !admin.isOnline()) {
                continue;
            }
            admin.sendMessage("[Observe] Target " + quittingPlayer.getName() + " went offline.");
        }
    }
}
