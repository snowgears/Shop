package com.snowgears.shop.util;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/**
 * Legacy implementation of PotionUtil for MC 1.13.2-1.14.
 * This implementation provides fallback behavior for versions with limited potion API support.
 */
public class PotionUtil {
    
    /**
     * Gets the base potion type from PotionMeta using reflection.
     * 
     * @param potionMeta The PotionMeta to examine
     * @return The base potion type, or null if not available
     */
    public static PotionType getBasePotionType(PotionMeta potionMeta) {
        if (potionMeta == null) {
            return null;
        }
        
        // Use reflection to check if modern API is available
        try {
            java.lang.reflect.Method hasBasePotionTypeMethod = potionMeta.getClass().getMethod("hasBasePotionType");
            Boolean hasBasePotionType = (Boolean) hasBasePotionTypeMethod.invoke(potionMeta);
            
            if (hasBasePotionType) {
                java.lang.reflect.Method getBasePotionTypeMethod = potionMeta.getClass().getMethod("getBasePotionType");
                return (PotionType) getBasePotionTypeMethod.invoke(potionMeta);
            }
        } catch (Exception e) {
            // Method doesn't exist in this version, use fallback
        }
        
        // Legacy fallback - return null since base potion types weren't well-supported
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
        if (potionMeta == null) {
            return false;
        }
        
        // Use reflection to check if modern API is available
        try {
            java.lang.reflect.Method hasBasePotionTypeMethod = potionMeta.getClass().getMethod("hasBasePotionType");
            return (Boolean) hasBasePotionTypeMethod.invoke(potionMeta);
        } catch (Exception e) {
            // Method doesn't exist in this version
            return false;
        }
    }
} 