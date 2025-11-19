package com.codegen.FarmingChest.listeners;

import com.codegen.FarmingChest.FarmingChest;
import com.codegen.FarmingChest.managers.ChestManager;
import com.codegen.FarmingChest.managers.GUIManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ChestListener implements Listener {
    private final FarmingChest plugin;
    private final ChestManager chestManager;
    private final GUIManager guiManager;

    public ChestListener(FarmingChest plugin) {
        this.plugin = plugin;
        this.chestManager = plugin.getChestManager();
        this.guiManager = plugin.getGuiManager();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.ENDER_CHEST && item.hasItemMeta() && item.getItemMeta().getDisplayName()
                .equals(ChatColor.translateAlternateColorCodes('&', "&aFarmable Chest"))) {
            this.chestManager.addChest(event.getBlock().getLocation(), event.getPlayer());
            event.getPlayer().sendMessage(ChatColor.GREEN + "Farming Chest placed! Right-click to configure it.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.ENDER_CHEST
                && this.chestManager.isFarmChest(event.getBlock().getLocation())) {
            this.chestManager.removeChest(event.getBlock().getLocation());
            event.getPlayer().sendMessage(ChatColor.RED + "Farming Chest removed.");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && event.getClickedBlock().getType() == Material.ENDER_CHEST) {
            Location loc = event.getClickedBlock().getLocation();
            if (this.chestManager.isFarmChest(loc)) {
                event.setCancelled(true);
                this.guiManager.openMainGUI(event.getPlayer(), loc);
            }
        }
    }
}
