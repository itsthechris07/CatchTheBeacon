package com.christian34.catchthebeacon.game.map;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.files.ArenasFile;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.files.LobbyFile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The map that comes with the plugin ("islands" with its lobby): installed with its whole setup on the first start,
 * so the plugin can be played right away. It is never installed again once the server has arenas (or had them).
 */
public final class DefaultMap {
    public static final String NAME = "islands";
    /**
     * false: the map isn't installed (the tests start without it)
     */
    public static final String PROPERTY = "ctb.default-map";
    /**
     * in arenas.yml (outside of the arenas): the server doesn't get the map (again)
     */
    private static final String CHECKED_KEY = "default-map-checked";

    private DefaultMap() {
    }

    /**
     * installs the map if the server doesn't have any arenas yet (not on lobby servers of a network)
     */
    static void installIfNew(CatchTheBeacon plugin, File mapDirectory) {
        if (!Boolean.parseBoolean(System.getProperty(PROPERTY, "true"))) return;
        ArenasFile arenasFile = plugin.getFileManager().getArenasFile();
        FileConfiguration arenas = arenasFile.getData();
        if (arenas.getBoolean(CHECKED_KEY)) return;
        if ("lobby".equalsIgnoreCase(plugin.getFileManager().getConfigFile().getString("network.mode"))) return;

        ConfigurationSection existing = arenasFile.getSection("");
        if (existing == null || existing.getKeys(false).isEmpty()) {
            try {
                install(plugin, mapDirectory);
                Debug.info("Installed the map '" + NAME + "' - it can be played right away");
            } catch (IOException | RuntimeException ex) {
                Debug.warn("Couldn't install the map '" + NAME + "': " + ex);
                return;
            }
        }
        arenas.set(CHECKED_KEY, true);
        arenasFile.save();
    }

    private static void install(CatchTheBeacon plugin, File mapDirectory) throws IOException {
        YamlConfiguration setup;
        try (InputStream in = plugin.getResource("maps/" + NAME + ".yml")) {
            if (in == null) throw new IOException("maps/" + NAME + ".yml is missing in the jar");
            setup = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        ConfigurationSection arena = setup.getConfigurationSection("arena");
        String lobby = arena == null ? null : arena.getString("lobby");
        if (lobby == null) throw new IOException("maps/" + NAME + ".yml has no arena or lobby");
        if (new File(mapDirectory, NAME).exists() || new File(mapDirectory, lobby).exists()) {
            throw new IOException("the folder maps/" + NAME + " or maps/" + lobby + " exists already");
        }

        try (InputStream in = plugin.getResource("maps/" + NAME + ".zip")) {
            if (in == null) throw new IOException("maps/" + NAME + ".zip is missing in the jar");
            unzip(in, mapDirectory);
        } catch (IOException | RuntimeException ex) {
            FileManager.deleteDirectory(new File(mapDirectory, NAME));
            FileManager.deleteDirectory(new File(mapDirectory, lobby));
            throw ex;
        }

        ArenasFile arenasFile = plugin.getFileManager().getArenasFile();
        copy(arena, arenasFile.getData(), "arenas." + NAME);
        arenasFile.save();
        LobbyFile lobbyFile = plugin.getFileManager().getLobbyFile();
        copy(setup.getConfigurationSection("lobby"), lobbyFile.getData(), lobby);
        lobbyFile.save();
    }

    private static void copy(ConfigurationSection from, FileConfiguration to, String path) {
        if (from == null) return;
        for (String key : from.getKeys(true)) {
            if (!from.isConfigurationSection(key)) {
                to.set(path + "." + key, from.get(key));
            }
        }
    }

    private static void unzip(InputStream in, File target) throws IOException {
        File root = target.getCanonicalFile();
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                File file = new File(root, entry.getName()).getCanonicalFile();
                if (!file.toPath().startsWith(root.toPath())) throw new IOException("invalid entry " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(file.toPath());
                } else {
                    Files.createDirectories(file.getParentFile().toPath());
                    Files.copy(zip, file.toPath());
                }
            }
        }
    }

}
