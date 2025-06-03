package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Legacy-compatible NMS handler that provides no-op functionality.
 * This version removes all modern NMS dependencies for Java 8 compatibility.
 */
public class NMSBullshitHandler {

    private Shop plugin;

    public NMSBullshitHandler() {
        Shop.getPlugin().getShopLogger().info("[Legacy] NMS features disabled for legacy compatibility");
    }

    // Constructor that matches the expected signature
    public NMSBullshitHandler(Shop plugin) {
        this();
        this.plugin = plugin;
    }

    // Placeholder methods that might be called by other parts of the plugin
    public static String getItemName(ItemStack item) {
        // Legacy-safe fallback
        if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }

    // Methods expected by calling code
    public String getServerVersion() {
        return "legacy";
    }

    public Object getPlayerConnection(Player player) {
        // No-op in legacy version
        return null;
    }
    
    // Other NMS methods can be added here as no-ops when needed
} 