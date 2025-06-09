package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern implementation of CompatibilityUtil for MC 1.14+ using direct API calls.
 * This implementation avoids reflection for better performance and cleaner code.
 */
public class CompatibilityUtil {
    
    // Cached feature availability (computed once at startup)
    private static final boolean HAS_ARMOR_META = hasClass("org.bukkit.inventory.meta.ArmorMeta");
    private static final boolean HAS_OMINOUS_BOTTLE = hasClass("org.bukkit.inventory.meta.OminousBottleMeta");
    private static final boolean HAS_MUSIC_INSTRUMENT_META = hasClass("org.bukkit.inventory.meta.MusicInstrumentMeta");
    private static final boolean HAS_LIGHT_BLOCK = hasClass("org.bukkit.block.data.type.Light");
    private static final boolean HAS_GLOW_ITEM_FRAME = hasClass("org.bukkit.entity.GlowItemFrame");
    
    // Version-specific API availability - always true in modern versions
    public static final boolean HAS_NAMESPACED_KEY = true;
    public static final boolean HAS_BLOCK_DATA = true;
    public static final boolean HAS_PERSISTENT_DATA_CONTAINER = true;
    
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
    
    // Entity types - always true in modern versions
    private static final boolean HAS_ITEM_ENTITY_TYPE = true;
    
    // Potion effect types - always true in modern versions
    private static final boolean HAS_INSTANT_HEALTH = true;
    private static final boolean HAS_INSTANT_DAMAGE = true;
    
    // Material types
    private static final boolean HAS_LIGHT_MATERIAL = hasEnumValue("org.bukkit.Material", "LIGHT");
    
    // Minecraft version detection
    private static final String MC_VERSION = detectMinecraftVersion();
    private static final boolean IS_1_17_PLUS = isMinecraftVersionAtLeast("1.17");
    private static final boolean IS_1_16_PLUS = isMinecraftVersionAtLeast("1.16");
    private static final boolean IS_1_14_PLUS = true; // Always true in modern versions
    
    // InventoryView compatibility detection - use feature detection instead of version checking
    private static final boolean NEEDS_INVENTORY_VIEW_REFLECTION = detectInventoryViewReflectionNeed();
    
    // Bukkit ItemMeta interface availability - always true in modern versions
    private static final boolean HAS_DAMAGEABLE_INTERFACE = true;
    private static final boolean HAS_REPAIRABLE_INTERFACE = true;
    
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
            // Check if it's an interface (pre-1.17.1) or abstract class (1.17.1+)
            // If it's an abstract class, we might need reflection depending on implementation
            java.lang.reflect.Method getTitleMethod = inventoryViewClass.getMethod("getTitle");
            
            // Additional check: try to see if the method is accessible
            // Some versions might have the method but not be directly callable
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