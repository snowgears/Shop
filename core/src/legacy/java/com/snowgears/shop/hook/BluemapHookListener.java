package com.snowgears.shop.hook;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopHandler;
import com.snowgears.shop.shop.AbstractShop;
import org.bukkit.event.Listener;

/**
 * Legacy-compatible BlueMap hook that provides no-op functionality.
 * This version removes all modern BlueMap dependencies for Java 8 compatibility.
 */
public class BluemapHookListener implements Listener {

    private Shop plugin;

    public BluemapHookListener() {
        Shop.getPlugin().getLogger().info("[Legacy] BlueMap integration disabled for legacy compatibility");
    }

    // Constructor that matches the expected signature
    public BluemapHookListener(Shop plugin) {
        this();
        this.plugin = plugin;
    }

    // No event handlers in legacy version - BlueMap integration unavailable
    
    // Placeholder methods that might be called by other parts of the plugin
    public void addShop(org.bukkit.Location location, String shopName) {
        // No-op in legacy version
    }
    
    public void removeShop(org.bukkit.Location location) {
        // No-op in legacy version  
    }

    // Methods expected by calling code
    public void updateMarker(AbstractShop shop) {
        // No-op in legacy version
    }

    public void reloadMarkers(ShopHandler shopHandler) {
        // No-op in legacy version
    }
} 