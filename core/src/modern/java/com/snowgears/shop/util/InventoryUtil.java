package com.snowgears.shop.util;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Modern implementation of InventoryUtil for MC 1.17+ using direct API calls when possible.
 * This implementation intelligently handles compatibility issues where InventoryView
 * changed from interface to class, using feature detection for maximum compatibility.
 */
public class InventoryUtil {
    
    /**
     * Gets the title of an inventory view from an InventoryClickEvent.
     * Uses reflection when needed based on runtime feature detection.
     * 
     * @param event The InventoryClickEvent to get the title from
     * @return The inventory title, or empty string if not available
     */
    public static String getInventoryViewTitle(InventoryClickEvent event) {
        if (event == null || event.getView() == null) {
            return "";
        }
        
        // Check if we need reflection based on cached feature detection
        if (CompatibilityUtil.needsInventoryViewReflection()) {
            return getInventoryViewTitleReflection(event);
        }
        
        try {
            // Direct API call for versions where it works
            return event.getView().getTitle();
        } catch (Exception e) {
            // Fallback to reflection if direct access fails
            return getInventoryViewTitleReflection(event);
        }
    }
    
    /**
     * Gets the inventory view title using reflection for compatibility.
     * This handles cases where direct API access fails.
     * 
     * @param event The InventoryClickEvent to get the title from
     * @return The inventory title, or empty string if not available
     */
    private static String getInventoryViewTitleReflection(InventoryClickEvent event) {
        try {
            Object view = event.getView();
            // Use reflection to access getTitle method
            java.lang.reflect.Method getTitle = view.getClass().getMethod("getTitle");
            getTitle.setAccessible(true);
            return (String) getTitle.invoke(view);
        } catch (Exception e) {
            // Reflection failed or method not available
            return "";
        }
    }
    
    /**
     * Checks if inventory view title methods are available in this version.
     * 
     * @return true if inventory view title is supported
     */
    public static boolean isInventoryViewTitleSupported() {
        return true; // Always true in modern versions, may use reflection
    }
} 