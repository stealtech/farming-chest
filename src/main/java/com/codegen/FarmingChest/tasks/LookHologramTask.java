package com.codegen.FarmingChest.tasks;

import com.codegen.FarmingChest.FarmingChest;
import com.codegen.FarmingChest.managers.ChestManager;
import com.codegen.FarmingChest.managers.HologramManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class LookHologramTask extends BukkitRunnable {
    private final FarmingChest plugin;
    private final ChestManager chestManager;

    public LookHologramTask(FarmingChest plugin) {
        this.plugin = plugin;
        this.chestManager = plugin.getChestManager();
    }

    @Override
    public void run() {
        if (!this.plugin.hasHologramManager()) {
            return;
        }
        HologramManager hologramManager = plugin.getHologramManager();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Block target = p.getTargetBlockExact(6);
            if (target != null && target.getType() == Material.ENDER_CHEST
                    && chestManager.isFarmChest(target.getLocation())) {
                hologramManager.showFor(p, target.getLocation());
            } else {
                hologramManager.hideFor(p);
            }
        }
    }
}
