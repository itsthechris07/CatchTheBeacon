package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.game.states.IngameState;
import com.christian34.catchthebeacon.lib.InteractionItems;
import com.christian34.catchthebeacon.stats.StatsManager;
import io.papermc.paper.event.block.BlockBreakProgressUpdateEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.attribute.AttributeInstanceMock;
import org.mockbukkit.mockbukkit.entity.LivingEntityMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The features of a round: mining boss bar, beacon compass, round summary, achievements, tips, teams by strength and
 * the variants.
 *
 * @author Christian34
 */
class RoundFeaturesTest extends GameTestBase {

    private void config(String key, Object value) {
        plugin.getFileManager().getConfigFile().set(key, value);
    }

    /**
     * waits for the queued database queries (stats, achievements) and runs the tasks they scheduled
     */
    private void waitForDatabase() {
        plugin.getStatsManager().flush().join();
        server.getScheduler().performOneTick();
    }

    private void addStat(PlayerMock player, StatsManager.Stat stat, int times) {
        waitForDatabase();
        for (int i = 0; i < times; i++) plugin.getStatsManager().add(player, stat);
        waitForDatabase();
    }

    private void kill(PlayerMock killer, PlayerMock victim) {
        victim.setKiller(killer);
        victim.damage(100, killer);
        victim.respawn();
    }

    private Block beaconBlock(Location location) {
        Block block = at(location, gameWorld()).getBlock();
        block.setType(Material.BEACON);
        return block;
    }

    private Block redLeft() {
        return beaconBlock(RED_BEACONS.getFirst());
    }

    private void destroyBlueBeacons(PlayerMock player) {
        for (Location location : BLUE_BEACONS) {
            player.simulateBlockBreak(beaconBlock(location));
        }
    }

