package com.christian34.catchthebeacon.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

public class BlockUtils {

    @Nullable
    public static Block findInRadius(Block startBlock, int radius, Material material) {
        Block middle = startBlock.getLocation().getBlock();
        for (int x = radius; x >= -radius; x--) {
            for (int y = radius; y >= -radius; y--) {
                for (int z = radius; z >= -radius; z--) {
                    Block block = middle.getRelative(x, y, z);
                    if (block.getType() == material) {
                        return block;
                    }
                }
            }
        }
        return null;
    }

}
