package com.christian34.catchthebeacon.integrations;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.game.map.Arena;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Join NPCs of Citizens or FancyNpcs: an admin links an NPC with an arena (on a lobby server: a game server) by
 * /ctb npc link <target> and clicking the NPC. Clicking it then works like a join sign. Saved in npcs.yml.
 *
 * @author Christian34
 */
public class NpcSupport {
    /**
     * time to click the NPC after /ctb npc link or unlink
     */
    static final long CLICK_MILLIS = 30_000;
    private final CatchTheBeacon plugin;
    private final File file;
    private final Map<String, Link> links = new LinkedHashMap<>();
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final List<String> plugins = new ArrayList<>();

    /**
     * @param npc    the name of the NPC when it was linked
     * @param target the arena, on a lobby server the game server
     */
    public record Link(String key, String npc, String target) {
    }

    /**
     * @param target null: unlink
     */
    private record Pending(@Nullable String target, long until) {
    }

    public NpcSupport(CatchTheBeacon plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
        ConfigurationSection section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("npcs");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String target = section.getString(key + ".target");
                if (target != null) links.put(key, new Link(key, section.getString(key + ".name", key), target));
            }
        }
        if (Bukkit.getPluginManager().isPluginEnabled(CitizensNpcs.PLUGIN)) register(CitizensNpcs.PLUGIN, new CitizensNpcs(this));
        if (Bukkit.getPluginManager().isPluginEnabled(FancyNpcsNpcs.PLUGIN)) register(FancyNpcsNpcs.PLUGIN, new FancyNpcsNpcs(this));
        // the arenas of NPCs get a game on start, like those of signs
        if (!plugin.getNetworkManager().isLobby()) {
            for (Link link : links.values()) {
                Arena arena = plugin.getMapHandler().getArena(link.target());
                if (arena != null) plugin.getSignManager().ensureGame(arena);
            }
        }
    }

    private void register(String name, Listener listener) {
        try {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            plugins.add(name);
            Debug.info("Hooked into " + name + " (join NPCs)");
        } catch (Exception | LinkageError ex) {
            Debug.warn("Couldn't hook into " + name + ", join NPCs don't work with it: " + ex);
        }
    }

    /**
     * for tests: pretends an NPC plugin is installed
     */
    public void addPlugin(String name) {
        plugins.add(name);
    }

    /**
     * @return the installed NPC plugins
     */
    public List<String> getPlugins() {
        return plugins;
    }

    public List<Link> getLinks() {
        return new ArrayList<>(links.values());
    }

    /**
     * the next NPC the admin clicks gets linked with the target
     *
     * @param target the arena or game server (checked already), null: removes the link of the NPC
     */
    public void startLinking(@NotNull Player admin, @Nullable String target) {
        pending.put(admin.getUniqueId(), new Pending(target, System.currentTimeMillis() + CLICK_MILLIS));
    }

    /**
     * a player clicked an NPC
     *
     * @param key  the plugin and the id of the NPC ("citizens:<uuid>")
     * @param name the name of the NPC
     * @return true if CatchTheBeacon handled the click (the NPC plugin shouldn't do anything else)
     */
    public boolean click(@NotNull Player player, @NotNull String key, @NotNull String name) {
        Pending action = pending.remove(player.getUniqueId());
        if (action != null && action.until() >= System.currentTimeMillis()) {
            if (action.target() == null) {
                if (links.remove(key) == null) {
                    player.sendMessage(I.prefixed(LangText.NPC_NOT_LINKED, name));
                } else {
                    save();
                    player.sendMessage(I.prefixed(LangText.NPC_UNLINKED, name));
                }
            } else {
                links.put(key, new Link(key, name, action.target()));
                save();
                player.sendMessage(I.prefixed(LangText.NPC_LINKED, name, action.target()));
            }
            return true;
        }
        Link link = links.get(key);
        if (link == null) return false;
        plugin.getSignManager().join(player, link.target());
        return true;
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Link link : links.values()) {
            data.set("npcs." + link.key() + ".name", link.npc());
            data.set("npcs." + link.key() + ".target", link.target());
        }
        try {
            data.save(file);
        } catch (IOException ex) {
            Debug.warn("Couldn't save npcs.yml: " + ex.getMessage());
        }
    }

}
