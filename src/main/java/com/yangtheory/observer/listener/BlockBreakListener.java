package com.yangtheory.observer.listener;

import com.yangtheory.observer.log.LogService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    private final LogService logService;

    public BlockBreakListener(LogService logService) {
        this.logService = logService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        logService.logBlockAction(event.getPlayer(), event.getBlock(), "break");
    }
}
