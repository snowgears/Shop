package com.snowgears.shop.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

/**
 * Legacy implementation of TranslationUtil for MC 1.13.2-1.14.
 * This implementation uses legacy name() methods instead of translation keys.
 */
public class TranslationUtil {
    
    /**
     * Gets the translation key for a material using reflection fallback.
     * 
     * @param material The material to get the translation key for
     * @return The translation key string (or legacy equivalent)
     */
    public static String getMaterialTranslationKey(Material material) {
        if (material == null) {
            return "";
        }
        
        // Try modern API using reflection
        try {
            java.lang.reflect.Method getTranslationKeyMethod = material.getClass().getMethod("getTranslationKey");
            return (String) getTranslationKeyMethod.invoke(material);
        } catch (Exception e) {
            // Fallback to legacy naming scheme
            return "block.minecraft." + material.name().toLowerCase();
        }
    }
    
    /**
     * Gets the translation key for an enchantment using reflection fallback.
     * 
     * @param enchantment The enchantment to get the translation key for
     * @return The translation key string (or legacy equivalent)
     */
    public static String getEnchantmentTranslationKey(Enchantment enchantment) {
        if (enchantment == null) {
            return "";
        }
        
        // Try modern API using reflection
        try {
            java.lang.reflect.Method getTranslationKeyMethod = enchantment.getClass().getMethod("getTranslationKey");
            return (String) getTranslationKeyMethod.invoke(enchantment);
        } catch (Exception e) {
            // Fallback to legacy name
            return enchantment.getName();
        }
    }
    
    /**
     * Translates a translation key to the actual display text.
     * 
     * @param translationKey The translation key to translate
     * @return The translated text
     */
    public static String translate(String translationKey) {
        if (translationKey == null || translationKey.isEmpty()) {
            return "";
        }
        
        return UtilMethods.translate(translationKey);
    }
    
    /**
     * Gets the translated display name for a material.
     * 
     * @param material The material to get the display name for
     * @return The translated display name
     */
    public static String getMaterialDisplayName(Material material) {
        if (material == null) {
            return "";
        }
        
        return translate(getMaterialTranslationKey(material));
    }
    
    /**
     * Gets the translated display name for an enchantment.
     * 
     * @param enchantment The enchantment to get the display name for
     * @return The translated display name
     */
    public static String getEnchantmentDisplayName(Enchantment enchantment) {
        if (enchantment == null) {
            return "";
        }
        
        // For legacy, we might want to return the raw name instead of trying to translate
        try {
            return translate(getEnchantmentTranslationKey(enchantment));
        } catch (Exception e) {
            // Ultimate fallback - just use the enum name formatted nicely
            return UtilMethods.capitalize(enchantment.getName().replace("_", " ").toLowerCase());
        }
    }
} 