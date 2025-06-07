package com.snowgears.shop.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

/**
 * Legacy implementation of TranslationUtil for MC 1.13.2-1.14.
 * This implementation uses legacy name() methods instead of translation keys.
 */
public class TranslationUtil {
    
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