package com.christian34.catchthebeacon.commands;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.GameItems;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.map.Arena;
import com.christian34.catchthebeacon.game.map.LobbyMap;
import com.christian34.catchthebeacon.game.map.MapHandler;
import com.christian34.catchthebeacon.game.map.MapInUseException;
import com.christian34.catchthebeacon.game.map.MapPreset;
import com.christian34.catchthebeacon.game.map.MapTemplate;
import com.christian34.catchthebeacon.game.setup.SetupSession;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;

import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.christian34.catchthebeacon.commands.CommandCatchTheBeacon.ADMIN;
import static com.christian34.catchthebeacon.commands.CommandCatchTheBeacon.ROOT;

/**
 * Admin commands to set up arenas (/ctb arena ...) and the game items (/ctb config ...).
 * <p>
 * Everything about one arena is below {@code /ctb arena <arena> ...}; {@code create} and {@code list} are no arena names.
 *
 * @author Christian34
 */
@Permission(ADMIN)
public class CommandArena {
    private static final String ARENA = ROOT + " arena";
    private static final String ONE_ARENA = ARENA + " <arena>";
    private static final Set<String> RESERVED_NAMES = Set.of("create", "list");

    private CatchTheBeacon getInstance() {
        return CatchTheBeacon.getInstance();
    }

    private GamePlayer user(Player player) {
        return getInstance().getUser(player);
    }

    private static void send(CommandSender sender, LangText message, Object... arguments) {
        sender.sendMessage(I.prefixed(message, arguments));
    }

    @Suggestions("importable-worlds")
    public List<String> importableWorlds(CommandContext<CommandSender> context, String input) {
        return MapHandler.getImportableWorlds().stream().map(World::getName).toList();
    }

    @Suggestions("maps-and-worlds")
    public List<String> mapsAndWorlds(CommandContext<CommandSender> context, String input) {
        Set<String> names = new LinkedHashSet<>(getInstance().getMapHandler().getMapNames());
        names.addAll(importableWorlds(context, input));
        return List.copyOf(names);
    }

    @Command(ARENA + " create <name>")
    @CommandDescription("command_arena_create")
    public void createCmd(CommandSender sender, @Argument("name") String name,
                          @Flag(value = "world", suggestions = "importable-worlds") @Nullable String worldName) {
        MapHandler mapHandler = getInstance().getMapHandler();
        String arenaName = MapHandler.toMapName(name);
        if (arenaName.isEmpty() || RESERVED_NAMES.contains(arenaName.toLowerCase())) {
            send(sender, LangText.ARENA_INVALID_NAME);
            return;
        }
        if (mapHandler.getArena(arenaName) != null) {
            send(sender, LangText.ARENA_EXISTS);
            return;
        }
        MapPreset preset = MapPreset.get(getInstance(), arenaName);
        if (worldName != null) {
            World world = MapHandler.getImportableWorld(worldName);
            if (world == null) {
                send(sender, LangText.WORLD_NOT_FOUND, worldName);
                return;
            }
            if (mapHandler.isMap(arenaName)) {
                send(sender, LangText.MAP_EXISTS, arenaName);
                return;
            }
            if (!mapHandler.importWorld(world, arenaName)) {
                send(sender, LangText.WORLD_COPY_FAILED, world.getName());
                return;
            }
        } else if (preset != null && mapHandler.findPresetDownload(preset) != null) {
            // a downloaded map whose setup comes with the plugin
            String installed = mapHandler.installPreset(preset);
            if (installed == null || !mapHandler.createMap(installed)) {
                send(sender, LangText.ARENA_CREATE_FAILED);
                return;
            }
            sender.sendMessage(I.prefixed(I.i18n(LangText.ARENA_READY, installed)
                    .append(I.button(LangText.BUTTON_CREATE_GAME,
                            ClickEvent.runCommand("/ctb arena " + installed + " creategame")))));
            return;
        } else if (!mapHandler.isMap(arenaName)) {
            if (preset != null) {
                // e.g. [Set up Sakura] of the first steps clicked before the download is there
                sender.sendMessage(I.prefixed(I.i18n(LangText.PRESET_DOWNLOAD_MISSING, preset.displayName())
                        .append(I.button(LangText.BUTTON_MAP_DOWNLOAD, ClickEvent.openUrl(preset.url())))));
                return;
            }
            send(sender, LangText.MAP_MISSING, arenaName);
            return;
        }
        if (!mapHandler.createMap(arenaName)) {
            send(sender, LangText.ARENA_CREATE_FAILED);
            return;
        }
        sender.sendMessage(I.prefixed(I.i18n(LangText.ARENA_CREATED, arenaName)
                .append(I.button(LangText.BUTTON_SET_LOBBY_WORLD,
                        ClickEvent.suggestCommand("/ctb arena " + arenaName + " lobby setworld ")))));
    }

