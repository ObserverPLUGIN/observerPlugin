package com.yangtheory.observer.listener;

import com.yangtheory.observer.tracker.PlayerTrackerService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {
    private final PlayerTrackerService trackerService;

    public PlayerMoveListener(PlayerTrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        trackerService.handleMove(event);
    }
}
