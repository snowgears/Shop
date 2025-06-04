package com.snowgears.shop.util;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Legacy implementation of EffectUtil for MC 1.13.2-1.14.
 * This implementation uses reflection for effect translation keys and legacy field access.
 */
public class EffectUtil {
    
    /**
     * Gets the translation key for a potion effect type using reflection fallback.
     * 
     * @param effectType The PotionEffectType to get the translation key for
     * @return The translation key string (or legacy equivalent)
     */
    public static String getEffectTranslationKey(PotionEffectType effectType) {
        if (effectType == null) {
            return "";
        }
        
        // Try modern API using reflection
        try {
            java.lang.reflect.Method getTranslationKeyMethod = effectType.getClass().getMethod("getTranslationKey");
            return (String) getTranslationKeyMethod.invoke(effectType);
        } catch (Exception e) {
            // Fallback to legacy name
            return effectType.getName();
        }
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
        
        // Try to translate, but fallback to formatted name if translation fails
        try {
            return TranslationUtil.translate(getEffectTranslationKey(effectType));
        } catch (Exception e) {
            // Ultimate fallback - just use the effect name formatted nicely
            return UtilMethods.capitalize(effectType.getName().replace("_", " ").toLowerCase());
        }
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
        
        // Use reflection to access static fields safely
        try {
            Object instantHealth = PotionEffectType.class.getField("INSTANT_HEALTH").get(null);
            Object instantDamage = PotionEffectType.class.getField("INSTANT_DAMAGE").get(null);
            return effectType.equals(instantHealth) || effectType.equals(instantDamage);
        } catch (Exception e) {
            // Fallback - check by name
            String effectName = effectType.getName();
            return "INSTANT_HEALTH".equals(effectName) || "INSTANT_DAMAGE".equals(effectName);
        }
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