    @Command(ARENA + " list")
    @CommandDescription("command_arena_list")
    public void listCmd(CommandSender sender) {
        Set<Arena> arenas = getInstance().getMapHandler().getGameMaps();
        if (arenas.isEmpty()) {
            send(sender, LangText.ARENA_LIST_EMPTY);
        } else {
            send(sender, LangText.ARENA_LIST_TITLE);
        }
        for (Arena arena : arenas) {
            int missing = arena.getMissingSetup().size();
            Component line = missing == 0
                    ? I.i18n(LangText.ARENA_LIST_READY, arena.getName()).append(I.button(LangText.BUTTON_CREATE_GAME,
                    ClickEvent.runCommand("/ctb arena " + arena.getName() + " creategame")))
                    : I.i18n(LangText.ARENA_LIST_MISSING, arena.getName(), missing).append(I.button(LangText.BUTTON_SETUP,
                    ClickEvent.runCommand("/ctb arena " + arena.getName() + " setup")));
            sender.sendMessage(line);
        }
        List<MapPreset> presets = MapPreset.notInstalled(getInstance());
        if (presets.isEmpty()) return;
        send(sender, LangText.ARENA_PRESETS);
        for (MapPreset preset : presets) {
            sender.sendMessage(I.i18n(LangText.ARENA_PRESET, preset.displayName(), preset.author())
                    .append(I.button(LangText.BUTTON_MAP_DOWNLOAD, ClickEvent.openUrl(preset.url()))));
        }
    }

    @Command(ONE_ARENA + " setup")
    @CommandDescription("command_arena_setup")
    public void setupCmd(Player player, @Argument("arena") Arena arena) {
        getInstance().getSetupManager().start(user(player), arena);
    }

    @Command(ONE_ARENA + " setup exit")
    @CommandDescription("command_arena_setup_exit")
    public void setupExitCmd(Player player, @Argument("arena") Arena arena) {
        SetupSession session = getInstance().getSetupManager().getSession(player);
        if (session == null) {
            user(player).sendMessage(LangText.SETUP_NOT_ACTIVE);
            return;
        }
        getInstance().getSetupManager().stop(session);
    }

    @Command(ONE_ARENA + " check")
    @CommandDescription("command_arena_check")
    public void checkCmd(CommandSender sender, @Argument("arena") Arena arena) {
        List<Component> missing = arena.getMissingSetup();
        if (missing.isEmpty()) {
            sender.sendMessage(I.prefixed(I.i18n(LangText.ARENA_READY, arena.getName())
                    .append(I.button(LangText.BUTTON_CREATE_GAME,
                            ClickEvent.runCommand("/ctb arena " + arena.getName() + " creategame")))));
            return;
        }
        send(sender, LangText.ARENA_MISSING, arena.getName());
        for (Component entry : missing) {
            sender.sendMessage(I.i18n(LangText.ARENA_MISSING_ENTRY, entry));
        }
        if (sender instanceof Player) {
            sender.sendMessage(I.i18n(LangText.ARENA_SETUP_HINT)
                    .append(I.button(LangText.BUTTON_START_SETUP,
                            ClickEvent.runCommand("/ctb arena " + arena.getName() + " setup"))));
        }
    }

