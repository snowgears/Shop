package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * Utility class for detecting Minecraft version compatibility features.
 * Provides simple boolean checks that are cached at startup for performance.
 * 
 * This allows clean conditional code like:
 * if (CompatibilityUtil.hasArmorMeta()) {
 *     org.bukkit.inventory.meta.ArmorMeta armorMeta = ...
 * }
 */
public class CompatibilityUtil {
    
    // Cached feature availability (computed once at startup)
    private static final boolean HAS_ARMOR_META = hasClass("org.bukkit.inventory.meta.ArmorMeta");
    private static final boolean HAS_OMINOUS_BOTTLE = hasClass("org.bukkit.inventory.meta.OminousBottleMeta");
    private static final boolean HAS_MUSIC_INSTRUMENT_META = hasClass("org.bukkit.inventory.meta.MusicInstrumentMeta");
    private static final boolean HAS_LIGHT_BLOCK = hasClass("org.bukkit.block.data.type.Light");
    private static final boolean HAS_GLOW_ITEM_FRAME = hasClass("org.bukkit.entity.GlowItemFrame");
    
    // Version-specific API availability
    private static final boolean HAS_NAMESPACED_KEY = hasClass("org.bukkit.NamespacedKey");
    private static final boolean HAS_BLOCK_DATA = hasClass("org.bukkit.block.data.BlockData");
    private static final boolean HAS_PERSISTENT_DATA_CONTAINER = hasClass("org.bukkit.persistence.PersistentDataContainer");
    
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
    
    // Bukkit ItemMeta interface availability
    private static final boolean HAS_DAMAGEABLE_INTERFACE = hasClass("org.bukkit.inventory.meta.Damageable");
    private static final boolean HAS_REPAIRABLE_INTERFACE = hasClass("org.bukkit.inventory.meta.Repairable");
    
