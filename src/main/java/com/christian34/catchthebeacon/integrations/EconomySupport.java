package com.christian34.catchthebeacon.integrations;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Money of an economy plugin (via VaultUnlocked or Vault) for kills, destroyed beacons, wins and played rounds
 * (rewards.* in config.yml).
 *
 * @author Christian34
 */
public class EconomySupport {
    private final CatchTheBeacon plugin;
    private final boolean vaultInstalled, vaultUnlockedInstalled;
    // set by tests instead of an economy plugin
    private @Nullable EconomyProvider provider;
    private boolean warned;

    public enum Reward {
        KILL("kill", LangText.REWARD_KILL),
        BEACON("beacon", LangText.REWARD_BEACON),
        WIN("win", LangText.REWARD_WIN),
        GAME("game", LangText.REWARD_GAME);

        private final String key;
        private final LangText reason;

        Reward(String key, LangText reason) {
            this.key = key;
            this.reason = reason;
        }
    }

    public EconomySupport(CatchTheBeacon plugin) {
        this.plugin = plugin;
        this.vaultInstalled = classExists(VaultEconomy.CLASS);
        this.vaultUnlockedInstalled = classExists(VaultUnlockedEconomy.CLASS);
        // economy plugins are often enabled after this plugin - their economy is looked up on every payment
        if (vaultInstalled && isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                EconomyProvider economy = getProvider();
                if (economy == null) {
                    Debug.warn("rewards.enabled is true, but no economy plugin is registered at Vault");
                } else {
                    Debug.info("Hooked into Vault (rewards are paid with " + economy.getName() + ")");
                }
            });
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }

    public void setProvider(@Nullable EconomyProvider provider) {
        this.provider = provider;
    }

    private boolean isEnabled() {
        return plugin.getFileManager().getConfigFile().getBoolean("rewards.enabled");
    }

    /**
     * @return the economy rewards are paid with, null if there is none
     */
    @Nullable
    public EconomyProvider getProvider() {
        if (provider != null) return provider;
        if (!vaultInstalled) return null;
        try {
            // VaultUnlocked: economy plugins may register only the new api, only the legacy one or both
            EconomyProvider economy = vaultUnlockedInstalled ? VaultUnlockedEconomy.find() : null;
            return economy != null ? economy : VaultEconomy.find();
        } catch (Exception | LinkageError ex) {
            warnOnce("Couldn't get the economy of Vault: " + ex);
            return null;
        }
    }

    /**
     * pays the reward (rewards.&lt;reward&gt;) to the player and tells him, nothing if rewards are disabled or there is
     * no economy plugin
     */
    public void reward(@NotNull Player player, @NotNull Reward reward) {
        if (!isEnabled()) return;
        double amount = plugin.getFileManager().getConfigFile().getDouble("rewards." + reward.key);
        if (amount <= 0) return;
        EconomyProvider economy = getProvider();
        if (economy == null) return;
        try {
            economy.deposit(player, amount);
            plugin.getUser(player).sendMessage(LangText.REWARD, economy.format(amount), I.i18n(reward.reason));
        } catch (Exception | LinkageError ex) {
            // a broken economy plugin mustn't break the game
            warnOnce("Couldn't pay " + amount + " to " + player.getName() + " with " + economy.getName() + ": " + ex);
        }
    }

    private void warnOnce(String message) {
        if (warned) return;
        warned = true;
        Debug.warn(message);
    }

}
