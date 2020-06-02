package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.Display;
import me.minebuilders.clearlag.events.EntityRemoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ClearLaggListener implements Listener {
    @SuppressWarnings("unused")
	private final Shop plugin;

    public ClearLaggListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler
    public void onClearLagg(EntityRemoveEvent event) {
        event.getEntityList().removeIf(Display::isDisplay);
    }
}