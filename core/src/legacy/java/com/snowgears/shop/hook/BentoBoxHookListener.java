package com.snowgears.shop.hook;

import com.snowgears.shop.Shop;
import org.bukkit.event.Listener;

/**
 * Legacy-compatible BentoBox hook that provides no-op functionality.
 * This version removes all modern BentoBox dependencies for Java 8 compatibility.
 */
public class BentoBoxHookListener implements Listener {

    private Shop plugin;

    public BentoBoxHookListener() {
        Shop.getPlugin().getLogger().info("[Legacy] BentoBox integration disabled for legacy compatibility");
    }

    // Constructor that matches the expected signature
    public BentoBoxHookListener(Shop plugin) {
        this();
        this.plugin = plugin;
    }

    // No event handlers in legacy version - BentoBox integration unavailable
} 