package com.yangtheory.observer.nearby;

import org.bukkit.Material;
import org.bukkit.block.Block;

public record NearbyBlockMatch(
        Material material,
        Block block
) {
}
