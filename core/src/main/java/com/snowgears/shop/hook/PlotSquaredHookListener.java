package com.snowgears.shop.hook;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import org.bukkit.event.Listener;

import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.events.post.PostPlotDeleteEvent;
import com.plotsquared.core.events.post.PostPlotClearEvent;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.location.Location;

import java.util.HashSet;
import java.util.UUID;
import com.google.common.eventbus.Subscribe;

public class PlotSquaredHookListener implements Listener {

    private Shop plugin;
    private PlotAPI plotAPI;

    public PlotSquaredHookListener(Shop instance) {
        plugin = instance;
        plotAPI = new PlotAPI();
        plotAPI.registerListener(this);
    }

    @Subscribe
    public void onPlotDelete(PostPlotDeleteEvent e) {
        deleteAllShopsInPlot(e.getPlot());
    }
    
    @Subscribe
    public void onPlotClear(PostPlotClearEvent e) {
        deleteAllShopsInPlot(e.getPlot());
    }

    private void deleteAllShopsInPlot(Plot plot){
        HashSet<UUID> shopOwnersToSave = new HashSet<>();
        int shopsDeleted = 0;
        for(UUID shopOwnerUUID : plugin.getShopHandler().getShopOwnerUUIDs()){
            for(AbstractShop shop : plugin.getShopHandler().getShops(shopOwnerUUID)) {
                if (shop != null && shop.getSignLocation() != null && shop.getSignLocation().getWorld().getName().equals(plot.getArea().getWorldName())) {
                    if (shop.getChestLocation() != null) {
                        Location location = Location.at(
                            shop.getChestLocation().getWorld().getName(), 
                            shop.getChestLocation().getBlockX(), 
                            shop.getChestLocation().getBlockY(), 
                            shop.getChestLocation().getBlockZ()
                        );
                        if (plot.getArea().contains(location)) {
                            plugin.getShopLogger().notice("Deleting Shop because PlotSquared Plot is being reset! " + shop);
                            shop.delete();
                            shopOwnersToSave.add(shopOwnerUUID);
                            shopsDeleted++;
                        }
                    }
                }
            }
        }

        for(UUID shopOwner : shopOwnersToSave) {
            plugin.getShopHandler().saveShops(shopOwner, true);
        }

        if (shopsDeleted > 0) {
            plugin.getShopLogger().notice("(PlotSquared Hook) Deleted " + shopsDeleted + " Shops inside Plot `" + plot.getId() + "` during plot reset");
        }
    }
}