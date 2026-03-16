package com.yangtheory.observer.listener;

import com.yangtheory.observer.log.LogService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {
    private final LogService logService;

    public BlockPlaceListener(LogService logService) {
        this.logService = logService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        logService.logBlockAction(event.getPlayer(), event.getBlockPlaced(), "place");
    }
}
