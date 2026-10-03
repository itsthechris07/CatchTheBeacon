package com.christian34.catchthebeacon.game.states;


public interface State {

    void start();

    void stop();

    /**
     * the game is stopped in this state: stops its running tasks without moving on to the next state
     */
    default void cancel() {
    }

    GameState getGameState();

}
