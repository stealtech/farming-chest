package com.codegen.FarmingChest.managers;

import com.codegen.FarmingChest.FarmingChest;
import com.codegen.FarmingChest.utils.ChatUtil;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import java.util.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class HologramManager {
    private final FarmingChest plugin;
    private final ChestManager chestManager;
    private final Map<UUID, Entry> holograms = new HashMap<>();

    public HologramManager(FarmingChest plugin, ChestManager chestManager) {
        this.plugin = plugin;
        this.chestManager = chestManager;
    }

    public void showFor(Player player, Location chestLoc) {
        if (player == null || chestLoc == null || chestLoc.getWorld() == null)
            return;

        Entry existing = holograms.get(player.getUniqueId());
        if (existing != null && !sameBlock(existing.chestLoc, chestLoc)) {
            removeInternal(existing);
            existing = null;
        }

        if (existing == null) {
            Entry created = create(player, chestLoc);
            if (created != null) {
                holograms.put(player.getUniqueId(), created);
            }
        } else {
            update(existing);
        }
    }

    public void hideFor(Player player) {
        Entry e = holograms.remove(player.getUniqueId());
        if (e != null)
            removeInternal(e);
    }

    public void shutdown() {
        for (Entry e : holograms.values()) {
            removeInternal(e);
        }
        holograms.clear();
    }

    private Entry create(Player p, Location chestLoc) {
        Location base = chestLoc.clone().add(0.5, 2.8, 0.5);
        String holoName = "fc-" + p.getUniqueId().toString();

        chestManager.recalculate(chestLoc);
        Material crop = chestManager.getSelectedCrop(chestLoc);
        int count = chestManager.getCropCount(chestLoc);
        double sell = chestManager.calculateSellPrice(chestLoc, p);
        double basePercent = plugin.getConfig().getDouble("sell-bonus.base-percent", 0.05d);
        int level = 0;
        if (chestManager.getChestData(chestLoc) != null)
            level = chestManager.getChestData(chestLoc).getSellBonusLevel();
        double bonusPct = basePercent * Math.max(0, level);

        Material displayMaterial = getItemMaterial(crop);
        List<String> lines = new ArrayList<>();
        lines.add("#ICON: " + displayMaterial.name());
        lines.add(ChatUtil.format("&a" + crop.name()));
        lines.add(ChatUtil.format("&8Mature: &e" + count + " &8Sell: &e$" + String.format("%.2f", sell)));
        lines.add(ChatUtil.format("&8Bonus: &e" + String.format("%.0f%%", bonusPct * 100.0)));

        Hologram hologram = DHAPI.createHologram(holoName, base, lines);
        hologram.setDefaultVisibleState(false);
        hologram.setShowPlayer(p);

        return new Entry(chestLoc, hologram);
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

    private void update(Entry e) {
        if (e == null || e.hologram == null)
            return;

        chestManager.recalculate(e.chestLoc);
        Material crop = chestManager.getSelectedCrop(e.chestLoc);
        int count = chestManager.getCropCount(e.chestLoc);

        Player p = null;
        for (Map.Entry<UUID, Entry> entry : holograms.entrySet()) {
            if (entry.getValue() == e) {
                p = plugin.getServer().getPlayer(entry.getKey());
                break;
            }
        }
        if (p == null)
            return;

        double sell = chestManager.calculateSellPrice(e.chestLoc, p);
        double basePercent = plugin.getConfig().getDouble("sell-bonus.base-percent", 0.05d);
        int level = 0;
        if (chestManager.getChestData(e.chestLoc) != null)
            level = chestManager.getChestData(e.chestLoc).getSellBonusLevel();
        double bonusPct = basePercent * Math.max(0, level);

        Material displayMaterial = getItemMaterial(crop);
        DHAPI.setHologramLine(e.hologram, 0, "#ICON: " + displayMaterial.name());
        DHAPI.setHologramLine(e.hologram, 1, ChatUtil.format("&a" + crop.name()));
        DHAPI.setHologramLine(e.hologram, 2,
                ChatUtil.format("&8Mature: &e" + count + " &8Sell: &e$" + String.format("%.2f", sell)));
        DHAPI.setHologramLine(e.hologram, 3,
                ChatUtil.format("&8Bonus: &e" + String.format("%.0f%%", bonusPct * 100.0)));
    }

    private void removeInternal(Entry e) {
        if (e == null || e.hologram == null)
            return;
        e.hologram.delete();
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null)
            return false;
        return a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private static class Entry {
        final Location chestLoc;
        final Hologram hologram;

        Entry(Location chestLoc, Hologram hologram) {
            this.chestLoc = chestLoc;
            this.hologram = hologram;
        }
    }
}
