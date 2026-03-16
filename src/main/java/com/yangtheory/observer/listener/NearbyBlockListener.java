package com.yangtheory.observer.listener;

import com.yangtheory.observer.nearby.NearbyBlockService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class NearbyBlockListener implements Listener {
    private final NearbyBlockService nearbyBlockService;

    public NearbyBlockListener(NearbyBlockService nearbyBlockService) {
        this.nearbyBlockService = nearbyBlockService;
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

        nearbyBlockService.handleMove(event.getPlayer());
    }
}
