package com.snowgears.shop.util;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Modern implementation of InventoryUtil for MC 1.17+ using direct API calls.
 * This implementation uses the modern inventory view methods without reflection.
 */
public class InventoryUtil {
    
    /**
     * Gets the title of an inventory view from an InventoryClickEvent using modern API.
     * 
     * @param event The InventoryClickEvent to get the title from
     * @return The inventory title, or empty string if not available
     */
    public static String getInventoryViewTitle(InventoryClickEvent event) {
        if (event == null || event.getView() == null) {
            return "";
        }
        
        try {
            // Modern API - direct call, no reflection needed
            return event.getView().getTitle();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Checks if inventory view title methods are available in this version.
     * 
     * @return true if inventory view title is supported (MC 1.17+)
     */
    public static boolean isInventoryViewTitleSupported() {
        return true; // Always true in modern versions
    }
} 