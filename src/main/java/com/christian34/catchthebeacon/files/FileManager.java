package com.christian34.catchthebeacon.files;

import com.christian34.catchthebeacon.CatchTheBeacon;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class FileManager {
    private ConfigFile configFile;
    private ArenasFile arenasFile;
    private TeamsFile teamsFile;
    private LobbyFile lobbyFile;
    private File logsDir;

    public FileManager() {
        load();
    }

    private static final Set<String> SKIPPED_FILES = Set.of("uid.dat", "session.lock", "chunk_tickets.dat", "raids.dat");

    public static File getPluginFolder() {
        return CatchTheBeacon.getInstance().getDataFolder();
    }

    /**
     * copies a world folder without the files that belong to the running world: its lock and uuid, the chunks kept
     * loaded (e.g. by /forceload) and the running raids - Paper also writes these while a loaded world is copied
     */
    public static boolean copyFolder(File sourceFolder, File destinationFolder) {
        try {
            if (sourceFolder.isDirectory()) {
                if (!destinationFolder.exists()) {
                    destinationFolder.mkdir();
                }
                String[] files = sourceFolder.list();
                if (files == null) return false;

                for (String file : files) {
                    if (SKIPPED_FILES.contains(file)) continue;
                    if (!copyFolder(new File(sourceFolder, file), new File(destinationFolder, file))) {
                        return false;
                    }
                }
            } else {
                Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void deleteDirectory(File path) {
        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDirectory(file);
            }
        }
        path.delete();
    }

    public File getLogsDir() {
        return logsDir;
    }

    public TeamsFile getTeamsFile() {
        return teamsFile;
    }

    public ArenasFile getArenasFile() {
        return arenasFile;
    }

    public ConfigFile getConfigFile() {
        return configFile;
    }

    public LobbyFile getLobbyFile() {
        return lobbyFile;
    }

    public void load() {
        File mapsDir = new File(getPluginFolder() + "/maps");
        if (!mapsDir.exists()) mapsDir.mkdirs();

        this.logsDir = new File(getPluginFolder() + "/logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        this.configFile = new ConfigFile();
        this.arenasFile = new ArenasFile();
        this.teamsFile = new TeamsFile();
        this.lobbyFile = new LobbyFile();
    }

}
