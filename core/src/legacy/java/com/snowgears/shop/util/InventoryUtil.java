package com.snowgears.shop.util;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Legacy implementation of InventoryUtil for MC 1.16.5 and below.
 * This implementation uses reflection for inventory view title compatibility.
 */
public class InventoryUtil {
    
    /**
     * Gets the title of an inventory view from an InventoryClickEvent using reflection.
     * 
     * @param event The InventoryClickEvent to get the title from
     * @return The inventory title, or empty string if not available
     */
    public static String getInventoryViewTitle(InventoryClickEvent event) {
        if (event == null || event.getView() == null) {
            return "";
        }
        
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
     * Checks if inventory view title methods are available in this version using reflection.
     * 
     * @return true if inventory view title is supported
     */
    public static boolean isInventoryViewTitleSupported() {
        try {
            // Test if InventoryView has getTitle method
            Class.forName("org.bukkit.inventory.InventoryView").getMethod("getTitle");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 