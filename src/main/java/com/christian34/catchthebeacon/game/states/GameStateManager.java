package com.christian34.catchthebeacon.game.states;

import com.christian34.catchthebeacon.game.Game;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class GameStateManager {
    private final Game game;
    private final ArrayList<State> states;
    private State currentGameState;

    public GameStateManager(Game game) {
        this.game = game;
        this.states = new ArrayList<>();
        states.add(new PendingState(game));
        states.add(new LobbyState(game));
        states.add(new IngameState(game));
        states.add(new EndingState(game));
        states.add(new RestartState(game));
        this.currentGameState = getGameState(GameState.PENDING);
    }

    public State getCurrentGameState() {
        return currentGameState;
    }

    public void setGameState(GameState newState) {
        this.game.log("setting state to " + newState.name());
        if (currentGameState != null) {
            currentGameState.stop();
        }
        this.currentGameState = getGameState(newState);
        this.currentGameState.start();
    }

    /**
     * the game is stopped: the tasks of the current state (countdowns, ...) don't run any more
     */
    public void cancel() {
        if (currentGameState != null) currentGameState.cancel();
    }

    @NotNull
    private State getGameState(GameState gameState) {
        for (State state : states) {
            if (state.getGameState() == gameState) {
                return state;
            }
        }
        throw new Error("Couldn't handle game status");
    }


}
