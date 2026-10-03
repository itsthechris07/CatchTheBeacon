package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.integrations.PartyProvider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parties of party plugins (integrations/PartySupport) with a fake party plugin
 *
 * @author Christian34
 */
class PartyTest extends GameTestBase {
    private final Map<UUID, PartyProvider.Party> parties = new HashMap<>();

    @BeforeEach
    void setUpParties() {
        plugin.getPartySupport().addProvider(new PartyProvider() {
            @Override
            public String getName() {
                return "TestParties";
            }

            @Override
            @Nullable
            public Party getParty(@NotNull Player player) {
                return parties.get(player.getUniqueId());
            }
        });
    }

    /**
     * @param players the leader first
     */
    private void party(Player... players) {
        Set<UUID> members = Arrays.stream(players).map(Player::getUniqueId).collect(Collectors.toSet());
        PartyProvider.Party party = new PartyProvider.Party(players[0].getUniqueId(), members);
        for (Player player : players) {
            parties.put(player.getUniqueId(), party);
        }
    }

    private Team team(Player player) {
        return plugin.getUser(player).getTeam();
    }

    @Test
    void leaderTakesPartyIntoTheGame() {
        PlayerMock leader = addPlayer("Leader");
        PlayerMock member = addPlayer("Member");
        PlayerMock other = addPlayer("Other");
        party(leader, member);

        execute(leader, "ctb join");
        assertEquals(game, plugin.getUser(leader).getGame());
        assertEquals(game, plugin.getUser(member).getGame());
        assertContains(messages(member), "You joined the game with your party leader Leader");
        assertNull(plugin.getUser(other).getGame());
    }

    @Test
    void membersDoNotTakeTheLeaderAlong() {
        PlayerMock leader = addPlayer("Leader");
        PlayerMock member = addPlayer("Member");
        party(leader, member);

        execute(member, "ctb join");
        assertEquals(game, plugin.getUser(member).getGame());
        assertNull(plugin.getUser(leader).getGame());
    }

    @Test
    void joinTogetherCanBeDisabled() {
        plugin.getFileManager().getConfigFile().set("parties.join-together", false);
        PlayerMock leader = addPlayer("Leader");
        PlayerMock member = addPlayer("Member");
        party(leader, member);

        execute(leader, "ctb join");
        assertNull(plugin.getUser(member).getGame());
    }

    @Test
    void partiesAreDisabled() {
        plugin.getFileManager().getConfigFile().set("parties.enabled", false);
        PlayerMock leader = addPlayer("Leader");
        PlayerMock member = addPlayer("Member");
        party(leader, member);

        execute(leader, "ctb join");
        assertNull(plugin.getUser(member).getGame());
    }

    @Test
    void partyPlaysInTheSameTeam() {
        PlayerMock a1 = addPlayer("A1");
        PlayerMock a2 = addPlayer("A2");
        PlayerMock single1 = addPlayer("S1");
        PlayerMock single2 = addPlayer("S2");
        party(a1, a2);
        for (PlayerMock player : new PlayerMock[]{single1, a1, single2}) {
            execute(player, "ctb join");
        }
        startGame();

        assertNotNull(team(a1));
        assertEquals(team(a1), team(a2));
        assertEquals(3, game.getGamePlayers(Team.RED).size());
        assertEquals(3, game.getGamePlayers(Team.BLUE).size());
    }

    @Test
    void partyJoinsTheTeamAMemberChose() {
        PlayerMock a1 = addPlayer("A1");
        PlayerMock a2 = addPlayer("A2");
        PlayerMock single = addPlayer("S1");
        party(a1, a2);
        execute(a1, "ctb join");
        execute(single, "ctb join");
        assertTrue(plugin.getUser(a2).setTeam(Team.BLUE));
        startGame();

        assertEquals(Team.BLUE, team(a1));
        assertEquals(Team.BLUE, team(a2));
        assertEquals(Team.RED, team(single));
    }

    @Test
    void partyIsSplitIfTheChosenTeamIsFull() {
        PlayerMock a1 = addPlayer("A1");
        PlayerMock a2 = addPlayer("A2");
        party(a1, a2);
        execute(a1, "ctb join");
        assertTrue(plugin.getUser(a2).setTeam(Team.BLUE));
        startGame();

        // 2 against 2 instead of 1 against 3
        assertEquals(Team.RED, team(a1));
    }

    @Test
    void bigPartyIsSplitToKeepTheTeamsEven() {
        PlayerMock[] members = new PlayerMock[4];
        for (int i = 0; i < members.length; i++) {
            members[i] = addPlayer("P" + i);
        }
        party(members);
        execute(members[0], "ctb join");
        startGame();

        assertEquals(3, game.getGamePlayers(Team.RED).size());
        assertEquals(3, game.getGamePlayers(Team.BLUE).size());
    }

    @Test
    void sameTeamCanBeDisabled() {
        plugin.getFileManager().getConfigFile().set("parties.same-team", false);
        PlayerMock a1 = addPlayer("A1");
        PlayerMock a2 = addPlayer("A2");
        party(a1, a2);
        execute(a1, "ctb join");
        assertTrue(plugin.getUser(a2).setTeam(Team.BLUE));
        assertTrue(plugin.getUser(red).setTeam(Team.BLUE));
        startGame();

        // a1 fills up the smaller team instead of following a2
        assertEquals(Team.RED, team(a1));
    }

    @Test
    void brokenPartyPluginDoesNotBreakJoining() {
        plugin.getPartySupport().getProviders().addFirst(new PartyProvider() {
            @Override
            public String getName() {
                return "Broken";
            }

            @Override
            public Party getParty(@NotNull Player player) {
                throw new NoSuchMethodError("incompatible version");
            }
        });
        PlayerMock leader = addPlayer("Leader");
        PlayerMock member = addPlayer("Member");
        party(leader, member);

        execute(leader, "ctb join");
        assertEquals(game, plugin.getUser(leader).getGame());
        // the other party plugin still works
        assertEquals(game, plugin.getUser(member).getGame());
    }

}
