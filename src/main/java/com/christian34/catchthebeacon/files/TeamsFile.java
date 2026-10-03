package com.christian34.catchthebeacon.files;

import java.io.File;
import java.io.IOException;


public class TeamsFile extends PluginFile {

    protected TeamsFile() {
        super(new File(FileManager.getPluginFolder(), "teams.yml"), "teams");
    }

    @Override
    public void createFile() throws IOException {
        try {
            instance.saveResource("teams.yml", true);
        } catch (IllegalArgumentException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public void update() throws IOException {
        ConfigUpdater.update(instance.getPlugin(), "teams.yml",
                new File(FileManager.getPluginFolder(), "teams.yml"));
    }

}
