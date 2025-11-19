package com.codegen.FarmingChest.managers;

import com.codegen.FarmingChest.FarmingChest;
import com.codegen.FarmingChest.data.FarmingChestData;
import com.codegen.FarmingChest.utils.ChatUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ChestManager {
    private final FarmingChest plugin;
    private final Map<Location, FarmingChestData> farmChests = new HashMap<>();
    private final Map<Location, Map<Material, Integer>> allCropCounts = new HashMap<>();
    private final Map<Location, Integer> cropCounts = new HashMap<>();

    public ChestManager(FarmingChest plugin) {
        this.plugin = plugin;
        loadChests();
    }

    public void addChest(Location location, Player owner) {
        Material defaultCrop;
        try {
            String defaultCropName = this.plugin.getConfig().getString("default-crop", "WHEAT");
            defaultCrop = Material.valueOf(defaultCropName.toUpperCase());
        } catch (Exception e) {
            defaultCrop = Material.WHEAT;
        }
        this.farmChests.put(location, new FarmingChestData(owner.getUniqueId(), defaultCrop));
        saveChests();
    }

    public void removeChest(Location location) {
        this.farmChests.remove(location);
        this.cropCounts.remove(location);
        this.allCropCounts.remove(location);
        saveChests();
    }

    public Map<Location, FarmingChestData> getChests() {
        return Collections.unmodifiableMap(this.farmChests);
    }

    public boolean isFarmChest(Location location) {
        return this.farmChests.containsKey(location);
    }

    public FarmingChestData getChestData(Location location) {
        return this.farmChests.get(location);
    }

    public Material getSelectedCrop(Location location) {
        FarmingChestData data = getChestData(location);
        return data != null ? data.getSelectedCrop() : Material.WHEAT;
    }

    public void setSelectedCrop(Location location, Material material) {
        FarmingChestData data = getChestData(location);
        if (data != null) {
            data.setSelectedCrop(material);
            saveChests();
            recalculate(location);
        }
    }

    public int getCropCount(Location location) {
        return this.cropCounts.getOrDefault(location, 0).intValue();
    }

    public Map<Material, Integer> getAllCropCounts(Location location) {
        return this.allCropCounts.getOrDefault(location, new HashMap<Material, Integer>());
    }

    public void recalculate(Location location) {
        if (!isFarmChest(location)) {
            return;
        }
        Block chestBlock = location.getBlock();
        Material cropType = getSelectedCrop(location);
        int count = countCropsInChunk(chestBlock, cropType);
        this.cropCounts.put(location, Integer.valueOf(count));
        Map<Material, Integer> allCrops = countAllCropsInChunk(chestBlock);
        this.allCropCounts.put(location, allCrops);
    }

    public double calculateSellPrice(Location location, Player player) {
        Material cropType = getSelectedCrop(location);
        int amount = getCropCount(location);
        double pricePerItem = this.plugin.getConfig().getDouble("sell-prices." + cropType.name(), 0.0d);
        double total = pricePerItem * amount;
        FarmingChestData data = getChestData(location);
        double basePercent = this.plugin.getConfig().getDouble("sell-bonus.base-percent", 0.05d);
        int level = data != null ? data.getSellBonusLevel() : 0;
        double bonusPct = basePercent * Math.max(0, level);
        return total + (total * bonusPct);
    }

    public void sellCrops(Player player, Location location) {
        if (!isFarmChest(location) || !this.plugin.getEconomyManager().isEconomyEnabled()) {
            return;
        }
        recalculate(location);
        Material cropType = getSelectedCrop(location);
        int amount = getCropCount(location);
        if (amount == 0) {
            player.sendMessage(ChatUtil.format("&cThere are no mature crops to sell."));
            return;
        }
        double sellPrice = calculateSellPrice(location, player);
        this.plugin.getEconomyManager().depositPlayer(player, sellPrice);
        player.sendMessage(ChatUtil.format("&aYou sold " + amount + " " + cropType.name() + " for &e$"
                + String.format("%.2f", Double.valueOf(sellPrice))));
        resetOrRemoveSpecificCropInChunk(location.getBlock(), cropType);
        recalculate(location);
    }

    public void resetOrRemoveSpecificCropInChunk(Block chestBlock, Material cropType) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < chestBlock.getWorld().getMaxHeight(); y++) {
                    Block block = chestBlock.getChunk().getBlock(x, y, z);
                    if (block.getType() == cropType) {
                        if (block.getBlockData() instanceof Ageable) {
                            Ageable ageable = (Ageable) block.getBlockData();
                            if (ageable.getAge() == ageable.getMaximumAge()) {
                                ageable.setAge(0);
                                block.setBlockData(ageable);
                            }
                        } else {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    private int countCropsInChunk(Block chestBlock, Material cropType) {
        int cropCount = 0;
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

    private Map<Material, Integer> countAllCropsInChunk(Block chestBlock) {
        Map<Material, Integer> cropCounts = new HashMap<>();
        ConfigurationSection farmableCrops = this.plugin.getConfig().getConfigurationSection("farmable-crops");
        if (farmableCrops == null) {
            return cropCounts;
        }
        Set<String> cropNames = farmableCrops.getKeys(false);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < chestBlock.getWorld().getMaxHeight(); y++) {
                    Block block = chestBlock.getChunk().getBlock(x, y, z);
                    Material blockType = block.getType();
                    if (cropNames.contains(blockType.name())) {
                        if (block.getBlockData() instanceof Ageable) {
                            Ageable ageable = (Ageable) block.getBlockData();
                            if (ageable.getAge() == ageable.getMaximumAge()) {
                                cropCounts.put(blockType,
                                        Integer.valueOf(cropCounts.getOrDefault(blockType, 0).intValue() + 1));
                            }
                        } else {
                            cropCounts.put(blockType,
                                    Integer.valueOf(cropCounts.getOrDefault(blockType, 0).intValue() + 1));
                        }
                    }
                }
            }
        }
        return cropCounts;
    }

    public void saveChests() {
        this.plugin.getConfig().set("chests", (Object) null);
        ConfigurationSection chestSection = this.plugin.getConfig().createSection("chests");
        for (Map.Entry<Location, FarmingChestData> entry : this.farmChests.entrySet()) {
            Location loc = entry.getKey();
            FarmingChestData data = entry.getValue();
            String locationString = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + ","
                    + loc.getBlockZ();
            chestSection.set(locationString + ".chest-id", data.getChestId().toString());
            chestSection.set(locationString + ".owner", data.getOwner().toString());
            chestSection.set(locationString + ".crop", data.getSelectedCrop().name());
            chestSection.set(locationString + ".sell-bonus-level", data.getSellBonusLevel());
        }
        this.plugin.saveConfig();
    }

    public void loadChests() {
        this.farmChests.clear();
        ConfigurationSection chestSection = this.plugin.getConfig().getConfigurationSection("chests");
        if (chestSection == null) {
            return;
        }
        for (String locationString : chestSection.getKeys(false)) {
            String[] parts = locationString.split(",");
            if (parts.length == 4) {
                try {
                    Location location = new Location(this.plugin.getServer().getWorld(parts[0]),
                            Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                    String chestIdStr = chestSection.getString(locationString + ".chest-id");
                    UUID chestId = chestIdStr != null ? UUID.fromString(chestIdStr) : UUID.randomUUID();
                    UUID owner = UUID.fromString(chestSection.getString(locationString + ".owner"));
                    Material material = Material.valueOf(chestSection.getString(locationString + ".crop"));
                    FarmingChestData data = new FarmingChestData(chestId, owner, material);
                    int level = chestSection.getInt(locationString + ".sell-bonus-level", 0);
                    data.setSellBonusLevel(level);
                    this.farmChests.put(location, data);
                    recalculate(location);
                } catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to load chest at: " + locationString);
                }
            }
        }
    }

    public void giveFarmingChest(Player player) {
        ItemStack farmingChestItem = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = farmingChestItem.getItemMeta();
        meta.setDisplayName(ChatUtil.format("&aFarmable Chest"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtil.format("&8Place this chest to start farming."));
        lore.add(ChatUtil.format("&8Each chest has a unique ID."));
        meta.setLore(lore);
        farmingChestItem.setItemMeta(meta);
        player.getInventory().addItem(new ItemStack[] { farmingChestItem });
    }

    public boolean upgradeSellBonus(Player player, Location location) {
        if (!isFarmChest(location))
            return false;
        FarmingChestData data = getChestData(location);
        int currentLevel = data.getSellBonusLevel();
        int maxLevel = this.plugin.getConfig().getInt("sell-bonus.max-level", 10);
        if (currentLevel >= maxLevel) {
            player.sendMessage(ChatUtil.format("&cSell bonus is already at max level."));
            return false;
        }
        int nextLevel = currentLevel + 1;
        double baseCost = this.plugin.getConfig().getDouble("sell-bonus.base-cost", 1000.0d);
        double multiplier = 1.0d;
        String path = "sell-bonus.level-cost-multipliers." + nextLevel;
        if (this.plugin.getConfig().isSet(path)) {
            multiplier = this.plugin.getConfig().getDouble(path, 1.0d);
        } else {
            multiplier = this.plugin.getConfig().getDouble("sell-bonus.level-cost-multipliers.default", 1.0d);
        }
        double cost = baseCost * Math.max(0.0d, multiplier);
        if (!this.plugin.getEconomyManager().isEconomyEnabled()) {
            player.sendMessage(ChatUtil.format("&cEconomy is not enabled."));
            return false;
        }
        double balance = this.plugin.getEconomyManager().getBalance(player);
        if (balance < cost) {
            player.sendMessage(ChatUtil.format("&cYou need &e$" + String.format("%.2f", cost) + "&c to upgrade."));
            return false;
        }
        if (!this.plugin.getEconomyManager().withdrawPlayer(player, cost)) {
            player.sendMessage(ChatUtil.format("&cPayment failed. Try again."));
            return false;
        }
        data.setSellBonusLevel(nextLevel);
        saveChests();
        player.sendMessage(ChatUtil.format("&aUpgraded sell bonus to level &e" + nextLevel + "&a!"));
        return true;
    }
}
