package com.christian34.catchthebeacon.integrations;

import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.lib.Menu;
import com.christian34.catchthebeacon.lib.lang.I;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;

/**
 * Floodgate (https://github.com/GeyserMC/Floodgate): menus as simple forms
 */
class FloodgateForms implements BedrockForms {
    private final Plugin plugin;
    private final FloodgateApi api;

    FloodgateForms(Plugin plugin) {
        this.plugin = plugin;
        this.api = FloodgateApi.getInstance();
    }

    @Override
    public boolean show(Player player, Menu menu) {
        try {
            if (!api.isFloodgatePlayer(player.getUniqueId())) return false;
            List<Menu.Button> buttons = List.copyOf(menu.getButtons());
            SimpleForm.Builder form = SimpleForm.builder().title(I.legacy(menu.getTitle()));
            for (Menu.Button button : buttons) {
                String text = I.legacy(button.name());
                if (button.detail() != null) text += "\n" + I.legacy(button.detail());
                form.button(text);
            }
            // the answer comes from the network thread
            form.validResultHandler(response -> Bukkit.getScheduler().runTask(plugin, () -> {
                int id = response.clickedButtonId();
                if (player.isOnline() && id >= 0 && id < buttons.size()) buttons.get(id).action().accept(player);
            }));
            return api.sendForm(player.getUniqueId(), form);
        } catch (Exception | LinkageError ex) {
            Debug.warn("Couldn't send a form to " + player.getName() + ", showing a chest menu: " + ex);
            return false;
        }
    }

}
