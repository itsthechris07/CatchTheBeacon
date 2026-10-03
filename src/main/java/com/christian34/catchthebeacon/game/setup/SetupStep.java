package com.christian34.catchthebeacon.game.setup;

import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.map.Arena;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The steps of the guided arena setup, in the order they are suggested.
 *
 * @author Christian34
 */
public enum SetupStep {
    LOBBY_SPAWN(true, null, null),
    RED_SPAWN(false, "RED", null),
    BLUE_SPAWN(false, "BLUE", null),
    RED_BEACON_LEFT(false, "RED", Beacon.Position.LEFT),
    RED_BEACON_RIGHT(false, "RED", Beacon.Position.RIGHT),
    BLUE_BEACON_LEFT(false, "BLUE", Beacon.Position.LEFT),
    BLUE_BEACON_RIGHT(false, "BLUE", Beacon.Position.RIGHT);

    private final boolean inLobby;
    // the name, as the Team enum reads teams.yml when it is loaded
    private final String teamName;
    private final Beacon.Position beaconPosition;

    SetupStep(boolean inLobby, String teamName, Beacon.Position beaconPosition) {
        this.inLobby = inLobby;
        this.teamName = teamName;
        this.beaconPosition = beaconPosition;
    }

    /**
     * @return true if the step is done in the lobby world, false if in the arena world
     */
    public boolean isInLobby() {
        return inLobby;
    }

    @Nullable
    public Team getTeam() {
        return teamName == null ? null : Team.valueOf(teamName);
    }

    @Nullable
    public Beacon.Position getBeaconPosition() {
        return beaconPosition;
    }

    public boolean isBeacon() {
        return beaconPosition != null;
    }

    public int getNumber() {
        return ordinal() + 1;
    }

    public Component getDescription() {
        if (this == LOBBY_SPAWN) return I.i18n(LangText.SETUP_STEP_LOBBY_SPAWN);
        Team team = getTeam();
        assert team != null;
        if (isBeacon()) return I.i18n(LangText.SETUP_STEP_BEACON, beaconPosition.getName(), team.getName());
        return I.i18n(LangText.SETUP_STEP_SPAWN, team.getName());
    }

    public Component getInstruction() {
        if (this == LOBBY_SPAWN) return I.i18n(LangText.SETUP_INSTRUCTION_LOBBY_SPAWN);
        if (isBeacon()) return I.i18n(LangText.SETUP_INSTRUCTION_BEACON, getDescription());
        return I.i18n(LangText.SETUP_INSTRUCTION_SPAWN, getTeam().getName());
    }

    public boolean isDone(Arena arena) {
        if (this == LOBBY_SPAWN) return arena.getLobbyMap().getSpawnLocation() != null;
        Team team = getTeam();
        assert team != null;
        if (isBeacon()) return arena.getBeaconLocations(team).containsKey(beaconPosition);
        return arena.hasSpawnLocation(team);
    }

    /**
     * @return the first step that is not done yet, null if the arena is set up completely
     */
    @Nullable
    public static SetupStep next(Arena arena) {
        for (SetupStep step : values()) {
            if (!step.isDone(arena)) return step;
        }
        return null;
    }

}