    private void mine(PlayerMock player, Block block, float progress) {
        server.getPluginManager().callEvent(new BlockBreakProgressUpdateEvent(block, progress, player));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static Set<BossBar> bossBars(PlayerMock player) {
        return player.getBossBars();
    }

    private static List<String> actionBars(PlayerMock player) {
        List<String> actionBars = new ArrayList<>();
        Component actionBar;
        while ((actionBar = player.nextActionBar()) != null) actionBars.add(plain(actionBar));
        return actionBars;
    }

    // --- mining boss bar

    @Test
    void everybodySeesTheMiningProgress() {
        startGame();
        mine(blue, redLeft(), 0.6f);
        for (PlayerMock player : List.of(red, blue)) {
            assertEquals(1, bossBars(player).size(), player.getName() + " doesn't see the boss bar");
            BossBar bossBar = bossBars(player).iterator().next();
            assertEquals(0.6f, bossBar.progress(), 0.001);
            assertTrue(plain(bossBar.name()).contains("60%"), plain(bossBar.name()));
        }
        assertEquals(blue.getUniqueId(), redLeftBeacon().getMiner());
    }

    @Test
    void miningTheOwnBeaconShowsNothing() {
        startGame();
        mine(red, redLeft(), 0.5f);
        assertTrue(bossBars(red).isEmpty());
        assertNull(redLeftBeacon().getMiner());
    }

    @Test
    void bossBarCanBeDisabled() {
        config("base.mining-bossbar", false);
        startGame();
        mine(blue, redLeft(), 0.5f);
        assertTrue(bossBars(red).isEmpty());
        assertEquals(blue.getUniqueId(), redLeftBeacon().getMiner(), "the compass needs the miner");
    }

    @Test
    void bossBarDisappearsWithoutMining() {
        startGame();
        mine(blue, redLeft(), 0.3f);
        server.getScheduler().performTicks(20);
        assertFalse(bossBars(red).isEmpty(), "disappeared too early");
        server.getScheduler().performTicks(20 * 3);
        assertTrue(bossBars(red).isEmpty());
        assertNull(redLeftBeacon().getMiner());
    }

    @Test
    void bossBarDisappearsWhenTheBeaconIsDestroyed() {
        startGame();
        Block beacon = redLeft();
        mine(blue, beacon, 0.9f);
        blue.simulateBlockBreak(beacon);
        assertFalse(redLeftBeacon().isAlive());
        assertTrue(bossBars(red).isEmpty());
        assertTrue(bossBars(blue).isEmpty());
    }

    @Test
    void bossBarDisappearsWhenTheMinerDies() {
        startGame();
        mine(blue, redLeft(), 0.5f);
        killAndRespawn(blue);
        assertTrue(bossBars(red).isEmpty());
        assertNull(redLeftBeacon().getMiner());
    }

    // --- beacon compass

    @Test
    void compassIsInTheKit() {
        startGame();
        assertTrue(red.getInventory().containsAtLeast(InteractionItems.getBeaconCompass(), 1));
    }

    private void assertCompassPointsTo(PlayerMock player, Location beacon) {
        Location target = player.getCompassTarget();
        assertEquals(gameWorld(), target.getWorld());
        assertEquals(beacon.getX() + 0.5, target.getX(), 0.001);
        assertEquals(beacon.getZ() + 0.5, target.getZ(), 0.001);
    }

    @Test
    void compassPointsToTheNearestEnemyBeacon() {
        startGame();
        red.setLocation(at(BLUE_BEACONS.getLast(), gameWorld()).add(0, 0, 20));
        server.getScheduler().performTicks(20);
        assertCompassPointsTo(red, BLUE_BEACONS.getLast());
        red.setLocation(at(BLUE_BEACONS.getFirst(), gameWorld()).add(0, 0, -20));
        server.getScheduler().performTicks(20);
        assertCompassPointsTo(red, BLUE_BEACONS.getFirst());
    }

    @Test
    void compassPointsToAnOwnBeaconBeingMined() {
        startGame();
        red.setLocation(at(BLUE_BEACONS.getFirst(), gameWorld()).add(5, 0, 0));
        mine(blue, beaconBlock(RED_BEACONS.getLast()), 0.2f);
        server.getScheduler().performTicks(20);
        assertCompassPointsTo(red, RED_BEACONS.getLast());
    }

    @Test
    void holdingTheCompassShowsTheDistance() {
        startGame();
        red.setLocation(at(BLUE_BEACONS.getFirst(), gameWorld()).add(10.5, 0, 0.5));
        red.getInventory().setItemInMainHand(InteractionItems.getBeaconCompass());
        actionBars(red);
        server.getScheduler().performTicks(20);
        assertContains(actionBars(red), "Enemy left beacon: 10m");
    }

    @Test
    void compassTargetIsRestored() {
        PlayerMock player = addPlayer("Red2");
        Location home = new Location(player.getWorld(), 100, 64, 100);
        player.setCompassTarget(home);
        joinWithTeam(player, Team.RED);
        startGame();
        assertNotEquals(home, player.getCompassTarget());
        execute(player, "ctb quit");
        assertEquals(home, player.getCompassTarget());
    }

    /**
     * remembers the drops of the last death
     */
    public static class DropRecorder implements Listener {
        private final List<ItemStack> drops = new ArrayList<>();

        @EventHandler(priority = EventPriority.MONITOR)
        public void onDeath(PlayerDeathEvent e) {
            drops.clear();
            drops.addAll(e.getDrops());
        }
    }

    @Test
    void compassIsNotDropped() {
        DropRecorder recorder = new DropRecorder();
        server.getPluginManager().registerEvents(recorder, plugin);
        startGame();
        red.damage(100);
        assertFalse(recorder.drops.isEmpty(), "nothing dropped at all");
        assertTrue(recorder.drops.stream().noneMatch(item -> item != null && item.isSimilar(InteractionItems.getBeaconCompass())));
    }

    @Test
    void compassCanBeDisabled() {
        config("game.beacon-compass", false);
        startGame();
        assertFalse(red.getInventory().containsAtLeast(InteractionItems.getBeaconCompass(), 1));
    }

    // --- round summary

    @Test
    void summaryShowsTheBestPlayers() {
        config("resources", List.of(Map.of("block", "IRON_BLOCK", "rewards", List.of("IRON_INGOT"))));
        startGame();
        kill(red, blue);
        Block iron = new Location(gameWorld(), 0, 64, 60).getBlock();
        iron.setType(Material.IRON_BLOCK);
        red.simulateBlockBreak(iron);
        red.simulateBlockBreak(beaconBlock(BLUE_BEACONS.getFirst()));
        messages(red);
        execute(blue, "ctb quit");
        assertEquals(GameState.ENDING, game.getGameState());
        List<String> messages = messages(red);
        assertContains(messages, "Round summary");
        assertContains(messages, "MVP: Red1 (1 kills, 1 beacons)");
        assertContains(messages, "Most kills: Red1 (1)");
        assertContains(messages, "Red1 destroyed the left beacon of Blue");
        assertContains(messages, "Mined by Red: 1x iron block");
    }

    @Test
    void summaryWithoutKills() {
        startGame();
        execute(blue, "ctb quit");
        List<String> messages = messages(red);
        assertContains(messages, "Round summary");
        assertNotContains(messages, "MVP");
        assertNotContains(messages, "Most kills");
    }

    @Test
    void beaconsCountMoreThanKillsForTheMvp() {
        PlayerMock red2 = addPlayer("Red2");
        joinWithTeam(red2, Team.RED);
        startGame();
        kill(red, blue);
        kill(red, blue);
        red2.simulateBlockBreak(beaconBlock(BLUE_BEACONS.getFirst()));
        assertEquals(plugin.getUser(red2), RoundSummary.getMvp(game));
    }

    @Test
    void summaryCanBeDisabled() {
        config("game.round-summary", false);
        startGame();
        execute(blue, "ctb quit");
        assertNotContains(messages(red), "Round summary");
    }

    // --- achievements

    private List<String> achievements(PlayerMock player) {
        waitForDatabase();
        return messages(player).stream().filter(message -> message.contains("Achievement unlocked")).toList();
    }

    @Test
    void firstBloodIsOnlyAnnouncedOnce() {
        startGame();
        kill(red, blue);
        assertContains(achievements(red), "First blood");
        kill(red, blue);
        assertNotContains(achievements(red), "First blood");
        assertTrue(achievements(blue).isEmpty());
    }

    @Test
    void killingTheMinerIsALastSecondRescue() {
        startGame();
        mine(blue, redLeft(), 0.9f);
        kill(red, blue);
        assertContains(achievements(red), "Last-second rescue");
    }

    @Test
    void killingSpree() {
        startGame();
        for (int i = 0; i < 4; i++) kill(red, blue);
        assertNotContains(achievements(red), "Unstoppable");
        kill(red, blue);
        assertContains(achievements(red), "Unstoppable");
    }

    @Test
    void dyingEndsTheKillingSpree() {
        startGame();
        for (int i = 0; i < 4; i++) kill(red, blue);
        kill(blue, red);
        kill(red, blue);
        assertNotContains(achievements(red), "Unstoppable");
    }

    @Test
    void winningAloneAndFast() {
        startGame();
        destroyBlueBeacons(red);
        assertEquals(GameState.ENDING, game.getGameState());
        List<String> achievements = achievements(red);
        assertContains(achievements, "One-man army");
        assertContains(achievements, "First victory");
        assertContains(achievements, "No problems");
        assertContains(achievements, "That was quick");
        assertNotContains(achievements, "Veteran");
    }

    @Test
    void noFlawlessOrQuickWinIfTheEnemiesLeft() {
        startGame();
        execute(blue, "ctb quit");
        List<String> achievements = achievements(red);
        assertContains(achievements, "First victory");
        assertNotContains(achievements, "No problems");
        assertNotContains(achievements, "That was quick");
    }

    @Test
    void noFlawlessWinAfterLosingABeacon() {
        startGame();
        blue.simulateBlockBreak(redLeft());
        destroyBlueBeacons(red);
        List<String> achievements = achievements(red);
        assertContains(achievements, "First victory");
        assertNotContains(achievements, "No problems");
    }

    @Test
    void noSoloWinWithHelp() {
        PlayerMock red2 = addPlayer("Red2");
        joinWithTeam(red2, Team.RED);
        startGame();
        red2.simulateBlockBreak(beaconBlock(BLUE_BEACONS.getFirst()));
        red.simulateBlockBreak(beaconBlock(BLUE_BEACONS.getLast()));
        assertNotContains(achievements(red), "One-man army");
    }

    @Test
    void veteranWithTheTenthWin() {
        addStat(red, StatsManager.Stat.WINS, 9);
        startGame();
        destroyBlueBeacons(red);
        assertContains(achievements(red), "Veteran");
    }

    @Test
    void achievementsCommand() {
        startGame();
        kill(red, blue);
        waitForDatabase();
        messages(red);
        execute(red, "ctb achievements");
        waitForDatabase();
        List<String> messages = messages(red);
        assertContains(messages, "Achievements of Red1 (1/8)");
        assertContains(messages, "✔ First blood");
        assertContains(messages, "✘ Veteran");
    }

    @Test
    void achievementsCanBeDisabled() {
        config("achievements.enabled", false);
        startGame();
        kill(red, blue);
        assertTrue(achievements(red).isEmpty());
        execute(red, "ctb achievements");
        assertContains(messages(red), "Achievements are disabled");
    }

    // --- tips

    @Test
    void newPlayersGetTips() {
        startGame();
        waitForDatabase();
        server.getScheduler().performTicks(40);
        assertTrue(actionBars(red).stream().anyMatch(actionBar -> actionBar.startsWith("Tip:")));
    }

    @Test
    void experiencedPlayersGetNoTips() {
        addStat(red, StatsManager.Stat.GAMES, 3);
        startGame();
        server.getScheduler().performTicks(60);
        assertTrue(actionBars(red).stream().noneMatch(actionBar -> actionBar.startsWith("Tip:")));
    }

    @Test
    void beaconTipNearAnEnemyBeacon() {
        startGame();
        waitForDatabase();
        red.setLocation(at(BLUE_BEACONS.getFirst(), gameWorld()).add(4, 0, 0));
        actionBars(red);
        server.getScheduler().performTicks(40);
        assertContains(actionBars(red), "A beacon takes");
    }

    @Test
    void tipsCanBeDisabled() {
        config("tips.rounds", 0);
        startGame();
        waitForDatabase();
        server.getScheduler().performTicks(60);
        assertTrue(actionBars(red).stream().noneMatch(actionBar -> actionBar.startsWith("Tip:")));
    }

    // --- teams by strength

    @RepeatedTest(10)
    void strongPlayersAreSplit() {
        plugin.getUser(red).setTeam(Team.RANDOM);
        plugin.getUser(blue).setTeam(Team.RANDOM);
        PlayerMock strong1 = addPlayer("Strong1");
        PlayerMock strong2 = addPlayer("Strong2");
        joinWithTeam(strong1, Team.RANDOM);
        joinWithTeam(strong2, Team.RANDOM);
        for (PlayerMock player : List.of(strong1, strong2)) {
            addStat(player, StatsManager.Stat.WINS, 10);
            addStat(player, StatsManager.Stat.GAMES, 10);
        }
        for (PlayerMock player : List.of(red, blue)) addStat(player, StatsManager.Stat.GAMES, 10);
        startGame();
        assertNotEquals(plugin.getUser(strong1).getTeam(), plugin.getUser(strong2).getTeam());
    }

    // --- variants

    private IngameState ingame() {
        return (IngameState) game.getGameStateManager().getCurrentGameState();
    }

    private void voteBoth(Variant variant) {
        game.vote(plugin.getUser(red), variant);
        game.vote(plugin.getUser(blue), variant);
    }

    @Test
    void variantWithTheMostVotesIsPlayed() {
        PlayerMock red2 = addPlayer("Red2");
        joinWithTeam(red2, Team.RED);
        voteBoth(Variant.RUSH);
        game.vote(plugin.getUser(red2), Variant.ONE_HIT);
        startGame();
        assertEquals(Variant.RUSH, game.getVariant());
        assertContains(messages(blue), "Variant: Rush");
    }

    @Test
    void votesCanBeChanged() {
        game.vote(plugin.getUser(red), Variant.RUSH);
        game.vote(plugin.getUser(red), Variant.ONE_HIT);
        assertEquals(0, game.getVotes(Variant.RUSH));
        assertEquals(1, game.getVotes(Variant.ONE_HIT));
    }

    @RepeatedTest(5)
    void tieIsDecidedRandomly() {
        game.vote(plugin.getUser(red), Variant.BOW_ONLY);
        game.vote(plugin.getUser(blue), Variant.ONE_HIT);
        startGame();
        assertTrue(Set.of(Variant.BOW_ONLY, Variant.ONE_HIT).contains(game.getVariant()), game.getVariant().name());
    }

    @Test
    void normalWithoutVotes() {
        startGame();
        assertEquals(Variant.NORMAL, game.getVariant());
        assertNotContains(messages(red), "Variant:");
    }

    @Test
    void voteItemOnlyWithVoting() {
        assertEquals(InteractionItems.getVoteItem(), red.getInventory().getItem(6));
        config("variants.voting", false);
        PlayerMock late = addPlayer("Late");
        execute(late, "ctb join");
        assertNotEquals(InteractionItems.getVoteItem(), late.getInventory().getItem(6));
        assertTrue(Variant.getVotable().isEmpty());
    }

    @Test
    void startItemNeedsPermissionAndEnoughPlayers() {
        assertEquals(InteractionItems.getStartItem(), red.getInventory().getItem(2));
        assertNotEquals(InteractionItems.getStartItem(), blue.getInventory().getItem(2));
        execute(blue, "ctb quit");
        execute(red, "ctb quit");
        PlayerMock starter = addPlayer("Starter", Game.START_PERMISSION);
        execute(starter, "ctb join");
        ItemStack item = starter.getInventory().getItem(2);
        assertEquals(InteractionItems.getStartItem(), item);
        messages(starter);

        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerInteractEvent(starter,
                org.bukkit.event.block.Action.RIGHT_CLICK_AIR, item, null, org.bukkit.block.BlockFace.SELF));
        assertContains(messages(starter), "at least 2 players");

        execute(blue, "ctb join");
        server.getPluginManager().callEvent(new org.bukkit.event.player.PlayerInteractEvent(starter,
                org.bukkit.event.block.Action.RIGHT_CLICK_AIR, item, null, org.bukkit.block.BlockFace.SELF));
        assertContains(messages(starter), "The game was started");
        server.getScheduler().performTicks(20 * 7);
        assertEquals(GameState.INGAME, game.getGameState());
    }

