package com.christian34.catchthebeacon.commands;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.UpdateChecker;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.game.states.LobbyState;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.stats.Achievement;
import com.christian34.catchthebeacon.stats.AchievementManager;
import com.christian34.catchthebeacon.stats.StatsManager;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * The general player and debug commands of /ctb.
 *
 * @author Christian34
 */
public class CommandCatchTheBeacon {
    static final String ROOT = "ctb|catchthebeacon|catchbeacon|ctbeacon";
    static final String ADMIN = "ctb.admin";
    static final String VIP = "ctb.vip";
    /**
     * number of players shown by /ctb top
     */
    static final int TOP_SIZE = 10;

    private static final Component DEBUG_HEADER = Component.text("---------------- ", NamedTextColor.DARK_GRAY)
            .append(Component.text("CatchTheBeacon", NamedTextColor.BLUE))
            .append(Component.text(" ----------------", NamedTextColor.DARK_GRAY));

    private final CommandManager commandManager;

    public CommandCatchTheBeacon(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    private CatchTheBeacon getInstance() {
        return CatchTheBeacon.getInstance();
    }

    @Command(ROOT)
    @CommandDescription("command_version")
    public void mainCmd(CommandSender sender) {
        sender.sendMessage(I.prefixed(LangText.PLUGIN_INFO, getInstance().getPluginMeta().getVersion()));
    }

    @Command(ROOT + " help [query]")
    @CommandDescription("command_help")
    public void helpCmd(CommandSender sender, @Argument("query") @Greedy @Nullable String query) {
        commandManager.getMinecraftHelp().queryCommands(query == null ? "" : query, sender);
    }

    @Command(ROOT + " join")
    @CommandDescription("command_join")
    public void joinCmd(Player player) {
        GamePlayer gamePlayer = getInstance().getUser(player);
        if (gamePlayer.getGame() != null) {
            gamePlayer.sendMessage(LangText.ALREADY_IN_GAME);
            return;
        }
        if (getInstance().getNetworkManager().isLobby()) {
            getInstance().getNetworkManager().joinAnyServer(player, false);
            return;
        }
        // watches a running game if no game is waiting for players
        getInstance().getGameManager().join(gamePlayer, null);
    }

    @Command(ROOT + " spectate")
    @CommandDescription("command_spectate")
    public void spectateCmd(Player player) {
        GamePlayer gamePlayer = getInstance().getUser(player);
        if (gamePlayer.getGame() != null) {
            gamePlayer.sendMessage(LangText.ALREADY_IN_GAME);
            return;
        }
        if (getInstance().getNetworkManager().isLobby()) {
            getInstance().getNetworkManager().joinAnyServer(player, true);
            return;
        }
        Game game = getInstance().getGameManager().findSpectatableGame(null);
        if (game == null) {
            gamePlayer.sendMessage(LangText.NO_GAME_TO_SPECTATE);
            return;
        }
        game.spectate(gamePlayer);
    }

    @Command(ROOT + " stats [player]")
    @CommandDescription("command_stats")
    public void statsCmd(CommandSender sender, @Argument("player") @Nullable String name) {
        StatsManager stats = getInstance().getStatsManager();
        if (!stats.isEnabled()) {
            sender.sendMessage(I.prefixed(LangText.STATS_DISABLED));
            return;
        }
        Player player = name == null ? (sender instanceof Player self ? self : null) : Bukkit.getPlayerExact(name);
        if (name == null && player == null) {
            sender.sendMessage(I.prefixed(LangText.PLAYER_NOT_FOUND));
            return;
        }
        // loaded from the database (the stats may have changed on another server of the network)
        CompletableFuture<StatsManager.NamedStats> future = player != null
                ? stats.load(player.getUniqueId()).thenApply(loaded -> new StatsManager.NamedStats(player.getName(), loaded))
                : stats.find(name);
        future.whenComplete((result, error) -> Bukkit.getScheduler().runTask(getInstance(), () -> {
            if (error != null) {
                sender.sendMessage(I.prefixed(LangText.STATS_LOAD_FAILED));
            } else if (result == null) {
                sender.sendMessage(I.prefixed(LangText.PLAYER_NOT_FOUND));
            } else {
                StatsManager.PlayerStats playerStats = result.stats();
                sender.sendMessage(i18n(LangText.STATS_TITLE, result.name()));
                sender.sendMessage(i18n(LangText.STATS, playerStats.kills(), playerStats.deaths(),
                        playerStats.killDeathRatio(), playerStats.wins(), playerStats.games(), playerStats.beacons()));
            }
        }));
    }

    @Command(ROOT + " top [stat]")
    @CommandDescription("command_top")
    public void topCmd(CommandSender sender, @Argument("stat") StatsManager.@Nullable Stat stat) {
        StatsManager stats = getInstance().getStatsManager();
        if (!stats.isEnabled()) {
            sender.sendMessage(I.prefixed(LangText.STATS_DISABLED));
            return;
        }
        StatsManager.Stat ranked = stat == null ? StatsManager.Stat.WINS : stat;
        // the own rank if the sender is not in the list
        CompletableFuture<StatsManager.@Nullable RankedStat> own = sender instanceof Player player
                ? stats.rank(player.getUniqueId(), ranked) : CompletableFuture.completedFuture(null);
        stats.top(ranked, TOP_SIZE).thenCombine(own, (top, rank) -> {
            List<Component> lines = new ArrayList<>();
            lines.add(i18n(LangText.TOP_TITLE, TOP_SIZE, i18n(ranked.getDisplayName())));
            if (top.isEmpty()) lines.add(i18n(LangText.TOP_EMPTY));
            for (StatsManager.RankedStat entry : top) {
                lines.add(i18n(LangText.TOP_ENTRY, entry.rank(), entry.name(), entry.value()));
            }
            if (rank != null && top.stream().noneMatch(entry -> Objects.equals(entry.name(), rank.name()))) {
                lines.add(i18n(LangText.TOP_OWN_RANK, rank.rank(), rank.value()));
            }
            return lines;
        }).whenComplete((lines, error) -> Bukkit.getScheduler().runTask(getInstance(), () -> {
            if (error != null) {
                sender.sendMessage(I.prefixed(LangText.STATS_LOAD_FAILED));
            } else {
                lines.forEach(sender::sendMessage);
            }
        }));
    }

    @Command(ROOT + " achievements [player]")
    @CommandDescription("command_achievements")
    public void achievementsCmd(CommandSender sender, @Argument("player") @Nullable String name) {
        AchievementManager achievements = getInstance().getAchievementManager();
        if (!achievements.isEnabled()) {
            sender.sendMessage(I.prefixed(LangText.ACHIEVEMENTS_DISABLED));
            return;
        }
        Player player = name == null ? (sender instanceof Player self ? self : null) : Bukkit.getPlayerExact(name);
        if (name == null && player == null) {
            sender.sendMessage(I.prefixed(LangText.PLAYER_NOT_FOUND));
            return;
        }
        String displayName = player != null ? player.getName() : name;
        CompletableFuture<UUID> uuid = player != null ? CompletableFuture.completedFuture(player.getUniqueId())
                : getInstance().getStatsManager().findUuid(name);
        uuid.thenCompose(found -> found == null ? CompletableFuture.completedFuture(null) : achievements.load(found))
                .whenComplete((owned, error) -> Bukkit.getScheduler().runTask(getInstance(), () -> {
                    if (error != null) {
                        sender.sendMessage(I.prefixed(LangText.ACHIEVEMENTS_LOAD_FAILED));
                    } else if (owned == null) {
                        sender.sendMessage(I.prefixed(LangText.PLAYER_NOT_FOUND));
                    } else {
                        sender.sendMessage(i18n(LangText.ACHIEVEMENTS_TITLE, displayName, owned.size(),
                                Achievement.values().length));
                        for (Achievement achievement : Achievement.values()) {
                            sender.sendMessage(i18n(owned.contains(achievement) ? LangText.ACHIEVEMENT_ENTRY
                                    : LangText.ACHIEVEMENT_ENTRY_LOCKED, achievement.getName(), achievement.getDescription()));
                        }
                    }
                }));
    }

    @Command(ROOT + " quit")
    @CommandDescription("command_quit")
    public void quitCmd(Player player) {
        GamePlayer gamePlayer = getInstance().getUser(player);
        Game game = gamePlayer.getGame();
        if (game == null) {
            gamePlayer.sendMessage(LangText.NOT_IN_GAME);
        } else {
            game.quit(gamePlayer);
            getInstance().getNetworkManager().sendToLobby(player);
        }
    }

    @Command(ROOT + " start")
    @CommandDescription("command_start")
    @Permission(VIP)
    public void startCmd(Player player) {
        GamePlayer gamePlayer = getInstance().getUser(player);
        Game game = gamePlayer.getGame();
        if (game == null) {
            gamePlayer.sendMessage(LangText.CMD_ONLY_INGAME);
            return;
        }
        if (game.getGameState() == GameState.LOBBY
                && ((LobbyState) game.getGameStateManager().getCurrentGameState()).forceStart()) {
            gamePlayer.sendMessage(LangText.FORCE_START);
        } else {
            gamePlayer.sendMessage(LangText.GAME_WAS_ALREADY_STARTED);
        }
    }

    @Command(ROOT + " tp")
    @CommandDescription("command_tp")
    @Permission(ADMIN)
    public void teleportCmd(Player player) {
        player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith("ctb_")) {
                Bukkit.unloadWorld(world, false);
            }
        }
    }

    @Command(ROOT + " debug")
    @CommandDescription("command_debug")
    @Permission(ADMIN)
    public void debugCmd(CommandSender sender) {
        sender.sendMessage(DEBUG_HEADER);
        if (sender instanceof Player player) {
            GamePlayer gamePlayer = getInstance().getUser(player);
            Game game = gamePlayer.getGame();
            if (game != null) {
                sender.sendMessage(debugLine("active game", "YES"));
                sender.sendMessage(debugLine("Game ID", game.getUniqueId()));
            } else {
                sender.sendMessage(debugLine("active game", "NO"));
            }
            sender.sendMessage(debugLine("world", player.getWorld().getName()));
            sender.sendMessage(debugLine("team", gamePlayer.getTeam()));
        }
        sender.sendMessage(debugLine("Arenas", getInstance().getMapHandler().getGameMaps().size()));
        sender.sendMessage(debugLine("Games", getInstance().getGameManager().getGames().size()));
        sender.sendMessage(debugLine("Version", getInstance().getPluginMeta().getVersion()));
        UpdateChecker.Update update = getInstance().getUpdateChecker().getUpdate();
        sender.sendMessage(debugLine("Latest version",
                update == null ? "installed (or not checked)" : update.version() + " (" + update.behind() + " behind)"));
        sender.sendMessage(debugLine("Users cached", getInstance().getUsers().size()));
        sender.sendMessage(debugLine("Server Version", Bukkit.getVersion()));
        sender.sendMessage(debugLine("Java Version", System.getProperty("java.version")));
        sender.sendMessage(Component.text("----------------------------------------------------", NamedTextColor.DARK_GRAY));
    }

    @Command(ROOT + " debug info [player]")
    @CommandDescription("command_debug_info")
    @Permission(ADMIN)
    public void debugInfoCmd(CommandSender sender, @Argument(value = "player", suggestions = "ingame-players") @Nullable String name) {
        GamePlayer gamePlayer = findPlayer(sender, name);
        if (gamePlayer == null) {
            sender.sendMessage(I.prefixed(LangText.PLAYER_NOT_FOUND));
            return;
        }
        Game game = gamePlayer.getGame();
        if (game == null) {
            sender.sendMessage(I.prefixed(LangText.PLAYER_NOT_PLAYING, gamePlayer.getPlayer().getName()));
            return;
        }
        sender.sendMessage(DEBUG_HEADER);
        sender.sendMessage(debugLine("game", game.getUniqueId()));
        sender.sendMessage(debugLine("arena", game.getArena().getName()));
        sender.sendMessage(debugLine("gamestate", game.getGameState().name()));
        sender.sendMessage(debugLine("players", game.getGamePlayers().size()));
        sender.sendMessage(debugLine("team", gamePlayer.getTeam() == null ? "-" : gamePlayer.getTeam().getName()));
    }

    @Command(ROOT + " debug sentry")
    @CommandDescription("command_debug_sentry")
    @Permission(ADMIN)
    public void debugSentryCmd(CommandSender sender) {
        if (getInstance().getTelemetry().sendTestReport()) {
            sender.sendMessage(I.prefixed(LangText.SENTRY_REPORT_SENT));
        } else {
            sender.sendMessage(I.prefixed(LangText.SENTRY_DISABLED));
        }
    }

    @Command(ROOT + " debug setstate <state>")
    @CommandDescription("command_debug_setstate")
    @Permission(ADMIN)
    public void debugSetStateCmd(Player player, @Argument("state") GameState state) {
        GamePlayer gamePlayer = getInstance().getUser(player);
        Game game = gamePlayer.getGame();
        if (game == null) {
            gamePlayer.sendMessage(LangText.CMD_ONLY_INGAME);
            return;
        }
        game.getGameStateManager().setGameState(state);
        gamePlayer.sendMessage(LangText.GAME_STATE_SET, state.name());
    }

    /**
     * a line of the debug information (only for admins, so not in messages.yml)
     */
    private static Component debugLine(String name, Object value) {
        return Component.text(name + ": ", NamedTextColor.BLUE).append(Component.text(String.valueOf(value), NamedTextColor.GRAY));
    }

    @Suggestions("ingame-players")
    public List<String> ingamePlayers(CommandContext<CommandSender> context, String input) {
        List<String> names = new ArrayList<>();
        synchronized (getInstance().getUsers()) {
            for (GamePlayer gamePlayer : getInstance().getUsers()) {
                names.add(gamePlayer.getPlayer().getName());
            }
        }
        return names;
    }

    @Nullable
    private GamePlayer findPlayer(CommandSender sender, @Nullable String name) {
        if (name == null) {
            return sender instanceof Player player ? getInstance().getUser(player) : null;
        }
        synchronized (getInstance().getUsers()) {
            for (GamePlayer gamePlayer : getInstance().getUsers()) {
                if (gamePlayer.getPlayer().getName().toLowerCase().startsWith(name.toLowerCase())) {
                    return gamePlayer;
                }
            }
        }
        return null;
    }

}
