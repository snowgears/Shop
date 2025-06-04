package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy implementation of CompatibilityUtil for MC 1.14-1.13.2.
 * This implementation provides compatibility for the transition period around "The Flattening".
 */
public class CompatibilityUtil {
    
    // Cached feature availability (computed once at startup)
    private static final boolean HAS_ARMOR_META = hasClass("org.bukkit.inventory.meta.ArmorMeta");
    private static final boolean HAS_OMINOUS_BOTTLE = hasClass("org.bukkit.inventory.meta.OminousBottleMeta");
    private static final boolean HAS_MUSIC_INSTRUMENT_META = hasClass("org.bukkit.inventory.meta.MusicInstrumentMeta");
    private static final boolean HAS_LIGHT_BLOCK = hasClass("org.bukkit.block.data.type.Light");
    private static final boolean HAS_GLOW_ITEM_FRAME = hasClass("org.bukkit.entity.GlowItemFrame");
    
    // Version-specific API availability (use reflection to detect)
    public static final boolean HAS_NAMESPACED_KEY = hasClass("org.bukkit.NamespacedKey");
    public static final boolean HAS_BLOCK_DATA = hasClass("org.bukkit.block.data.BlockData");
    public static final boolean HAS_PERSISTENT_DATA_CONTAINER = hasClass("org.bukkit.persistence.PersistentDataContainer");
    
    // Bukkit API method availability
    private static final boolean HAS_GET_TRANSLATION_KEY = hasMethod("org.bukkit.enchantments.Enchantment", "getTranslationKey");
    private static final boolean HAS_POTION_BASE_TYPE = hasMethod("org.bukkit.inventory.meta.PotionMeta", "getBasePotionType");
    private static final boolean HAS_GLOWING_TEXT = hasMethod("org.bukkit.block.Sign", "setGlowingText", boolean.class);
    private static final boolean HAS_OWNER_PROFILE = hasMethod("org.bukkit.inventory.meta.SkullMeta", "getOwnerProfile");
    private static final boolean HAS_CUSTOM_CHAT_COMPLETIONS = hasMethod("org.bukkit.entity.Player", "setCustomChatCompletions", java.util.List.class);
    private static final boolean HAS_GET_AS_COMPONENT_STRING = hasMethod("org.bukkit.inventory.meta.ItemMeta", "getAsComponentString");
    private static final boolean HAS_CREATE_ITEM_STACK_FROM_STRING = hasMethod("org.bukkit.inventory.ItemFactory", "createItemStack", String.class);
    
    // External API availability
    private static final boolean HAS_BLUEMAP_API = hasClass("de.bluecolored.bluemap.api.BlueMapAPI");
    
    // Entity types
    private static final boolean HAS_ITEM_ENTITY_TYPE = hasEnumValue("org.bukkit.entity.EntityType", "ITEM");
    
    // Potion effect types
    private static final boolean HAS_INSTANT_HEALTH = hasEnumValue("org.bukkit.potion.PotionEffectType", "INSTANT_HEALTH");
    private static final boolean HAS_INSTANT_DAMAGE = hasEnumValue("org.bukkit.potion.PotionEffectType", "INSTANT_DAMAGE");
    
    // Material types
    private static final boolean HAS_LIGHT_MATERIAL = hasEnumValue("org.bukkit.Material", "LIGHT");
    
    // Minecraft version detection
    private static final String MC_VERSION = detectMinecraftVersion();
    private static final boolean IS_1_17_PLUS = isMinecraftVersionAtLeast("1.17");
    private static final boolean IS_1_16_PLUS = isMinecraftVersionAtLeast("1.16");
    private static final boolean IS_1_14_PLUS = isMinecraftVersionAtLeast("1.14");
    
    // InventoryView compatibility detection - use feature detection instead of version checking
    private static final boolean NEEDS_INVENTORY_VIEW_REFLECTION = detectInventoryViewReflectionNeed();
    
    // Bukkit ItemMeta interface availability
    private static final boolean HAS_DAMAGEABLE_INTERFACE = hasClass("org.bukkit.inventory.meta.Damageable");
    private static final boolean HAS_REPAIRABLE_INTERFACE = hasClass("org.bukkit.inventory.meta.Repairable");
    
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
    
