package com.christian34.catchthebeacon.files;

import com.christian34.catchthebeacon.CatchTheBeacon;

import java.io.File;
import java.io.IOException;


public class ConfigFile extends PluginFile {

    protected ConfigFile() {
        super(new File(FileManager.getPluginFolder(), "config.yml"), null);
    }

    @Override
    public void createFile() throws IOException {
        try {
            CatchTheBeacon.getInstance().getPlugin().saveResource("config.yml", true);
        } catch (IllegalArgumentException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public void update() throws IOException {
        ConfigUpdater.update(instance.getPlugin(), "config.yml",
                new File(FileManager.getPluginFolder(), "config.yml"));
    }

}
