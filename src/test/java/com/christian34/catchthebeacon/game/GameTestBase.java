package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.PluginTestBase;
import com.christian34.catchthebeacon.game.states.GameState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A playable arena "castle" with a game and two players in the lobby: Red1 (team red, may start the game) and
 * Blue1 (team blue).
 *
 * @author Christian34
 */
public abstract class GameTestBase extends PluginTestBase {
    protected static final Location RED_SPAWN = new Location(null, 10.5, 70, 0.5);
    protected static final Location BLUE_SPAWN = new Location(null, -10.5, 70, 0.5);
    protected static final List<Location> RED_BEACONS = List.of(new Location(null, 30, 64, 30), new Location(null, 30, 64, -30));
    protected static final List<Location> BLUE_BEACONS = List.of(new Location(null, -30, 64, 30), new Location(null, -30, 64, -30));
    protected PlayerMock red;
    protected PlayerMock blue;
    protected Game game;

    protected static Location at(Location location, World world) {
        Location copy = location.clone();
        copy.setWorld(world);
        return copy;
    }

    @BeforeEach
    void setUpArena() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("lobby");
        setUpArena(admin, "castle");
        execute(admin, "ctb arena castle creategame");
        game = plugin.getGameManager().getGames().iterator().next();

        red = addPlayer("Red1", "ctb.vip");
        blue = addPlayer("Blue1");
        assertNull(execute(red, "ctb join"));
        assertNull(execute(blue, "ctb join"));
        plugin.getUser(red).setTeam(Team.RED);
        plugin.getUser(blue).setTeam(Team.BLUE);
        messages(red);
        messages(blue);
    }

    /**
     * sets up a playable arena (lobby: the map "lobby")
     */
    protected void setUpArena(PlayerMock admin, String name) {
        World world = admin.getWorld();
        addMapFolder(name);
        execute(admin, "ctb arena create " + name);
        execute(admin, "ctb arena " + name + " lobby setworld lobby");
        admin.setLocation(new Location(world, 0.5, 70, 0.5));
        execute(admin, "ctb arena " + name + " lobby setspawn");
        admin.setLocation(at(RED_SPAWN, world));
        execute(admin, "ctb arena " + name + " team red setspawn");
        admin.setLocation(at(BLUE_SPAWN, world));
        execute(admin, "ctb arena " + name + " team blue setspawn");
        String[] positions = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            for (var entry : List.of(new Object[]{"red", RED_BEACONS.get(i)}, new Object[]{"blue", BLUE_BEACONS.get(i)})) {
                Location beacon = at((Location) entry[1], world);
                beacon.getBlock().setType(Material.BEACON);
                admin.setLocation(beacon.clone().add(1, 0, 0));
                execute(admin, "ctb arena " + name + " team " + entry[0] + " setbeacon " + positions[i]);
            }
        }
        assertTrue(plugin.getMapHandler().getArena(name).isPlayable(),
                "missing: " + plugin.getMapHandler().getArena(name).getMissingSetup());
    }

    protected void startGame() {
        execute(red, "ctb start");
        server.getScheduler().performTicks(20 * 7);
        assertEquals(GameState.INGAME, game.getGameState());
    }

    protected World gameWorld() {
        return Objects.requireNonNull(game.getGameWorld().getWorld());
    }

    /**
     * @return the beacons of team blue in the game world
     */
    protected List<Location> blueBeacons() {
        return BLUE_BEACONS.stream().map(location -> at(location, gameWorld())).toList();
    }

    protected Beacon redLeftBeacon() {
        return Objects.requireNonNull(game.getBeacon(Team.RED, Beacon.Position.LEFT));
    }

    protected void killAndRespawn(PlayerMock player) {
        player.damage(100);
        player.respawn();
        assertEquals(gameWorld(), player.getWorld(), "not respawned at the team spawn");
    }

    /**
     * fires the damage event of an attack (PlayerMock#damage(amount, attacker) applies the damage even if the event
     * has been cancelled)
     *
     * @return false if the attack has been cancelled
     */
    protected boolean attack(PlayerMock attacker, PlayerMock victim) {
        return !damage(attacker, attacker, victim).isCancelled();
    }

    /**
     * fires the damage event of a melee attack or an arrow (damager) of the attacker
     */
    // the server creates the event internally: the only constructor that isn't marked for removal needs the
    // deprecated DamageModifier maps
    @SuppressWarnings("removal")
    protected EntityDamageByEntityEvent damage(Entity damager, PlayerMock attacker, PlayerMock victim) {
        boolean arrow = damager instanceof Arrow;
        var event = new EntityDamageByEntityEvent(damager, victim,
                arrow ? EntityDamageEvent.DamageCause.PROJECTILE : EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                DamageSource.builder(arrow ? DamageType.ARROW : DamageType.PLAYER_ATTACK)
                        .withCausingEntity(attacker).withDirectEntity(damager).build(), 5);
        server.getPluginManager().callEvent(event);
        return event;
    }

    protected void joinWithTeam(PlayerMock player, Team team) {
        execute(player, "ctb join");
        if (team != null) assertTrue(plugin.getUser(player).setTeam(team));
    }

}
