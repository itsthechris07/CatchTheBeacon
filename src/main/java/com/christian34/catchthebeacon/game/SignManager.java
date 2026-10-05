package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.game.map.Arena;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.network.NetworkManager;
import com.christian34.catchthebeacon.network.ServerStatus;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * Join signs: an admin writes "[ctb]" and the name of an arena on a sign. It shows the state of the game and
 * players join it (or watch it while it is running) by clicking the sign. Arenas with signs get a game on start.
 * On the lobby server of a network, signs show a game server instead (see {@link NetworkManager}).
 *
 * @author Christian34
 */
public class SignManager implements Listener {
    /**
     * first line of a new sign
     */
    public static final String SIGN_KEY = "[ctb]";
    private final CatchTheBeacon plugin;
    private final File file;
    private final List<JoinSign> signs = new ArrayList<>();
    /**
     * every sign shows its own game of the arena, until that game is over (then the next round)
     */
    private final Map<JoinSign, Game> linkedGames = new HashMap<>();

    /**
     * @param target the arena, on a lobby server of a network the game server
     */
    public record JoinSign(String world, int x, int y, int z, String target) {

        boolean is(Block block) {
            return block.getWorld().getName().equals(world) && block.getX() == x && block.getY() == y && block.getZ() == z;
        }

        String serialize() {
            return world + ";" + x + ";" + y + ";" + z + ";" + target;
        }

        @Nullable
        static JoinSign parse(String text) {
            String[] parts = text.split(";");
            if (parts.length != 5) return null;
            try {
                return new JoinSign(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]), parts[4]);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    public SignManager(CatchTheBeacon plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "signs.yml");
        for (String line : YamlConfiguration.loadConfiguration(file).getStringList("signs")) {
            JoinSign sign = JoinSign.parse(line);
            if (sign != null) signs.add(sign);
        }
        if (!plugin.getNetworkManager().isLobby()) {
            for (JoinSign sign : signs) {
                Arena arena = plugin.getMapHandler().getArena(sign.target());
                if (arena != null) ensureGame(arena);
            }
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    /**
     * creates games for the arena, so that every join sign of it has its own one (at least one game, the games
     * themselves aren't saved)
     */
    public void ensureGame(Arena arena) {
        if (!arena.isPlayable()) return;
        long needed = Math.max(1, signs.stream().filter(sign -> sign.target().equals(arena.getName())).count());
        try {
            for (long i = plugin.getGameManager().getGames(arena).size(); i < needed; i++) {
                plugin.getGameManager().createGame(arena);
            }
        } catch (RuntimeException ex) {
            Debug.warn("Couldn't create a game for the join sign of arena '" + arena.getName() + "': " + ex.getMessage());
        }
    }

    public List<JoinSign> getSigns() {
        return signs;
    }

    @Nullable
    public JoinSign getSign(Block block) {
        for (JoinSign sign : signs) {
            if (sign.is(block)) return sign;
        }
        return null;
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("signs", signs.stream().map(JoinSign::serialize).toList());
        try {
            data.save(file);
        } catch (IOException ex) {
            Debug.warn("Couldn't save signs.yml: " + ex.getMessage());
        }
    }

    private void updateAll() {
        for (JoinSign sign : signs) {
            World world = Bukkit.getWorld(sign.world());
            if (world == null || !world.isChunkLoaded(sign.x() >> 4, sign.z() >> 4)) continue;
            if (!(world.getBlockAt(sign.x(), sign.y(), sign.z()).getState() instanceof Sign state)) continue;
            List<Component> lines = getLines(sign);
            SignSide side = state.getSide(Side.FRONT);
            boolean changed = false;
            for (int i = 0; i < lines.size(); i++) {
                if (!lines.get(i).equals(side.line(i))) {
                    side.line(i, lines.get(i));
                    changed = true;
                }
            }
            // updating sends the sign to the players, so only on changes
            if (changed) state.update();
        }
    }

    /**
     * @return the four lines: plugin, arena (or server), state, players
     */
    List<Component> getLines(JoinSign sign) {
        String target = sign.target();
        Component name;
        if (plugin.getNetworkManager().isLobby()) {
            name = Component.text(target);
        } else {
            Arena arena = plugin.getMapHandler().getArena(target);
            name = arena == null ? Component.text(target) : arena.getDisplayName();
        }
        ServerStatus status = plugin.getNetworkManager().isLobby() ? getStatus(target) : ServerStatus.of(getGame(sign));
        return List.of(
                i18n(LangText.SIGN_TITLE),
                i18n(LangText.SIGN_NAME, name),
                stateText(status),
                status.state() == ServerStatus.State.OFFLINE ? Component.empty()
                        : i18n(LangText.SIGN_PLAYERS, status.players(), status.maxPlayers()));
    }

    /**
     * @return the game shown on the sign: the one players can join, otherwise a running one
     */
    @Nullable
    private Game getGame(Arena arena) {
        Game game = plugin.getGameManager().findJoinableGame(arena);
        if (game != null) return game;
        List<Game> games = plugin.getGameManager().getGames(arena);
        return games.isEmpty() ? null : games.getFirst();
    }

    /**
     * @return the game of the sign: the one it showed until now, otherwise one no other sign shows (preferably the
     * one players can join), null if the arena has no game or on a lobby server
     */
    @Nullable
    public Game getGame(JoinSign sign) {
        if (plugin.getNetworkManager().isLobby()) return null;
        Arena arena = plugin.getMapHandler().getArena(sign.target());
        if (arena == null) return null;
        List<Game> games = plugin.getGameManager().getGames(arena);
        // over (the next round of the arena takes its place)
        linkedGames.values().removeIf(game -> !plugin.getGameManager().getGames().contains(game));
        Game linked = linkedGames.get(sign);
        if (linked != null) return linked;
        Collection<Game> taken = linkedGames.values();
        Game game = games.stream().filter(Game::isJoinable).filter(g -> !taken.contains(g)).findFirst()
                .orElse(games.stream().filter(g -> !taken.contains(g)).findFirst().orElse(null));
        // more signs than games: show one of them, but don't take it from its sign
        if (game == null) return getGame(arena);
        linkedGames.put(sign, game);
        return game;
    }

    private static String plain(@Nullable Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component).trim();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent e) {
        if (!plain(e.line(0)).equalsIgnoreCase(SIGN_KEY) || !e.getPlayer().hasPermission("ctb.admin")) return;
        String name = plain(e.line(1));
        String target = resolveTarget(e.getPlayer(), name);
        if (target == null) return;
        Component created = i18n(plugin.getNetworkManager().isLobby() ? LangText.SIGN_CREATED_SERVER : LangText.SIGN_CREATED, target);
        Block block = e.getBlock();
        JoinSign old = getSign(block);
        if (old != null) remove(old);
        JoinSign sign = new JoinSign(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), target);
        signs.add(sign);
        save();
        Arena arena = plugin.getMapHandler().getArena(target);
        if (arena != null) ensureGame(arena);
        List<Component> lines = getLines(sign);
        for (int i = 0; i < lines.size(); i++) {
            e.line(i, lines.get(i));
        }
        e.getPlayer().sendMessage(I.prefixed(created));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        JoinSign sign = getSign(e.getClickedBlock());
        if (sign == null) return;
        // no sign editor
        e.setCancelled(true);
        Game game = getGame(sign);
        GamePlayer gamePlayer = plugin.getUser(e.getPlayer());
        if (game != null && gamePlayer.getGame() == null) {
            plugin.getGameManager().join(gamePlayer, game);
        } else {
            join(e.getPlayer(), sign.target());
        }
    }

