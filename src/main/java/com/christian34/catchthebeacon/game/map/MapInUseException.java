package com.christian34.catchthebeacon.game.map;

import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.jetbrains.annotations.NotNull;

public class MapInUseException extends Exception implements ComponentMessageThrowable {

    public MapInUseException(GameMap map) {
        super("Map '" + map.getName() + "' is in use!");
    }

    /**
     * @return the message for the player (messages.yml)
     */
    @Override
    public @NotNull Component componentMessage() {
        return I.i18n(LangText.MAP_IN_USE);
    }

}