    /**
     * Check if a class exists in the current runtime
     */
    private static boolean hasClass(String className) {
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
    
    // =================================
    // Public API Methods
    // =================================
    
    /**
     * @return true if ArmorMeta (armor trims) is available
     */
    public static boolean hasArmorMeta() {
        return HAS_ARMOR_META;
    }
    
    /**
     * @return true if OminousBottleMeta (Bad Omen bottles) is available
     */
    public static boolean hasOminousBottle() {
        return HAS_OMINOUS_BOTTLE;
    }
    
    /**
     * @return true if MusicInstrumentMeta (goat horns) is available
     */
    public static boolean hasMusicInstrumentMeta() {
        return HAS_MUSIC_INSTRUMENT_META;
    }
    
    /**
     * @return true if Light block type is available
     */
    public static boolean hasLightBlock() {
        return HAS_LIGHT_BLOCK;
    }
    
    /**
     * @return true if GlowItemFrame entity is available
     */
    public static boolean hasGlowItemFrame() {
        return HAS_GLOW_ITEM_FRAME;
    }
    
    /**
     * @return true if Enchantment.getTranslationKey() method is available
     */
    public static boolean hasGetTranslationKey() {
        return HAS_GET_TRANSLATION_KEY;
    }
    
    /**
     * @return true if PotionMeta.getBasePotionType() method is available
     */
    public static boolean hasPotionBaseType() {
        return HAS_POTION_BASE_TYPE;
    }
    
    /**
     * @return true if Sign.setGlowingText() method is available
     */
    public static boolean hasGlowingText() {
        return HAS_GLOWING_TEXT;
    }
    
    /**
     * @return true if SkullMeta.getOwnerProfile() method is available
     */
    public static boolean hasOwnerProfile() {
        return HAS_OWNER_PROFILE;
    }
    
    /**
     * @return true if Player.setCustomChatCompletions() method is available
     */
    public static boolean hasCustomChatCompletions() {
        return HAS_CUSTOM_CHAT_COMPLETIONS;
    }
    
    /**
     * @return true if EntityType.ITEM is available
     */
    public static boolean hasItemEntityType() {
        return HAS_ITEM_ENTITY_TYPE;
    }
    
    /**
     * @return true if PotionEffectType.INSTANT_HEALTH is available
     */
    public static boolean hasInstantHealth() {
        return HAS_INSTANT_HEALTH;
    }
    
    /**
     * @return true if PotionEffectType.INSTANT_DAMAGE is available
     */
    public static boolean hasInstantDamage() {
        return HAS_INSTANT_DAMAGE;
    }
    
    /**
     * @return true if Material.LIGHT is available
     */
    public static boolean hasLightMaterial() {
        return HAS_LIGHT_MATERIAL;
    }
    
    /**
     * @return true if running Minecraft 1.17 or later
     */
    public static boolean isMinecraft17Plus() {
        return IS_1_17_PLUS;
    }
    
    /**
     * @return true if running Minecraft 1.16 or later
     */
    public static boolean isMinecraft16Plus() {
        return IS_1_16_PLUS;
    }
    
    /**
     * @return true if running Minecraft 1.14 or later
     */
    public static boolean isMinecraft14Plus() {
        return IS_1_14_PLUS;
    }
    
    /**
     * @return the detected Minecraft version string
     */
    public static String getMinecraftVersion() {
        return MC_VERSION;
    }
    
    /**
     * Log all detected compatibility features (useful for debugging)
     */
    public static void logCompatibilityInfo() {
        if (Shop.getPlugin() != null) {
            Shop.getPlugin().getShopLogger().info("=== Compatibility Detection Results ===");
            Shop.getPlugin().getShopLogger().info("Minecraft Version: " + MC_VERSION);
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

    /**
     * @return true if ItemMeta.getAsComponentString() method is available
     */
    public static boolean hasGetAsComponentString() {
        return HAS_GET_AS_COMPONENT_STRING;
    }

    /**
     * @return true if ItemFactory.createItemStack(String) method is available
     */
    public static boolean hasCreateItemStackFromString() {
        return HAS_CREATE_ITEM_STACK_FROM_STRING;
    }

    /**
     * @return true if BlueMapAPI is available
     */
    public static boolean hasBlueMapAPI() {
        return HAS_BLUEMAP_API;
    }

    /**
     * @return true if Damageable interface is available
     */
    public static boolean hasDamageableInterface() {
        return HAS_DAMAGEABLE_INTERFACE;
    }

    /**
     * @return true if Repairable interface is available
     */
    public static boolean hasRepairableInterface() {
        return HAS_REPAIRABLE_INTERFACE;
    }

    /**
     * @return true if NamespacedKey is available (MC 1.13+)
     */
    public static boolean hasNamespacedKey() {
        return HAS_NAMESPACED_KEY;
    }
    
    /**
     * @return true if BlockData is available (MC 1.13+)
     */
    public static boolean hasBlockData() {
        return HAS_BLOCK_DATA;
    }
    
    /**
     * @return true if PersistentDataContainer is available (MC 1.14+)
     */
    public static boolean hasPersistentDataContainer() {
        return HAS_PERSISTENT_DATA_CONTAINER;
    }
    
    /**
     * Creates a NamespacedKey safely with fallback for older versions.
     * In legacy versions (MC ≤ 1.12.2), returns null as NamespacedKey doesn't exist.
     * 
     * @param plugin The plugin instance
     * @param key The key string
     * @return NamespacedKey instance or null in legacy versions
     */
    public static Object createNamespacedKey(Object plugin, String key) {
        if (!HAS_NAMESPACED_KEY) {
            // NamespacedKey doesn't exist in MC ≤ 1.12.2
            return null;
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
     * Gets the facing direction from PersistentDataContainer safely.
     * Returns null in legacy versions where PersistentDataContainer doesn't exist.
     * 
     * @param container The PersistentDataContainer (can be null in legacy versions)
     * @param namespacedKey The NamespacedKey (can be null in legacy versions) 
     * @param defaultValue The default value to return if key not found or incompatible version
     * @return The integer value, or defaultValue if not found/incompatible
     */
    public static int getPersistentDataInt(Object container, Object namespacedKey, int defaultValue) {
        if (!HAS_PERSISTENT_DATA_CONTAINER || !HAS_NAMESPACED_KEY || container == null || namespacedKey == null) {
            return defaultValue; // Return default in legacy versions
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
     * Sets an integer value in PersistentDataContainer safely.
     * In legacy versions, this is a no-op as PersistentDataContainer doesn't exist.
     * 
     * @param container The PersistentDataContainer (can be null in legacy versions)
     * @param namespacedKey The NamespacedKey (can be null in legacy versions)
     * @param value The value to store
     */
    public static void setPersistentDataInt(Object container, Object namespacedKey, int value) {
        if (!HAS_PERSISTENT_DATA_CONTAINER || !HAS_NAMESPACED_KEY || container == null || namespacedKey == null) {
            return; // No-op in legacy versions
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
     * Gets a string value from PersistentDataContainer safely.
     * In legacy versions, returns defaultValue as PersistentDataContainer doesn't exist.
     * 
     * @param container The PersistentDataContainer (can be null in legacy versions)
     * @param namespacedKey The NamespacedKey (can be null in legacy versions)
     * @param defaultValue The default value to return if not available
     * @return The stored string value or defaultValue
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
     * Sets a string value in PersistentDataContainer safely.
     * In legacy versions, this is a no-op as PersistentDataContainer doesn't exist.
     * 
     * @param container The PersistentDataContainer (can be null in legacy versions)
     * @param namespacedKey The NamespacedKey (can be null in legacy versions)
     * @param value The value to store
     */
    public static void setPersistentDataString(Object container, Object namespacedKey, String value) {
        if (!HAS_PERSISTENT_DATA_CONTAINER || !HAS_NAMESPACED_KEY || container == null || namespacedKey == null) {
            return; // No-op in legacy versions
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
     * Checks if a block is a wall sign using version-appropriate methods.
     * In modern versions (1.13+), uses BlockData instanceof WallSign.
     * In legacy versions, uses MaterialData checks.
     * 
     * @param block The block to check
     * @return true if the block is a wall sign
     */
    public static boolean isWallSignBlock(Block block) {
        if (block == null) {
            return false;
        }
        
        if (HAS_BLOCK_DATA) {
            // Modern method (MC 1.13+)
            try {
                Object blockData = block.getBlockData();
                Class<?> wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");
                return wallSignClass.isInstance(blockData);
            } catch (Exception e) {
                // Fall through to legacy method
            }
        }
        
        // Legacy method (MC ≤ 1.12.2)
        String typeName = block.getType().toString();
        return typeName.contains("WALL_SIGN") || 
               (typeName.contains("_SIGN") && !typeName.equals("SIGN_POST") && !typeName.equals("SIGN"));
    }
    
    /**
     * Gets the facing direction from a wall sign block safely.
     * In legacy versions (MC ≤ 1.12.2), uses legacy MaterialData approach.
     * In modern versions (MC 1.13+), uses BlockData approach.
     * 
     * @param block The block to check (should be a wall sign)
     * @return The BlockFace direction the sign is facing, or null if not a wall sign
     */
    public static BlockFace getWallSignFacing(Block block) {
        if (block == null) {
            return null;
        }
        
        if (HAS_BLOCK_DATA) {
            // Modern approach using BlockData (MC 1.13+)
            try {
                Object blockData = block.getBlockData();
                Class<?> wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");
                
                if (wallSignClass.isInstance(blockData)) {
                    java.lang.reflect.Method getFacingMethod = wallSignClass.getMethod("getFacing");
                    return (BlockFace) getFacingMethod.invoke(blockData);
                }
            } catch (Exception e) {
                if (Shop.getPlugin() != null) {
                    Shop.getPlugin().getShopLogger().debug("Failed to get modern sign facing: " + e.getMessage());
                }
            }
        }
        
        // Legacy approach using MaterialData (MC ≤ 1.12.2)
        try {
            org.bukkit.material.MaterialData materialData = block.getState().getData();
            if (materialData instanceof org.bukkit.material.Sign) {
                org.bukkit.material.Sign signData = (org.bukkit.material.Sign) materialData;
                return signData.getAttachedFace().getOppositeFace();
            }
        } catch (Exception e) {
            if (Shop.getPlugin() != null) {
                Shop.getPlugin().getShopLogger().debug("Failed to get legacy sign facing: " + e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * Sets the facing direction of a wall sign using version-appropriate methods.
     * In modern versions (1.13+), uses Directional BlockData.
     * In legacy versions, uses MaterialData.
     * 
     * @param signBlock The wall sign block
     * @param direction The direction the sign should face
     * @return true if successfully set, false otherwise
     */
    public static boolean setWallSignFacing(Block signBlock, BlockFace direction) {
        if (signBlock == null || direction == null) {
            return false;
        }
        
        if (HAS_BLOCK_DATA) {
            // Modern method (MC 1.13+)
            try {
                Object blockData = signBlock.getBlockData();
                Class<?> directionalClass = Class.forName("org.bukkit.block.data.Directional");
                
                if (directionalClass.isInstance(blockData)) {
                    java.lang.reflect.Method setFacingMethod = directionalClass.getMethod("setFacing", BlockFace.class);
                    setFacingMethod.invoke(blockData, direction);
                    
                    java.lang.reflect.Method setBlockDataMethod = Block.class.getMethod("setBlockData", Class.forName("org.bukkit.block.data.BlockData"));
                    setBlockDataMethod.invoke(signBlock, blockData);
                    return true;
                }
            } catch (Exception e) {
                // Fall through to legacy method
            }
        }
        
        // Legacy method (MC ≤ 1.12.2)
        try {
            // Use legacy MaterialData approach
            Class<?> signClass = Class.forName("org.bukkit.material.Sign");
            Object signData = signClass.getConstructor(org.bukkit.Material.class).newInstance(signBlock.getType());
            
            java.lang.reflect.Method setFacingMethod = signClass.getMethod("setFacingDirection", BlockFace.class);
            setFacingMethod.invoke(signData, direction);
            
            java.lang.reflect.Method setDataMethod = Block.class.getMethod("setData", byte.class);
            java.lang.reflect.Method getDataMethod = Class.forName("org.bukkit.material.MaterialData").getMethod("getData");
            byte dataValue = (Byte) getDataMethod.invoke(signData);
            setDataMethod.invoke(signBlock, dataValue);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Converts a regular sign to a wall sign and sets its facing direction.
     * Handles both modern (1.13+) and legacy (≤1.12.2) Minecraft versions.
     * 
     * @param signBlock The sign block to convert
     * @param direction The direction the wall sign should face
     * @return true if successfully converted, false otherwise
     */
    public static boolean convertToWallSign(Block signBlock, BlockFace direction) {
        if (signBlock == null || direction == null) {
            return false;
        }
        
        String currentType = signBlock.getType().toString();
        if (!currentType.contains("_SIGN")) {
            return false; // Not a sign block
        }
        
        // Determine the wall sign material name
        String wallSignType;
        if (currentType.contains("WALL_SIGN")) {
            // Already a wall sign, just set facing
            return setWallSignFacing(signBlock, direction);
        } else {
            // Convert to wall sign
            wallSignType = currentType.replaceAll("_SIGN$", "_WALL_SIGN");
            if (currentType.equals("SIGN") || currentType.equals("SIGN_POST")) {
                // Legacy naming
                wallSignType = "WALL_SIGN";
            }
        }
        
        try {
            org.bukkit.Material wallSignMaterial = org.bukkit.Material.valueOf(wallSignType);
            signBlock.setType(wallSignMaterial);
            return setWallSignFacing(signBlock, direction);
        } catch (IllegalArgumentException e) {
            // Material doesn't exist, try fallback
            try {
                signBlock.setType(org.bukkit.Material.valueOf("WALL_SIGN"));
                return setWallSignFacing(signBlock, direction);
            } catch (Exception fallbackException) {
                return false;
            }
        }
    }
} 