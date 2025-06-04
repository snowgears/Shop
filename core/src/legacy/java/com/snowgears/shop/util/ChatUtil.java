package com.snowgears.shop.util;

import org.bukkit.entity.Player;
import java.util.List;

/**
 * Legacy implementation of ChatUtil for MC 1.16.5 and below.
 * This implementation uses reflection for custom chat completions compatibility where available.
 */
public class ChatUtil {
    
    /**
     * Sets custom chat completions for a player using reflection.
     * 
     * @param player The player to set completions for
     * @param completions The list of completions to set
     * @return true if successfully set, false otherwise
     */
    public static boolean setCustomChatCompletions(Player player, List<String> completions) {
        if (player == null || completions == null) {
            return false;
        }
        
        // Check if custom chat completions are available via CompatibilityUtil
        if (CompatibilityUtil.hasCustomChatCompletions()) {
            try {
                // Use reflection to access setCustomChatCompletions
                java.lang.reflect.Method setChatCompletionsMethod = 
                    player.getClass().getMethod("setCustomChatCompletions", java.util.List.class);
                setChatCompletionsMethod.invoke(player, completions);
                return true;
            } catch (Exception e) {
                // Reflection failed or method not available
                return false;
            }
        }
        
        // Custom chat completions not supported in this version
        return false;
    }
    
    /**
     * Clears custom chat completions for a player using reflection.
     * 
     * @param player The player to clear completions for
     * @return true if successfully cleared, false otherwise
     */
    public static boolean clearCustomChatCompletions(Player player) {
        if (player == null) {
            return false;
        }
        
        // Check if custom chat completions are available via CompatibilityUtil
        if (CompatibilityUtil.hasCustomChatCompletions()) {
            try {
                // Use reflection to access setCustomChatCompletions
                java.lang.reflect.Method setChatCompletionsMethod = 
                    player.getClass().getMethod("setCustomChatCompletions", java.util.List.class);
                setChatCompletionsMethod.invoke(player, (List<String>) null);
                return true;
            } catch (Exception e) {
                // Reflection failed or method not available
                return false;
            }
        }
        
        // Custom chat completions not supported in this version
        return false;
    }
    
    /**
     * Checks if custom chat completions methods are available in this version using reflection.
     * 
     * @return true if custom chat completions are supported
     */
    public static boolean isCustomChatCompletionsSupported() {
        return CompatibilityUtil.hasCustomChatCompletions();
    }
} 