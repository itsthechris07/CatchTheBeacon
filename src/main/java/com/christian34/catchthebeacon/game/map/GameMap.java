package com.christian34.catchthebeacon.game.map;


public interface GameMap {

    /**
     * @return name of the map, equals folder name
     */
    String getName();

    boolean isPlayable();

}