    private void remove(JoinSign sign) {
        signs.remove(sign);
        linkedGames.remove(sign);
    }

    /**
     * checks the target of a sign or NPC (and creates a game for the arena if it has none)
     *
     * @param name the arena, on a lobby server the game server
     * @return the name of the arena or server, null if it doesn't exist (the sender got a message)
     */
    @Nullable
    public String resolveTarget(CommandSender sender, String name) {
        if (plugin.getNetworkManager().isLobby()) {
            NetworkManager.Server server = plugin.getNetworkManager().getServer(name);
            if (server == null) {
                sender.sendMessage(I.prefixed(LangText.SERVER_NOT_FOUND, name));
                return null;
            }
            return server.name();
        }
        Arena arena = plugin.getMapHandler().getArena(name);
        if (arena == null) {
            sender.sendMessage(I.prefixed(LangText.ARENA_NOT_FOUND, name));
            return null;
        }
        ensureGame(arena);
        return arena.getName();
    }

    /**
     * @param target the arena, on a lobby server the game server
     * @return the state of its game (for signs, NPCs and placeholders)
     */
    public ServerStatus getStatus(String target) {
        if (plugin.getNetworkManager().isLobby()) return plugin.getNetworkManager().getStatus(target);
        Arena arena = plugin.getMapHandler().getArena(target);
        return ServerStatus.of(arena == null ? null : getGame(arena));
    }

    /**
     * @return the state as on the signs ("Waiting", "Running", ...)
     */
    public static Component stateText(ServerStatus status) {
        return i18n(switch (status.state()) {
            case LOBBY -> LangText.SIGN_STATUS_LOBBY;
            case INGAME -> LangText.SIGN_STATUS_INGAME;
            case ENDING -> LangText.SIGN_STATUS_ENDING;
            case OFFLINE -> LangText.SIGN_STATUS_OFFLINE;
        });
    }

    /**
     * joins the game of the arena (or watches it), on a lobby server connects to the game server
     *
     * @param target the arena, on a lobby server the game server
     */
    public void join(Player player, String target) {
        GamePlayer gamePlayer = plugin.getUser(player);
        if (gamePlayer.getGame() != null) return;
        if (plugin.getNetworkManager().isLobby()) {
            plugin.getNetworkManager().joinServer(player, target);
            return;
        }
        plugin.getGameManager().join(gamePlayer, plugin.getMapHandler().getArena(target));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        JoinSign sign = getSign(e.getBlock());
        if (sign == null) return;
        // sneaking, so the sign isn't removed by accident
        if (e.getPlayer().hasPermission("ctb.admin") && e.getPlayer().isSneaking()) {
            remove(sign);
            save();
            e.getPlayer().sendMessage(I.prefixed(LangText.SIGN_REMOVED));

        } else {
            e.setCancelled(true);
        }
    }

}
