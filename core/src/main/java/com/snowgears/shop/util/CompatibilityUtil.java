package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;

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
    
    // Bukkit API method availability
    private static final boolean HAS_GET_TRANSLATION_KEY = hasMethod("org.bukkit.enchantments.Enchantment", "getTranslationKey");
    private static final boolean HAS_POTION_BASE_TYPE = hasMethod("org.bukkit.inventory.meta.PotionMeta", "getBasePotionType");
    private static final boolean HAS_GLOWING_TEXT = hasMethod("org.bukkit.block.Sign", "setGlowingText", boolean.class);
    private static final boolean HAS_OWNER_PROFILE = hasMethod("org.bukkit.inventory.meta.SkullMeta", "getOwnerProfile");
    private static final boolean HAS_TEXT_COMPONENT_FROM_LEGACY = hasMethod("net.md_5.bungee.api.chat.TextComponent", "fromLegacy", String.class);
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
                Shop.getPlugin().getLogger().warning("Could not detect Minecraft version: " + e.getMessage());
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
     * @return true if TextComponent.fromLegacy() method is available
     */
    public static boolean hasTextComponentFromLegacy() {
        return HAS_TEXT_COMPONENT_FROM_LEGACY;
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
            Shop.getPlugin().getLogger().info("=== Compatibility Detection Results ===");
            Shop.getPlugin().getLogger().info("Minecraft Version: " + MC_VERSION);
            Shop.getPlugin().getLogger().info("ArmorMeta: " + HAS_ARMOR_META);
            Shop.getPlugin().getLogger().info("OminousBottle: " + HAS_OMINOUS_BOTTLE);
            Shop.getPlugin().getLogger().info("MusicInstrumentMeta: " + HAS_MUSIC_INSTRUMENT_META);
            Shop.getPlugin().getLogger().info("Light Block: " + HAS_LIGHT_BLOCK);
            Shop.getPlugin().getLogger().info("Glow Item Frame: " + HAS_GLOW_ITEM_FRAME);
            Shop.getPlugin().getLogger().info("Translation Keys: " + HAS_GET_TRANSLATION_KEY);
            Shop.getPlugin().getLogger().info("Potion Base Types: " + HAS_POTION_BASE_TYPE);
            Shop.getPlugin().getLogger().info("=======================================");
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
} 