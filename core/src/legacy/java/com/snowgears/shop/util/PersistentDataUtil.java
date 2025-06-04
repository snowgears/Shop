package com.snowgears.shop.util;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Legacy implementation of PersistentDataUtil for MC 1.13.2-1.14.
 * This implementation uses reflection for PersistentDataContainer compatibility.
 */
public class PersistentDataUtil {
    
    /**
     * Gets an integer value from an entity's persistent data container using reflection.
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
        
        // Use reflection to access PersistentDataContainer safely
        if (CompatibilityUtil.HAS_PERSISTENT_DATA_CONTAINER) {
            try {
                // Get PersistentDataContainer using reflection
                Object persistentData = entity.getClass().getMethod("getPersistentDataContainer").invoke(entity);
                if (persistentData == null) {
                    return null;
                }
                
                // Create NamespacedKey using CompatibilityUtil
                Object namespacedKey = CompatibilityUtil.createNamespacedKey(plugin, key);
                if (namespacedKey == null) {
                    return null;
                }
                
                // Get INTEGER PersistentDataType using reflection
                Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
                Object integerType = persistentDataTypeClass.getField("INTEGER").get(null);
                
                // Get data using reflection
                Object dataValue = persistentData.getClass()
                    .getMethod("get", Class.forName("org.bukkit.NamespacedKey"), Class.forName("org.bukkit.persistence.PersistentDataType"))
                    .invoke(persistentData, namespacedKey, integerType);
                
                return (Integer) dataValue;
            } catch (Exception e) {
                // Not supported in this version or reflection failed
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Sets an integer value in an entity's persistent data container using reflection.
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
        
        // Use reflection to access PersistentDataContainer safely
        if (CompatibilityUtil.HAS_PERSISTENT_DATA_CONTAINER) {
            try {
                // Get PersistentDataContainer using reflection
                Object persistentData = entity.getClass().getMethod("getPersistentDataContainer").invoke(entity);
                if (persistentData == null) {
                    return false;
                }
                
                // Create NamespacedKey using CompatibilityUtil
                Object namespacedKey = CompatibilityUtil.createNamespacedKey(plugin, key);
                if (namespacedKey == null) {
                    return false;
                }
                
                // Get INTEGER PersistentDataType using reflection
                Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
                Object integerType = persistentDataTypeClass.getField("INTEGER").get(null);
                
                // Set data using reflection
                persistentData.getClass()
                    .getMethod("set", Class.forName("org.bukkit.NamespacedKey"), Class.forName("org.bukkit.persistence.PersistentDataType"), Object.class)
                    .invoke(persistentData, namespacedKey, integerType, value);
                
                return true;
            } catch (Exception e) {
                // Not supported in this version or reflection failed
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if an entity has a specific key in its persistent data container using reflection.
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
        
        // Use reflection to access PersistentDataContainer safely
        if (CompatibilityUtil.HAS_PERSISTENT_DATA_CONTAINER) {
            try {
                // Get PersistentDataContainer using reflection
                Object persistentData = entity.getClass().getMethod("getPersistentDataContainer").invoke(entity);
                if (persistentData == null) {
                    return false;
                }
                
                // Create NamespacedKey using CompatibilityUtil
                Object namespacedKey = CompatibilityUtil.createNamespacedKey(plugin, key);
                if (namespacedKey == null) {
                    return false;
                }
                
                // Get INTEGER PersistentDataType using reflection
                Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
                Object integerType = persistentDataTypeClass.getField("INTEGER").get(null);
                
                // Check if key exists using reflection
                Boolean hasKey = (Boolean) persistentData.getClass()
                    .getMethod("has", Class.forName("org.bukkit.NamespacedKey"), Class.forName("org.bukkit.persistence.PersistentDataType"))
                    .invoke(persistentData, namespacedKey, integerType);
                
                return hasKey != null && hasKey;
            } catch (Exception e) {
                // Not supported in this version or reflection failed
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Removes a key from an entity's persistent data container using reflection.
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
        
        // Use reflection to access PersistentDataContainer safely
        if (CompatibilityUtil.HAS_PERSISTENT_DATA_CONTAINER) {
            try {
                // Get PersistentDataContainer using reflection
                Object persistentData = entity.getClass().getMethod("getPersistentDataContainer").invoke(entity);
                if (persistentData == null) {
                    return false;
                }
                
                // Create NamespacedKey using CompatibilityUtil
                Object namespacedKey = CompatibilityUtil.createNamespacedKey(plugin, key);
                if (namespacedKey == null) {
                    return false;
                }
                
                // Remove key using reflection
                persistentData.getClass()
                    .getMethod("remove", Class.forName("org.bukkit.NamespacedKey"))
                    .invoke(persistentData, namespacedKey);
                
                return true;
            } catch (Exception e) {
                // Not supported in this version or reflection failed
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if PersistentDataContainer is available in this version using reflection.
     * 
     * @return true if PersistentDataContainer is supported
     */
    public static boolean isPersistentDataSupported() {
        return CompatibilityUtil.HAS_PERSISTENT_DATA_CONTAINER;
    }
} 