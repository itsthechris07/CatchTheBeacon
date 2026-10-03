package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.lib.lang.LangText;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * The variant of a round, voted for in the lobby (variants.* in config.yml).
 *
 * @author Christian34
 */
public enum Variant {
    NORMAL(Material.BEACON, LangText.VARIANT_NORMAL, LangText.VARIANT_NORMAL_DESCRIPTION),
    /**
     * mining takes half the time, events come twice as fast
     */
    RUSH(Material.SUGAR, LangText.VARIANT_RUSH, LangText.VARIANT_RUSH_DESCRIPTION),
    /**
     * only arrows hurt, bows have infinity
     */
    BOW_ONLY(Material.BOW, LangText.VARIANT_BOW_ONLY, LangText.VARIANT_BOW_ONLY_DESCRIPTION),
    /**
     * resource blocks give nothing
     */
    NO_IRON(Material.BARRIER, LangText.VARIANT_NO_IRON, LangText.VARIANT_NO_IRON_DESCRIPTION),
    /**
     * every hit of an enemy kills
     */
    ONE_HIT(Material.WITHER_ROSE, LangText.VARIANT_ONE_HIT, LangText.VARIANT_ONE_HIT_DESCRIPTION);

    /**
     * the block break speed in a rush (vanilla: 1)
     */
    public static final double RUSH_BREAK_SPEED = 2.0;
    private final Material icon;
    private final LangText name;
    private final LangText description;

    Variant(Material icon, LangText name, LangText description) {
        this.icon = icon;
        this.name = name;
        this.description = description;
    }

    public Material getIcon() {
        return icon;
    }

    public Component getName() {
        return i18n(name);
    }

    public Component getDescription() {
        return i18n(description);
    }

    /**
     * @return the factor for the times of the events (rush: twice as fast)
     */
    public double getTimeFactor() {
        return this == RUSH ? 0.5 : 1;
    }

    /**
     * @return the variant, "bow-only" or "BOW_ONLY"; null if there is none
     */
    @Nullable
    public static Variant get(String name) {
        for (Variant variant : values()) {
            if (variant.name().equalsIgnoreCase(name.trim().replace('-', '_'))) return variant;
        }
        return null;
    }

    /**
     * @return the variants players can vote for (normal first), empty if voting is disabled
     */
    public static List<Variant> getVotable() {
        List<Variant> variants = new ArrayList<>();
        var config = CatchTheBeacon.getInstance().getFileManager().getConfigFile();
        if (!config.getBoolean("variants.voting")) return variants;
        for (String name : config.getList("variants.list")) {
            Variant variant = get(name);
            if (variant != null && variant != NORMAL && !variants.contains(variant)) variants.add(variant);
        }
        if (!variants.isEmpty()) variants.addFirst(NORMAL);
        return variants;
    }

}