    @Test
    void votesForDisabledVariantsDontCount() {
        config("variants.list", List.of("rush"));
        voteBoth(Variant.ONE_HIT);
        startGame();
        assertEquals(Variant.NORMAL, game.getVariant());
    }

    /**
     * MockBukkit players don't have it, and registerAttribute has no default value for it
     */
    @SuppressWarnings("unchecked")
    private static void registerBreakSpeed(PlayerMock player) throws ReflectiveOperationException {
        Field field = LivingEntityMock.class.getDeclaredField("attributes");
        field.setAccessible(true);
        ((Map<Attribute, AttributeInstanceMock>) field.get(player)).put(Attribute.BLOCK_BREAK_SPEED,
                new AttributeInstanceMock(Attribute.BLOCK_BREAK_SPEED, GameItems.DEFAULT_BLOCK_BREAK_SPEED));
    }

    @Test
    void rushMinesFasterAndEventsComeEarlier() throws ReflectiveOperationException {
        registerBreakSpeed(red);
        voteBoth(Variant.RUSH);
        startGame();
        assertEquals(Variant.RUSH_BREAK_SPEED, red.getAttribute(Attribute.BLOCK_BREAK_SPEED).getBaseValue());
        GameEvent next = ingame().getNextEvent();
        assertNotNull(next);
        assertTrue(ingame().getSecondsUntil(next) <= next.second() / 2, "the event doesn't come earlier");
        execute(red, "ctb quit");
        assertEquals(GameItems.DEFAULT_BLOCK_BREAK_SPEED, red.getAttribute(Attribute.BLOCK_BREAK_SPEED).getBaseValue());
    }

