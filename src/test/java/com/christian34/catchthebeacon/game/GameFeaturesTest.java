package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.game.states.IngameState;
import com.christian34.catchthebeacon.lib.InteractionItems;
import com.christian34.catchthebeacon.user.GamePlayer;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The features that make the game more exciting: last hit, mining fatigue, base effects, resources, timed events,
 * spectators, rejoin, team chat, combat and the settings of the map.
 *
 * @author Christian34
 */
class GameFeaturesTest extends GameTestBase {

    /**
     * a block of the map far away from spawns and beacons
     */
    private Block mapBlock(Material material) {
        Block block = new Location(gameWorld(), 0, 64, 60).getBlock();
        block.setType(material);
        return block;
    }

    private Location nearRedBeacon(double distance) {
        return at(RED_BEACONS.getFirst(), gameWorld()).add(0.5 + distance, 0, 0.5);
    }

    private void config(String key, Object value) {
        plugin.getFileManager().getConfigFile().set(key, value);
    }

    private static boolean has(PlayerMock player, PotionEffectType type) {
        return player.getPotionEffect(type) != null;
    }

    // --- bug fix: translated beacon names

    @Test
    void warningUsesTheTranslatedBeaconName() {
        startGame();
        messages(red);
        blue.setLocation(nearRedBeacon(8));
        server.getScheduler().performTicks(40);
        List<String> messages = messages(red);
        assertContains(messages, "your left beacon");
        assertNotContains(messages, "LEFT");
    }

    // --- bug fix: beacon locations without world

    @Test
    void beaconsAreInTheGameWorld() {
        startGame();
        assertEquals(gameWorld(), redLeftBeacon().getLocation().getWorld());
        Location beacon = at(RED_BEACONS.getFirst(), gameWorld());
        assertEquals(redLeftBeacon(), game.getBeacon(beacon));
        assertNull(game.getBeacon(at(RED_BEACONS.getFirst(), red.getServer().getWorlds().getFirst())),
                "a block of another world is a beacon");
    }

    // --- bug fix: no effects when a beacon is destroyed

    @Test
    void destroyedBeaconHasEffects() {
        startGame();
        Block beacon = blueBeacons().getFirst().getBlock();
        beacon.setType(Material.BEACON);
        red.simulateBlockBreak(beacon);
        assertEquals(Material.AIR, beacon.getType());
        assertEquals(GameState.INGAME, game.getGameState());
        blue.assertSoundHeard(Sound.ENTITY_ENDER_DRAGON_GROWL);
        red.assertSoundHeard(Sound.ENTITY_PLAYER_LEVELUP);
    }

    // --- bug fix: the compass of WorldEdit teleported players in the game

