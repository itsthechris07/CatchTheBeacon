package com.christian34.catchthebeacon.listeners;

import com.christian34.catchthebeacon.utils.WorldUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.StructureGrowEvent;

@SuppressWarnings("unused")
public class WorldEventListener implements Listener {
    private final EventListener eventListener;

    public WorldEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    @EventHandler
    public void onBlockBurnEvent(BlockBurnEvent e) {
        if (eventListener.cancel(e.getBlock().getLocation(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplodeEvent(EntityExplodeEvent e) {
        if (eventListener.cancel(e.getLocation(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onBlockFadeEvent(BlockFadeEvent e) {
        if (eventListener.cancel(e.getBlock().getLocation(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onBlockFormEvent(BlockFormEvent e) {
        if (eventListener.cancel(e.getBlock().getLocation(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onStructureGrowEvent(StructureGrowEvent e) {
        if (eventListener.cancel(e.getLocation(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onCreatureSpawnEvent(CreatureSpawnEvent e) {
        if (eventListener.cancel(e.getLocation(), e)) e.setCancelled(true);
    }

    /**
     * no mobs in the worlds of games - only plugins may spawn them
     */
    @EventHandler(ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (WorldUtils.isPlayWorld(e.getLocation().getWorld())
                && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            e.setCancelled(true);
        }
    }

    /**
     * removes the mobs saved in the map
     */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        if (!WorldUtils.isPlayWorld(e.getWorld())) return;
        for (Entity entity : e.getEntities()) {
            if (entity instanceof Mob) entity.remove();
        }
    }

    @EventHandler
    public void onEntityChangeBlockEvent(EntityChangeBlockEvent e) {
        if (eventListener.cancel(e.getBlock().getLocation(), e)) e.setCancelled(true);
    }

}
