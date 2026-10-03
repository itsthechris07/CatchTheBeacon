package com.christian34.catchthebeacon.integrations;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * an economy plugin (via Vault) that pays the rewards of {@link EconomySupport}
 */
public interface EconomyProvider {

    String getName();

    /**
     * @throws IllegalStateException if the economy plugin refused the payment
     */
    void deposit(@NotNull Player player, double amount);

    /**
     * @return the amount with the currency of the economy plugin, e.g. "$5.00"
     */
    String format(double amount);

}
