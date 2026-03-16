package com.yangtheory.observer.web;

import com.yangtheory.observer.log.BlockLogEntry;
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
    private final boolean enabled;
    private final String endpoint;
    private final OkHttpClient client;

    public WebBridge(JavaPlugin plugin, boolean enabled, String endpoint, long timeoutMs) {
        this.plugin = plugin;
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.enabled = enabled && !this.endpoint.isEmpty();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .writeTimeout(Duration.ofMillis(timeoutMs))
                .build();

        if (!this.enabled) {
            plugin.getLogger().info("WebBridge is disabled. Set web.enabled=true and web.endpoint in config.yml.");
        }
    }

    public void sendBlockLog(BlockLogEntry entry) {
        if (!enabled) {
            return;
        }

        RequestBody body = RequestBody.create(entry.toJson(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(endpoint)
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
