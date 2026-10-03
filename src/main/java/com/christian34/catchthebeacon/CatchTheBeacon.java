package com.christian34.catchthebeacon;

import com.christian34.catchthebeacon.commands.CommandManager;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.GameItems;
import com.christian34.catchthebeacon.game.GameManager;
import com.christian34.catchthebeacon.game.map.GameWorld;
import com.christian34.catchthebeacon.game.map.MapHandler;
import com.christian34.catchthebeacon.game.map.MapTemplate;
import com.christian34.catchthebeacon.game.setup.SetupManager;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.game.states.LobbyState;
import com.christian34.catchthebeacon.game.SignManager;
import com.christian34.catchthebeacon.integrations.BedrockForms;
import com.christian34.catchthebeacon.integrations.CtbMiniPlaceholders;
import com.christian34.catchthebeacon.integrations.EssentialsSupport;
import com.christian34.catchthebeacon.integrations.MultiverseSupport;
import com.christian34.catchthebeacon.integrations.NpcSupport;
import com.christian34.catchthebeacon.integrations.PartySupport;
import com.christian34.catchthebeacon.integrations.VanishSupport;
import com.christian34.catchthebeacon.lib.GameScoreboard;
import com.christian34.catchthebeacon.stats.AchievementManager;
import com.christian34.catchthebeacon.stats.StatsExpansion;
import com.christian34.catchthebeacon.stats.StatsManager;
import com.christian34.catchthebeacon.lib.TabListManager;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.listeners.EventListener;
import com.christian34.catchthebeacon.network.NetworkManager;
import com.christian34.catchthebeacon.user.GamePlayer;
import com.christian34.catchthebeacon.user.TestPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CatchTheBeacon extends JavaPlugin {
    /**
     * the prefix of all messages, from `prefix` in config.yml (set when the plugin is enabled)
     */
    public static Component PREFIX = Component.empty();
    /**
     * start the server with -Dctb.debug.autostart=true to join all online players and a {@link TestPlayer}
     * into the first game and force start it
     */
    private static final String DEBUG_AUTOSTART_PROPERTY = "ctb.debug.autostart";
    private static CatchTheBeacon instance;
    private Plugin plugin;
    private GameManager gameManager;
    private FileManager fileManager;
    private Set<GamePlayer> gamePlayers;
    private CommandManager commandManager;
    private MapHandler mapHandler;
    private EventListener eventListener;
    private TabListManager tabListManager;
    private SetupManager setupManager;
    private GameScoreboard gameScoreboard;
    private StatsManager statsManager;
    private AchievementManager achievementManager;
    private SignManager signManager;
    private NetworkManager networkManager;
    private PartySupport partySupport;
    private NpcSupport npcSupport;
    private BedrockForms bedrockForms = BedrockForms.NONE;
    private EssentialsSupport essentials = EssentialsSupport.NONE;
    private Telemetry telemetry;

    public static CatchTheBeacon getInstance() {
        return instance;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public boolean isInGame(Player player) {
        for (GamePlayer gamePlayer : getUsers()) {
            if (gamePlayer.getPlayer().equals(player) && gamePlayer.getGame() != null) {
                return true;
            }
        }
        return false;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public Telemetry getTelemetry() {
        return telemetry;
    }

    public NpcSupport getNpcSupport() {
        return npcSupport;
    }

    public BedrockForms getBedrockForms() {
        return bedrockForms;
    }

    /**
     * replaces the forms for Bedrock players (the tests use fake ones)
     */
    public void setBedrockForms(BedrockForms bedrockForms) {
        this.bedrockForms = bedrockForms;
    }

    public EssentialsSupport getEssentials() {
        return essentials;
    }

    /**
     * replaces the hook into EssentialsX (the tests use a fake one)
     */
    public void setEssentials(EssentialsSupport essentials) {
        this.essentials = essentials;
    }

    public PartySupport getPartySupport() {
        return partySupport;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public SignManager getSignManager() {
        return signManager;
    }

    public GameScoreboard getGameScoreboard() {
        return gameScoreboard;
    }

    public SetupManager getSetupManager() {
        return setupManager;
    }

    public MapHandler getMapHandler() {
        return mapHandler;
    }

    public Set<GamePlayer> getUsers() {
        return gamePlayers;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * replaces the command manager (the tests use one without a server behind it)
     */
    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public void onEnable() {
        instance = this;
        // Multiverse may have loaded worlds of games again
        MultiverseSupport.hook(this);
        GameWorld.deleteLeftovers();
        this.plugin = this;
        this.gamePlayers = Collections.synchronizedSet(new HashSet<>());
        this.fileManager = new FileManager();
        String prefix = fileManager.getConfigFile().getString("prefix");
        PREFIX = prefix == null || prefix.isBlank() ? Component.empty() : I.text(prefix + " ");
        I.load();
        this.mapHandler = new MapHandler(this);
        this.statsManager = new StatsManager(this);
        this.achievementManager = new AchievementManager(this);
        this.essentials = EssentialsSupport.create(this);
        this.gameManager = new GameManager(this);
        this.partySupport = new PartySupport(this);
        this.networkManager = new NetworkManager(this);
        this.gameManager.createAutoGames();
        this.signManager = new SignManager(this);
        this.npcSupport = new NpcSupport(this);
        this.bedrockForms = BedrockForms.create(this);
        this.setupManager = new SetupManager(this);
        this.gameScoreboard = new GameScoreboard(this);
        try {
            this.commandManager = new CommandManager(this);
        } catch (Exception | LinkageError ex) {
            // cloud reflects into server internals, which may break on server updates (and doesn't work on
            // MockBukkit) - the rest of the plugin keeps working
            Debug.warn("Couldn't register the commands of CatchTheBeacon: " + ex);
        }
        this.eventListener = new EventListener();
        this.eventListener.registerEvents();
        this.tabListManager = new TabListManager(this);
        new GameItems(this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new StatsExpansion(this).register();
        }
        if (Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders")) {
            try {
                new CtbMiniPlaceholders(this).register();
                Debug.info("Hooked into MiniPlaceholders");
            } catch (Exception | LinkageError ex) {
                Debug.warn("Couldn't register the placeholders for MiniPlaceholders: " + ex);
            }
        }
        this.telemetry = new Telemetry(this);
        this.telemetry.start();

        if (Boolean.getBoolean(DEBUG_AUTOSTART_PROPERTY)) {
            Bukkit.getScheduler().runTaskLater(this, this::debugAutostart, 10L);
        }
    }

    private void debugAutostart() {
        Optional<Game> optionalGame = gameManager.getGames().stream().findAny();
        if (optionalGame.isEmpty() || Bukkit.getOnlinePlayers().isEmpty()) {
            Debug.warn("Debug autostart needs a game and at least one online player");
            return;
        }
        Game game = optionalGame.get();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!VanishSupport.isVanished(player)) game.join(getUser(player));
        }
        Player player1 = TestPlayer.create("Player1");
        game.join(new GamePlayer(player1, this));
        ((LobbyState) game.getGameStateManager().getCurrentGameState()).forceStart();
    }

    public void onDisable() {
        if (telemetry != null) {
            telemetry.stop();
        }
        if (setupManager != null) {
            setupManager.stopAll();
        }
        if (npcSupport != null) {
            npcSupport.removeStatus();
        }
        if (gameManager != null) {
            // no ENDING state here: it would go on to RESTART, which creates a new game for the arena
            for (Game game : new ArrayList<>(gameManager.getGames())) {
                game.stop();
            }
            gameManager.getGames().clear();
        }
        if (statsManager != null) {
            statsManager.close();
        }
        // close() removes the template from the set
        for (MapTemplate template : new ArrayList<>(MapTemplate.getTemplates())) {
            template.close();
        }
    }

    /**
     * returns user instance
     *
     * @param player
     */
    public GamePlayer getUser(@NotNull Player player) {
        for (GamePlayer gamePlayer : gamePlayers) {
            if (gamePlayer.getPlayer().equals(player)) {
                return gamePlayer;
            }
        }
        GamePlayer gamePlayer = new GamePlayer(player, this);
        this.gamePlayers.add(gamePlayer);
        return gamePlayer;
    }

}
