package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern implementation of CompatibilityUtil for MC 1.14+ using direct API calls.
 * This implementation avoids reflection for better performance and cleaner code.
 */
public class CompatibilityUtil {
    
        
    // Version-specific API availability - always true in modern versions
    public static final boolean HAS_NAMESPACED_KEY = true;
    public static final boolean HAS_BLOCK_DATA = true;
    public static final boolean HAS_PERSISTENT_DATA_CONTAINER = true;
    
    // External API availability
    private static final boolean HAS_BLUEMAP_API = hasClass("de.bluecolored.bluemap.api.BlueMapAPI");
    
    // Entity types - always true in modern versions
    private static final boolean HAS_ITEM_ENTITY_TYPE = true;

    /**
     * Safely checks if a class exists without throwing ClassNotFoundException
     */
    public static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    // =================================
    // Public API Methods
    // =================================
    

    public static boolean hasItemEntityType() { return HAS_ITEM_ENTITY_TYPE; }
    public static boolean hasBlueMapAPI() { return HAS_BLUEMAP_API; }
    
    /**
    /**
     * Creates a NamespacedKey using modern API (no reflection needed).
     */
    public static NamespacedKey createNamespacedKey(Object plugin, String key) {
        if (plugin instanceof org.bukkit.plugin.Plugin) {
            return new NamespacedKey((org.bukkit.plugin.Plugin) plugin, key);
        }
        return null;
    }
    
    /**
     * Sets item data using PersistentDataContainer (modern approach).
     */
    public static void setItemData(ItemStack item, String key, String value) {
        if (!MCVersion.atLeast("1.13")) {
            setLegacyItemData(item, key, value);
            return;
        }

        if (item == null || item.getItemMeta() == null) return;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey namespacedKey = createNamespacedKey(Shop.getPlugin(), key);
        if (namespacedKey != null) {
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
    }
    
    /**
     * Gets item data using PersistentDataContainer (modern approach).
     */
    public static String getItemData(ItemStack item, String key, String defaultValue) {
        if (!MCVersion.atLeast("1.13")) {
            return getLegacyItemData(item, key, defaultValue);
        }

        if (item == null || item.getItemMeta() == null) return defaultValue;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey namespacedKey = createNamespacedKey(Shop.getPlugin(), key);
        if (namespacedKey != null) {
            return meta.getPersistentDataContainer().getOrDefault(namespacedKey, PersistentDataType.STRING, defaultValue);
        }
        return defaultValue;
    }
    
    // Legacy implementation using lore-based storage
    private static void setLegacyItemData(ItemStack item, String key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        // Remove existing data for this key
        lore.removeIf(line -> line.contains("§7§k§r" + key + ":"));
        
        // Add new data (using invisible formatting to minimize visual impact)
        lore.add("§7§k§r" + key + ":" + value + "§r");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
    
    private static String getLegacyItemData(ItemStack item, String key, String defaultValue) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return defaultValue;
        
        String searchPrefix = "§7§k§r" + key + ":";
        for (String line : meta.getLore()) {
            if (line.contains(searchPrefix)) {
                int startIndex = line.indexOf(searchPrefix) + searchPrefix.length();
                int endIndex = line.indexOf("§r", startIndex);
                if (endIndex == -1) endIndex = line.length();
                return line.substring(startIndex, endIndex);
            }
        }
        return defaultValue;
    }
} 