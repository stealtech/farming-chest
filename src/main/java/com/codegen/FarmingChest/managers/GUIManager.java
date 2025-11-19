package com.codegen.FarmingChest.managers;

import com.codegen.FarmingChest.FarmingChest;
import com.codegen.FarmingChest.data.FarmingChestData;
import com.codegen.FarmingChest.utils.ChatUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

public class GUIManager {
    private final FarmingChest plugin;
    private final ChestManager chestManager;
    private final Map<UUID, Location> openChests = new HashMap<UUID, Location>();

    public GUIManager(FarmingChest plugin, ChestManager chestManager) {
        this.plugin = plugin;
        this.chestManager = chestManager;
    }

    public void openMainGUI(Player player, Location location) {
        this.openChests.put(player.getUniqueId(), location);
        this.chestManager.recalculate(location);
        Material selectedCrop = this.chestManager.getSelectedCrop(location);
        int cropCount = this.chestManager.getCropCount(location);
        double sellPrice = this.chestManager.calculateSellPrice(location, player);
        int level = 0;
        FarmingChestData chestData = this.chestManager.getChestData(location);
        if (chestData != null) {
            level = chestData.getSellBonusLevel();
        }
        double basePercent = this.plugin.getConfig().getDouble("sell-bonus.base-percent", 0.05d);
        double bonusPercent = basePercent * Math.max(0, level);
        Map<Material, Integer> allCrops = this.chestManager.getAllCropCounts(location);
        Inventory gui = Bukkit.createInventory(player, 27,
                ChatUtil.format(this.plugin.getConfig().getString("gui-titles.main-menu", "&2Farming Chest")));
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }
        Material displayCrop = getItemMaterial(selectedCrop);
        ItemStack cropIcon = new ItemStack(displayCrop);
        ItemMeta cropMeta = cropIcon.getItemMeta();
        cropMeta.setDisplayName(ChatUtil.format("&aSelected Crop: &e" + selectedCrop.name()));
        List<String> cropLore = new ArrayList<>();
        cropLore.add(ChatUtil.format("&8This chest will only sell &e" + selectedCrop.name()));
        cropLore.add("");
        cropLore.add(ChatUtil.format("&8Click to change the crop type."));
        cropMeta.setLore(cropLore);
        cropIcon.setItemMeta(cropMeta);
        gui.setItem(11, cropIcon);
        ItemStack infoIcon = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoIcon.getItemMeta();
        infoMeta.setDisplayName(ChatUtil.format("&6Chest Information"));
        List<String> infoLore = new ArrayList<>();
        if (chestData != null) {
            String chestIdShort = chestData.getChestId().toString().substring(0, 8);
            infoLore.add(ChatUtil.format("&8Chest ID: &e" + chestIdShort + "..."));
            infoLore.add(ChatUtil
                    .format("&8Owner: &e" + this.plugin.getServer().getOfflinePlayer(chestData.getOwner()).getName()));
            infoLore.add("");
        }
        infoLore.add(ChatUtil.format("&8Selected Crop: &e" + selectedCrop.name()));
        infoLore.add(ChatUtil.format("&8Mature Count: &e" + cropCount));
        infoLore.add(ChatUtil.format("&8Sell Price: &e$" + String.format("%.2f", Double.valueOf(sellPrice))));
        infoLore.add(ChatUtil.format("&8Sell Bonus: &e+" + String.format("%.0f%%", bonusPercent * 100.0d)));
        infoLore.add("");
        infoLore.add(ChatUtil.format("&6All Mature Crops in Chunk:"));
        if (allCrops.isEmpty()) {
            infoLore.add(ChatUtil.format("&8- &eNone"));
        } else {
            for (Map.Entry<Material, Integer> entry : allCrops.entrySet()) {
                infoLore.add(ChatUtil.format("&8- &e" + entry.getKey().name() + "&8: &f" + entry.getValue()));
            }
        }
        infoMeta.setLore(infoLore);
        infoIcon.setItemMeta(infoMeta);
        gui.setItem(13, infoIcon);
        ItemStack sellIcon = new ItemStack(Material.EMERALD);
        ItemMeta sellMeta = sellIcon.getItemMeta();
        sellMeta.setDisplayName(ChatUtil.format("&aSell All " + selectedCrop.name()));
        List<String> sellLore = new ArrayList<>();
        if (cropCount == 0 && !allCrops.isEmpty()) {
            sellLore.add(ChatUtil.format("&cNo mature " + selectedCrop.name() + " to sell!"));
            sellLore.add(ChatUtil.format("&8You have other crops in this chunk:"));
            for (Map.Entry<Material, Integer> entry : allCrops.entrySet()) {
                sellLore.add(ChatUtil.format("  &e" + entry.getKey().name() + "&8: &f" + entry.getValue()));
            }
            sellLore.add("");
            sellLore.add(ChatUtil.format("&8Change your selected crop to sell them!"));
        } else {
            sellLore.add(ChatUtil.format("&8Selling: &e" + cropCount + " mature " + selectedCrop.name()));
            sellLore.add(ChatUtil.format("&8Price: &e$" + String.format("%.2f", Double.valueOf(sellPrice))));
        }
        sellMeta.setLore(sellLore);
        sellIcon.setItemMeta(sellMeta);
        gui.setItem(15, sellIcon);

