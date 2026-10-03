package com.christian34.catchthebeacon.lib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The chest menu layout (showing InventoryGui doesn't work on MockBukkit: it needs Paper's rewriting of the old
 * Sound enum calls)
 *
 * @author Christian34
 */
class MenuTest {

    @Test
    void fewButtonsAreCenteredWithGaps() {
        assertArrayEquals(new String[]{"    g    "}, Menu.layout(1));
        assertArrayEquals(new String[]{"  g g g  "}, Menu.layout(3));
        assertArrayEquals(new String[]{"g g g g g"}, Menu.layout(5));
    }

    @Test
    void manyButtonsFillRows() {
        assertArrayEquals(new String[]{"ggggggggg"}, Menu.layout(6));
        assertArrayEquals(new String[]{"ggggggggg", "ggggggggg"}, Menu.layout(10));
        assertEquals(6, Menu.layout(100).length);
    }

    @Test
    void emptyMenuHasOneRow() {
        assertEquals(1, Menu.layout(0).length);
        assertEquals(9, Menu.layout(0)[0].length());
    }

}
