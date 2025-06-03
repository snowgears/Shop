package com.snowgears.shop.util;

/**
 * Factory for creating the appropriate ItemDisplayHandler based on runtime compatibility.
 * This combines CompatibilityUtil feature detection with profile-specific implementations.
 */
public class ItemDisplayFactory {
    
    private static ItemDisplayHandler instance;
    
    /**
     * Gets the appropriate ItemDisplayHandler for the current runtime environment.
     * Uses CompatibilityUtil to detect available features and choose the best implementation.
     */
    public static ItemDisplayHandler getInstance() {
        if (instance == null) {
            instance = createHandler();
        }
        return instance;
    }
    
    private static ItemDisplayHandler createHandler() {
        // Use CompatibilityUtil to detect which implementation to use
        // This allows runtime feature detection even when compiled with different profiles
        boolean hasModernFeatures = CompatibilityUtil.hasArmorMeta() || 
                                   CompatibilityUtil.hasOminousBottle() ||
                                   CompatibilityUtil.hasMusicInstrumentMeta();
        
        if (hasModernFeatures) {
            try {
                // Try to load modern implementation first
                Class<?> modernClass = Class.forName("com.snowgears.shop.util.ModernItemDisplayHandler");
                return (ItemDisplayHandler) modernClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // Modern implementation not available, fall back to legacy
            }
        }
        
        try {
            // Load legacy implementation
            Class<?> legacyClass = Class.forName("com.snowgears.shop.util.LegacyItemDisplayHandler");
            return (ItemDisplayHandler) legacyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ItemDisplayHandler", e);
        }
    }
} 