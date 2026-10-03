package com.christian34.catchthebeacon.exceptions;

public class MapNotFoundException extends RuntimeException {

    public MapNotFoundException() {

    }

    public MapNotFoundException(String mapName) {
        super("Couldn't find map with name '" + mapName + '"');
    }

}
