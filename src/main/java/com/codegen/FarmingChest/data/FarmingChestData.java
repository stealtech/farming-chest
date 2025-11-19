package com.codegen.FarmingChest.data;

import java.util.UUID;
import org.bukkit.Material;

public class FarmingChestData {
    private final UUID chestId;
    private final UUID owner;
    private Material selectedCrop;
    private int sellBonusLevel;

    public FarmingChestData(UUID owner, Material selectedCrop) {
        this.chestId = UUID.randomUUID();
        this.owner = owner;
        this.selectedCrop = selectedCrop;
        this.sellBonusLevel = 0;
    }

    public FarmingChestData(UUID chestId, UUID owner, Material selectedCrop) {
        this.chestId = chestId;
        this.owner = owner;
        this.selectedCrop = selectedCrop;
        this.sellBonusLevel = 0;
    }

    public UUID getChestId() {
        return this.chestId;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public Material getSelectedCrop() {
        return this.selectedCrop;
    }

    public void setSelectedCrop(Material selectedCrop) {
        this.selectedCrop = selectedCrop;
    }

    public int getSellBonusLevel() {
        return this.sellBonusLevel;
    }

    public void setSellBonusLevel(int sellBonusLevel) {
        this.sellBonusLevel = Math.max(0, sellBonusLevel);
    }
}
