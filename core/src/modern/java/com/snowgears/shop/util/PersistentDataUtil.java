package com.snowgears.shop.util;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Modern implementation of PersistentDataUtil for MC 1.14+ using direct API calls.
 * This implementation uses the modern PersistentDataContainer API without reflection.
 */
public class PersistentDataUtil {
    
    /**
     * Gets an integer value from an entity's persistent data container using modern API.
     * 
     * @param entity The entity to get data from
     * @param plugin The plugin instance for creating the NamespacedKey
     * @param key The key to look up
     * @return The integer value, or null if not found
     */
    public static Integer getIntegerData(Entity entity, Plugin plugin, String key) {
        if (entity == null || plugin == null || key == null) {
            return null;
        }
        
        // Modern API - direct call, no reflection needed
        PersistentDataContainer container = entity.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        
        return container.get(namespacedKey, PersistentDataType.INTEGER);
    }
    
    /**
     * Sets an integer value in an entity's persistent data container using modern API.
     * 
     * @param entity The entity to set data on
     * @param plugin The plugin instance for creating the NamespacedKey
     * @param key The key to set
     * @param value The integer value to set
     * @return true if successfully set, false otherwise
     */
    public static boolean setIntegerData(Entity entity, Plugin plugin, String key, Integer value) {
        if (entity == null || plugin == null || key == null || value == null) {
            return false;
        }
        
        try {
            // Modern API - direct call, no reflection needed
            PersistentDataContainer container = entity.getPersistentDataContainer();
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            
            container.set(namespacedKey, PersistentDataType.INTEGER, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if an entity has a specific key in its persistent data container.
     * 
     * @param entity The entity to check
     * @param plugin The plugin instance for creating the NamespacedKey
     * @param key The key to check for
     * @return true if the key exists
     */
    public static boolean hasKey(Entity entity, Plugin plugin, String key) {
        if (entity == null || plugin == null || key == null) {
            return false;
        }
        
        try {
            // Modern API - direct call, no reflection needed
            PersistentDataContainer container = entity.getPersistentDataContainer();
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            
            return container.has(namespacedKey, PersistentDataType.INTEGER);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Removes a key from an entity's persistent data container.
     * 
     * @param entity The entity to remove data from
     * @param plugin The plugin instance for creating the NamespacedKey
     * @param key The key to remove
     * @return true if successfully removed, false otherwise
     */
    public static boolean removeKey(Entity entity, Plugin plugin, String key) {
        if (entity == null || plugin == null || key == null) {
            return false;
        }
        
        try {
            // Modern API - direct call, no reflection needed
            PersistentDataContainer container = entity.getPersistentDataContainer();
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            
            container.remove(namespacedKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if PersistentDataContainer is available in this version.
     * 
     * @return true if PersistentDataContainer is supported (MC 1.14+)
     */
    public static boolean isPersistentDataSupported() {
        return true; // Always true in modern versions
    }
} 