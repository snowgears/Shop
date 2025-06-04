package com.snowgears.shop.util;

import org.bukkit.block.Sign;

/**
 * Modern implementation of SignUtil for MC 1.17+ using direct API calls.
 * This implementation uses the modern glowing text methods without reflection.
 */
public class SignUtil {
    
    /**
     * Sets the glowing text property on a sign using modern API.
     * 
     * @param sign The sign to modify
     * @param glowing Whether the text should glow
     * @return true if successfully set, false otherwise
     */
    public static boolean setGlowingText(Sign sign, boolean glowing) {
        if (sign == null) {
            return false;
        }
        
        try {
            // Modern API - direct call, no reflection needed
            sign.setGlowingText(glowing);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the glowing text property from a sign using modern API.
     * 
     * @param sign The sign to check
     * @return true if the text is glowing, false otherwise
     */
    public static boolean isGlowingText(Sign sign) {
        if (sign == null) {
            return false;
        }
        
        try {
            // Modern API - direct call, no reflection needed
            return sign.isGlowingText();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if glowing text methods are available in this version.
     * 
     * @return true if glowing text is supported (MC 1.17+)
     */
    public static boolean isGlowingTextSupported() {
        return true; // Always true in modern versions
    }
} 