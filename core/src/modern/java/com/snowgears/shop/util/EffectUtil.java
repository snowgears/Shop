package com.snowgears.shop.util;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Modern implementation of EffectUtil for MC 1.14+ using direct API calls.
 * This implementation uses the modern PotionEffect translation keys without reflection.
 */
public class EffectUtil {
    
    /**
     * Gets the translation key for a potion effect type using modern API.
     * 
     * @param effectType The PotionEffectType to get the translation key for
     * @return The translation key string
     */
    public static String getEffectTranslationKey(PotionEffectType effectType) {
        if (effectType == null) {
            return "";
        }
        
        // Modern API - direct call, no reflection needed
        return effectType.getTranslationKey();
    }
    
    /**
     * Gets the translated display name for a potion effect type.
     * 
     * @param effectType The PotionEffectType to get the display name for
     * @return The translated display name
     */
    public static String getEffectDisplayName(PotionEffectType effectType) {
        if (effectType == null) {
            return "";
        }
        
        return TranslationUtil.translate(getEffectTranslationKey(effectType));
    }
    
    /**
     * Checks if a potion effect is an instant effect (no duration).
     * 
     * @param effectType The PotionEffectType to check
     * @return true if the effect is instant
     */
    public static boolean isInstantEffect(PotionEffectType effectType) {
        if (effectType == null) {
            return false;
        }
        
        // Modern API - direct comparison, no reflection needed
        return effectType.equals(PotionEffectType.INSTANT_HEALTH) || 
               effectType.equals(PotionEffectType.INSTANT_DAMAGE);
    }
    
    /**
     * Checks if a potion effect is an instant effect (no duration).
     * 
     * @param effect The PotionEffect to check
     * @return true if the effect is instant
     */
    public static boolean isInstantEffect(PotionEffect effect) {
        return effect != null && isInstantEffect(effect.getType());
    }
    
    /**
     * Gets the formatted name for a potion effect including level.
     * 
     * @param effect The PotionEffect to format
     * @return Formatted effect name with level
     */
    public static String getFormattedEffectName(PotionEffect effect) {
        if (effect == null) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append(getEffectDisplayName(effect.getType()));
        
        if (effect.getAmplifier() > 0) {
            formatted.append(UtilMethods.formatRomanNumerals(effect.getAmplifier() + 1));
        }
        
        return formatted.toString();
    }
} 