package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.lib.lang.LangText;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * Something that happens some minutes after the start of a game (`events` in config.yml), e.g. the beacons lose
 * their protection or the game ends.
 *
 * @param second  seconds after the start
 * @param action  what happens
 * @param effects the effects everybody gets (action {@link Action#EFFECTS})
 * @author Christian34
 */
public record GameEvent(int second, @NotNull Action action, @NotNull List<PotionEffect> effects) {

    /**
     * @return the events of config.yml, sorted by time (invalid entries are skipped with a warning)
     */
    public static List<GameEvent> load() {
        List<GameEvent> events = new ArrayList<>();
        for (Map<?, ?> entry : CatchTheBeacon.getInstance().getFileManager().getConfigFile().getData().getMapList("events")) {
            Action action = Action.get(String.valueOf(entry.get("action")));
            if (!(entry.get("minute") instanceof Number minute) || action == null) {
                Debug.warn("Invalid event in config.yml: " + entry);
                continue;
            }
            List<PotionEffect> effects = new ArrayList<>();
            if (entry.get("effects") instanceof List<?> list) {
                for (Object effect : list) {
                    PotionEffect potionEffect = parseEffect(String.valueOf(effect));
                    if (potionEffect == null) {
                        Debug.warn("Invalid effect in config.yml: " + effect);
                    } else {
                        effects.add(potionEffect);
                    }
                }
            }
            events.add(new GameEvent((int) Math.round(minute.doubleValue() * 60), action, effects));
        }
        events.sort(Comparator.comparingInt(GameEvent::second));
        return events;
    }

    /**
     * @param text e.g. "SPEED:1" (speed II) or "speed" (speed I)
     */
    @Nullable
    static PotionEffect parseEffect(String text) {
        String[] parts = text.split(":");
        NamespacedKey key = NamespacedKey.fromString(parts[0].trim().toLowerCase(Locale.ROOT));
        PotionEffectType type = key == null ? null : Registry.EFFECT.get(key);
        if (type == null) return null;
        int amplifier = 0;
        if (parts.length > 1) {
            try {
                amplifier = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier);
    }

    /**
     * @return the name shown in the scoreboard
     */
    public Component getName() {
        return i18n(action.name);
    }

    public enum Action {
        /**
         * blocks near the beacons can be changed
         */
        BEACON_PROTECTION_OFF(LangText.EVENT_NAME_BEACON_PROTECTION_OFF, LangText.EVENT_BEACON_PROTECTION_OFF),
        /**
         * everybody gets the effects (until the end of the game)
         */
        EFFECTS(LangText.EVENT_NAME_EFFECTS, LangText.EVENT_EFFECTS),
        /**
         * the game ends: the team with more beacons wins, otherwise nobody
         */
        END(LangText.EVENT_NAME_END, LangText.EVENT_END);

        private final LangText name;
        private final LangText message;

        Action(LangText name, LangText message) {
            this.name = name;
            this.message = message;
        }

        public LangText getMessage() {
            return message;
        }

        @Nullable
        public static Action get(String name) {
            for (Action action : values()) {
                if (action.name().replace('_', '-').equalsIgnoreCase(name) || action.name().equalsIgnoreCase(name)) {
                    return action;
                }
            }
            return null;
        }
    }

}
