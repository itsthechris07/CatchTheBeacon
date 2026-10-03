package com.christian34.catchthebeacon.user;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;


public class GamePlayer {
    private final Player player;
    private final UserStorage userStorage;
    private final CatchTheBeacon plugin;
    private WeakReference<Game> game;
    private Team team;
    private int kills;
    /**
     * kills since the last death
     */
    private int killStreak;
    private int protectedUntilTick;
    private UUID lastAttacker;
    private int lastAttackTick;
    private boolean spectator;
    private int campingSeconds;

    public GamePlayer(Player player, CatchTheBeacon plugin) {
        this.player = player;
        this.userStorage = new UserStorage(player);
        this.plugin = plugin;
        this.team = null;
    }

    /**
     * @return the display name of the player, in the team color if it has no own color
     */
    public Component getDisplayName() {
        Component name = getPlayer().displayName();
        return getTeam() == null ? name : name.colorIfAbsent(getTeam().getColor());
    }

    /**
     * the player can't get damage for the given time (after a respawn)
     */
    public void protectFor(int seconds) {
        this.protectedUntilTick = Bukkit.getCurrentTick() + seconds * 20;
    }

    public void endProtection() {
        this.protectedUntilTick = 0;
    }

    public boolean isProtected() {
        return Bukkit.getCurrentTick() < protectedUntilTick;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        kills++;
        killStreak++;
    }

    public int getKillStreak() {
        return killStreak;
    }

    /**
     * the player died
     */
    public void resetKillStreak() {
        this.killStreak = 0;
    }

    /**
     * when rejoining a game
     */
    public void setKills(int kills) {
        this.kills = kills;
    }

    /**
     * remembers the enemy who hit the player last (the kill is his if the player dies soon after, e.g. in the void)
     */
    public void setLastAttacker(@Nullable UUID attacker) {
        this.lastAttacker = attacker;
        this.lastAttackTick = Bukkit.getCurrentTick();
    }

    /**
     * @return the enemy who hit the player within the last seconds, null if there is none
     */
    @Nullable
    public UUID getLastAttacker(int seconds) {
        if (lastAttacker == null || seconds <= 0 || Bukkit.getCurrentTick() - lastAttackTick > seconds * 20) return null;
        return lastAttacker;
    }

    /**
     * @return seconds the player has been standing near his own beacon (base.camping-seconds)
     */
    public int getCampingSeconds() {
        return campingSeconds;
    }

    public void setCampingSeconds(int campingSeconds) {
        this.campingSeconds = campingSeconds;
    }

    public boolean isSpectator() {
        return spectator;
    }

    public void setSpectator(boolean spectator) {
        this.spectator = spectator;
    }

    public Team getTeam() {
        return team;
    }

    /**
     * @param team the team the player wants to be on
     * @return false if team is full
     */
    public boolean setTeam(Team team) {
        Game game = getGame();
        if (game == null) throw new IllegalArgumentException("The player is not in a game!");

        if (!team.equals(Team.RANDOM) && !team.equals(this.team)
                && game.getGamePlayers(team).size() >= game.getArena().getMaxPlayers() / 2) {
            return false;
        }
        this.team = team;
        return true;
    }

    /**
     * Sets the team without checking whether it is full (used to split the players when the game starts).
     */
    public void assignTeam(Team team) {
        this.team = team;
    }

    public void teleport(Location location) {
        this.player.teleport(location);
    }

    /**
     * sends the message with the prefix
     */
    public void sendMessage(Component message) {
        this.player.sendMessage(I.prefixed(message));
    }

    public void sendMessage(LangText message, Object... arguments) {
        sendMessage(I.i18n(message, arguments));
    }

    public UserStorage getUserStorage() {
        return userStorage;
    }

    @Nullable
    public Game getGame() {
        if (game == null) return null;
        return game.get();
    }

    public void setGame(Game game) {
        this.game = new WeakReference<>(game);
    }

    public Player getPlayer() {
        return player;
    }

}
