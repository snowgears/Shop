package com.snowgears.shop.hook;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import net.alex9849.arm.events.RestoreRegionEvent;
import net.alex9849.arm.regions.Region;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class ARMHookListener implements Listener {

    private Shop plugin;

    public ARMHookListener(Shop instance) {
        plugin = instance;
    }
//
//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    public void onRegionDelete(RemoveRegionEvent event) {
//        plugin.getLogger().log(Level.INFO, "RemoveRegionEvent ARM fired");
//
//        deleteShopsInRegion(event.getRegion(), event.getRegion().getOwner());
//    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegionRestore(RestoreRegionEvent event) {
        deleteAllShopsInRegion(event.getRegion());
    }

    private void deleteAllShopsInRegion(Region region){

        for(UUID shopOwnerUUID : plugin.getShopHandler().getShopOwnerUUIDs()){
            for(AbstractShop shop : plugin.getShopHandler().getShops(shopOwnerUUID)) {
                if (shop != null && shop.getSignLocation() != null && shop.getSignLocation().getWorld().getName().equals(region.getRegionworld().getName())) {
                    if (region.getRegion().contains(shop.getSignLocation().getBlockX(), shop.getSignLocation().getBlockY(), shop.getSignLocation().getBlockZ())) {
                        shop.delete();
                    }
                }
            }
        }
    }
}