    @Test
    void compassIsNoToolOfOtherPlugins() {
        startGame();
        ItemStack compass = InteractionItems.getBeaconCompass();
        red.getInventory().setItemInMainHand(compass);
        for (Action action : List.of(Action.LEFT_CLICK_AIR, Action.RIGHT_CLICK_AIR)) {
            PlayerInteractEvent event = new PlayerInteractEvent(red, action, compass, null, BlockFace.SELF, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(event);
            assertEquals(Event.Result.DENY, event.useItemInHand(), action.name());
        }
    }

    // --- last hit

    @Test
    void lastHitGetsTheKill() {
        startGame();
        assertTrue(attack(red, blue));
        // e.g. knocked into the void: no killer
        blue.damage(100);
        assertEquals(1, plugin.getUser(red).getKills());
        assertContains(messages(blue), "was killed by");
    }

    @Test
    void lastHitExpires() {
        config("game.last-hit-seconds", 2);
        startGame();
        attack(red, blue);
        server.getScheduler().performTicks(60);
        blue.damage(100);
        assertEquals(0, plugin.getUser(red).getKills());
    }

    @Test
    void lastHitCanBeDisabled() {
        config("game.last-hit-seconds", 0);
        startGame();
        attack(red, blue);
        blue.damage(100);
        assertEquals(0, plugin.getUser(red).getKills());
    }

    // --- blocks

    @Test
    void wholeMapCanBeMinedByDefault() {
        startGame();
        BlockBreakEvent event = red.simulateBlockBreak(mapBlock(Material.STONE));
        assertNotNull(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void onlyPlacedBlocksCanBeMined() {
        config("game.only-placed-blocks-breakable", true);
        startGame();
        messages(red);
        BlockBreakEvent event = red.simulateBlockBreak(mapBlock(Material.STONE));
        assertTrue(event == null || event.isCancelled(), "a block of the map was broken");
        assertContains(messages(red), "You can only break blocks placed by players!");

        Location location = new Location(gameWorld(), 0, 65, 60);
        red.simulateBlockPlace(Material.RED_WOOL, location);
        assertTrue(game.isPlacedBlock(location.getBlock()));
        event = red.simulateBlockBreak(location.getBlock());
        assertNotNull(event);
        assertFalse(event.isCancelled());
        assertFalse(game.isPlacedBlock(location.getBlock()));
    }

    @Test
    void resourcesCanAlwaysBeMined() {
        config("game.only-placed-blocks-breakable", true);
        startGame();
        BlockBreakEvent event = red.simulateBlockBreak(mapBlock(Material.IRON_BLOCK));
        assertNotNull(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void enchantingTablesCantBeBroken() {
        startGame();
        messages(red);
        BlockBreakEvent event = red.simulateBlockBreak(mapBlock(Material.ENCHANTING_TABLE));
        assertTrue(event == null || event.isCancelled());
        assertContains(messages(red), "This block cannot be broken!");
    }

    private List<Block> explode(Block... blocks) {
        List<Block> list = new ArrayList<>(List.of(blocks));
        Block source = new Location(gameWorld(), 0, 70, 60).getBlock();
        server.getPluginManager().callEvent(new BlockExplodeEvent(source, source.getState(), list, 1f, ExplosionResult.DESTROY));
        return list;
    }

    @Test
    void explosionsDontDestroyBeaconsProtectedBlocksAndResources() {
        startGame();
        Block beacon = at(RED_BEACONS.getFirst(), gameWorld()).getBlock();
        beacon.setType(Material.BEACON);
        Block nearBeacon = beacon.getRelative(1, 0, 0);
        Block table = new Location(gameWorld(), 1, 64, 60).getBlock();
        table.setType(Material.ENCHANTING_TABLE);
        Block iron = new Location(gameWorld(), 2, 64, 60).getBlock();
        iron.setType(Material.IRON_BLOCK);
        Block stone = mapBlock(Material.STONE);
        assertEquals(List.of(stone), explode(beacon, nearBeacon, table, iron, stone));
    }

    @Test
    void explosionsOnlyDestroyPlacedBlocksIfConfigured() {
        config("game.only-placed-blocks-breakable", true);
        startGame();
        Location location = new Location(gameWorld(), 0, 65, 60);
        red.simulateBlockPlace(Material.RED_WOOL, location);
        assertEquals(List.of(location.getBlock()), explode(mapBlock(Material.STONE), location.getBlock()));
        assertFalse(game.isPlacedBlock(location.getBlock()));
    }

    // --- resources

    @Test
    void resourcesGiveArmor() {
        config("resources", List.of(Map.of("block", "IRON_BLOCK", "rewards", List.of("IRON_CHESTPLATE"))));
        startGame();
        messages(red);
        BlockBreakEvent event = red.simulateBlockBreak(mapBlock(Material.IRON_BLOCK));
        assertNotNull(event);
        assertFalse(event.isDropItems(), "the iron block is dropped too");
        assertEquals(Material.IRON_CHESTPLATE, red.getInventory().getItem(EquipmentSlot.CHEST).getType(),
                "the better armor isn't put on");
        assertContains(messages(red), "You got Iron chestplate!");
    }

    @Test
    void worseArmorAndItemsGoIntoTheInventory() {
        config("resources", List.of(Map.of("block", "IRON_BLOCK", "rewards", List.of("IRON_INGOT:4"))));
        startGame();
        red.getInventory().setItem(EquipmentSlot.CHEST, new ItemStack(Material.DIAMOND_CHESTPLATE));
        red.simulateBlockBreak(mapBlock(Material.IRON_BLOCK));
        assertTrue(red.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 4));

        ResourceBlocks.give(red, new ItemStack(Material.IRON_CHESTPLATE));
        assertEquals(Material.DIAMOND_CHESTPLATE, red.getInventory().getItem(EquipmentSlot.CHEST).getType());
        assertTrue(red.getInventory().contains(Material.IRON_CHESTPLATE));
    }

    @Test
    void placedResourcesAreNormalBlocks() {
        startGame();
        Location location = new Location(gameWorld(), 0, 65, 60);
        red.simulateBlockPlace(Material.IRON_BLOCK, location);
        BlockBreakEvent event = red.simulateBlockBreak(location.getBlock());
        assertNotNull(event);
        assertTrue(event.isDropItems());
    }

    @Test
    void invalidRewardsAreSkipped() {
        assertNull(ResourceBlocks.parseItem("NO_ITEM"));
        assertNull(ResourceBlocks.parseItem("IRON_INGOT:x"));
        assertEquals(new ItemStack(Material.IRON_INGOT, 4), ResourceBlocks.parseItem("iron_ingot:4"));
    }

    // --- bases

    @Test
    void enemiesNearABeaconGetMiningFatigue() {
        startGame();
        blue.setLocation(nearRedBeacon(4));
        red.setLocation(nearRedBeacon(4));
        server.getScheduler().performTicks(40);
        PotionEffect fatigue = blue.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        assertNotNull(fatigue, "no mining fatigue");
        assertEquals(0, fatigue.getAmplifier());
        assertFalse(has(red, PotionEffectType.MINING_FATIGUE), "the defender gets mining fatigue");
    }

    @Test
    void miningFatigueLevelCanBeConfigured() {
        config("base.mining-fatigue-level", 2);
        startGame();
        blue.setLocation(nearRedBeacon(4));
        server.getScheduler().performTicks(40);
        assertEquals(1, blue.getPotionEffect(PotionEffectType.MINING_FATIGUE).getAmplifier());
    }

    @Test
    void noMiningFatigueFarAway() {
        startGame();
        blue.setLocation(nearRedBeacon(8));
        server.getScheduler().performTicks(40);
        assertFalse(has(blue, PotionEffectType.MINING_FATIGUE));
    }

    @Test
    void defendersGetRegeneration() {
        startGame();
        red.setLocation(nearRedBeacon(6));
        server.getScheduler().performTicks(40);
        assertTrue(has(red, PotionEffectType.REGENERATION));
    }

    @Test
    void trapCatchesTheFirstEnemyOnce() {
        startGame();
        messages(red);
        blue.setLocation(nearRedBeacon(3));
        server.getScheduler().performTicks(40);
        assertTrue(has(blue, PotionEffectType.BLINDNESS), "the trap didn't go off");
        assertTrue(has(blue, PotionEffectType.SLOWNESS));
        assertContains(messages(red), "ran into the trap of your left beacon");
        assertContains(messages(blue), "You ran into a trap!");

        blue.clearActivePotionEffects();
        blue.setLocation(nearRedBeacon(20));
        server.getScheduler().performTicks(40);
        blue.setLocation(nearRedBeacon(3));
        server.getScheduler().performTicks(40);
        assertFalse(has(blue, PotionEffectType.BLINDNESS), "the trap went off twice");
    }

    @Test
    void campingAtTheOwnBeaconPoisons() {
        config("base.camping-seconds", 3);
        startGame();
        messages(red);
        red.setLocation(nearRedBeacon(2));
        server.getScheduler().performTicks(20 * 2);
        assertFalse(has(red, PotionEffectType.POISON), "poisoned too early");
        server.getScheduler().performTicks(20 * 3);
        assertTrue(has(red, PotionEffectType.POISON));
        assertContains(messages(red), "Don't camp at your beacon");
    }

    @Test
    void defendingIsNoCamping() {
        config("base.camping-seconds", 3);
        startGame();
        red.setLocation(nearRedBeacon(2));
        // an enemy near the beacon, but not in the trap
        blue.setLocation(nearRedBeacon(8));
        server.getScheduler().performTicks(20 * 6);
        assertFalse(has(red, PotionEffectType.POISON));
    }

    // --- timed events

    private IngameState ingame() {
        return (IngameState) game.getGameStateManager().getCurrentGameState();
    }

    @Test
    void defaultEventsAreShownInTheScoreboard() {
        startGame();
        GameEvent next = ingame().getNextEvent();
        assertNotNull(next);
        assertEquals(GameEvent.Action.BEACON_PROTECTION_OFF, next.action());
        assertEquals(600, next.second());
    }

    @Test
    void beaconProtectionEnds() {
        config("events", List.of(Map.of("minute", 0.1, "action", "beacon-protection-off")));
        startGame();
        Block nearBeacon = at(BLUE_BEACONS.getFirst(), gameWorld()).getBlock().getRelative(1, 0, 0);
        nearBeacon.setType(Material.STONE);
        BlockBreakEvent before = red.simulateBlockBreak(nearBeacon);
        assertTrue(before == null || before.isCancelled());

        server.getScheduler().performTicks(20 * 8);
        assertFalse(game.hasBeaconProtection());
        assertContains(messages(red), "The protection zones around the beacons are gone!");
        nearBeacon.setType(Material.STONE);
        BlockBreakEvent after = red.simulateBlockBreak(nearBeacon);
        assertNotNull(after);
        assertFalse(after.isCancelled());
        assertNull(ingame().getNextEvent());
    }

    @Test
    void effectsEventKeepsEffectsAfterRespawn() {
        config("events", List.of(Map.of("minute", 0.1, "action", "effects", "effects", List.of("SPEED:1"))));
        startGame();
        server.getScheduler().performTicks(20 * 8);
        assertEquals(1, red.getPotionEffect(PotionEffectType.SPEED).getAmplifier());
        killAndRespawn(red);
        server.getScheduler().performTicks(1);
        assertTrue(has(red, PotionEffectType.SPEED), "the effect is gone after the respawn");
    }

    @Test
    void endEventLetsTheTeamWithMoreBeaconsWin() {
        config("events", List.of(Map.of("minute", 0.1, "action", "end")));
        startGame();
        Block beacon = at(BLUE_BEACONS.getFirst(), gameWorld()).getBlock();
        beacon.setType(Material.BEACON);
        red.simulateBlockBreak(beacon);
        server.getScheduler().performTicks(20 * 8);
        assertEquals(GameState.ENDING, game.getGameState());
        assertEquals(Team.RED, game.getWinner());
    }

    @Test
    void endEventWithEqualBeaconsIsADraw() {
        config("events", List.of(Map.of("minute", 0.1, "action", "end")));
        startGame();
        server.getScheduler().performTicks(20 * 8);
        assertEquals(GameState.ENDING, game.getGameState());
        assertNull(game.getWinner());
        assertContains(messages(red), "nobody has won");
    }

    @Test
    void invalidEventsAreSkipped() {
        config("events", List.of(Map.of("minute", 1, "action", "explode"), Map.of("action", "end"),
                Map.of("minute", 2, "action", "effects", "effects", List.of("SPEED", "nothing"))));
        List<GameEvent> events = GameEvent.load();
        assertEquals(1, events.size());
        assertEquals(1, events.getFirst().effects().size());
        assertEquals(PotionEffectType.SPEED, events.getFirst().effects().getFirst().getType());
    }

    // --- spectators

    private PlayerMock spectator() {
        PlayerMock late = addPlayer("Late");
        execute(late, "ctb spectate");
        assertTrue(plugin.getUser(late).isSpectator(), "not spectating: " + messages(late));
        return late;
    }

    @Test
    void spectatorsWatchInvisibly() {
        startGame();
        PlayerMock late = spectator();
        assertEquals(gameWorld(), late.getWorld());
        assertTrue(late.getAllowFlight());
        assertTrue(late.isInvulnerable());
        assertEquals(InteractionItems.getSpectatorCompass(), late.getInventory().getItem(0));
        assertFalse(red.canSee(late), "players see the spectator");
        assertTrue(late.canSee(red));
        assertFalse(game.getGamePlayers().contains(plugin.getUser(late)));
    }

    @Test
    void spectatorsCantInterfere() {
        startGame();
        PlayerMock late = spectator();
        BlockBreakEvent event = late.simulateBlockBreak(mapBlock(Material.STONE));
        assertTrue(event == null || event.isCancelled(), "a spectator broke a block");
        assertFalse(attack(late, red), "a spectator hurt a player");
        assertFalse(attack(red, late), "a spectator got hurt");
        Block beacon = at(BLUE_BEACONS.getFirst(), gameWorld()).getBlock();
        beacon.setType(Material.BEACON);
        late.simulateBlockBreak(beacon);
        assertEquals(Material.BEACON, beacon.getType());
    }

    @Test
    void spectatorsLeave() {
        startGame();
        PlayerMock late = spectator();
        execute(late, "ctb quit");
        assertFalse(plugin.getUser(late).isSpectator());
        assertEquals("world", late.getWorld().getName());
        assertFalse(late.isInvulnerable());
    }

    @Test
    void leaveItemEndsSpectating() {
        startGame();
        PlayerMock late = spectator();
        server.getPluginManager().callEvent(new PlayerInteractEvent(late, Action.RIGHT_CLICK_AIR,
                InteractionItems.getLeaveItem(), null, org.bukkit.block.BlockFace.SELF));
        assertNull(plugin.getUser(late).getGame());
        assertEquals("world", late.getWorld().getName());
    }

    @Test
    void spectatorsAreSentBackAtTheEnd() {
        startGame();
        PlayerMock late = spectator();
        execute(blue, "ctb quit");
        assertEquals(GameState.ENDING, game.getGameState());
        server.getScheduler().performTicks(20 * 17);
        assertEquals("world", late.getWorld().getName());
        assertContains(messages(late), "[Play again]");
    }

    @Test
    void nothingToWatchInTheLobby() {
        PlayerMock late = addPlayer("Late");
        execute(late, "ctb spectate");
        assertNull(plugin.getUser(late).getGame());
        assertContains(messages(late), "There is no running game you could watch!");
    }

    // --- rejoin

    @Test
    void playersCanRejoinTheirTeam() {
        startGame();
        plugin.getUser(red).addKill();
        red.disconnect();
        assertEquals(GameState.INGAME, game.getGameState(), "the game ended although red can come back");
        assertContains(messages(blue), "can come back within");

        red.reconnect();
        server.getScheduler().performTicks(1);
        GamePlayer gamePlayer = plugin.getUser(red);
        assertEquals(game, gamePlayer.getGame());
        assertEquals(Team.RED, gamePlayer.getTeam());
        assertEquals(1, gamePlayer.getKills(), "the kills are lost");
        assertEquals(gameWorld(), red.getWorld());
        assertContains(messages(blue), "is back in the game!");
    }

    @Test
    void teamLosesIfNobodyComesBack() {
        config("game.rejoin-seconds", 5);
        startGame();
        red.disconnect();
        server.getScheduler().performTicks(20 * 4);
        assertEquals(GameState.INGAME, game.getGameState());
        server.getScheduler().performTicks(20 * 2);
        assertEquals(GameState.ENDING, game.getGameState());
        assertEquals(Team.BLUE, game.getWinner());
    }

    @Test
    void rejoinCanBeDisabled() {
        config("game.rejoin-seconds", 0);
        startGame();
        red.disconnect();
        assertEquals(GameState.ENDING, game.getGameState());
    }

    @Test
    void gameGoesOnWhileTheTeamHasPlayers() {
        PlayerMock red2 = addPlayer("Red2");
        joinWithTeam(red2, Team.RED);
        startGame();
        execute(red2, "ctb quit");
        assertEquals(GameState.INGAME, game.getGameState());
        execute(blue, "ctb quit");
        assertEquals(GameState.ENDING, game.getGameState());
        assertEquals(Team.RED, game.getWinner());
    }

    // --- chat

    private void chat(PlayerMock player, String message) {
        server.getPluginManager().callEvent(new AsyncChatEvent(false, player, new HashSet<>(server.getOnlinePlayers()),
                ChatRenderer.defaultRenderer(), Component.text(message), Component.text(message),
                SignedMessage.system(message, null)));
    }

    @Test
    void chatOnlyGoesToTheTeam() {
        PlayerMock red2 = addPlayer("Red2");
        joinWithTeam(red2, Team.RED);
        startGame();
        messages(red2);
        messages(blue);
        chat(red, "attack left");
        assertContains(messages(red2), "Red1: attack left");
        assertNotContains(messages(blue), "attack left");
    }

    @Test
    void shoutGoesToEverybody() {
        startGame();
        messages(blue);
        chat(red, "!good game");
        assertContains(messages(blue), "[All] Red1: good game");
    }

    @Test
    void teamChatCanBeDisabled() {
        config("chat.team-chat", false);
        startGame();
        messages(blue);
        chat(red, "hello");
        assertContains(messages(blue), "Red1: hello");
    }

    @Test
    void colorCodesOfPlayersAreKept() {
        startGame();
        messages(blue);
        chat(red, "!&chello");
        assertContains(messages(blue), "&chello");
    }

    @Test
    void spectatorsChatAmongThemselves() {
        startGame();
        PlayerMock late = spectator();
        messages(red);
        chat(late, "nice");
        assertNotContains(messages(red), "nice");
        assertContains(messages(late), "[Spectator] Late: nice");
    }

    // --- players

    @Test
    void playersHaveExtraHeartsAndSixteenArrows() {
        startGame();
        assertEquals(26, red.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
        assertEquals(26, red.getHealth());
        assertTrue(red.getInventory().containsAtLeast(new ItemStack(Material.ARROW), 16));
        assertFalse(red.getInventory().containsAtLeast(new ItemStack(Material.ARROW), 17));
        execute(red, "ctb quit");
        assertEquals(20, red.getAttribute(Attribute.MAX_HEALTH).getBaseValue(), "the extra hearts stay");
    }

    @Test
    void modernCombatByDefault() {
        // MockBukkit players don't have it
        red.registerAttribute(Attribute.ATTACK_SPEED);
        startGame();
        assertEquals(GameItems.DEFAULT_ATTACK_SPEED, red.getAttribute(Attribute.ATTACK_SPEED).getBaseValue());
    }

    @Test
    void legacyCombatWithoutCooldown() {
        red.registerAttribute(Attribute.ATTACK_SPEED);
        config("game.combat", "legacy");
        startGame();
        assertEquals(GameItems.LEGACY_ATTACK_SPEED, red.getAttribute(Attribute.ATTACK_SPEED).getBaseValue());
        execute(red, "ctb quit");
        assertEquals(GameItems.DEFAULT_ATTACK_SPEED, red.getAttribute(Attribute.ATTACK_SPEED).getBaseValue());
    }

}
