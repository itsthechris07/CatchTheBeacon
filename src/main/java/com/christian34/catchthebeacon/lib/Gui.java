package com.christian34.catchthebeacon.lib;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.lib.lang.I;
import de.themoep.inventorygui.InventoryGui;
import net.kyori.adventure.text.Component;

import java.util.Collections;

public class Gui {

    /**
     * InventoryGui only knows texts with color codes
     */
    public static InventoryGui create(Component title, String[] rows) {
        return new InventoryGui(CatchTheBeacon.getInstance(), null, I.legacy(title), rows, Collections.emptyList());
    }

}
