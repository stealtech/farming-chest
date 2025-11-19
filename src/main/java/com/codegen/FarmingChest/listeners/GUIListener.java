package com.codegen.FarmingChest.listeners;

import com.codegen.FarmingChest.FarmingChest;
import com.codegen.FarmingChest.managers.ChestManager;
import com.codegen.FarmingChest.managers.GUIManager;
import com.codegen.FarmingChest.utils.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {
    private final FarmingChest plugin;
    private final GUIManager guiManager;
    private final ChestManager chestManager;

    public GUIListener(FarmingChest plugin, GUIManager guiManager, ChestManager chestManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.chestManager = chestManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Location chestLocation;
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack currentItem = event.getCurrentItem();
        if (clickedInventory == null || currentItem == null || currentItem.getType() == Material.AIR) {
            return;
        }
        String inventoryTitle = event.getView().getTitle();
        String mainGUITitle = ChatUtil
                .format(this.plugin.getConfig().getString("gui-titles.main-menu", "&2Farming Chest"));
        String selectionGUITitle = ChatUtil
                .format(this.plugin.getConfig().getString("gui-titles.selection-menu", "&2Farmable Selection"));
        if (inventoryTitle.equals(mainGUITitle)) {
            event.setCancelled(true);
            if (event.getSlot() == 11) {
                Location loc = this.guiManager.getOpenChestLocation(player.getUniqueId());
                this.guiManager.openCropSelectionGUI(player, loc);
                return;
            } else if (event.getSlot() == 15
                    && (chestLocation = this.guiManager.getOpenChestLocation(player.getUniqueId())) != null) {
                this.chestManager.sellCrops(player, chestLocation);
                Location finalChestLocation = chestLocation;
                this.plugin.getServer().getScheduler().runTask(this.plugin,
                        () -> this.guiManager.openMainGUI(player, finalChestLocation));
                return;
            } else if (event.getSlot() == 22
                    && (chestLocation = this.guiManager.getOpenChestLocation(player.getUniqueId())) != null) {
                Location finalChestLocation2 = chestLocation;
                if (this.chestManager.upgradeSellBonus(player, chestLocation)) {
                    this.plugin.getServer().getScheduler().runTask(this.plugin,
                            () -> this.guiManager.openMainGUI(player, finalChestLocation2));
                }
                return;
            } else {
                return;
            }
        }
        if (inventoryTitle.equals(selectionGUITitle)) {
            event.setCancelled(true);
            Material clickedMaterial = currentItem.getType();
            Location chestLocation2 = this.guiManager.getOpenChestLocation(player.getUniqueId());
            if (clickedMaterial == Material.BARRIER && event.getSlot() == 49) {
                if (chestLocation2 != null) {
                    Location finalChestLocation3 = chestLocation2;
                    this.plugin.getServer().getScheduler().runTask(this.plugin,
                            () -> this.guiManager.openMainGUI(player, finalChestLocation3));
                    return;
                } else {
                    player.closeInventory();
                    return;
                }
            }
            if (chestLocation2 != null && clickedMaterial != Material.BLACK_STAINED_GLASS_PANE
                    && currentItem.hasItemMeta() && clickedMaterial != Material.BARRIER) {
                Material newCropMaterial = null;
                switch (clickedMaterial) {
                    case CARROT:
                        newCropMaterial = Material.CARROTS;
                        break;
                    case POTATO:
                        newCropMaterial = Material.POTATOES;
                        break;
                    case BEETROOT:
                        newCropMaterial = Material.BEETROOTS;
                        break;
                    case MELON_SLICE:
                        newCropMaterial = Material.MELON;
                        break;
                    default:
                        if (this.plugin.getConfig().isSet("farmable-crops." + clickedMaterial.name())) {
                            newCropMaterial = clickedMaterial;
                        } else {
                            String asBlock = clickedMaterial.name();
                            if (this.plugin.getConfig().isSet("farmable-crops." + asBlock)) {
                                try {
                                    newCropMaterial = Material.valueOf(asBlock);
                                } catch (Exception ignore) {
                                }
                            }
                        }
                        break;
                }
                if (newCropMaterial != null) {
                    Material oldCrop = this.chestManager.getSelectedCrop(chestLocation2);
                    if (oldCrop != newCropMaterial) {
                        this.chestManager.resetOrRemoveSpecificCropInChunk(chestLocation2.getBlock(), oldCrop);
                    }
                    this.chestManager.setSelectedCrop(chestLocation2, newCropMaterial);
                    player.closeInventory();
                    Location finalChestLocation4 = chestLocation2;
                    this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                            () -> this.guiManager.openMainGUI(player, finalChestLocation4), 1L);
                    player.sendMessage(ChatUtil
                            .format("&aYou have selected &e" + newCropMaterial.name() + "&a as the farmable crop."));
                }
            }
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$org$bukkit$Material = new int[Material.values().length];

        static {
            try {
                $SwitchMap$org$bukkit$Material[Material.CARROT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$bukkit$Material[Material.POTATO.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$org$bukkit$Material[Material.BEETROOT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$org$bukkit$Material[Material.MELON_SLICE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String mainGUITitle = ChatUtil
                .format(this.plugin.getConfig().getString("gui-titles.main-menu", "&2Farming Chest"));
        String selectionGUITitle = ChatUtil
                .format(this.plugin.getConfig().getString("gui-titles.selection-menu", "&2Farmable Selection"));
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            String openTitle = event.getPlayer().getOpenInventory().getTitle();
            if (!openTitle.equals(mainGUITitle) && !openTitle.equals(selectionGUITitle)) {
                this.guiManager.removeOpenChest(event.getPlayer().getUniqueId());
            }
        });
    }
}
