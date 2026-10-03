package com.christian34.catchthebeacon.files;

import java.io.File;
import java.io.IOException;

public class LobbyFile extends PluginFile {

    public LobbyFile() {
        super(new File(FileManager.getPluginFolder(), "lobby.yml"), null);
    }

    @Override
    public void createFile() throws IOException {
        if (!super.getSourceFile().createNewFile()) {
            throw new Error("Couldn't create file 'arenas.yml'!");
        }
    }

    @Override
    public void update() throws IOException {
        //no update necessary
    }

}
