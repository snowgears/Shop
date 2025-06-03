package com.snowgears.shop.hook;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import net.alex9849.arm.events.RestoreRegionEvent;
import net.alex9849.arm.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

public class ARMHookListener implements Listener {

    private Shop plugin;
    private Plugin armPlugin;

    public ARMHookListener(Shop instance) {
        plugin = instance;
        armPlugin = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegionRestore(RestoreRegionEvent event) {
        deleteAllShopsInRegion(event.getRegion());
    }

    private void deleteAllShopsInRegion(Region region){
        HashSet<UUID> shopOwnersToSave = new HashSet<>();
        int shopsDeleted = 0;
        for(UUID shopOwnerUUID : plugin.getShopHandler().getShopOwnerUUIDs()){
            for(AbstractShop shop : plugin.getShopHandler().getShops(shopOwnerUUID)) {
                if (shop != null && shop.getSignLocation() != null && shop.getSignLocation().getWorld().getName().equals(region.getRegionworld().getName())) {
                    if (region.getRegion().contains(shop.getSignLocation().getBlockX(), shop.getSignLocation().getBlockY(), shop.getSignLocation().getBlockZ())) {
                        plugin.getShopLogger().notice("Deleting Shop because ARM region is being restored! " + shop);
                        shop.delete();
                        shopOwnersToSave.add(shopOwnerUUID);
                        shopsDeleted++;
                    }
                }
            }
        }

        for(UUID shopOwner : shopOwnersToSave) {
            plugin.getShopHandler().saveShops(shopOwner, true);
        }

        if (shopsDeleted > 0) {
            plugin.getShopLogger().notice("(ARM Hook) Deleted " + shopsDeleted + " Shops inside ARM Region `" + region.getRegion().getId() + "` during region restore");
        }
    }
}