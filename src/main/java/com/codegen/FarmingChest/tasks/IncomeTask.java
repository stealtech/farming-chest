package com.codegen.FarmingChest.tasks;

import com.codegen.FarmingChest.FarmingChest;
import com.codegen.FarmingChest.managers.ChestManager;
import com.codegen.FarmingChest.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.scheduler.BukkitRunnable;

public class IncomeTask extends BukkitRunnable {
    private final FarmingChest plugin;
    private final ChestManager chestManager;
    private final EconomyManager economyManager;

    public IncomeTask(FarmingChest plugin) {
        this.plugin = plugin;
        this.chestManager = plugin.getChestManager();
        this.economyManager = plugin.getEconomyManager();
    }

    public void run() {
        if (!this.economyManager.isEconomyEnabled()) {
            return;
        }
        this.chestManager.getChests().forEach((location, chestData) -> {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(chestData.getOwner());
            int cropCount = countCrops(location, chestData.getSelectedCrop());
            double pricePerItem = this.plugin.getConfig().getDouble("sell-prices." + chestData.getSelectedCrop().name(),
                    0.0d);
            double basePercent = this.plugin.getConfig().getDouble("sell-bonus.base-percent", 0.05d);
            int level = chestData.getSellBonusLevel();
            double bonusPct = basePercent * Math.max(0, level);
            double income = cropCount * pricePerItem * bonusPct;
            if (income > 0.0d) {
                this.economyManager.depositPlayer(owner, income);
            }
        });
    }

    private int countCrops(Location location, Material cropType) {
        int cropCount = 0;
        Block chestBlock = location.getBlock();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < chestBlock.getWorld().getMaxHeight(); y++) {
                    Block block = chestBlock.getChunk().getBlock(x, y, z);
                    if (block.getType() == cropType) {
                        if (block.getBlockData() instanceof Ageable) {
                            Ageable ageable = (Ageable) block.getBlockData();
                            if (ageable.getAge() == ageable.getMaximumAge()) {
                                cropCount++;
                            }
                        } else {
                            cropCount++;
                        }
                    }
                }
            }
        }
        return cropCount;
    }
}
