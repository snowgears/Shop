package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.Display;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class ArmorStandListener implements Listener {
    @SuppressWarnings("unused")
	private final Shop plugin;

    public ArmorStandListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (Display.isDisplay(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorStandClick(PlayerInteractEntityEvent event) {
        if (Display.isDisplay(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }
}