package com.christian34.catchthebeacon.lib;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.lib.lang.I;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A menu of buttons: a chest menu for Java players, a form for Bedrock players (Floodgate, see
 * {@link com.christian34.catchthebeacon.integrations.BedrockForms}).
 *
 * @author Christian34
 */
public class Menu {
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
        InventoryGui gui = Gui.create(title, layout(buttons.size()));
        GuiElementGroup group = new GuiElementGroup('g');
        for (Button button : buttons) {
            String[] text = Stream.concat(Stream.of(button.name()), button.lore().stream()).map(I::legacy)
                    .toArray(String[]::new);
            group.addElement(new StaticGuiElement('g', button.icon(), click -> {
                click.getWhoClicked().closeInventory();
                if (click.getWhoClicked() instanceof Player clicker) button.action().accept(clicker);
                return true;
            }, text));
        }
        gui.addElement(group);
        gui.show(player);
    }

    /**
     * @return up to 5 buttons centered with gaps in one row, otherwise full rows (at most 6)
     */
    static String[] layout(int buttons) {
        if (buttons <= 5) {
            String row = String.join(" ", java.util.Collections.nCopies(Math.max(1, buttons), "g"));
            int padding = (9 - row.length()) / 2;
            return new String[]{" ".repeat(padding) + row + " ".repeat(9 - padding - row.length())};
        }
        String[] rows = new String[Math.min(6, (buttons + 8) / 9)];
        Arrays.fill(rows, "ggggggggg");
        return rows;
    }

}
