package com.christian34.catchthebeacon.game.setup;

import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

/**
 * The tools in the hotbar during the guided setup.
 *
 * @author Christian34
 */
public enum SetupItem {
    LOBBY_SPAWN(SetupStep.LOBBY_SPAWN, 0, Material.NETHER_STAR, NamedTextColor.YELLOW),
    RED_SPAWN(SetupStep.RED_SPAWN, 0, Material.RED_BANNER, NamedTextColor.RED),
    BLUE_SPAWN(SetupStep.BLUE_SPAWN, 1, Material.BLUE_BANNER, NamedTextColor.BLUE),
    RED_BEACON_LEFT(SetupStep.RED_BEACON_LEFT, 2, Material.RED_STAINED_GLASS, NamedTextColor.RED),
    RED_BEACON_RIGHT(SetupStep.RED_BEACON_RIGHT, 3, Material.RED_STAINED_GLASS, NamedTextColor.RED),
    BLUE_BEACON_LEFT(SetupStep.BLUE_BEACON_LEFT, 4, Material.BLUE_STAINED_GLASS, NamedTextColor.BLUE),
    BLUE_BEACON_RIGHT(SetupStep.BLUE_BEACON_RIGHT, 5, Material.BLUE_STAINED_GLASS, NamedTextColor.BLUE),
    CHECKLIST(6, Material.BOOK, NamedTextColor.WHITE, LangText.SETUP_ITEM_CHECKLIST, LangText.SETUP_ITEM_CHECKLIST_LORE),
    SWITCH_WORLD(7, Material.ENDER_PEARL, NamedTextColor.LIGHT_PURPLE, LangText.SETUP_ITEM_SWITCH_WORLD,
            LangText.SETUP_ITEM_SWITCH_WORLD_LORE),
    SAVE(8, Material.EMERALD, NamedTextColor.GREEN, LangText.SETUP_ITEM_SAVE, LangText.SETUP_ITEM_SAVE_LORE);

    private static final NamespacedKey KEY = new NamespacedKey("catchthebeacon", "setup_item");
    private final SetupStep step;
    private final int slot;
    private final Material material;
    private final NamedTextColor color;
    // only for the tools, the items of the steps are named after their step
    private final LangText name;
    private final LangText lore;

    SetupItem(SetupStep step, int slot, Material material, NamedTextColor color) {
        this(step, slot, material, color, null, null);
    }

    SetupItem(int slot, Material material, NamedTextColor color, LangText name, LangText lore) {
        this(null, slot, material, color, name, lore);
    }

    SetupItem(SetupStep step, int slot, Material material, NamedTextColor color, LangText name, LangText lore) {
        this.step = step;
        this.slot = slot;
        this.material = material;
        this.color = color;
        this.name = name;
        this.lore = lore;
    }

    @Nullable
    public static SetupItem of(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String name = item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Nullable
    public static SetupItem of(SetupStep step) {
        for (SetupItem item : values()) {
            if (item.step == step) return item;
        }
        return null;
    }

    /**
     * @return the step this item completes, null for the tools
     */
    @Nullable
    public SetupStep getStep() {
        return step;
    }

    public int getSlot() {
        return slot;
    }

    /**
     * @param inLobby true if the player is in the lobby world
     */
    public boolean isAvailable(boolean inLobby) {
        return step == null || step.isInLobby() == inLobby;
    }

    /**
     * @param done      shows the item as done (grey name)
     * @param highlight lets the item glint (the next step)
     */
    public ItemStack create(boolean done, boolean highlight) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        Component title = step == null ? I.i18n(name) : I.i18n(LangText.SETUP_ITEM_STEP, step.getDescription());
        title = title.colorIfAbsent(done ? NamedTextColor.GRAY : color);
        if (done) {
            title = title.append(Component.text(" ✔", NamedTextColor.GREEN));
        }
        meta.displayName(I.item(title));
        if (step == null) {
            meta.lore(I.itemLines(lore));
        } else if (step.isBeacon()) {
            meta.lore(I.itemLines(LangText.SETUP_ITEM_BEACON_LORE));
        } else {
            meta.lore(I.itemLines(LangText.SETUP_ITEM_SPAWN_LORE, step.getDescription()));
        }
        meta.setEnchantmentGlintOverride(highlight);
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, name());
        item.setItemMeta(meta);
        return item;
    }

}
