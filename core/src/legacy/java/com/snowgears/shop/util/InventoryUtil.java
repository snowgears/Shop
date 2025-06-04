package com.snowgears.shop.util;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Legacy implementation of InventoryUtil for MC 1.16.5 and below, with compatibility support.
 * This implementation uses reflection for inventory view title compatibility across versions.
 */
public class InventoryUtil {
    
    /**
     * Gets the title of an inventory view from an InventoryClickEvent using reflection.
     * Enhanced with feature detection for better compatibility.
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
            // Log only if we expect this to work
            if (CompatibilityUtil.needsInventoryViewReflection()) {
                try {
                    if (com.snowgears.shop.Shop.getPlugin() != null) {
                        com.snowgears.shop.Shop.getPlugin().getShopLogger().warning(
                            "Failed to get inventory view title via reflection on MC " + 
                            CompatibilityUtil.getMinecraftVersion() + ": " + e.getMessage());
                    }
                } catch (Exception logError) {
                    // Ignore logging errors
                }
            }
            return "";
        }
    }
    
    /**
     * Checks if inventory view title methods are available in this version using compatibility detection.
     * 
     * @return true if inventory view title is supported
     */
    public static boolean isInventoryViewTitleSupported() {
        // Use our enhanced compatibility detection
        if (CompatibilityUtil.needsInventoryViewReflection()) {
            return true; // We expect reflection to work
        }
        
        // Fallback to direct testing
        try {
            // Test if InventoryView has getTitle method
            Class.forName("org.bukkit.inventory.InventoryView").getMethod("getTitle");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 