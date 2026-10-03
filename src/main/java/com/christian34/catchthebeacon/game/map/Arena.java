package com.christian34.catchthebeacon.game.map;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.files.ArenasFile;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.IncompleteConfigurationException;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.setup.SetupStep;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.utils.BlockUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Arena implements GameMap {
    private final String name;
    private final File mapDirectory;
    private final int minPlayers;
    private final int maxPlayers;
    private final String displayName;
    private final CatchTheBeacon instance;
    private final ArenasFile arenasFile;
    private LobbyMap lobbyMap;

    public Arena(@NotNull String name) throws IncompleteConfigurationException {
        this.name = name;
        this.mapDirectory = new File(FileManager.getPluginFolder() + "/maps/" + name);
        this.instance = CatchTheBeacon.getInstance();

        String keyPrefix = name + ".";
        this.arenasFile = instance.getFileManager().getArenasFile();
        this.minPlayers = arenasFile.getInt(keyPrefix + "min-players");
        this.maxPlayers = arenasFile.getInt(keyPrefix + "max-players");
        this.displayName = arenasFile.getString(keyPrefix + "display-name");
        this.lobbyMap = new LobbyMap(this);
    }

    /**
     * reads the beacons from arenas.yml (so new beacons are used right away)
     */
    @NotNull
    public Map<Beacon.Position, Location> getBeaconLocations(Team team) {
        Map<Beacon.Position, Location> locations = new HashMap<>();
        String path = name + ".teams." + team.name() + ".beacon";
        ConfigurationSection section = arenasFile.getSection(path);
        if (section == null) return locations;
        for (String posName : section.getKeys(false)) {
            Beacon.Position position = Beacon.Position.get(posName);
            if (position == null) continue;
            String coordsPath = path + "." + posName + ".";
            locations.put(position, new Location(null, arenasFile.getDouble(coordsPath + "x"),
                    arenasFile.getDouble(coordsPath + "y"), arenasFile.getDouble(coordsPath + "z")));
        }
        return locations;
    }

    public boolean hasSpawnLocation(@NotNull Team team) {
        return arenasFile.getData().contains("arenas." + name + ".teams." + team.name() + ".spawn.x");
    }

    /**
     * @return what has to be set up before a game with this arena can be created (empty if nothing is missing)
     */
    @NotNull
    public List<Component> getMissingSetup() {
        List<Component> missing = new ArrayList<>();
        if (!mapDirectory.isDirectory()) {
            missing.add(I.i18n(LangText.SETUP_MISSING_MAP, name));
        }
        if (lobbyMap.getName() == null || lobbyMap.getDirectory() == null || !lobbyMap.getDirectory().isDirectory()) {
            missing.add(I.i18n(LangText.SETUP_MISSING_LOBBY, name));
        }
        for (SetupStep step : SetupStep.values()) {
            if (!step.isDone(this)) {
                missing.add(I.i18n(LangText.SETUP_MISSING_STEP, step.getDescription()));
            }
        }
        return missing;
    }

    public File getDirectory() {
        return mapDirectory;
    }

    public LobbyMap getLobbyMap() {
        return lobbyMap;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isPlayable() {
        return getMissingSetup().isEmpty();
    }

    public List<String> getBuilders() {
        return null;
    }

    /**
     * @return display-name of arenas.yml (MiniMessage or color codes), otherwise the name
     */
    public Component getDisplayName() {
        return displayName != null ? I.text(displayName) : Component.text(name);
    }

    /**
     * @return true if the arena gets a game when the server starts (set by /ctb arena &lt;arena&gt; creategame)
     */
    public boolean hasAutoGame() {
        return arenasFile.getBoolean(name + ".auto-game");
    }

    /**
     * a game with the arena is created when the server starts (also possible while a game is running)
     */
    public void setAutoGame(boolean autoGame) {
        arenasFile.set(name + ".auto-game", autoGame ? true : null);
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    /**
     * @param players
     * @return false if arena is in use
     */
    public void setMinPlayers(int players) throws MapInUseException {
        if (isInUse()) throw new MapInUseException(this);
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * @param players
     * @return false if arena is in use
     */
    public void setMaxPlayers(int players) throws MapInUseException {
        if (isInUse()) throw new MapInUseException(this);
    }

    public void setSpawnLocation(Team team, Location location) throws MapInUseException {
        if (isInUse()) throw new MapInUseException(this);
        String path = "arenas." + getName() + ".teams." + team.name() + ".spawn.";
        arenasFile.getData().set(path + "x", location.getX());
        arenasFile.getData().set(path + "y", location.getY());
        arenasFile.getData().set(path + "z", location.getZ());
        arenasFile.getData().set(path + "yaw", location.getYaw());
        arenasFile.getData().set(path + "pitch", location.getPitch());
        arenasFile.save();
    }

    public Location getSpawnLocation(@NotNull Team team) {
        String path = getName() + ".teams." + team.name() + ".spawn.";
        double x = arenasFile.getDouble(path + "x");
        double y = arenasFile.getDouble(path + "y");
        double z = arenasFile.getDouble(path + "z");
        float yaw = (float) arenasFile.getDouble(path + "yaw");
        float pitch = (float) arenasFile.getDouble(path + "pitch");
        return new Location(null, x, y, z, yaw, pitch);
    }

    /**
     * @param location
     * @return false if a beacon was not found in a 5 block radius
     */
    public boolean setBeacon(@NotNull Team team, @NotNull Location location, @NotNull Beacon.Position position)
            throws MapInUseException {
        if (isInUse()) throw new MapInUseException(this);

        Block beaconBlock = BlockUtils.findInRadius(location.getBlock(), 3, Material.BEACON);
        if (beaconBlock == null) {
            return false;
        }
        return setBeacon(team, beaconBlock, position);
    }

    /**
     * @return false if the block is not a beacon
     */
    public boolean setBeacon(@NotNull Team team, @NotNull Block beaconBlock, @NotNull Beacon.Position position)
            throws MapInUseException {
        if (isInUse()) throw new MapInUseException(this);
        if (beaconBlock.getType() != Material.BEACON) {
            return false;
        }

        String path = "arenas." + getName() + ".teams." + team.name() + ".beacon." + position.name() + ".";
        arenasFile.getData().set(path + "x", beaconBlock.getX());
        arenasFile.getData().set(path + "y", beaconBlock.getY());
        arenasFile.getData().set(path + "z", beaconBlock.getZ());
        arenasFile.save();
        return true;
    }

    /**
     * @param name of the map in plugins/CatchTheBeacon/maps
     * @return false if folder was not found
     * @throws MapInUseException if a game with this arena is running
     */
    public boolean setLobbyWorld(String name) throws MapInUseException {
        if (isInUse()) throw new MapInUseException(this);
        File file = new File(FileManager.getPluginFolder(), "maps/" + name);
        if (!file.isDirectory() || !file.exists()) {
            return false;
        }
        arenasFile.set(getName() + ".lobby", name);
        this.lobbyMap = new LobbyMap(this);
        return true;
    }

    public World getLobbyWorld() {
        return null;
    }

    public World getGameWorld() {
        return null;
    }

    /**
     * @return true if a game with this arena exists (the arena can't be changed then)
     */
    public boolean isInUse() {
        Set<Game> games = this.instance.getGameManager().getGames();
        for (Game game : games) {
            if (game.getArena().equals(this)) {
                return true;
            }
        }
        return false;
    }

}