    /**
     * Check if a method exists on a class
     */
    private static boolean hasMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class<?> clazz = Class.forName(className);
            clazz.getMethod(methodName, parameterTypes);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return false;
        }
    }
    
    /**
     * Check if an enum value exists
     */
    private static boolean hasEnumValue(String enumClassName, String valueName) {
        try {
            Class<?> enumClass = Class.forName(enumClassName);
            if (enumClass.isEnum()) {
                for (Object enumConstant : enumClass.getEnumConstants()) {
                    if (enumConstant.toString().equals(valueName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Detect the Minecraft version
     */
    private static String detectMinecraftVersion() {
        try {
            return Bukkit.getVersion();
        } catch (Exception e) {
            if (Shop.getPlugin() != null) {
                Shop.getPlugin().getShopLogger().warning("Could not detect Minecraft version: " + e.getMessage());
            }
            return "unknown";
        }
    }
    
    /**
     * Check if current Minecraft version is at least the specified version
     */
    private static boolean isMinecraftVersionAtLeast(String version) {
        try {
            String current = Bukkit.getBukkitVersion();
            if (current.contains(version)) {
                return true;
            }
            // Simple version comparison (works for most cases)
            String[] currentParts = current.split("[-.]");
            String[] targetParts = version.split("\\.");
            
            for (int i = 0; i < Math.min(currentParts.length, targetParts.length); i++) {
                try {
                    int currentNum = Integer.parseInt(currentParts[i]);
                    int targetNum = Integer.parseInt(targetParts[i]);
                    if (currentNum > targetNum) return true;
                    if (currentNum < targetNum) return false;
                } catch (NumberFormatException e) {
                    // Continue with string comparison
                    break;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Detect if we need to use reflection for InventoryView access.
     * Uses feature detection instead of version checking for better compatibility.
     */
    private static boolean detectInventoryViewReflectionNeed() {
        // Test if InventoryView.getTitle() can be called directly without issues
        try {
            Class<?> inventoryViewClass = Class.forName("org.bukkit.inventory.InventoryView");
            java.lang.reflect.Method getTitleMethod = inventoryViewClass.getMethod("getTitle");
            
            // Additional check: try to see if the method is accessible
            getTitleMethod.setAccessible(true);
            
            return false; // Direct access should work
        } catch (Exception e) {
            return true; // Need reflection fallback
        }
    }
    
    // =================================
    // Public API Methods
    // =================================
    
    public static boolean hasArmorMeta() { return HAS_ARMOR_META; }
    public static boolean hasOminousBottle() { return HAS_OMINOUS_BOTTLE; }
    public static boolean hasMusicInstrumentMeta() { return HAS_MUSIC_INSTRUMENT_META; }
    public static boolean hasLightBlock() { return HAS_LIGHT_BLOCK; }
    public static boolean hasGlowItemFrame() { return HAS_GLOW_ITEM_FRAME; }
    public static boolean hasGetTranslationKey() { return HAS_GET_TRANSLATION_KEY; }
    public static boolean hasPotionBaseType() { return HAS_POTION_BASE_TYPE; }
    public static boolean hasGlowingText() { return HAS_GLOWING_TEXT; }
    public static boolean hasOwnerProfile() { return HAS_OWNER_PROFILE; }
    public static boolean hasCustomChatCompletions() { return HAS_CUSTOM_CHAT_COMPLETIONS; }
    public static boolean hasItemEntityType() { return HAS_ITEM_ENTITY_TYPE; }
    public static boolean hasInstantHealth() { return HAS_INSTANT_HEALTH; }
    public static boolean hasInstantDamage() { return HAS_INSTANT_DAMAGE; }
    public static boolean hasLightMaterial() { return HAS_LIGHT_MATERIAL; }
    public static boolean isMinecraft17Plus() { return IS_1_17_PLUS; }
    public static boolean isMinecraft16Plus() { return IS_1_16_PLUS; }
    public static boolean isMinecraft14Plus() { return IS_1_14_PLUS; }
    public static boolean hasGetAsComponentString() { return HAS_GET_AS_COMPONENT_STRING; }
    public static boolean hasCreateItemStackFromString() { return HAS_CREATE_ITEM_STACK_FROM_STRING; }
    public static boolean hasBlueMapAPI() { return HAS_BLUEMAP_API; }
    public static boolean hasDamageableInterface() { return HAS_DAMAGEABLE_INTERFACE; }
    public static boolean hasRepairableInterface() { return HAS_REPAIRABLE_INTERFACE; }
    public static boolean hasBlockData() { return HAS_BLOCK_DATA; }
    public static String getMinecraftVersion() { return MC_VERSION; }
    
    /**
     * Check if we need to use reflection for InventoryView access
     */
    public static boolean needsInventoryViewReflection() { return NEEDS_INVENTORY_VIEW_REFLECTION; }
    
    /**
     * Creates a NamespacedKey safely with fallback for older versions.
     * Uses reflection to handle version differences.
     */
    public static Object createNamespacedKey(Object plugin, String key) {
        if (!HAS_NAMESPACED_KEY) {
            return null; // NamespacedKey doesn't exist in older versions
        }
        
        try {
            Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
            java.lang.reflect.Constructor<?> constructor = namespacedKeyClass.getConstructor(
                org.bukkit.plugin.Plugin.class, String.class);
            return constructor.newInstance(plugin, key);
        } catch (Exception e) {
            if (Shop.getPlugin() != null) {
                Shop.getPlugin().getShopLogger().warning("Failed to create NamespacedKey: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * Gets an integer value from PersistentDataContainer safely using reflection.
     */
    public static int getPersistentDataInt(Object container, Object namespacedKey, int defaultValue) {
        if (!HAS_PERSISTENT_DATA_CONTAINER || !HAS_NAMESPACED_KEY || container == null || namespacedKey == null) {
            return defaultValue;
        }
        
        try {
            Class<?> containerClass = Class.forName("org.bukkit.persistence.PersistentDataContainer");
            Class<?> keyClass = Class.forName("org.bukkit.NamespacedKey");
            Class<?> typeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            
            java.lang.reflect.Field integerField = typeClass.getField("INTEGER");
            Object integerType = integerField.get(null);
            
            java.lang.reflect.Method getMethod = containerClass.getMethod("get", keyClass, typeClass);
            Integer result = (Integer) getMethod.invoke(container, namespacedKey, integerType);
            return result != null ? result : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Sets an integer value in PersistentDataContainer safely using reflection.
     */
    public static void setPersistentDataInt(Object container, Object namespacedKey, int value) {
        if (!HAS_PERSISTENT_DATA_CONTAINER || !HAS_NAMESPACED_KEY || container == null || namespacedKey == null) {
            return;
        }
        
        try {
            Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object integerType = persistentDataTypeClass.getField("INTEGER").get(null);
            
            java.lang.reflect.Method setMethod = container.getClass().getMethod("set", 
                Class.forName("org.bukkit.NamespacedKey"), persistentDataTypeClass, Object.class);
            setMethod.invoke(container, namespacedKey, integerType, value);
        } catch (Exception e) {
            if (Shop.getPlugin() != null) {
                Shop.getPlugin().getShopLogger().debug("Failed to set persistent data: " + e.getMessage());
            }
        }
    }

    /**
     * Gets a string value from PersistentDataContainer safely using reflection.
     */
    public static String getPersistentDataString(Object container, Object namespacedKey, String defaultValue) {
        if (!HAS_PERSISTENT_DATA_CONTAINER || !HAS_NAMESPACED_KEY || container == null || namespacedKey == null) {
            return defaultValue;
        }
        
        try {
            Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = persistentDataTypeClass.getField("STRING").get(null);
            
            java.lang.reflect.Method getMethod = container.getClass().getMethod("get", 
                Class.forName("org.bukkit.NamespacedKey"), persistentDataTypeClass);
            Object result = getMethod.invoke(container, namespacedKey, stringType);
            
            return result != null ? (String) result : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Sets a string value in PersistentDataContainer safely using reflection.
     */
    public static void setPersistentDataString(Object container, Object namespacedKey, String value) {
        if (!HAS_PERSISTENT_DATA_CONTAINER || !HAS_NAMESPACED_KEY || container == null || namespacedKey == null) {
            return;
        }
        
        try {
            Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = persistentDataTypeClass.getField("STRING").get(null);
            
            java.lang.reflect.Method setMethod = container.getClass().getMethod("set", 
                Class.forName("org.bukkit.NamespacedKey"), persistentDataTypeClass, Object.class);
            setMethod.invoke(container, namespacedKey, stringType, value);
        } catch (Exception e) {
            if (Shop.getPlugin() != null) {
                Shop.getPlugin().getShopLogger().debug("Failed to set persistent data: " + e.getMessage());
            }
        }
    }
    
    /**
     * Sets item data using the appropriate storage method.
     * Uses PersistentDataContainer if available, otherwise falls back to lore-based storage.
     */
    public static void setItemData(ItemStack item, String key, String value) {
        if (item == null || item.getItemMeta() == null) return;
        
        if (HAS_PERSISTENT_DATA_CONTAINER && HAS_NAMESPACED_KEY) {
            // Modern approach: Use PersistentDataContainer with reflection
            setModernItemData(item, key, value);
        } else {
            // Legacy approach: Use lore-based storage
            setLegacyItemData(item, key, value);
        }
    }
    
    /**
     * Gets item data using the appropriate storage method.
     */
    public static String getItemData(ItemStack item, String key, String defaultValue) {
        if (item == null || item.getItemMeta() == null) return defaultValue;
        
        if (HAS_PERSISTENT_DATA_CONTAINER && HAS_NAMESPACED_KEY) {
            // Modern approach: Use PersistentDataContainer with reflection
            return getModernItemData(item, key, defaultValue);
        } else {
            // Legacy approach: Use lore-based storage
            return getLegacyItemData(item, key, defaultValue);
        }
    }
    
    /**
     * Removes item data using the appropriate storage method.
     */
    public static void removeItemData(ItemStack item, String key) {
        if (item == null || item.getItemMeta() == null) return;
        
        if (HAS_PERSISTENT_DATA_CONTAINER && HAS_NAMESPACED_KEY) {
            // Modern approach: Use PersistentDataContainer with reflection
            removeModernItemData(item, key);
        } else {
            // Legacy approach: Use lore-based storage
            removeLegacyItemData(item, key);
        }
    }
    
    // Modern implementation using PersistentDataContainer via reflection
    private static void setModernItemData(ItemStack item, String key, String value) {
        try {
            Object namespacedKey = createNamespacedKey(Shop.getPlugin(), key);
            if (namespacedKey != null) {
                ItemMeta meta = item.getItemMeta();
                java.lang.reflect.Method getPDCMethod = meta.getClass().getMethod("getPersistentDataContainer");
                Object container = getPDCMethod.invoke(meta);
                setPersistentDataString(container, namespacedKey, value);
                item.setItemMeta(meta);
            }
        } catch (Exception e) {
            // Fallback to legacy method
            setLegacyItemData(item, key, value);
        }
    }
    
    private static String getModernItemData(ItemStack item, String key, String defaultValue) {
        try {
            Object namespacedKey = createNamespacedKey(Shop.getPlugin(), key);
            if (namespacedKey != null) {
                ItemMeta meta = item.getItemMeta();
                java.lang.reflect.Method getPDCMethod = meta.getClass().getMethod("getPersistentDataContainer");
                Object container = getPDCMethod.invoke(meta);
                return getPersistentDataString(container, namespacedKey, defaultValue);
            }
        } catch (Exception e) {
            // Fallback to legacy method
            return getLegacyItemData(item, key, defaultValue);
        }
        return defaultValue;
    }
    
    private static void removeModernItemData(ItemStack item, String key) {
        try {
            Object namespacedKey = createNamespacedKey(Shop.getPlugin(), key);
            if (namespacedKey != null) {
                ItemMeta meta = item.getItemMeta();
                java.lang.reflect.Method getPDCMethod = meta.getClass().getMethod("getPersistentDataContainer");
                Object container = getPDCMethod.invoke(meta);
                
                // Remove from PersistentDataContainer using reflection
                java.lang.reflect.Method removeMethod = container.getClass().getMethod("remove", Class.forName("org.bukkit.NamespacedKey"));
                removeMethod.invoke(container, namespacedKey);
                item.setItemMeta(meta);
            }
        } catch (Exception e) {
            // Fallback to legacy method
            removeLegacyItemData(item, key);
        }
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
    
    private static void removeLegacyItemData(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        
        List<String> lore = new ArrayList<>(meta.getLore());
        lore.removeIf(line -> line.contains("§7§k§r" + key + ":"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
    
    // =================================
    // BlockData Methods (delegate to BlockDataUtil)
    // =================================
    
    /**
     * Checks if a block is a wall sign.
     * @deprecated Use BlockDataUtil.isWallSign() instead
     */
    @Deprecated
    public static boolean isWallSignBlock(Block block) {
        return BlockDataUtil.isWallSign(block);
    }
    
    /**
     * Gets the facing direction from a wall sign block.
     * @deprecated Use BlockDataUtil.getSignFacing() instead
     */
    @Deprecated
    public static BlockFace getWallSignFacing(Block block) {
        return BlockDataUtil.getSignFacing(block);
    }
    
    /**
     * Sets the facing direction of a wall sign.
     * @deprecated Use BlockDataUtil.setSignFacing() instead
     */
    @Deprecated
    public static boolean setWallSignFacing(Block signBlock, BlockFace direction) {
        return BlockDataUtil.setSignFacing(signBlock, direction);
    }
    
    /**
     * Converts a regular sign to a wall sign and sets its facing direction.
     * @deprecated Use BlockDataUtil.convertToWallSign() instead
     */
    @Deprecated
    public static boolean convertToWallSign(Block signBlock, BlockFace direction) {
        return BlockDataUtil.convertToWallSign(signBlock, direction);
    }
    
    /**
     * Gets block data using version-appropriate methods.
     * @deprecated Use BlockDataUtil.getBlockData() instead
     */
    @Deprecated
    public static Object getBlockData(Block block) {
        return BlockDataUtil.getBlockData(block);
    }
    
    /**
     * Sets block data using version-appropriate methods.
     * @deprecated Use BlockDataUtil.setBlockData() instead
     */
    @Deprecated
    public static boolean setBlockData(Block block, Object data) {
        return BlockDataUtil.setBlockData(block, data);
    }
    
    /**
     * Checks if a material is a shulker box.
     * @deprecated Use BlockDataUtil.isShulkerBox() instead
     */
    @Deprecated
    public static boolean isShulkerBox(Material material) {
        return BlockDataUtil.isShulkerBox(material);
    }
    
    /**
     * Log all detected compatibility features (useful for debugging)
     */
    public static void logCompatibilityInfo() {
        if (Shop.getPlugin() != null) {
            Shop.getPlugin().getShopLogger().info("=== Legacy Compatibility Detection Results ===");
            Shop.getPlugin().getShopLogger().info("Minecraft Version: " + MC_VERSION);
            Shop.getPlugin().getShopLogger().info("Needs InventoryView Reflection: " + NEEDS_INVENTORY_VIEW_REFLECTION);
            Shop.getPlugin().getShopLogger().info("NamespacedKey: " + HAS_NAMESPACED_KEY);
            Shop.getPlugin().getShopLogger().info("BlockData: " + HAS_BLOCK_DATA);
            Shop.getPlugin().getShopLogger().info("PersistentDataContainer: " + HAS_PERSISTENT_DATA_CONTAINER);
            Shop.getPlugin().getShopLogger().info("ArmorMeta: " + HAS_ARMOR_META);
            Shop.getPlugin().getShopLogger().info("OminousBottle: " + HAS_OMINOUS_BOTTLE);
            Shop.getPlugin().getShopLogger().info("MusicInstrumentMeta: " + HAS_MUSIC_INSTRUMENT_META);
            Shop.getPlugin().getShopLogger().info("Light Block: " + HAS_LIGHT_BLOCK);
            Shop.getPlugin().getShopLogger().info("Glow Item Frame: " + HAS_GLOW_ITEM_FRAME);
            Shop.getPlugin().getShopLogger().info("Translation Keys: " + HAS_GET_TRANSLATION_KEY);
            Shop.getPlugin().getShopLogger().info("Potion Base Types: " + HAS_POTION_BASE_TYPE);
            Shop.getPlugin().getShopLogger().info("=======================================");
        }
    }
} 