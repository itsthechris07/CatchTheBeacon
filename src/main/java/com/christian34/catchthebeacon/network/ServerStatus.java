package com.christian34.catchthebeacon.network;

import com.christian34.catchthebeacon.game.Game;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The state of the game on a game server. Game servers send it as their MOTD ("CTB;LOBBY;3;8;castle"), the lobby
 * server reads it with {@link ServerPinger}.
 *
 * @author Christian34
 */
public record ServerStatus(State state, int players, int maxPlayers, String arena) {
    public static final String MOTD_PREFIX = "CTB";
    public static final ServerStatus OFFLINE = new ServerStatus(State.OFFLINE, 0, 0, "");

    public enum State {
        /**
         * waiting for players
         */
        LOBBY,
        INGAME,
        /**
         * ending or restarting
         */
        ENDING,
        /**
         * the server isn't reachable or has no game
         */
        OFFLINE
    }

    /**
     * @return the status of the game (OFFLINE if there is none)
     */
    public static ServerStatus of(@Nullable Game game) {
        if (game == null) return OFFLINE;
        State state = switch (game.getGameState()) {
            case PENDING, LOBBY -> State.LOBBY;
            case INGAME -> State.INGAME;
            default -> State.ENDING;
        };
        return new ServerStatus(state, game.getGamePlayers().size(), game.getArena().getMaxPlayers(), game.getArena().getName());
    }

    public String toMotd() {
        return String.join(";", MOTD_PREFIX, state.name(), players + "", maxPlayers + "", arena);
    }

    /**
     * @return the status in the MOTD, OFFLINE if it isn't one (e.g. not a game server)
     */
    @NotNull
    public static ServerStatus parse(@Nullable String motd) {
        if (motd == null) return OFFLINE;
        String[] parts = motd.trim().split(";", 5);
        if (parts.length != 5 || !parts[0].equals(MOTD_PREFIX)) return OFFLINE;
        try {
            return new ServerStatus(State.valueOf(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4]);
        } catch (IllegalArgumentException ex) {
            return OFFLINE;
        }
    }

    /**
     * @return true if players can join the game
     */
    public boolean isJoinable() {
        return state == State.LOBBY && players < maxPlayers;
    }

    /**
     * @return true if the game is running (players are sent there to watch it, if the server allows spectators)
     */
    public boolean isSpectatable() {
        return state == State.INGAME;
    }

}
