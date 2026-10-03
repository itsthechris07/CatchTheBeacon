package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Beacon {
    private final Position position;
    private final Team team;
    private final Game game;
    /**
     * the enemy mining the beacon right now (null: nobody), see {@link #updateMining}
     */
    @Nullable
    private UUID miner;
    private float miningProgress;
    private int lastMiningTick;
    @Nullable
    private BossBar bossBar;
    private final Set<Player> bossBarViewers = new HashSet<>();
    private boolean isAlive;
    private boolean isInDanger;
    private boolean trapArmed = true;
    private Location location;

    public Beacon(Position position, Team team, Game game) {
        this.position = position;
        this.team = team;
        this.game = game;
        this.isAlive = true;
    }

    public Team getTeam() {
        return team;
    }

    public Position getPosition() {
        return position;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }

    public void destroy() {
        this.isAlive = false;
    }

    /**
     * @return true until an enemy has run into the trap of the beacon (once per game)
     */
    public boolean isTrapArmed() {
        return trapArmed;
    }

    public void disarmTrap() {
        this.trapArmed = false;
    }

    /**
     * an enemy mines the beacon: remembers him and shows the progress as boss bar (if bossBar)
     *
     * @param progress 0 - 1
     * @param tick     the current server tick
     * @param viewers  everybody in the game
     * @return true if nobody was mining it before (the mining has just started)
     */
    public boolean updateMining(Player player, float progress, int tick, boolean showBossBar, Collection<Player> viewers) {
        boolean started = miner == null;
        this.miner = player.getUniqueId();
        this.miningProgress = Math.clamp(progress, 0f, 1f);
        this.lastMiningTick = tick;
        if (!showBossBar) return started;
        Component name = I.i18n(LangText.BOSSBAR_MINING, team.getDisplayName(), position.getName(),
                Math.round(miningProgress * 100));
        BossBar.Color color = team == Team.BLUE ? BossBar.Color.BLUE : BossBar.Color.RED;
        if (bossBar == null) {
            bossBar = BossBar.bossBar(name, miningProgress, color, BossBar.Overlay.NOTCHED_10);
        } else {
            bossBar.name(name).progress(miningProgress);
        }
        for (Player viewer : viewers) {
            if (bossBarViewers.add(viewer)) viewer.showBossBar(bossBar);
        }
        return started;
    }

    /**
     * nobody mines the beacon any more (stopped, died or destroyed it): hides the boss bar
     */
    public void stopMining() {
        this.miner = null;
        this.miningProgress = 0;
        if (bossBar != null) {
            for (Player viewer : bossBarViewers) {
                viewer.hideBossBar(bossBar);
            }
        }
        bossBarViewers.clear();
    }

    /**
     * the player doesn't see the boss bar any more (left the game)
     */
    public void hideBossBar(Player player) {
        if (bossBar != null && bossBarViewers.remove(player)) player.hideBossBar(bossBar);
    }

    @Nullable
    public UUID getMiner() {
        return miner;
    }

    public float getMiningProgress() {
        return miningProgress;
    }

    /**
     * @return the tick of the last progress (the mining has stopped if there is none for a while)
     */
    public int getLastMiningTick() {
        return lastMiningTick;
    }

    @Nullable
    public BossBar getBossBar() {
        return bossBar;
    }

    public boolean isInDanger() {
        return isInDanger;
    }

    public void setInDanger(boolean inDanger) {
        this.isInDanger = inDanger;
    }

    public enum Position {
        LEFT(LangText.BEACON_LEFT),
        RIGHT(LangText.BEACON_RIGHT);

        private final LangText translation;

        Position(LangText translation) {
            this.translation = translation;
        }

        @Nullable
        public static Position get(String name) {
            for (Position position : values()) {
                if (position.name().equalsIgnoreCase(name)) return position;
            }
            return null;
        }

        public Component getName() {
            return I.i18n(translation);
        }

    }

}
