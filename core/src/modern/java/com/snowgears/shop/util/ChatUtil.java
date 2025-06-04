package com.snowgears.shop.util;

import org.bukkit.entity.Player;
import java.util.List;

/**
 * Modern implementation of ChatUtil for MC 1.17+ using direct API calls.
 * This implementation uses the modern custom chat completions methods without reflection.
 */
public class ChatUtil {
    
    /**
     * Sets custom chat completions for a player using modern API.
     * 
     * @param player The player to set completions for
     * @param completions The list of completions to set
     * @return true if successfully set, false otherwise
     */
    public static boolean setCustomChatCompletions(Player player, List<String> completions) {
        if (player == null || completions == null) {
            return false;
        }
        
        try {
            // Modern API - direct call, no reflection needed
            player.setCustomChatCompletions(completions);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Clears custom chat completions for a player using modern API.
     * 
     * @param player The player to clear completions for
     * @return true if successfully cleared, false otherwise
     */
    public static boolean clearCustomChatCompletions(Player player) {
        if (player == null) {
            return false;
        }
        
        try {
            // Modern API - direct call, no reflection needed
            player.setCustomChatCompletions(null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if custom chat completions methods are available in this version.
     * 
     * @return true if custom chat completions are supported (MC 1.17+)
     */
    public static boolean isCustomChatCompletionsSupported() {
        return true; // Always true in modern versions
    }
} 