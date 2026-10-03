package com.christian34.catchthebeacon.game.states;

import com.christian34.catchthebeacon.game.Game;

public class PendingState implements State {
    private final Game game;

    public PendingState(Game game) {
        this.game = game;
    }

    @Override
    public void start() {
        //nothing to do
    }

    @Override
    public void stop() {
        //
    }

    @Override
    public GameState getGameState() {
        return GameState.PENDING;
    }

}
