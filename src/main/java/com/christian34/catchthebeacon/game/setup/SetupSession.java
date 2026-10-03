package com.christian34.catchthebeacon.game.setup;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.map.Arena;
import com.christian34.catchthebeacon.game.map.MapInUseException;
import com.christian34.catchthebeacon.game.map.MapTemplate;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import com.christian34.catchthebeacon.utils.BlockUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An admin setting up an arena with the tools in the hotbar. The player is moved into editable copies of the lobby and
 * the arena world; leaving the setup saves both copies back to the maps and restores the player.
 *
 * @author Christian34
 */
public class SetupSession {
    private final CatchTheBeacon plugin;
    private final GamePlayer gamePlayer;
    private final Arena arena;
    private final Set<MapTemplate> templates = new LinkedHashSet<>();
    private boolean inLobby;
    private BukkitTask actionBarTask;
    private boolean busy;
    private SetupItem lastItem;
    private int lastUseTick;

    SetupSession(CatchTheBeacon plugin, GamePlayer gamePlayer, Arena arena) {
        this.plugin = plugin;
        this.gamePlayer = gamePlayer;
        this.arena = arena;
    }

    public Arena getArena() {
        return arena;
    }

    public Player getPlayer() {
        return gamePlayer.getPlayer();
    }

    private boolean hasLobby() {
        return arena.getLobbyMap().getName() != null;
    }

    /**
     * the next step that can be done: the lobby spawn is skipped as long as the arena has no lobby world
     *
     * @return null if nothing more can be done (see {@link #isComplete()})
     */
    @Nullable
    private SetupStep nextStep() {
        for (SetupStep step : SetupStep.values()) {
            if (step.isDone(arena) || (step.isInLobby() && !hasLobby())) continue;
            return step;
        }
        return null;
    }

    private boolean isComplete() {
        return SetupStep.next(arena) == null;
    }

