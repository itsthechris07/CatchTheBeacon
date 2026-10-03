package com.christian34.catchthebeacon.integrations;

import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.lib.Menu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Menus as forms for Bedrock players (Floodgate): chest menus are awkward to use on Bedrock
 *
 * @author Christian34
 */
public interface BedrockForms {
    BedrockForms NONE = (player, menu) -> false;

    /**
     * @return true if the menu was shown as a form (the player is a Bedrock player)
     */
    boolean show(Player player, Menu menu);

    static BedrockForms create(Plugin plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) return NONE;
        try {
            BedrockForms forms = new FloodgateForms(plugin);
            Debug.info("Hooked into Floodgate (menus as forms for Bedrock players)");
            return forms;
        } catch (Exception | LinkageError ex) {
            Debug.warn("Couldn't hook into Floodgate, Bedrock players get chest menus: " + ex);
            return NONE;
        }
    }

}
