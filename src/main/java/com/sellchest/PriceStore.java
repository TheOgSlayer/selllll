package com.sellchest;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

/** Reads prices.yml (MATERIAL: price per item). */
public final class PriceStore {

    private final Map<Material, Double> prices = new EnumMap<>(Material.class);

    public void load(File file, Logger log) {
        prices.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            double price = yml.getDouble(key, 0.0);
            if (price <= 0) {
                continue;
            }
            Material m = Material.matchMaterial(key);
            if (m == null || !m.isItem()) {
                log.warning("prices.yml: unknown item \"" + key + "\"; skipped.");
                continue;
            }
            prices.put(m, price);
        }
        log.info("Loaded " + prices.size() + " sell prices.");
    }

    /** Price per item, or null if the item can't be sold. */
    public Double get(Material m) {
        return prices.get(m);
    }
}
