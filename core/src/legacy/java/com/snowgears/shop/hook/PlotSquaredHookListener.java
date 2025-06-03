package com.snowgears.shop.hook;

import com.snowgears.shop.Shop;
import org.bukkit.event.Listener;

/**
 * Legacy-compatible PlotSquared hook that provides no-op functionality.
 * This version removes all modern PlotSquared dependencies for Java 8 compatibility.
 */
public class PlotSquaredHookListener implements Listener {

    private Shop plugin;

    public PlotSquaredHookListener() {
        Shop.getPlugin().getLogger().info("[Legacy] PlotSquared integration disabled for legacy compatibility");
    }

    // Constructor that matches the expected signature
    public PlotSquaredHookListener(Shop plugin) {
        this();
        this.plugin = plugin;
    }

    // No event handlers in legacy version - PlotSquared integration unavailable
} 