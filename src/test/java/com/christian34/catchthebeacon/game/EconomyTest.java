package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.integrations.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * money for kills, beacons, wins and rounds (rewards.*) - with a fake economy instead of Vault
 *
 * @author Christian34
 */
class EconomyTest extends GameTestBase {
    private final Map<UUID, Double> balances = new HashMap<>();

    private class FakeEconomy implements EconomyProvider {
        @Override
        public String getName() {
            return "FakeEconomy";
        }

        @Override
        public void deposit(@NotNull Player player, double amount) {
            balances.merge(player.getUniqueId(), amount, Double::sum);
        }

        @Override
        public String format(double amount) {
            return "$" + (int) amount;
        }
    }

    @BeforeEach
    void enableRewards() {
        plugin.getFileManager().getConfigFile().set("rewards.enabled", true);
    }

    private double balance(PlayerMock player) {
        return balances.getOrDefault(player.getUniqueId(), 0d);
    }

    private void kill() {
        blue.setKiller(red);
        blue.damage(100, red);
    }

    private void destroyBlueBeacons() {
        for (Location location : blueBeacons()) {
            Block beacon = location.getBlock();
            beacon.setType(Material.BEACON);
            breakBlock(red, beacon);
        }
        assertEquals(GameState.ENDING, game.getGameState());
    }

    @Test
    void rewardsForKillsBeaconsWinsAndRounds() {
        plugin.getEconomySupport().setProvider(new FakeEconomy());
        startGame();
        kill();
        assertEquals(5, balance(red));
        assertContains(messages(red), "+$5 (kill)");
        destroyBlueBeacons();
        // 5 (kill) + 2 * 20 (beacons) + 50 (win) + 10 (round)
        assertEquals(105, balance(red));
        var messages = messages(red);
        assertContains(messages, "+$20 (beacon destroyed)");
        assertContains(messages, "+$50 (victory)");
        assertContains(messages, "+$10 (round played)");
        // the losers only get the money for the round
        assertEquals(10, balance(blue));
    }

    @Test
    void nothingIfDisabledOrZero() {
        plugin.getEconomySupport().setProvider(new FakeEconomy());
        plugin.getFileManager().getConfigFile().set("rewards.kill", 0);
        startGame();
        kill();
        assertEquals(0, balance(red));
        plugin.getFileManager().getConfigFile().set("rewards.enabled", false);
        destroyBlueBeacons();
        assertTrue(balances.isEmpty());
    }

    @Test
    void brokenEconomyDoesNotBreakTheGame() {
        plugin.getEconomySupport().setProvider(new FakeEconomy() {
            @Override
            public void deposit(@NotNull Player player, double amount) {
                throw new IllegalStateException("account locked");
            }
        });
        startGame();
        kill();
        destroyBlueBeacons();
        assertEquals(Team.RED, game.getWinner());
    }

    @Test
    void withoutEconomyNothingHappens() {
        startGame();
        kill();
        destroyBlueBeacons();
        assertNull(plugin.getEconomySupport().getProvider());
    }

    @Test
    @SuppressWarnings("deprecation") // the old Vault API is still used by many economy plugins (EssentialsX)
    void paysWithTheEconomyRegisteredAtVault() {
        Map<String, Double> deposits = new HashMap<>();
        Economy economy = (Economy) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Economy.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "VaultFake";
                    case "isEnabled" -> true;
                    case "format" -> args[0] + " coins";
                    case "depositPlayer" -> {
                        deposits.merge(((OfflinePlayer) args[0]).getName(), (double) args[args.length - 1], Double::sum);
                        yield new EconomyResponse((double) args[args.length - 1], 0, EconomyResponse.ResponseType.SUCCESS, null);
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "VaultFake";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        server.getServicesManager().register(Economy.class, economy, plugin, ServicePriority.Normal);
        assertEquals("VaultFake", plugin.getEconomySupport().getProvider().getName());
        startGame();
        kill();
        assertEquals(5, deposits.get("Red1"));
        assertContains(messages(red), "+5.0 coins (kill)");
    }

    @Test
    void prefersTheNewApiOfVaultUnlocked() {
        Map<UUID, BigDecimal> deposits = new HashMap<>();
        net.milkbowl.vault2.economy.Economy economy = (net.milkbowl.vault2.economy.Economy) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{net.milkbowl.vault2.economy.Economy.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "UnlockedFake";
                    case "isEnabled" -> true;
                    case "hasAccount" -> deposits.containsKey((UUID) args[0]);
                    case "createAccount" -> deposits.putIfAbsent((UUID) args[0], BigDecimal.ZERO) == null;
                    case "format" -> args[1] + " gems";
                    case "deposit" -> {
                        assertEquals("CatchTheBeacon", args[0]);
                        BigDecimal amount = (BigDecimal) args[args.length - 1];
                        deposits.merge((UUID) args[1], amount, BigDecimal::add);
                        yield new net.milkbowl.vault2.economy.EconomyResponse(amount, amount,
                                net.milkbowl.vault2.economy.EconomyResponse.ResponseType.SUCCESS, null);
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "UnlockedFake";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        server.getServicesManager().register(net.milkbowl.vault2.economy.Economy.class, economy, plugin, ServicePriority.Normal);
        assertEquals("UnlockedFake", plugin.getEconomySupport().getProvider().getName());
        startGame();
        kill();
        assertEquals(0, BigDecimal.valueOf(5).compareTo(deposits.get(red.getUniqueId())));
        assertContains(messages(red), "+5.0 gems (kill)");
    }

}
