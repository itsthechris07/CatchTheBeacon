package com.christian34.catchthebeacon.integrations;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Citizens (https://github.com/CitizensDev/Citizens2): clicks on NPCs
 */
class CitizensNpcs implements Listener {
    static final String PLUGIN = "Citizens";
    private final NpcSupport support;

    CitizensNpcs(NpcSupport support) {
        this.support = support;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(NPCRightClickEvent e) {
        if (support.click(e.getClicker(), "citizens:" + e.getNPC().getUniqueId(), e.getNPC().getName())) {
            e.setCancelled(true);
        }
    }

}
