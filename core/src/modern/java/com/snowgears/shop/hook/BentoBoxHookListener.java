package com.snowgears.shop.hook;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.BoundingBox;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;

import java.util.UUID;

public class BentoBoxHookListener implements Listener {

    private Shop plugin;

    public BentoBoxHookListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteEvent event) {
        BoundingBox boundingBox = event.getIsland().getBoundingBox();
        World world = event.getIsland().getCenter().getWorld();

        for(UUID shopOwnerUUID : plugin.getShopHandler().getShopOwnerUUIDs()){
            for(AbstractShop shop : plugin.getShopHandler().getShops(shopOwnerUUID)){
                if(shop != null && shop.getSignLocation() != null && shop.getSignLocation().getWorld().getName().equals(world.getName())) {
                    if (boundingBox.contains(shop.getSignLocation().getX(), shop.getSignLocation().getY(), shop.getSignLocation().getZ())){
                        plugin.getShopLogger().notice("Deleting Shop because BentoBox Island is being deleted! " + shop);
                        shop.delete();
                    }
                }
            }
        }
    }
}