    @Command(ONE_ARENA + " edit")
    @CommandDescription("command_arena_edit")
    public void editCmd(Player player, @Argument("arena") Arena arena) {
        GamePlayer sender = user(player);
        MapTemplate.get(arena).join(sender);
        sender.sendMessage(LangText.WORLD_READY, arena.getName());
    }

    @Command(ONE_ARENA + " save")
    @CommandDescription("command_arena_save")
    public void saveCmd(Player player, @Argument("arena") Arena arena) {
        GamePlayer sender = user(player);
        if (MapTemplate.hasTemplate(arena)) {
            MapTemplate.get(arena).close();
            sender.sendMessage(LangText.WORLD_SAVED);
        } else {
            sender.sendMessage(LangText.WORLD_SAVE_FAILED, arena.getName());
        }
    }

    @Command(ONE_ARENA + " team <team> setspawn")
    @CommandDescription("command_arena_team_setspawn")
    public void teamSetSpawnCmd(Player player, @Argument("arena") Arena arena, @Argument("team") Team team) {
        GamePlayer sender = user(player);
        try {
            arena.setSpawnLocation(team, player.getLocation());
            sender.sendMessage(LangText.TEAM_SPAWN_SET, team.getDisplayName());
        } catch (MapInUseException e) {
            sender.sendMessage(e.componentMessage());
        }
    }

    @Command(ONE_ARENA + " team <team> setbeacon <position>")
    @CommandDescription("command_arena_team_setbeacon")
    public void teamSetBeaconCmd(Player player, @Argument("arena") Arena arena, @Argument("team") Team team,
                                 @Argument("position") Beacon.Position position) {
        GamePlayer sender = user(player);
        try {
            if (arena.setBeacon(team, player.getLocation(), position)) {
                sender.sendMessage(LangText.TEAM_BEACON_SET, position.getName(), team.getDisplayName());
            } else {
                sender.sendMessage(LangText.BEACON_NOT_FOUND);
            }
        } catch (MapInUseException e) {
            sender.sendMessage(e.componentMessage());
        }
    }

    @Command(ONE_ARENA + " lobby setworld <world>")
    @CommandDescription("command_arena_lobby_setworld")
    public void lobbySetWorldCmd(CommandSender sender, @Argument("arena") Arena arena,
                                 @Argument(value = "world", suggestions = "maps-and-worlds") String worldName) {
        MapHandler mapHandler = getInstance().getMapHandler();
        String mapName = worldName;
        if (!mapHandler.isMap(mapName)) {
            World world = MapHandler.getImportableWorld(worldName);
            if (world == null) {
                send(sender, LangText.MAP_OR_WORLD_NOT_FOUND, worldName);
                return;
            }
            mapName = MapHandler.toMapName(world.getName());
            if (!mapHandler.isMap(mapName) && !mapHandler.importWorld(world, mapName)) {
                send(sender, LangText.WORLD_COPY_FAILED, world.getName());
                return;
            }
            send(sender, LangText.WORLD_IMPORTED, world.getName(), mapName);
        }
        try {
            if (!arena.setLobbyWorld(mapName)) {
                send(sender, LangText.MAP_NOT_FOUND, mapName);
                return;
            }
        } catch (MapInUseException e) {
            sender.sendMessage(I.prefixed(e.componentMessage()));
            return;
        }
        Component message = I.i18n(LangText.LOBBY_WORLD_SET, arena.getName(), mapName);
        if (sender instanceof Player) {
            message = message.append(I.button(LangText.BUTTON_START_SETUP,
                    ClickEvent.runCommand("/ctb arena " + arena.getName() + " setup")));
        }
        sender.sendMessage(I.prefixed(message));
    }

    @Command(ONE_ARENA + " lobby edit")
    @CommandDescription("command_arena_lobby_edit")
    public void lobbyEditCmd(Player player, @Argument("arena") Arena arena) {
        GamePlayer sender = user(player);
        LobbyMap lobbyMap = arena.getLobbyMap();
        if (lobbyMap.getName() == null) {
            sender.sendMessage(LangText.LOBBY_MISSING, arena.getName());
            return;
        }
        MapTemplate.get(lobbyMap).join(sender);
        sender.sendMessage(LangText.LOBBY_WORLD_READY, arena.getName());
    }

