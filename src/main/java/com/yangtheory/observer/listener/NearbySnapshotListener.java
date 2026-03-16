package com.yangtheory.observer.listener;

import com.yangtheory.observer.snapshot.NearbySnapshotService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class NearbySnapshotListener implements Listener {
    private final NearbySnapshotService nearbySnapshotService;

    public NearbySnapshotListener(NearbySnapshotService nearbySnapshotService) {
        this.nearbySnapshotService = nearbySnapshotService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        nearbySnapshotService.handleMove(event.getPlayer());
    }
}
