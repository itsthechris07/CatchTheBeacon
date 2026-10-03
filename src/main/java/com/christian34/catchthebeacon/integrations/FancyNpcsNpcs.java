package com.christian34.catchthebeacon.integrations;

import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * FancyNpcs (https://github.com/FancyInnovations/FancyPlugins): clicks on NPCs
 */
class FancyNpcsNpcs implements Listener {
    static final String PLUGIN = "FancyNpcs";
    private final NpcSupport support;

    FancyNpcsNpcs(NpcSupport support) {
        this.support = support;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(NpcInteractEvent e) {
        if (support.click(e.getPlayer(), "fancynpcs:" + e.getNpc().getData().getId(), e.getNpc().getData().getName())) {
            // no actions of FancyNpcs (e.g. commands) on linked NPCs
            e.setCancelled(true);
        }
    }

}
