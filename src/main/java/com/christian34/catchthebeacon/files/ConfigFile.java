package com.christian34.catchthebeacon.files;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Objects;


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

    /**
     * {@code ${NAME}} is replaced with the environment variable NAME ({@link EnvironmentVariables})
     */
    @Override
    @Nullable
    public String getString(@NotNull String key) {
        return EnvironmentVariables.resolve(super.getString(key), key);
    }

    /**
     * a number or {@code "${NAME}"} (an environment variable holding the number, e.g. the MySQL port)
     */
    @Override
    public int getInt(@NotNull String key) {
        if (!(getData().get(key) instanceof String)) return super.getInt(key);
        String value = Objects.requireNonNull(getString(key)).trim();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            if (!value.isEmpty()) Debug.warn(key + " in config.yml isn't a number");
            return 0;
        }
    }

    @Override
    public void update() throws IOException {
        ConfigUpdater.update(instance.getPlugin(), "config.yml",
                new File(FileManager.getPluginFolder(), "config.yml"));
    }

}
