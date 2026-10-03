package com.christian34.catchthebeacon.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ObjectUtils {

    public static int getDifference(int a, int b) {
        int difference = a - b;
        if (difference < 0) {
            difference = difference * -1;
        }
        return difference;
    }

    public static int parseInt(String a) {
        try {
            return Integer.parseInt(a);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static ItemStack parseItemStack(Object object) {
        try {
            return (ItemStack) object;
        } catch (ClassCastException ignored) {
            return new ItemStack(Material.AIR);
        }
    }

}
