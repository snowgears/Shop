package com.snowgears.shop.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Modern implementation of ComponentUtil for MC 1.17+ using direct API calls.
 * This implementation uses the modern component string methods without reflection.
 */
public class ComponentUtil {
    
    /**
     * Removes zero damage metadata from an ItemStack using modern component string API.
     * 
     * @param item The ItemStack to process
     * @return ItemStack with zero damage metadata removed, or original if not applicable
     */
    public static ItemStack removeZeroDamageFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return item;
        }
        
        try {
            ItemMeta itemMeta = item.getItemMeta();
            
            // Use modern API - direct call, no reflection needed
            String components = itemMeta.getAsComponentString(); // example: "[minecraft:damage=53]"

            // Remove zero damage entries from the component string
            components = components.replace(",minecraft:damage=0", ""); // Middle of an array
            components = components.replace("minecraft:damage=0,", ""); // Start of an array
            components = components.replace("minecraft:damage=0", ""); // Only object in array

            // Convert it back into an item using modern API
            String itemTypeKey = item.getType().getKey().toString(); // example: "minecraft:diamond_sword"
            String itemAsString = itemTypeKey + components; // results in: "minecraft:diamond_sword[minecraft:damage=53]"
            
            // Create ItemStack from string using modern API
            return Bukkit.getItemFactory().createItemStack(itemAsString);
            
        } catch (Error | Exception e) {
            // If modern methods fail, return original item
            return item;
        }
    }
    
    /**
     * Gets the component string representation of an ItemMeta using modern API.
     * 
     * @param itemMeta The ItemMeta to get components from
     * @return Component string representation, or empty string if not available
     */
    public static String getComponentString(ItemMeta itemMeta) {
        if (itemMeta == null) {
            return "";
        }
        
        try {
            // Modern API - direct call, no reflection needed
            return itemMeta.getAsComponentString();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Creates an ItemStack from a component string using modern API.
     * 
     * @param componentString The component string to create ItemStack from
     * @return Created ItemStack, or null if creation failed
     */
    public static ItemStack createItemStackFromString(String componentString) {
        if (componentString == null || componentString.isEmpty()) {
            return null;
        }
        
        try {
            // Modern API - direct call, no reflection needed
            return Bukkit.getItemFactory().createItemStack(componentString);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Checks if component string methods are available in this version.
     * 
     * @return true if component string methods are supported (MC 1.17+)
     */
    public static boolean isComponentStringSupported() {
        return true; // Always true in modern versions
    }
} 