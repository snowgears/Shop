package com.snowgears.shop.util;

import org.bukkit.block.Sign;

/**
 * Legacy implementation of SignUtil for MC 1.16.5 and below.
 * This implementation uses reflection for glowing text compatibility where available.
 */
public class SignUtil {
    
    /**
     * Sets the glowing text property on a sign using reflection.
     * 
     * @param sign The sign to modify
     * @param glowing Whether the text should glow
     * @return true if successfully set, false otherwise
     */
    public static boolean setGlowingText(Sign sign, boolean glowing) {
        if (sign == null) {
            return false;
        }
        
        // Check if glowing text methods are available via CompatibilityUtil
        if (CompatibilityUtil.hasGlowingText()) {
            try {
                // Use reflection to access setGlowingText
                java.lang.reflect.Method setGlowingTextMethod = sign.getClass().getMethod("setGlowingText", boolean.class);
                setGlowingTextMethod.invoke(sign, glowing);
                return true;
            } catch (Exception e) {
                // Reflection failed or method not available
                return false;
            }
        }
        
        // Glowing text not supported in this version
        return false;
    }
    
    /**
     * Gets the glowing text property from a sign using reflection.
     * 
     * @param sign The sign to check
     * @return true if the text is glowing, false otherwise
     */
    public static boolean isGlowingText(Sign sign) {
        if (sign == null) {
            return false;
        }
        
        // Check if glowing text methods are available via CompatibilityUtil
        if (CompatibilityUtil.hasGlowingText()) {
            try {
                // Use reflection to access isGlowingText
                java.lang.reflect.Method isGlowingTextMethod = sign.getClass().getMethod("isGlowingText");
                return (Boolean) isGlowingTextMethod.invoke(sign);
            } catch (Exception e) {
                // Reflection failed or method not available
                return false;
            }
        }
        
        // Glowing text not supported in this version
        return false;
    }
    
    /**
     * Checks if glowing text methods are available in this version using reflection.
     * 
     * @return true if glowing text is supported
     */
    public static boolean isGlowingTextSupported() {
        return CompatibilityUtil.hasGlowingText();
    }
} 