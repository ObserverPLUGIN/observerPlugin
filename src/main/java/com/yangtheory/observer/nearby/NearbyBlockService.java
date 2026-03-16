package com.yangtheory.observer.nearby;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NearbyBlockService {
    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastCheckedAt = new ConcurrentHashMap<>();
    private final Set<UUID> triggeredPlayers = ConcurrentHashMap.newKeySet();

    private boolean enabled;
    private int radius;
    private long intervalMs;
    private String messageTemplate;
    private List<String> commands;
    private Set<Material> targetBlocks;

    public NearbyBlockService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();

        enabled = plugin.getConfig().getBoolean("nearby-block.enabled", true);
        radius = Math.max(1, plugin.getConfig().getInt("nearby-block.radius", 5));
        intervalMs = Math.max(100L, plugin.getConfig().getLong("nearby-block.interval-ms", 1000L));
        messageTemplate = plugin.getConfig().getString(
                "nearby-block.trigger.message",
                "&eNearby target block detected: &f%block% &7(%x%, %y%, %z%)"
        );
        commands = new ArrayList<>(plugin.getConfig().getStringList("nearby-block.trigger.commands"));
        targetBlocks = loadTargetBlocks(plugin.getConfig().getConfigurationSection("nearby-block"));

        lastCheckedAt.clear();
        triggeredPlayers.clear();
    }

    public void handleMove(Player player) {
        if (!enabled || targetBlocks.isEmpty()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastChecked = lastCheckedAt.get(playerId);
        if (lastChecked != null && now - lastChecked < intervalMs) {
            return;
        }

        lastCheckedAt.put(playerId, now);
        NearbyBlockMatch match = findNearbyBlock(player);
        if (match == null) {
            triggeredPlayers.remove(playerId);
            return;
        }

        if (triggeredPlayers.add(playerId)) {
            trigger(player, match);
        }
    }

    public NearbyBlockMatch checkNow(Player player) {
        if (!enabled || targetBlocks.isEmpty()) {
            return null;
        }
        return findNearbyBlock(player);
    }

    public void clearPlayer(UUID playerId) {
        lastCheckedAt.remove(playerId);
        triggeredPlayers.remove(playerId);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getRadius() {
        return radius;
    }

    public Set<Material> getTargetBlocks() {
        return Collections.unmodifiableSet(targetBlocks);
    }

    private NearbyBlockMatch findNearbyBlock(Player player) {
        Location center = player.getLocation();
        if (center.getWorld() == null) {
            return null;
        }

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        int radiusSquared = radius * radius;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    int dx = x - centerX;
                    int dy = y - centerY;
                    int dz = z - centerZ;
                    if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                        continue;
                    }

                    Block block = center.getWorld().getBlockAt(x, y, z);
                    if (!targetBlocks.contains(block.getType())) {
                        continue;
                    }

                    return new NearbyBlockMatch(block.getType(), block);
                }
            }
        }

        return null;
    }

    private void trigger(Player player, NearbyBlockMatch match) {
        Block block = match.block();
        if (messageTemplate != null && !messageTemplate.isBlank()) {
            player.sendMessage(colorize(applyPlaceholders(messageTemplate, player, block)));
        }

        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }
            plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    applyPlaceholders(command, player, block)
            );
        }
    }

    private String applyPlaceholders(String value, Player player, Block block) {
        return value
                .replace("%player%", player.getName())
                .replace("%block%", block.getType().name())
                .replace("%world%", block.getWorld().getName())
                .replace("%x%", Integer.toString(block.getX()))
                .replace("%y%", Integer.toString(block.getY()))
                .replace("%z%", Integer.toString(block.getZ()));
    }

    private String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private Set<Material> loadTargetBlocks(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptySet();
        }

        Set<Material> blocks = new HashSet<>();
        for (String name : section.getStringList("blocks")) {
            if (name == null || name.isBlank()) {
                continue;
            }

            Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
            if (material == null || !material.isBlock()) {
                plugin.getLogger().warning("Invalid nearby-block material in config.yml: " + name);
                continue;
            }
            blocks.add(material);
        }
        return blocks;
    }
}
