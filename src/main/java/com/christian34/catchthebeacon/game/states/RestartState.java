package com.christian34.catchthebeacon.game.states;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.GameManager;


public class RestartState implements State {
    private final Game game;

    public RestartState(Game game) {
        this.game = game;
    }

    @Override
    public void start() {

        stop();
    }

    @Override
    public void stop() {
        this.game.log("Game has been stopped!");
        GameManager gameManager = CatchTheBeacon.getInstance().getGameManager();
        // the next round of the arena (join signs and [Play again] need it)
        gameManager.getGames().remove(game);
        gameManager.createGame(game.getArena());
    }

    @Override
    public GameState getGameState() {
        return GameState.RESTART;
    }

}
