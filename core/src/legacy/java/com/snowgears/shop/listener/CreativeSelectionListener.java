package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Legacy-compatible creative selection listener that provides no-op functionality.
 * This version removes all modern creative selection dependencies for Java 8 compatibility.
 */
public class CreativeSelectionListener implements Listener {

    public CreativeSelectionListener(Shop shop) {
        Shop.getPlugin().getLogger().info("[Legacy] Creative selection features disabled for legacy compatibility");
    }

    /**
     * Legacy no-op: Always returns false since creative selection is disabled
     */
    public boolean isPlayerInCreativeSelection(Player player) {
        return false;
    }

    /**
     * Legacy no-op: Does nothing since creative selection is disabled
     */
    public static void removePlayerFromCreativeSelection(Player player) {
        // No-op in legacy version
    }

    /**
     * Legacy no-op: Does nothing since creative selection is disabled
     */
    public static void putPlayerInCreativeSelection(Player player, Location location, boolean something) {
        // No-op in legacy version
    }

    // No event handlers in legacy version - creative selection features unavailable
} 