    @Command(ONE_ARENA + " lobby save")
    @CommandDescription("command_arena_lobby_save")
    public void lobbySaveCmd(Player player, @Argument("arena") Arena arena) {
        GamePlayer sender = user(player);
        LobbyMap lobbyMap = arena.getLobbyMap();
        if (MapTemplate.hasTemplate(lobbyMap)) {
            MapTemplate.get(lobbyMap).close();
            sender.sendMessage(LangText.LOBBY_SAVED);
        } else {
            sender.sendMessage(LangText.LOBBY_SAVE_FAILED);
        }
    }

    @Command(ONE_ARENA + " lobby setspawn")
    @CommandDescription("command_arena_lobby_setspawn")
    public void lobbySetSpawnCmd(Player player, @Argument("arena") Arena arena) {
        GamePlayer sender = user(player);
        if (arena.getLobbyMap().getName() == null) {
            sender.sendMessage(LangText.LOBBY_MISSING, arena.getName());
            return;
        }
        arena.getLobbyMap().setSpawnLocation(player.getLocation());
        sender.sendMessage(LangText.LOBBY_SPAWN_SET);
    }

    @Command(ONE_ARENA + " creategame")
    @CommandDescription("command_arena_creategame")
    public void createGameCmd(CommandSender sender, @Argument("arena") Arena arena, @Flag("force") boolean force) {
        if (!arena.getLobbyMap().isPlayable() || (!force && !arena.isPlayable())) {
            checkCmd(sender, arena);
            return;
        }
        createGame(sender, arena);
    }

    /**
     * creates a game with the arena (also from the {@link GameMenu}) - a playable arena gets a game after every
     * restart, too
     *
     * @return the new game, null if it couldn't be created
     */
    @Nullable
    static Game createGame(CommandSender sender, Arena arena) {
        Game game;
        try {
            game = CatchTheBeacon.getInstance().getGameManager().createGame(arena);
        } catch (RuntimeException ex) {
            send(sender, LangText.GAME_CREATE_FAILED, ex.getMessage());
            Debug.warn("Couldn't create a game with arena '" + arena.getName() + "'", ex);
            return null;
        }
        send(sender, LangText.GAME_CREATED, game.getUniqueId(), game.getArena().getName(),
                game.getLobbyWorld().getWorldName());
        // --force is only for testing
        if (arena.isPlayable() && !arena.hasAutoGame()) {
            arena.setAutoGame(true);
            send(sender, LangText.AUTO_GAME_ENABLED, arena.getName());
        }
        return game;
    }

    @Command(ONE_ARENA + " removegame")
    @CommandDescription("command_arena_removegame")
    public void removeGameCmd(CommandSender sender, @Argument("arena") Arena arena) {
        arena.setAutoGame(false);
        int removed = getInstance().getGameManager().removeGames(arena);
        send(sender, removed == 0 ? LangText.NO_GAME_REMOVED : LangText.GAMES_REMOVED, arena.getName(), removed);
    }

    @Command(ROOT + " config items set")
    @CommandDescription("command_config_items_set")
    public void setItemsCmd(Player player) {
        GameItems.setDefaultItems(player.getInventory());
        user(player).sendMessage(LangText.ITEMS_SET);
    }

    @Command(ROOT + " config items get")
    @CommandDescription("command_config_items_get")
    public void getItemsCmd(Player player, @Flag("confirm") boolean confirm) {
        GamePlayer sender = user(player);
        if (!confirm) {
            sender.sendMessage(LangText.ITEMS_GET_CONFIRM);
            return;
        }
        if (sender.getTeam() == null) {
            sender.sendMessage(LangText.ITEMS_GET_NO_TEAM);
            return;
        }
        GameItems.setItems(sender);
    }

}
