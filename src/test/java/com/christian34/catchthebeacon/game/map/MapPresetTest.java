package com.christian34.catchthebeacon.game.map;

import com.christian34.catchthebeacon.PluginTestBase;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Team;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Maps of others aren't shipped, but their setup is: the downloaded zip is set up as arena.
 */
class MapPresetTest extends PluginTestBase {

    private File maps() {
        return new File(plugin.getDataFolder(), "maps");
    }

    /**
     * like the download of Planet Minecraft: an old world in a folder of the zip
     */
    private File addDownload(String fileName) throws IOException {
        File zip = new File(maps(), fileName);
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            for (String entry : List.of("Sakura/level.dat", "Sakura/region/r.0.0.mca", "Sakura/playerdata/x.dat")) {
                out.putNextEntry(new ZipEntry(entry));
                out.write(new byte[]{1, 2, 3});
                out.closeEntry();
            }
        }
        return zip;
    }

    private void assertSetUp(String name) {
        Arena arena = plugin.getMapHandler().getArena(name);
        assertNotNull(arena);
        assertTrue(arena.getMissingSetup().isEmpty(), () -> "missing: " + arena.getMissingSetup());
        assertEquals(MapPreset.LOBBY, arena.getLobbyMap().getName());
        assertTrue(arena.getLobbyMap().isPlayable());
        assertEquals(2, arena.getBeaconLocations(Team.BLUE).size());
        assertEquals(18, arena.getBeaconLocations(Team.RED).get(Beacon.Position.LEFT).getX());
    }

    @Test
    void downloadedZipIsSetUpOnTheStart() throws IOException {
        File zip = addDownload("sakura-cores-map.zip");

        MapPreset.installDownloaded(plugin, maps());

        assertFalse(zip.exists());
        assertTrue(new File(maps(), "sakura/region/r.0.0.mca").isFile());
        assertTrue(new File(maps(), "sakura/level.dat").isFile());
        // no normal terrain around the old map
        assertTrue(new File(maps(), "sakura/data/minecraft/world_gen_settings.dat").isFile());
        assertFalse(new File(maps(), ".sakura_download").exists());
        assertTrue(new File(maps(), MapPreset.LOBBY + "/region").isDirectory());

        plugin.getMapHandler().createMap("sakura");
        assertSetUp("sakura");
        assertTrue(plugin.getMapHandler().getArena("sakura").hasAutoGame());
    }

    @Test
    void createCommandSetsUpTheDownload() throws IOException {
        addDownload("Sakura.zip");
        PlayerMock admin = addAdmin("Admin");

        execute(admin, "ctb arena create sakura");

        assertContains(messages(admin), "ready");
        assertSetUp("sakura");
    }

    @Test
    void extractedFolderIsSetUp() {
        File folder = new File(addMapFolder("Sakura"), "region");
        assertTrue(folder.mkdirs());

        MapPreset.installDownloaded(plugin, maps());
        plugin.getMapHandler().createMap("Sakura");

        assertSetUp("Sakura");
    }

    @Test
    void existingArenaIsNotChanged() throws IOException {
        plugin.getFileManager().getArenasFile().set("Sakura.min-players", 4);
        File zip = addDownload("sakura.zip");

        MapPreset.installDownloaded(plugin, maps());

        assertTrue(zip.exists());
        assertFalse(new File(maps(), "sakura").exists());
        assertNull(plugin.getFileManager().getArenasFile().getString("Sakura.lobby"));
    }

    @Test
    void zipWithoutWorldIsKept() throws IOException {
        File zip = new File(maps(), "sakura.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("readme.txt"));
            out.closeEntry();
        }

        MapPreset.installDownloaded(plugin, maps());

        assertTrue(zip.exists());
        assertFalse(new File(maps(), "sakura").exists());
        assertFalse(plugin.getFileManager().getArenasFile().getData().contains("arenas.sakura"));
    }

    @Test
    void arenaListTellsAboutPresetsUntilInstalled() throws IOException {
        PlayerMock admin = addAdmin("Admin");
        execute(admin, "ctb arena list");
        List<String> messages = messages(admin);
        assertContains(messages, "sakura by BreadBuilds");
        assertContains(messages, "[Download]");

        addDownload("sakura.zip");
        execute(admin, "ctb arena create sakura");
        messages(admin);
        execute(admin, "ctb arena list");
        assertNotContains(messages(admin), "by BreadBuilds");
    }

    @Test
    void lobbyServerOfANetworkDoesNotSetItUp() throws IOException {
        plugin.getFileManager().getConfigFile().set("network.mode", "lobby");
        File zip = addDownload("sakura.zip");

        MapPreset.installDownloaded(plugin, maps());

        assertTrue(zip.exists());
    }

}