    @Test
    void normalRoundMinesNormally() throws ReflectiveOperationException {
        registerBreakSpeed(red);
        startGame();
        assertEquals(GameItems.DEFAULT_BLOCK_BREAK_SPEED, red.getAttribute(Attribute.BLOCK_BREAK_SPEED).getBaseValue());
    }

    private EntityDamageByEntityEvent damage(Entity damager, PlayerMock attacker, PlayerMock victim) {
        var event = new EntityDamageByEntityEvent(damager, victim,
                damager instanceof Arrow ? EntityDamageEvent.DamageCause.PROJECTILE : EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                DamageSource.builder(damager instanceof Arrow ? DamageType.ARROW : DamageType.PLAYER_ATTACK)
                        .withCausingEntity(attacker).withDirectEntity(damager).build(), 5);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private Arrow arrow(PlayerMock shooter) {
        Arrow arrow = gameWorld().spawn(shooter.getLocation(), Arrow.class);
        arrow.setShooter(shooter);
        return arrow;
    }

    @Test
    void bowOnlyAllowsOnlyArrows() {
        voteBoth(Variant.BOW_ONLY);
        startGame();
        assertFalse(attack(red, blue), "a melee attack hurt");
        assertFalse(damage(arrow(red), red, blue).isCancelled(), "an arrow didn't hurt");
        ItemStack bow = red.getInventory().all(Material.BOW).values().stream().findFirst().orElseThrow();
        assertTrue(bow.containsEnchantment(Enchantment.INFINITY));
    }

    @Test
    void meleeWorksInNormalRounds() {
        startGame();
        assertTrue(attack(red, blue));
        ItemStack bow = red.getInventory().all(Material.BOW).values().stream().findFirst().orElse(null);
        assertTrue(bow == null || !bow.containsEnchantment(Enchantment.INFINITY));
    }

    @Test
    void oneHitKills() {
        voteBoth(Variant.ONE_HIT);
        startGame();
        EntityDamageByEntityEvent event = damage(red, red, blue);
        assertFalse(event.isCancelled());
        assertEquals(1000, event.getDamage());
        assertEquals(1000, damage(arrow(red), red, blue).getDamage());
    }

    @Test
    void oneHitOnlyForEnemies() {
        config("game.friendly-fire", true);
        PlayerMock red2 = addPlayer("Red2");
        joinWithTeam(red2, Team.RED);
        voteBoth(Variant.ONE_HIT);
        startGame();
        assertNotEquals(1000, damage(red, red, red2).getDamage());
    }

    @Test
    void noIronGivesNothing() {
        config("resources", List.of(Map.of("block", "IRON_BLOCK", "rewards", List.of("IRON_CHESTPLATE"))));
        voteBoth(Variant.NO_IRON);
        startGame();
        Block iron = new Location(gameWorld(), 0, 64, 60).getBlock();
        iron.setType(Material.IRON_BLOCK);
        BlockBreakEvent event = red.simulateBlockBreak(iron);
        assertNotNull(event);
        assertFalse(event.isDropItems(), "the iron block is dropped");
        assertNotEquals(Material.IRON_CHESTPLATE, red.getInventory().getItem(EquipmentSlot.CHEST).getType());
        assertTrue(game.getMinedResources().getOrDefault(Team.RED, Map.of()).isEmpty());
    }

}
