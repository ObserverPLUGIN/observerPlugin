package com.yangtheory.observer.snapshot;

import java.util.List;

public record PlayerNearbySnapshot(
        String serverName,
        String playerUuid,
        String playerName,
        String worldName,
        String dimensionType,
        int centerX,
        int centerY,
        int centerZ,
        float yaw,
        float pitch,
        int radius,
        String capturedAt,
        List<Layer> layers
) {
    public String signature() {
        StringBuilder builder = new StringBuilder();
        builder.append(serverName).append('|')
                .append(playerUuid).append('|')
                .append(playerName).append('|')
                .append(worldName).append('|')
                .append(centerX).append('|')
                .append(centerY).append('|')
                .append(centerZ).append('|')
                .append(radius);

        for (Layer layer : layers) {
            builder.append('|').append(layer.type()).append('@').append(layer.absoluteY());
            for (List<Cell> row : layer.rows()) {
                for (Cell cell : row) {
                    builder.append(';')
                            .append(cell.material())
                            .append(':')
                            .append(cell.highlighted() ? '1' : '0')
                            .append(':')
                            .append(cell.loaded() ? '1' : '0');
                }
            }
        }
        return builder.toString();
    }

    public record Layer(
            String type,
            int relativeY,
            int absoluteY,
            List<List<Cell>> rows
    ) {
    }

    public record Cell(
            String material,
            int blockX,
            int blockY,
            int blockZ,
            boolean highlighted,
            boolean loaded
    ) {
    }
}
