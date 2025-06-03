package com.snowgears.shop.util;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.inventory.ItemStack;

/**
 * Interface for handling version-specific item display functionality.
 * This allows us to have clean separation between modern and legacy implementations
 * while keeping the CompatibilityUtil for runtime feature detection.
 */
public interface ItemDisplayHandler {
    
    /**
     * Gets armor-specific display information (trims, etc.)
     * @param item The item to analyze
     * @param component The component to add armor info to
     */
    void addArmorDisplayInfo(ItemStack item, TextComponent component);
    
    /**
     * Gets potion-specific display information
     * @param item The item to analyze  
     * @param component The component to add potion info to
     */
    void addPotionDisplayInfo(ItemStack item, TextComponent component);
    
    /**
     * Gets enchantment display information with proper translation support
     * @param item The item to analyze
     * @param component The component to add enchantment info to
     */
    void addEnchantmentDisplayInfo(ItemStack item, TextComponent component);
    
    /**
     * Gets ominous bottle display information (Bad Omen level)
     * @param item The item to analyze
     * @param component The component to add ominous bottle info to
     */
    void addOminousBottleDisplayInfo(ItemStack item, TextComponent component);
    
    /**
     * Gets music instrument display information (goat horns)
     * @param item The item to analyze
     * @param component The component to add instrument info to
     */
    void addMusicInstrumentDisplayInfo(ItemStack item, TextComponent component);
} 