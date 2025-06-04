package com.snowgears.shop.util;

import org.bukkit.block.Beehive;

/**
 * Modern implementation of BeehiveUtil for MC 1.15+ using direct API calls.
 * This implementation uses the modern Beehive and BeehiveData APIs without reflection.
 */
public class BeehiveUtil {
    
    /**
     * Gets the honey level from a Beehive block using modern API.
     * 
     * @param beehive The Beehive block to examine (as Object for compatibility)
     * @return The honey level (0-5), or 0 if not available
     */
    public static int getHoneyLevel(Object beehive) {
        if (beehive == null || !(beehive instanceof Beehive)) {
            return 0;
        }
        
        Beehive beehiveBlock = (Beehive) beehive;
        
        // Modern API - direct call, no reflection needed
        if (beehiveBlock.getBlockData() instanceof org.bukkit.block.data.type.Beehive) {
            org.bukkit.block.data.type.Beehive beehiveData = (org.bukkit.block.data.type.Beehive) beehiveBlock.getBlockData();
            return beehiveData.getHoneyLevel();
        }
        
        return 0;
    }
    
    /**
     * Gets the bee count from a Beehive block using modern API.
     * 
     * @param beehive The Beehive block to examine (as Object for compatibility)
     * @return The number of bees in the hive
     */
    public static int getBeeCount(Object beehive) {
        if (beehive == null || !(beehive instanceof Beehive)) {
            return 0;
        }
        
        Beehive beehiveBlock = (Beehive) beehive;
        
        // Modern API - direct call, no reflection needed
        return beehiveBlock.getEntityCount();
    }
    
    /**
     * Formats beehive information into a readable string.
     * 
     * @param beehive The Beehive block to format (as Object for compatibility)
     * @return Formatted string with honey and bee information, or empty if no info
     */
    public static String formatBeehiveInfo(Object beehive) {
        if (beehive == null || !(beehive instanceof Beehive)) {
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
        return true; // Always true in modern versions
    }
} 