package com.christian34.catchthebeacon.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class WorldUtils {

    public static String getWorldName(@Nullable World world) {
        if (world == null) return "";
        return world.getName();
    }

    public static boolean isGameWorld(@Nullable World world) {
        String name = getWorldName(world);
        return name.startsWith("ctb_");
    }

    /**
     * @return true for the worlds of running games (lobby and arena), false for the copies edited by admins
     */
    public static boolean isPlayWorld(@Nullable World world) {
        String name = getWorldName(world);
        return name.startsWith("ctb_") && !name.endsWith("_temp");
    }

    /**
     * @return true if both locations are in the same block of the same world
     */
    public static boolean isSameBlock(Location loc1, Location loc2) {
        return Objects.equals(loc1.getWorld(), loc2.getWorld()) && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY() && loc1.getBlockZ() == loc2.getBlockZ();
    }

    /**
     * @return the distance, infinite if the locations are in different worlds
     */
    public static double distance(Location loc1, Location loc2) {
        if (!Objects.equals(loc1.getWorld(), loc2.getWorld())) return Double.POSITIVE_INFINITY;
        return Math.sqrt(NumberConversions.square(loc1.getX() - loc2.getX())
                + NumberConversions.square(loc1.getY() - loc2.getY())
                + NumberConversions.square(loc1.getZ() - loc2.getZ()));
    }

}
