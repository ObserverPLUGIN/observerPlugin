package com.yangtheory.observer.web;

import com.google.gson.Gson;
import com.yangtheory.observer.log.BlockLogEntry;
import com.yangtheory.observer.snapshot.PlayerNearbySnapshot;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.time.Duration;

public class WebBridge {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final JavaPlugin plugin;
    private final boolean logEnabled;
    private final String logEndpoint;
    private final boolean snapshotEnabled;
    private final String snapshotEndpoint;
    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public WebBridge(
            JavaPlugin plugin,
            boolean webEnabled,
            String logEndpoint,
            boolean snapshotEnabled,
            String snapshotEndpoint,
            long timeoutMs
    ) {
        this.plugin = plugin;
        this.logEndpoint = logEndpoint == null ? "" : logEndpoint.trim();
        this.snapshotEndpoint = snapshotEndpoint == null ? "" : snapshotEndpoint.trim();
        this.logEnabled = webEnabled && !this.logEndpoint.isEmpty();
        this.snapshotEnabled = webEnabled && snapshotEnabled && !this.snapshotEndpoint.isEmpty();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .writeTimeout(Duration.ofMillis(timeoutMs))
                .build();

        if (!this.logEnabled) {
            plugin.getLogger().info("Block log bridge is disabled. Set web.enabled=true and web.endpoint in config.yml.");
        }

        if (!this.snapshotEnabled) {
            plugin.getLogger().info("Snapshot bridge is disabled. Set web.snapshot-enabled=true and web.snapshot-endpoint in config.yml.");
        }
    }

    public void sendBlockLog(BlockLogEntry entry) {
        if (!logEnabled) {
            return;
        }

        RequestBody body = RequestBody.create(entry.toJson(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(logEndpoint)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                plugin.getLogger().warning("Failed to send block log to web endpoint: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful()) {
                        plugin.getLogger().warning(
                                "Web endpoint returned non-success status: " + response.code()
                        );
                    }
                }
            }
        });
    }

    public void sendPlayerSnapshot(PlayerNearbySnapshot snapshot) {
        if (!snapshotEnabled) {
            return;
        }

        RequestBody body = RequestBody.create(gson.toJson(snapshot), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(snapshotEndpoint)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                plugin.getLogger().warning("Failed to send player snapshot to web endpoint: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful()) {
                        plugin.getLogger().warning(
                                "Snapshot endpoint returned non-success status: " + response.code()
                        );
                    }
                }
            }
        });
    }

    public void close() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        if (client.cache() != null) {
            try {
                client.cache().close();
            } catch (IOException ignored) {
                // Ignore cache close errors while shutting down.
            }
        }
    }
}
