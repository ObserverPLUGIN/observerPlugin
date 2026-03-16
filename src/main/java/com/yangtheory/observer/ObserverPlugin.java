package com.yangtheory.observer;

import com.yangtheory.observer.command.BlockLogCommand;
import com.yangtheory.observer.command.NearbyBlockCommand;
import com.yangtheory.observer.command.ObserveCommand;
import com.yangtheory.observer.listener.BlockBreakListener;
import com.yangtheory.observer.listener.BlockPlaceListener;
import com.yangtheory.observer.listener.NearbyBlockListener;
import com.yangtheory.observer.listener.PlayerMoveListener;
import com.yangtheory.observer.listener.PlayerQuitCleanupListener;
import com.yangtheory.observer.log.LogService;
import com.yangtheory.observer.nearby.NearbyBlockService;
import com.yangtheory.observer.observer.ObserverManager;
import com.yangtheory.observer.tracker.PlayerTrackerService;
import com.yangtheory.observer.web.WebBridge;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ObserverPlugin extends JavaPlugin {
    private ObserverManager observerManager;
    private PlayerTrackerService trackerService;
    private LogService logService;
    private WebBridge webBridge;
    private NearbyBlockService nearbyBlockService;
    private int maxBlockLogQueryResults;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        long trackingIntervalMs = getConfig().getLong("tracking.interval-ms", 1000L);
        boolean webEnabled = getConfig().getBoolean("web.enabled", false);
        String endpoint = getConfig().getString("web.endpoint", "");
        long timeoutMs = getConfig().getLong("web.timeout-ms", 2000L);
        int maxStoredLogs = getConfig().getInt("logs.memory-limit", 5000);
        maxBlockLogQueryResults = getConfig().getInt("logs.query-max-results", 50);

        observerManager = new ObserverManager();
        trackerService = new PlayerTrackerService(this, observerManager, trackingIntervalMs);
        webBridge = new WebBridge(this, webEnabled, endpoint, timeoutMs);
        logService = new LogService(this, webBridge, maxStoredLogs);
        nearbyBlockService = new NearbyBlockService(this);

        registerListeners();
        registerCommands();

        getLogger().info("ObserverPlugin enabled.");
    }

    @Override
    public void onDisable() {
        if (webBridge != null) {
            webBridge.close();
        }
        getLogger().info("ObserverPlugin disabled.");
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerMoveListener(trackerService), this);
        pluginManager.registerEvents(new NearbyBlockListener(nearbyBlockService), this);
        pluginManager.registerEvents(new BlockPlaceListener(logService), this);
        pluginManager.registerEvents(new BlockBreakListener(logService), this);
        pluginManager.registerEvents(
                new PlayerQuitCleanupListener(observerManager, trackerService, nearbyBlockService),
                this
        );
    }

    private void registerCommands() {
        PluginCommand observeCommand = getCommand("observe");
        if (observeCommand == null) {
            getLogger().warning("Command 'observe' is missing from plugin.yml.");
        } else {
            ObserveCommand command = new ObserveCommand(observerManager);
            observeCommand.setExecutor(command);
            observeCommand.setTabCompleter(command);
        }

        PluginCommand blockLogCommand = getCommand("blocklog");
        if (blockLogCommand == null) {
            getLogger().warning("Command 'blocklog' is missing from plugin.yml.");
        } else {
            BlockLogCommand logsCommand = new BlockLogCommand(logService, maxBlockLogQueryResults);
            blockLogCommand.setExecutor(logsCommand);
            blockLogCommand.setTabCompleter(logsCommand);
        }

        PluginCommand nearBlockCommand = getCommand("nearblock");
        if (nearBlockCommand == null) {
            getLogger().warning("Command 'nearblock' is missing from plugin.yml.");
        } else {
            NearbyBlockCommand command = new NearbyBlockCommand(nearbyBlockService);
            nearBlockCommand.setExecutor(command);
            nearBlockCommand.setTabCompleter(command);
        }
    }
}
