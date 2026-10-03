package com.christian34.catchthebeacon.files;

import java.io.File;
import java.io.IOException;

public class ArenasFile extends PluginFile {

    protected ArenasFile() {
        super(new File(FileManager.getPluginFolder(), "arenas.yml"), "arenas");
    }

    @Override
    public void createFile() throws IOException {
        if (!super.getSourceFile().createNewFile()) {
            throw new Error("Couldn't create file 'arenas.yml'!");
        }
    }

    @Override
    public void update() {
        //no update necessary
    }

}
