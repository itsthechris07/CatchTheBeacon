package com.christian34.catchthebeacon.integrations;

import com.christian34.catchthebeacon.CatchTheBeacon;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

/**
 * the economy registered with the new api of VaultUnlocked (https://github.com/TheNewEconomy/VaultUnlocked) - only
 * loaded if its classes exist
 */
class VaultUnlockedEconomy implements EconomyProvider {
    static final String CLASS = "net.milkbowl.vault2.economy.Economy";
    private final Economy economy;

    private VaultUnlockedEconomy(Economy economy) {
        this.economy = economy;
    }

    /**
     * @return the economy, null if no economy plugin has registered one with the new api
     */
    @Nullable
    static VaultUnlockedEconomy find() {
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        return registration == null ? null : new VaultUnlockedEconomy(registration.getProvider());
    }

    private static String pluginName() {
        return CatchTheBeacon.getInstance().getName();
    }

    @Override
    public String getName() {
        return economy.getName();
    }

    @Override
    public void deposit(@NotNull Player player, double amount) {
        if (!economy.hasAccount(player.getUniqueId())) economy.createAccount(player.getUniqueId(), player.getName(), true);
        EconomyResponse response = economy.deposit(pluginName(), player.getUniqueId(), BigDecimal.valueOf(amount));
        if (response == null || !response.transactionSuccess()) {
            throw new IllegalStateException(response == null ? "no response" : response.errorMessage);
        }
    }

    @Override
    public String format(double amount) {
        return economy.format(pluginName(), BigDecimal.valueOf(amount));
    }

}
