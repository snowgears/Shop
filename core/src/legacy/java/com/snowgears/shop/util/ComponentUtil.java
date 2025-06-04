package com.snowgears.shop.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Legacy implementation of ComponentUtil for MC 1.16.5 and below.
 * This implementation uses reflection for component string compatibility where available.
 */
public class ComponentUtil {
    
    /**
     * Removes zero damage metadata from an ItemStack using reflection-based component string API.
     * 
     * @param item The ItemStack to process
     * @return ItemStack with zero damage metadata removed, or original if not applicable
     */
    public static ItemStack removeZeroDamageFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return item;
        }
        
        // Check if modern component string methods are available via CompatibilityUtil
        if (CompatibilityUtil.hasGetAsComponentString() && CompatibilityUtil.hasCreateItemStackFromString()) {
            try {
                ItemMeta itemMeta = item.getItemMeta();
                
                // Use reflection to access getAsComponentString
                java.lang.reflect.Method getAsComponentStringMethod = itemMeta.getClass().getMethod("getAsComponentString");
                String components = (String) getAsComponentStringMethod.invoke(itemMeta); // example: "[minecraft:damage=53]"

                // Remove zero damage entries from the component string
                components = components.replace(",minecraft:damage=0", ""); // Middle of an array
                components = components.replace("minecraft:damage=0,", ""); // Start of an array
                components = components.replace("minecraft:damage=0", ""); // Only object in array

                // Convert it back into an item using reflection
                String itemTypeKey = item.getType().getKey().toString(); // example: "minecraft:diamond_sword"
                String itemAsString = itemTypeKey + components; // results in: "minecraft:diamond_sword[minecraft:damage=53]"
                
                // Use reflection to access createItemStack
                java.lang.reflect.Method createItemStackMethod = Bukkit.getItemFactory().getClass().getMethod("createItemStack", String.class);
                return (ItemStack) createItemStackMethod.invoke(Bukkit.getItemFactory(), itemAsString);
                
            } catch (Exception e) {
                // If reflection fails, return original item
                return item;
            }
        }
        
        // Component string methods not available, return original item
        return item;
    }
    
    /**
     * Gets the component string representation of an ItemMeta using reflection.
     * 
     * @param itemMeta The ItemMeta to get components from
     * @return Component string representation, or empty string if not available
     */
    public static String getComponentString(ItemMeta itemMeta) {
        if (itemMeta == null) {
            return "";
        }
        
        // Check if modern component string methods are available via CompatibilityUtil
        if (CompatibilityUtil.hasGetAsComponentString()) {
            try {
                // Use reflection to access getAsComponentString
                java.lang.reflect.Method getAsComponentStringMethod = itemMeta.getClass().getMethod("getAsComponentString");
                return (String) getAsComponentStringMethod.invoke(itemMeta);
            } catch (Exception e) {
                return "";
            }
        }
        
        return "";
    }
    
    /**
     * Creates an ItemStack from a component string using reflection.
     * 
     * @param componentString The component string to create ItemStack from
     * @return Created ItemStack, or null if creation failed
     */
    public static ItemStack createItemStackFromString(String componentString) {
        if (componentString == null || componentString.isEmpty()) {
            return null;
        }
        
        // Check if modern component string methods are available via CompatibilityUtil
        if (CompatibilityUtil.hasCreateItemStackFromString()) {
            try {
                // Use reflection to access createItemStack
                java.lang.reflect.Method createItemStackMethod = Bukkit.getItemFactory().getClass().getMethod("createItemStack", String.class);
                return (ItemStack) createItemStackMethod.invoke(Bukkit.getItemFactory(), componentString);
            } catch (Exception e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Checks if component string methods are available in this version using reflection.
     * 
     * @return true if component string methods are supported
     */
    public static boolean isComponentStringSupported() {
        return CompatibilityUtil.hasGetAsComponentString() && CompatibilityUtil.hasCreateItemStackFromString();
    }
} 