        ItemStack upgradeIcon = new ItemStack(Material.NETHER_STAR);
        ItemMeta upgradeMeta = upgradeIcon.getItemMeta();
        upgradeMeta.setDisplayName(ChatUtil.format("&bUpgrade Sell Bonus"));
        List<String> upgradeLore = new ArrayList<>();
        int maxLevel = this.plugin.getConfig().getInt("sell-bonus.max-level", 10);
        int nextLevel = level + 1;
        if (nextLevel > maxLevel) {
            upgradeLore.add(ChatUtil.format("&8Current Level: &e" + level + " &8/ &a" + maxLevel));
            upgradeLore.add(ChatUtil.format("&aMax level reached."));
        } else {
            double baseCost = this.plugin.getConfig().getDouble("sell-bonus.base-cost", 1000.0d);
            double multiplier = this.plugin.getConfig()
                    .getDouble("sell-bonus.level-cost-multipliers." + nextLevel,
                            this.plugin.getConfig().getDouble("sell-bonus.level-cost-multipliers.default", 1.0d));
            double cost = baseCost * Math.max(0.0d, multiplier);
            double nextBonusPct = basePercent * nextLevel;
            upgradeLore.add(ChatUtil.format("&8Current Level: &e" + level + " &8/ &a" + maxLevel));
            upgradeLore.add(ChatUtil.format("&8Next Bonus: &e+" + String.format("%.0f%%", nextBonusPct * 100.0d)));
            upgradeLore.add("");
            upgradeLore.add(ChatUtil.format("&8Cost: &e$" + String.format("%.2f", cost)));
            upgradeLore.add(ChatUtil.format("&aClick to upgrade"));
        }
        upgradeMeta.setLore(upgradeLore);
        upgradeIcon.setItemMeta(upgradeMeta);
        gui.setItem(22, upgradeIcon);
        player.openInventory(gui);
    }

    public void openCropSelectionGUI(Player player, Location location) {
        Inventory gui = Bukkit.createInventory(player, 54,
                ChatUtil.format(
                        this.plugin.getConfig().getString("gui-titles.selection-menu", "&2Farmable Selection")));
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        int slot = 10;
        Material selected = location != null ? this.chestManager.getSelectedCrop(location) : null;
        if (this.plugin.getConfig().getConfigurationSection("farmable-crops") != null) {
            for (String key : this.plugin.getConfig().getConfigurationSection("farmable-crops").getKeys(false)) {
                Material mat;
                try {
                    mat = Material.valueOf(key);
                } catch (Exception e) {
                    continue;
                }
                Material displayMat = mat;
                switch (mat) {
                    case CARROTS:
                        displayMat = Material.CARROT;
                        break;
                    case POTATOES:
                        displayMat = Material.POTATO;
                        break;
                    case BEETROOTS:
                        displayMat = Material.BEETROOT;
                        break;
                    case MELON:
                        displayMat = Material.MELON_SLICE;
                        break;
                    default:
                        break;
                }
                ItemStack icon;
                try {
                    icon = new ItemStack(displayMat);
                } catch (IllegalArgumentException ex) {
                    icon = new ItemStack(Material.PAPER);
                }
                ItemMeta meta = icon.getItemMeta();
                String name = this.plugin.getConfig().getString("farmable-crops." + key + ".display-name",
                        "&e" + key);
                meta.setDisplayName(ChatUtil.format(name));
                List<String> loreCfg = this.plugin.getConfig().getStringList("farmable-crops." + key + ".lore");
                List<String> lore = new ArrayList<>();
                for (String line : loreCfg)
                    lore.add(ChatUtil.format(line));
                if (selected != null && selected == mat) {
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    lore.add(0, ChatUtil.format("&aCurrently Selected"));
                }
                meta.setLore(lore);
                icon.setItemMeta(meta);
                if (slot >= 54)
                    break;
                gui.setItem(slot, icon);
                slot++;
                if ((slot + 1) % 9 == 0)
                    slot += 2;
                if (slot % 9 == 0)
                    slot++;
            }
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatUtil.format("&cBack"));
        back.setItemMeta(backMeta);
        gui.setItem(49, back);

        player.openInventory(gui);
        if (location != null) {
            this.openChests.put(player.getUniqueId(), location);
        }
    }

    public Location getOpenChestLocation(UUID playerUUID) {
        return this.openChests.get(playerUUID);
    }

    public void removeOpenChest(UUID playerUUID) {
        this.openChests.remove(playerUUID);
    }

    private Material getItemMaterial(Material blockMaterial) {
        switch (blockMaterial) {
            case CARROTS:
                return Material.CARROT;
            case POTATOES:
                return Material.POTATO;
            case BEETROOTS:
                return Material.BEETROOT;
            case MELON:
                return Material.MELON_SLICE;
            default:
                return blockMaterial;
        }
    }
}
