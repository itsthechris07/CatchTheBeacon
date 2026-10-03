package com.christian34.catchthebeacon.lib;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.lib.lang.I;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A menu of buttons: a chest menu for Java players, a form for Bedrock players (Floodgate, see
 * {@link com.christian34.catchthebeacon.integrations.BedrockForms}).
 *
 * @author Christian34
 */
public class Menu {
    /**
     * the marker of a button in the rows of {@link #layout(int)}
     */
    private static final char BUTTON = 'g';
    private final Component title;
    private final List<Button> buttons = new ArrayList<>();

    /**
     * @param detail a short second line on Bedrock (e.g. the number of players), null: none
     * @param lore   the lines below the name of the item in the chest menu
     */
    public record Button(ItemStack icon, Component name, @Nullable Component detail, List<Component> lore,
                         Consumer<Player> action) {
    }

    public Menu(Component title) {
        this.title = title;
    }

    public Menu button(ItemStack icon, Component name, @Nullable Component detail, List<Component> lore,
                       Consumer<Player> action) {
        buttons.add(new Button(icon, name, detail, lore, action));
        return this;
    }

    public Component getTitle() {
        return title;
    }

    public List<Button> getButtons() {
        return buttons;
    }

    /**
     * clicking a button closes the menu and runs its action
     */
    public void show(Player player) {
        if (CatchTheBeacon.getInstance().getBedrockForms().show(player, this)) return;
        player.openInventory(new Chest(this).getInventory());
    }

    /**
     * @return up to 5 buttons centered with gaps in one row, otherwise full rows (at most 6, more buttons aren't
     * shown)
     */
    static String[] layout(int buttons) {
        if (buttons <= 5) {
            String row = String.join(" ", Collections.nCopies(Math.max(1, buttons), String.valueOf(BUTTON)));
            int padding = (9 - row.length()) / 2;
            return new String[]{" ".repeat(padding) + row + " ".repeat(9 - padding - row.length())};
        }
        String[] rows = new String[Math.min(6, (buttons + 8) / 9)];
        Arrays.fill(rows, String.valueOf(BUTTON).repeat(9));
        return rows;
    }

    /**
     * the chest menu of a Java player (the holder identifies it on clicks)
     */
    static final class Chest implements InventoryHolder {
        private final Inventory inventory;
        private final Map<Integer, Button> slots = new HashMap<>();

        Chest(Menu menu) {
            String[] rows = layout(menu.buttons.size());
            this.inventory = Bukkit.createInventory(this, rows.length * 9, menu.title);
            int next = 0;
            for (int slot = 0; slot < rows.length * 9 && next < menu.buttons.size(); slot++) {
                if (rows[slot / 9].charAt(slot % 9) != BUTTON) continue;
                Button button = menu.buttons.get(next++);
                slots.put(slot, button);
                inventory.setItem(slot, icon(button));
            }
        }

        private static ItemStack icon(Button button) {
            ItemStack item = button.icon().clone();
            item.editMeta(meta -> {
                meta.displayName(I.item(button.name()));
                meta.lore(button.lore().stream().map(line -> I.item(line.colorIfAbsent(NamedTextColor.GRAY))).toList());
            });
            return item;
        }

        @Nullable
        Button getButton(int slot) {
            return slots.get(slot);
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    /**
     * nothing can be taken out of or put into a chest menu, clicking a button runs its action
     */
    public static class ChestListener implements Listener {

        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (!(e.getView().getTopInventory().getHolder() instanceof Chest chest)) return;
            e.setCancelled(true);
            if (e.getClickedInventory() != e.getView().getTopInventory()) return;
            Button button = chest.getButton(e.getSlot());
            if (button == null || !(e.getWhoClicked() instanceof Player player)) return;
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            // the inventory mustn't be closed or changed during the click event
            Bukkit.getScheduler().runTask(CatchTheBeacon.getInstance(), () -> {
                player.closeInventory();
                button.action().accept(player);
            });
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getView().getTopInventory().getHolder() instanceof Chest) e.setCancelled(true);
        }

    }

}
