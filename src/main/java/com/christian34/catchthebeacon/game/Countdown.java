package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.CatchTheBeacon;
import org.bukkit.Bukkit;

import java.util.function.Consumer;
import java.util.function.Function;

public class Countdown {
    private final CatchTheBeacon instance;
    private final int time;
    private int taskID;
    private Consumer<Integer> onTickConsumer;
    private Function<Integer, Integer> onTickFunction;
    private int timeRemaining;

    public Countdown(int seconds) {
        this.instance = CatchTheBeacon.getInstance();
        this.time = seconds;
        this.timeRemaining = seconds;
        this.onTickConsumer = null;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(int timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    /**
     * @param consumer supplies the remaining time
     * @return the instance
     */
    public Countdown onTick(Consumer<Integer> consumer) {
        this.onTickConsumer = consumer;
        return this;
    }

    /**
     * @param supplier applies and retrieves the time; to not adjust the time, return -1
     * @return the instance
     */
    public Countdown onTick(Function<Integer, Integer> supplier) {
        this.onTickFunction = supplier;
        return this;
    }

    public void run() {
        if (this.taskID == 0) {
            this.timeRemaining = time;
        }
        this.taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, () -> {
            this.timeRemaining--;
            if (onTickConsumer != null) {
                onTickConsumer.accept(timeRemaining);
            }
            if (onTickFunction != null) {
                int time = onTickFunction.apply(this.timeRemaining);
                if (time != -1) {
                    this.timeRemaining = time;
                }
            }
            if (timeRemaining <= 1) {
                stop();
            }
        }, 0L, 20L);
    }

    public void stop() {
        Bukkit.getScheduler().cancelTask(this.taskID);
        this.taskID = 0;
    }

}