    /**
     * @return false if the world couldn't be loaded (the player has been told why)
     */
    boolean start() {
        gamePlayer.getUserStorage().store();
        gamePlayer.getUserStorage().cleanPlayer();
        getPlayer().setGameMode(GameMode.CREATIVE);
        getPlayer().setAllowFlight(true);

        SetupStep next = nextStep();
        if (!enter(next != null && next.isInLobby())) {
            gamePlayer.getUserStorage().restore();
            return false;
        }
        this.actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, this::showActionBar, 0L, 20L);
        gamePlayer.sendMessage(LangText.SETUP_STARTED, arena.getName());
        getPlayer().sendMessage(I.i18n(LangText.SETUP_HELP, arena.getName()));
        announceNextStep();
        return true;
    }

    /**
     * @param lobby true to teleport to the lobby world, false for the arena world
     * @return false if the world couldn't be loaded
     */
    private boolean enter(boolean lobby) {
        MapTemplate template = MapTemplate.get(lobby ? arena.getLobbyMap() : arena);
        try {
            template.load();
        } catch (RuntimeException ex) {
            gamePlayer.sendMessage(LangText.SETUP_WORLD_LOAD_FAILED, worldName(lobby), ex.getMessage());
            return false;
        }
        templates.add(template);
        this.inLobby = lobby;
        getPlayer().teleport(template.getSpawn());
        giveItems();
        return true;
    }

    /**
     * @return true if the world is one of the worlds of this setup
     */
    boolean isSetupWorld(@Nullable World world) {
        for (MapTemplate template : templates) {
            if (template.getWorld() != null && template.getWorld().equals(world)) return true;
        }
        return false;
    }

    private void giveItems() {
        PlayerInventory inventory = getPlayer().getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (SetupItem.of(inventory.getItem(slot)) != null) inventory.setItem(slot, null);
        }
        SetupStep next = nextStep();
        SetupItem nextItem;
        if (next == null) {
            // done or only the lobby world is missing (the switch item explains how to set it)
            nextItem = isComplete() ? SetupItem.SAVE : SetupItem.SWITCH_WORLD;
        } else {
            nextItem = next.isInLobby() != inLobby ? SetupItem.SWITCH_WORLD : SetupItem.of(next);
        }
        for (SetupItem item : SetupItem.values()) {
            if (!item.isAvailable(inLobby)) continue;
            boolean done = item.getStep() != null && item.getStep().isDone(arena);
            inventory.setItem(item.getSlot(), item.create(done, item == nextItem));
        }
        inventory.setHeldItemSlot(nextItem.getSlot());
    }

    void use(SetupItem item, @Nullable Block clickedBlock) {
        // a right-click on a block fires two interact events (use item on block + use item), and loading a world
        // handles waiting packets - so ignore the second one and anything while a world is loading
        int tick = Bukkit.getCurrentTick();
        if (busy || (item == lastItem && tick == lastUseTick)) return;
        lastItem = item;
        lastUseTick = tick;
        busy = true;
        try {
            handle(item, clickedBlock);
        } finally {
            busy = false;
        }
    }

    private void handle(SetupItem item, @Nullable Block clickedBlock) {
        switch (item) {
            case CHECKLIST -> sendChecklist();
            case SWITCH_WORLD -> {
                if (inLobby) {
                    enter(false);
                } else if (!hasLobby()) {
                    sendSetLobbyHint();
                } else {
                    enter(true);
                }
            }
            case SAVE -> plugin.getSetupManager().stop(this);
            default -> complete(item.getStep(), clickedBlock);
        }
    }

    private void complete(SetupStep step, @Nullable Block clickedBlock) {
        try {
            if (step == SetupStep.LOBBY_SPAWN) {
                arena.getLobbyMap().setSpawnLocation(getPlayer().getLocation());
            } else if (step.isBeacon()) {
                Block beacon = clickedBlock != null && clickedBlock.getType() == Material.BEACON ? clickedBlock
                        : BlockUtils.findInRadius(getPlayer().getLocation().getBlock(), 3, Material.BEACON);
                Team team = step.getTeam();
                Beacon.Position position = step.getBeaconPosition();
                if (beacon == null || !arena.setBeacon(team, beacon, position)) {
                    gamePlayer.sendMessage(LangText.SETUP_CLICK_BEACON);
                    return;
                }
            } else {
                arena.setSpawnLocation(step.getTeam(), getPlayer().getLocation());
            }
        } catch (MapInUseException ex) {
            gamePlayer.sendMessage(ex.componentMessage());
            return;
        }
        gamePlayer.sendMessage(LangText.SETUP_STEP_DONE, step.getDescription());
        giveItems();
        announceNextStep();
    }

    private void announceNextStep() {
        SetupStep next = nextStep();
        if (isComplete()) {
            gamePlayer.sendMessage(I.i18n(LangText.SETUP_COMPLETE)
                    .append(I.button(LangText.BUTTON_SAVE_EXIT,
                            ClickEvent.runCommand("/ctb arena " + arena.getName() + " setup exit"))));
            return;
        }
        if (next == null) {
            sendSetLobbyHint();
            return;
        }
        gamePlayer.sendMessage(next.isInLobby() != inLobby ? LangText.SETUP_NEXT_STEP_OTHER_WORLD : LangText.SETUP_NEXT_STEP,
                next.getNumber(), SetupStep.values().length, next.getInstruction());
    }

    private void sendSetLobbyHint() {
        gamePlayer.sendMessage(I.i18n(LangText.SETUP_NO_LOBBY)
                .append(I.button(LangText.BUTTON_SET_LOBBY_WORLD,
                        ClickEvent.suggestCommand("/ctb arena " + arena.getName() + " lobby setworld "))));
    }

    private void showActionBar() {
        if (!getPlayer().isOnline()) return;
        SetupStep next = nextStep();
        Component text;
        if (isComplete()) {
            text = I.i18n(LangText.SETUP_ACTIONBAR_COMPLETE);
        } else if (next == null) {
            text = I.i18n(LangText.SETUP_ACTIONBAR_LOBBY, arena.getName());
        } else if (next.isInLobby() != inLobby) {
            text = I.i18n(LangText.SETUP_ACTIONBAR_SWITCH, next.getNumber(), SetupStep.values().length,
                    next.getDescription(), worldName(next.isInLobby()));
        } else {
            text = I.i18n(LangText.SETUP_NEXT_STEP, next.getNumber(), SetupStep.values().length, next.getInstruction());
        }
        getPlayer().sendActionBar(text);
    }

    void sendChecklist() {
        gamePlayer.sendMessage(LangText.SETUP_CHECKLIST_TITLE, arena.getName(), worldName(inLobby));
        getPlayer().sendMessage(hasLobby() ? I.i18n(LangText.SETUP_CHECKLIST_LOBBY_DONE, arena.getLobbyMap().getName())
                : I.i18n(LangText.SETUP_CHECKLIST_LOBBY_MISSING));
        for (SetupStep step : SetupStep.values()) {
            getPlayer().sendMessage(I.i18n(step.isDone(arena) ? LangText.SETUP_CHECKLIST_DONE
                    : LangText.SETUP_CHECKLIST_MISSING, step.getDescription()));
        }
    }

    /**
     * @return "lobby" or "arena" (messages.yml)
     */
    private static Component worldName(boolean lobby) {
        return I.i18n(lobby ? LangText.SETUP_WORLD_LOBBY : LangText.SETUP_WORLD_ARENA);
    }

    /**
     * clears the tools, restores the player and saves the edited worlds
     */
    void close() {
        if (actionBarTask != null) actionBarTask.cancel();
        PlayerInventory inventory = getPlayer().getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (SetupItem.of(inventory.getItem(slot)) != null) inventory.setItem(slot, null);
        }
        gamePlayer.getUserStorage().restore();
        for (MapTemplate template : templates) {
            if (MapTemplate.getTemplates().contains(template)) {
                template.close();
            }
        }
        templates.clear();
    }

}
