package com.snowgears.shop.util;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/**
 * Modern implementation of PotionUtil for MC 1.14+ using direct API calls.
 * This implementation uses the modern PotionMeta.getBasePotionType() API without reflection.
 */
public class PotionUtil {
    
    /**
     * Gets the base potion type from PotionMeta using modern API.
     * 
     * @param potionMeta The PotionMeta to examine
     * @return The base potion type, or null if not available
     */
    public static PotionType getBasePotionType(PotionMeta potionMeta) {
        if (potionMeta == null) {
            return null;
        }
        
        // Modern API - direct call, no reflection needed
        if (potionMeta.hasBasePotionType()) {
            return potionMeta.getBasePotionType();
        }
        
        return null;
    }
    
    /**
     * Gets a formatted name for a potion type.
     * 
     * @param potionType The potion type to format
     * @return A formatted, human-readable name
     */
    public static String getFormattedPotionName(PotionType potionType) {
        if (potionType == null) {
            return "";
        }
        
        // Convert LONG_STRENGTH to "Long Strength"
        return UtilMethods.capitalize(potionType.toString().replace("_", " ").toLowerCase());
    }
    
    /**
     * Checks if a PotionMeta has a base potion type available.
     * 
     * @param potionMeta The PotionMeta to check
     * @return true if base potion type is available
     */
    public static boolean hasBasePotionType(PotionMeta potionMeta) {
        return potionMeta != null && potionMeta.hasBasePotionType();
    }
} 