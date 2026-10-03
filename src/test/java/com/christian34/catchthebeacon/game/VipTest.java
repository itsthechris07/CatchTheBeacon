package com.christian34.catchthebeacon.game;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What players with ctb.vip get ({@link VipPerks}) - Red1 is a VIP, Blue1 isn't.
 */
class VipTest extends GameTestBase {

    /**
     * fills the lobby with players (with the given permissions)
     */
    private List<PlayerMock> fillLobby(String... permissions) {
        List<PlayerMock> players = new ArrayList<>();
        while (game.getGamePlayers().size() < game.getArena().getMaxPlayers()) {
            PlayerMock player = addPlayer("Player" + players.size(), permissions);
            execute(player, "ctb join");
            players.add(player);
        }
        assertFalse(game.isJoinable());
        return players;
    }

    @Test
    void vipGetsIntoFullLobby() {
        List<PlayerMock> players = fillLobby();
        PlayerMock normal = addPlayer("Normal");
        execute(normal, "ctb join");
        assertNull(plugin.getUser(normal).getGame());

        PlayerMock vip = addPlayer("Steve", VipPerks.PERMISSION);
        execute(vip, "ctb join");
        assertEquals(game, plugin.getUser(vip).getGame());
        assertEquals(game.getArena().getMaxPlayers(), game.getGamePlayers().size());
        // the last one who joined without VIP
        PlayerMock last = players.getLast();
        assertNull(plugin.getUser(last).getGame());
        assertContains(messages(last), "made room for the VIP Steve");
        assertContains(messages(blue), "VIP Steve joined the game!");
    }

    @Test
    void lobbyFullOfVips() {
        fillLobby(VipPerks.PERMISSION);
        PlayerMock vip = addPlayer("Steve", VipPerks.PERMISSION);
        execute(vip, "ctb join");
        assertEquals(game, plugin.getUser(vip).getGame());
        assertNull(plugin.getUser(blue).getGame(), "the only one without VIP makes room");

        PlayerMock another = addPlayer("Alex", VipPerks.PERMISSION);
        execute(another, "ctb join");
        assertNull(plugin.getUser(another).getGame());
        assertEquals(game, plugin.getUser(red).getGame());
    }

    @Test
    void notIntoRunningGame() {
        startGame();
        PlayerMock vip = addPlayer("Steve", VipPerks.PERMISSION);
        assertFalse(game.isJoinable(vip));
    }

    @Test
    void voteCountsTwice() {
        game.vote(plugin.getUser(red), Variant.ONE_HIT);
        game.vote(plugin.getUser(blue), Variant.BOW_ONLY);
        assertEquals(VipPerks.VOTE_WEIGHT, game.getVotes(Variant.ONE_HIT));
        assertEquals(1, game.getVotes(Variant.BOW_ONLY));
        startGame();
        assertEquals(Variant.ONE_HIT, game.getVariant());
    }

}
