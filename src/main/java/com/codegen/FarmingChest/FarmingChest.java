package com.codegen.FarmingChest;

import com.codegen.FarmingChest.commands.FarmingChestCommand;
import com.codegen.FarmingChest.listeners.ChestListener;
import com.codegen.FarmingChest.listeners.GUIListener;
import com.codegen.FarmingChest.managers.ChestManager;
import com.codegen.FarmingChest.managers.EconomyManager;
import com.codegen.FarmingChest.managers.HologramManager;
import com.codegen.FarmingChest.managers.GUIManager;
import com.codegen.FarmingChest.tasks.IncomeTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class FarmingChest extends JavaPlugin {
    private static FarmingChest instance;
    private ChestManager chestManager;
    private GUIManager guiManager;
    private EconomyManager economyManager;
    private HologramManager hologramManager;

    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.economyManager = new EconomyManager(this);
        this.chestManager = new ChestManager(this);
        this.guiManager = new GUIManager(this, this.chestManager);

        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            this.hologramManager = new HologramManager(this, this.chestManager);
            new com.codegen.FarmingChest.tasks.LookHologramTask(this).runTaskTimer(this, 0L, 5L);
            getLogger().info("DecentHolograms integration enabled!");
        } else {
            getLogger().warning("DecentHolograms not found! Holograms will not be displayed.");
        }

        getCommand("farmingchest").setExecutor(new FarmingChestCommand(this));
        getServer().getPluginManager().registerEvents(new ChestListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this, this.guiManager, this.chestManager), this);
        new IncomeTask(this).runTaskTimerAsynchronously(this, 0L, 20L);
        getLogger().info("FarmingChest has been enabled!");
    }

    public void onDisable() {
        if (this.hologramManager != null) {
            this.hologramManager.shutdown();
        }
        getLogger().info("FarmingChest has been disabled!");
    }

    public void reload() {
        reloadConfig();
        this.chestManager.loadChests();
    }

    public static FarmingChest getInstance() {
        return instance;
    }

    public ChestManager getChestManager() {
        return this.chestManager;
    }

    public GUIManager getGuiManager() {
        return this.guiManager;
    }

    public EconomyManager getEconomyManager() {
        return this.economyManager;
    }

    public HologramManager getHologramManager() {
        return this.hologramManager;
    }

    public boolean hasHologramManager() {
        return this.hologramManager != null;
    }
}
