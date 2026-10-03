package com.christian34.catchthebeacon.lib;

import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

public class InteractionItems {
    private static ItemStack selectTeamItem = null;

    public static ItemStack getSelectTeamItem() {
        if (selectTeamItem == null) {
            ItemStack item = new ItemStack(Material.RED_BED);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.customName(I.item(i18n(LangText.ITEM_SELECT_TEAM)));
            }
            item.setItemMeta(meta);
            selectTeamItem = item;
        }
        return selectTeamItem;
    }

    /**
     * opens the list of players a spectator can teleport to
     */
    public static ItemStack getSpectatorCompass() {
        return named(Material.COMPASS, LangText.ITEM_SPECTATE_COMPASS);
    }

    /**
     * lobby: opens the menu to vote for the variant of the round
     */
    public static ItemStack getVoteItem() {
        return named(Material.PAPER, LangText.ITEM_VOTE);
    }

    /**
     * lobby (ctb.startitem): starts the game in 5 seconds, like /ctb start
     */
    public static ItemStack getStartItem() {
        return named(Material.LIME_DYE, LangText.ITEM_START);
    }

    /**
     * game: points to the nearest enemy beacon, or to an own one while it is being mined
     */
    public static ItemStack getBeaconCompass() {
        return named(Material.COMPASS, LangText.ITEM_BEACON_COMPASS);
    }

    public static ItemStack getLeaveItem() {
        return named(Material.RED_DYE, LangText.ITEM_LEAVE);
    }

    private static ItemStack named(Material material, LangText name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.customName(I.item(i18n(name)));
        item.setItemMeta(meta);
        return item;
    }


}
