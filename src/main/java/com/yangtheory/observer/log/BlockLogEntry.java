package com.yangtheory.observer.log;

public record BlockLogEntry(
        String player,
        String world,
        int x,
        int y,
        int z,
        String blockType,
        String timestamp,
        String action
) {
    public String toJson() {
        return "{"
                + "\"player\":\"" + escape(player) + "\","
                + "\"world\":\"" + escape(world) + "\","
                + "\"x\":" + x + ","
                + "\"y\":" + y + ","
                + "\"z\":" + z + ","
                + "\"blockType\":\"" + escape(blockType) + "\","
                + "\"timestamp\":\"" + escape(timestamp) + "\","
                + "\"action\":\"" + escape(action) + "\""
                + "}";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
