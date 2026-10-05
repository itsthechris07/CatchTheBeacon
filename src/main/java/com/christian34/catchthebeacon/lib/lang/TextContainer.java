package com.christian34.catchthebeacon.lib.lang;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.files.ConfigUpdater;
import com.christian34.catchthebeacon.files.FileManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;


public class TextContainer {
    private final File file;
    private final YamlConfiguration data;

    public TextContainer() {
        this.file = new File(FileManager.getPluginFolder(), "messages.yml");
        try {
            ConfigUpdater.update(CatchTheBeacon.getInstance(), "messages.yml", this.file);
        } catch (IOException ex) {
            Debug.warn("Couldn't update file 'messages.yml'", ex);
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public String getString(String key) {
        return data.getString(key);
    }

}
