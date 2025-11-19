package com.codegen.FarmingChest.managers;

import com.codegen.FarmingChest.FarmingChest;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private final FarmingChest plugin;
    private Economy economy;

    public EconomyManager(FarmingChest plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (this.plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            this.plugin.getLogger().warning("Vault not found! Economy features will be disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = this.plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) {
            this.plugin.getLogger().warning("No economy plugin found! Economy features will be disabled.");
        } else {
            this.economy = (Economy) rsp.getProvider();
        }
    }

    public boolean isEconomyEnabled() {
        return this.economy != null;
    }

    public void depositPlayer(OfflinePlayer player, double amount) {
        if (isEconomyEnabled()) {
            this.economy.depositPlayer(player, amount);
        }
    }

    public void depositPlayer(Player player, double amount) {
        if (isEconomyEnabled()) {
            this.economy.depositPlayer(player, amount);
        }
    }

    public double getBalance(Player player) {
        if (!isEconomyEnabled())
            return 0.0d;
        return this.economy.getBalance(player);
    }

    public boolean withdrawPlayer(Player player, double amount) {
        if (!isEconomyEnabled())
            return false;
        return this.economy.withdrawPlayer(player, amount).transactionSuccess();
    }
}
