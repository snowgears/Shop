package com.snowgears.shop.util;

/**
 * Legacy implementation of BeehiveUtil for MC 1.13.2-1.14.
 * This implementation provides fallback behavior for versions without Beehive support.
 */
public class BeehiveUtil {
    
    /**
     * Gets the honey level from a Beehive block using reflection.
     * 
     * @param beehive The Beehive block to examine (as Object to avoid compilation issues)
     * @return The honey level (0-5), or 0 if not available
     */
    public static int getHoneyLevel(Object beehive) {
        if (beehive == null) {
            return 0;
        }
        
        // Use reflection to access BeehiveData safely
        try {
            Class<?> beehiveClass = Class.forName("org.bukkit.block.Beehive");
            if (beehiveClass.isInstance(beehive)) {
                Class<?> beehiveDataClass = Class.forName("org.bukkit.block.data.type.Beehive");
                Object blockData = beehive.getClass().getMethod("getBlockData").invoke(beehive);
                
                if (beehiveDataClass.isInstance(blockData)) {
                    return (Integer) beehiveDataClass.getMethod("getHoneyLevel").invoke(blockData);
                }
            }
        } catch (Exception e) {
            // Beehive not supported in this version
        }
        
        return 0;
    }
    
    /**
     * Gets the bee count from a Beehive block using reflection.
     * 
     * @param beehive The Beehive block to examine (as Object to avoid compilation issues)
     * @return The number of bees in the hive, or 0 if not available
     */
    public static int getBeeCount(Object beehive) {
        if (beehive == null) {
            return 0;
        }
        
        // Use reflection to access Beehive safely
        try {
            Class<?> beehiveClass = Class.forName("org.bukkit.block.Beehive");
            if (beehiveClass.isInstance(beehive)) {
                return (Integer) beehiveClass.getMethod("getEntityCount").invoke(beehive);
            }
        } catch (Exception e) {
            // Beehive not supported in this version
        }
        
        return 0;
    }
    
    /**
     * Formats beehive information into a readable string.
     * 
     * @param beehive The Beehive block to format (as Object to avoid compilation issues)
     * @return Formatted string with honey and bee information, or empty if no info
     */
    public static String formatBeehiveInfo(Object beehive) {
        if (beehive == null) {
            return "";
        }
        
        int honeyLevel = getHoneyLevel(beehive);
        int beeCount = getBeeCount(beehive);
        
        if (honeyLevel > 0 || beeCount > 0) {
            StringBuilder beeInfo = new StringBuilder(" [");
            if (honeyLevel > 0) {
                beeInfo.append("Honey: ").append(honeyLevel).append("/5");
                if (beeCount > 0) {
                    beeInfo.append(", ");
                }
            }
            if (beeCount > 0) {
                beeInfo.append("Bees: ").append(beeCount);
            }
            beeInfo.append("]");
            return beeInfo.toString();
        }
        
        return "";
    }
    
    /**
     * Checks if the Beehive class is available in this Minecraft version.
     * 
     * @return true if Beehive is supported (MC 1.15+)
     */
    public static boolean isBeehiveSupported() {
        try {
            Class.forName("org.bukkit.block.Beehive");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
} 