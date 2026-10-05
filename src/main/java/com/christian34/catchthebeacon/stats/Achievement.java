package com.christian34.catchthebeacon.stats;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.lib.lang.LangText;
import net.kyori.adventure.text.Component;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * Achievements for special moments of a round (not for playing time: that rewarded camping on other servers).
 *
 * @author Christian34
 */
public enum Achievement {
    FIRST_WIN(LangText.ACHIEVEMENT_FIRST_WIN, LangText.ACHIEVEMENT_FIRST_WIN_DESCRIPTION),
    FIRST_BLOOD(LangText.ACHIEVEMENT_FIRST_BLOOD, LangText.ACHIEVEMENT_FIRST_BLOOD_DESCRIPTION),
    /**
     * kill an enemy while they are mining an own beacon
     */
    LAST_SECOND(LangText.ACHIEVEMENT_LAST_SECOND, LangText.ACHIEVEMENT_LAST_SECOND_DESCRIPTION),
    /**
     * win without losing a beacon
     */
    FLAWLESS(LangText.ACHIEVEMENT_FLAWLESS, LangText.ACHIEVEMENT_FLAWLESS_DESCRIPTION),
    /**
     * win within achievements.quick-win-minutes
     */
    QUICK_WIN(LangText.ACHIEVEMENT_QUICK_WIN, LangText.ACHIEVEMENT_QUICK_WIN_DESCRIPTION),
    /**
     * destroy all enemy beacons alone
     */
    SOLO(LangText.ACHIEVEMENT_SOLO, LangText.ACHIEVEMENT_SOLO_DESCRIPTION),
    KILLING_SPREE(LangText.ACHIEVEMENT_KILLING_SPREE, LangText.ACHIEVEMENT_KILLING_SPREE_DESCRIPTION),
    VETERAN(LangText.ACHIEVEMENT_VETERAN, LangText.ACHIEVEMENT_VETERAN_DESCRIPTION);

    /**
     * kills without dying for {@link #KILLING_SPREE}
     */
    public static final int KILLING_SPREE_KILLS = 5;
    /**
     * wins for {@link #VETERAN}
     */
    public static final int VETERAN_WINS = 10;
    private final LangText name;
    private final LangText description;

    Achievement(LangText name, LangText description) {
        this.name = name;
        this.description = description;
    }

    public Component getName() {
        return i18n(name);
    }

    public Component getDescription() {
        return i18n(description, getQuickWinMinutes());
    }

    /**
     * @return the key in the database
     */
    public String getKey() {
        return name().toLowerCase();
    }

    public static int getQuickWinMinutes() {
        return CatchTheBeacon.getInstance().getFileManager().getConfigFile().getInt("achievements.quick-win-minutes");
    }

}
