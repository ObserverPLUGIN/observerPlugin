package com.yangtheory.observer.snapshot;

import com.yangtheory.observer.nearby.NearbyBlockService;
import com.yangtheory.observer.web.WebBridge;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NearbySnapshotService {
    private final JavaPlugin plugin;
    private final WebBridge webBridge;
    private final NearbyBlockService nearbyBlockService;
    private final boolean enabled;
    private final String serverName;
    private final int radius;
    private final long intervalMs;
    private final Map<UUID, Long> lastSentAt = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSnapshotSignatures = new ConcurrentHashMap<>();

    public NearbySnapshotService(
            JavaPlugin plugin,
            WebBridge webBridge,
            NearbyBlockService nearbyBlockService,
            boolean enabled,
            String serverName,
            int radius,
            long intervalMs
    ) {
        this.plugin = plugin;
        this.webBridge = webBridge;
        this.nearbyBlockService = nearbyBlockService;
        this.enabled = enabled;
        this.serverName = serverName;
        this.radius = Math.max(1, radius);
        this.intervalMs = Math.max(250L, intervalMs);
    }

    public void handleMove(Player player) {
        if (!enabled) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastSent = lastSentAt.get(playerId);
        if (lastSent != null && now - lastSent < intervalMs) {
            return;
        }

        PlayerNearbySnapshot snapshot = capture(player);
        if (snapshot == null) {
            return;
        }

        String signature = snapshot.signature();
        if (signature.equals(lastSnapshotSignatures.get(playerId))) {
            lastSentAt.put(playerId, now);
            return;
        }

        lastSentAt.put(playerId, now);
        lastSnapshotSignatures.put(playerId, signature);
        webBridge.sendPlayerSnapshot(snapshot);
    }

    public void clearPlayer(UUID playerId) {
        lastSentAt.remove(playerId);
        lastSnapshotSignatures.remove(playerId);
    }

    private PlayerNearbySnapshot capture(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        int centerX = location.getBlockX();
        int centerY = location.getBlockY();
        int centerZ = location.getBlockZ();
        Set<Material> highlights = nearbyBlockService.getTargetBlocks();
        List<PlayerNearbySnapshot.Layer> layers = new ArrayList<>(3);

        for (LayerSpec spec : LayerSpec.values()) {
            int absoluteY = centerY + spec.relativeY();
            List<List<PlayerNearbySnapshot.Cell>> rows = new ArrayList<>(radius * 2 + 1);
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                List<PlayerNearbySnapshot.Cell> row = new ArrayList<>(radius * 2 + 1);
                for (int x = centerX - radius; x <= centerX + radius; x++) {
                    row.add(readCell(world, x, absoluteY, z, highlights));
                }
                rows.add(row);
            }

            layers.add(new PlayerNearbySnapshot.Layer(
                    spec.type(),
                    spec.relativeY(),
                    absoluteY,
                    rows
            ));
        }

        return new PlayerNearbySnapshot(
                serverName,
                player.getUniqueId().toString(),
                player.getName(),
                world.getName(),
                resolveDimensionType(world),
                centerX,
                centerY,
                centerZ,
                location.getYaw(),
                location.getPitch(),
                radius,
                Instant.now().toString(),
                layers
        );
    }

    private PlayerNearbySnapshot.Cell readCell(
            World world,
            int blockX,
            int blockY,
            int blockZ,
            Set<Material> highlights
    ) {
        if (blockY < world.getMinHeight() || blockY >= world.getMaxHeight()) {
            return new PlayerNearbySnapshot.Cell("OUT_OF_WORLD", blockX, blockY, blockZ, false, false);
        }

        if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
            return new PlayerNearbySnapshot.Cell("UNLOADED", blockX, blockY, blockZ, false, false);
        }

        Block block = world.getBlockAt(blockX, blockY, blockZ);
        return new PlayerNearbySnapshot.Cell(
                block.getType().name(),
                blockX,
                blockY,
                blockZ,
                highlights.contains(block.getType()),
                true
        );
    }

    private String resolveDimensionType(World world) {
        return switch (world.getEnvironment()) {
            case NORMAL -> "OVERWORLD";
            case NETHER -> "NETHER";
            case THE_END -> "END";
            default -> "CUSTOM";
        };
    }

    private enum LayerSpec {
        FEET("FEET", -1),
        BODY("BODY", 0),
        HEAD("HEAD", 1);

        private final String type;
        private final int relativeY;

        LayerSpec(String type, int relativeY) {
            this.type = type;
            this.relativeY = relativeY;
        }

        public String type() {
            return type;
        }

        public int relativeY() {
            return relativeY;
        }
    }
}
