package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * Blocks of the map that give rewards (`resources` in config.yml), e.g. iron blocks give iron armor. There are no
 * generators - the resources are part of the map, so taking the ones of the enemy is a tactic.
 *
 * @author Christian34
 */
public final class ResourceBlocks {
    /**
     * armor materials from bad to good
     */
    private static final List<String> ARMOR_TIERS = List.of("LEATHER", "GOLDEN", "CHAINMAIL", "COPPER", "IRON",
            "DIAMOND", "NETHERITE");

    private ResourceBlocks() {
    }

    /**
     * @return the rewards for breaking the block (empty if it isn't a resource)
     */
    @NotNull
    public static List<ItemStack> getRewards(@NotNull Material block) {
        List<ItemStack> rewards = new ArrayList<>();
        for (Map<?, ?> entry : CatchTheBeacon.getInstance().getFileManager().getConfigFile().getData().getMapList("resources")) {
            if (!block.name().equalsIgnoreCase(String.valueOf(entry.get("block")))) continue;
            if (!(entry.get("rewards") instanceof List<?> list)) continue;
            for (Object reward : list) {
                ItemStack item = parseItem(String.valueOf(reward));
                if (item == null) {
                    Debug.warn("Invalid reward in config.yml: " + reward);
                } else {
                    rewards.add(item);
                }
            }
        }
        return rewards;
    }

    public static boolean isResource(@NotNull Material block) {
        return !getRewards(block).isEmpty();
    }

    /**
     * @param text e.g. "IRON_SWORD" or "IRON_INGOT:4"
     */
    @Nullable
    static ItemStack parseItem(String text) {
        String[] parts = text.split(":");
        Material material = Material.matchMaterial(parts[0].trim().toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem() || material.isAir()) return null;
        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return new ItemStack(material, Math.max(1, amount));
    }

    /**
     * gives the player one of the rewards by chance
     *
     * @return false if the block isn't a resource
     */
    public static boolean reward(@NotNull GamePlayer gamePlayer, @NotNull Material block) {
        List<ItemStack> rewards = getRewards(block);
        if (rewards.isEmpty()) return false;
        ItemStack reward = rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));
        give(gamePlayer.getPlayer(), reward);
        gamePlayer.sendMessage(LangText.RESOURCE_REWARD, (reward.getAmount() > 1 ? reward.getAmount() + "x " : "")
                + name(reward.getType()));
        gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        return true;
    }

    private static String name(Material material) {
        String name = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * armor is put on if it is better than the one the player wears, everything else goes into the inventory
     */
    static void give(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        EquipmentSlot slot = armorSlot(item.getType());
        if (slot != null) {
            ItemStack worn = inventory.getItem(slot);
            if (worn.getType().isAir() || tier(item.getType()) > tier(worn.getType())) {
                inventory.setItem(slot, item);
                return;
            }
        }
        for (ItemStack leftover : inventory.addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    @Nullable
    private static EquipmentSlot armorSlot(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET")) return EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlot.FEET;
        return null;
    }

    private static int tier(Material material) {
        String name = material.name();
        for (int i = ARMOR_TIERS.size() - 1; i >= 0; i--) {
            if (name.startsWith(ARMOR_TIERS.get(i) + "_")) return i;
        }
        return -1;
    }

}
