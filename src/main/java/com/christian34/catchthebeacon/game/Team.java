package com.christian34.catchthebeacon.game;


import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.lib.lang.I;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public enum Team {
    RED(),
    BLUE(),
    RANDOM;

    private final static HashSet<Team> teams;

    static {
        teams = new HashSet<>();
        for (Team team : values()) {
            if (team.equals(RANDOM)) continue;
            teams.add(team);
        }
    }

    private final CatchTheBeacon instance = CatchTheBeacon.getInstance();
    private final String name;
    private final ItemStack item;
    private final Component displayName;
    private final TextColor color;
    private Color leatherColor;

    Team() {
        this.name = getString("name");
        this.color = parseColor(getString("chat_color"));
        this.displayName = Component.text(name, color);
        String itemName = getString("item");
        Material material = itemName == null ? null : Material.matchMaterial(itemName);
        if (material == null || !material.isItem()) {
            Debug.warn("Unknown item for team " + name);
            this.item = new ItemStack(Material.GREEN_WOOL);
        } else {
            this.item = new ItemStack(material);
        }

        ItemMeta itemMeta = Objects.requireNonNull(item).getItemMeta();
        if (itemMeta != null) {
            itemMeta.customName(I.item(displayName));
            this.item.setItemMeta(itemMeta);
        }
        try {
            String leatherColor = getString("leather_color").replace(" ", "");
            String[] colors = leatherColor.split(",");
            this.leatherColor = Color.fromRGB(Integer.parseInt(colors[0]),
                    Integer.parseInt(colors[1]),
                    Integer.parseInt(colors[2]));
        } catch (IllegalArgumentException ignored) {
            this.leatherColor = Color.GREEN;
        }
    }

    @Nullable
    public static Team get(String name) {
        if (name.equalsIgnoreCase("red")) {
            return RED;
        } else if (name.equalsIgnoreCase("blue")) {
            return BLUE;
        }
        return null;
    }

    public static Set<Team> getTeams() {
        return teams;
    }

    /**
     * @return the other playable team (null for RANDOM)
     */
    @Nullable
    public Team getOpponent() {
        return switch (this) {
            case RED -> BLUE;
            case BLUE -> RED;
            case RANDOM -> null;
        };
    }

    public Color getLeatherColor() {
        return leatherColor;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the name in the team color
     */
    public Component getDisplayName() {
        return displayName;
    }

    public TextColor getColor() {
        return color;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    /**
     * @param value a color code (c), a color name (red) or a hex color (#ff0000)
     */
    private TextColor parseColor(@Nullable String value) {
        if (value != null && !value.isBlank()) {
            value = value.trim().toLowerCase();
            TextColor color = value.length() == 1
                    ? LegacyComponentSerializer.legacySection().deserialize("§" + value + "x").color()
                    : value.startsWith("#") ? TextColor.fromHexString(value) : NamedTextColor.NAMES.value(value);
            if (color != null) return color;
        }
        Debug.warn("Illegal chat color for team " + name);
        return NamedTextColor.WHITE;
    }

    private String getString(String key) {
        String path = name().toLowerCase() + "." + key;
        return instance.getFileManager().getTeamsFile().getString(path);
    }

}
