package com.christian34.catchthebeacon.files;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class PluginFile {
    private final File sourceFile;
    private final String dataPrefix;
    public CatchTheBeacon instance = CatchTheBeacon.getInstance();
    private FileConfiguration data;

    public PluginFile(File sourceFile, String dataPrefix) {
        this.sourceFile = sourceFile;
        if (dataPrefix != null) {
            this.dataPrefix = dataPrefix + ".";
        } else {
            this.dataPrefix = "";
        }
        load();
    }

    public FileConfiguration getData() {
        return data;
    }

    private void load() {
        if (!sourceFile.exists()) {
            try {
                createFile();
            } catch (IOException ex) {
                throw new Error(ex.getMessage());
            }
        }
        try {
            update();
        } catch (IOException ex) {
            Debug.warn("Couldn't update file '" + sourceFile.getName() + "'!");
        }
        this.data = YamlConfiguration.loadConfiguration(sourceFile);
    }

    /**
     * sets the value relative to the data prefix (like the getters read it) and saves the file
     */
    public void set(String path, Object value) {
        data.set(dataPrefix + path, value);
        save();
    }

    public void save() {
        try {
            data.options().copyDefaults(true);
            data.save(sourceFile);
        } catch (IOException ex) {
            Debug.warn("Couldn't save file '" + sourceFile.getName() + "'", ex);
        }
        load();
    }

    public File getSourceFile() {
        return sourceFile;
    }

    @Nullable
    public String getString(@NotNull String key) {
        return data.getString(dataPrefix + key);
    }

    public int getInt(@NotNull String key) {
        return data.getInt(dataPrefix + key);
    }

    public boolean getBoolean(@NotNull String key) {
        return data.getBoolean(dataPrefix + key);
    }

    public double getDouble(@NotNull String key) {
        return data.getDouble(dataPrefix + key);
    }

    @Nullable
    public ConfigurationSection getSection(@Nullable String key) {
        String prefix;
        if (key == null || key.isEmpty()) {
            prefix = dataPrefix.substring(0, dataPrefix.length() - 1);
        } else {
            prefix = dataPrefix;
        }
        return data.getConfigurationSection(prefix + key);
    }

    @NotNull
    public List<String> getList(@NotNull String key) {
        return data.getStringList(dataPrefix + key);
    }

    public abstract void createFile() throws IOException;

    public abstract void update() throws IOException;

}
