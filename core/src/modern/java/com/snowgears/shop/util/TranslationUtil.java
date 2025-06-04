package com.snowgears.shop.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

/**
 * Modern implementation of TranslationUtil for MC 1.14+ using direct API calls.
 * This implementation uses the modern getTranslationKey() API without reflection.
 */
public class TranslationUtil {
    
    /**
     * Gets the translation key for a material using modern API.
     * 
     * @param material The material to get the translation key for
     * @return The translation key string
     */
    public static String getMaterialTranslationKey(Material material) {
        if (material == null) {
            return "";
        }
        
        // Modern API - direct call, no reflection needed
        return material.getTranslationKey();
    }
    
    /**
     * Gets the translation key for an enchantment using modern API.
     * 
     * @param enchantment The enchantment to get the translation key for
     * @return The translation key string
     */
    public static String getEnchantmentTranslationKey(Enchantment enchantment) {
        if (enchantment == null) {
            return "";
        }
        
        // Modern API - direct call, no reflection needed
        return enchantment.getTranslationKey();
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
        
        return translate(getEnchantmentTranslationKey(enchantment));
    }
} 