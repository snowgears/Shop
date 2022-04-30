package com.snowgears.shop.hook;

import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.GenericMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerSet;

import java.io.File;
import java.util.UUID;

public class DynmapHookListener implements Listener {

    private Shop plugin;
    private DynmapAPI api;
    private MarkerSet shopMarkerSet;

    private boolean enabled;
    private String markerName;
    private String markerPreview;
    private String markerDescription;

    public DynmapHookListener(Shop plugin){
        this.plugin = plugin;
        readConfig();
        if(!enabled)
            return;
        try {
            api = (DynmapAPI) plugin.getServer().getPluginManager().getPlugin("dynmap");
            shopMarkerSet = api.getMarkerAPI().getMarkerSet("shop");
            if (shopMarkerSet == null) {
                shopMarkerSet = api.getMarkerAPI().createMarkerSet("shop", markerName, null, false);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    updateMarkers();
                }
            }.runTaskTimer(plugin, 1, 20 * 120 * 60);
        } catch(NullPointerException e){
            enabled = false;
            plugin.getLogger().warning("Dynmap marker api was null. Disabling DynMap integration.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShopRemoved(PlayerDestroyShopEvent event) {
        if(!enabled)
            return;
        plugin.getServer().getScheduler().runTask(plugin, this::updateMarkers);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShopCreated(PlayerInitializeShopEvent event) {
        if(!enabled)
            return;
        plugin.getServer().getScheduler().runTask(plugin, this::updateMarkers);
    }

    private void updateMarkers() {
        if(!enabled)
            return;
        if (api == null || api.getMarkerAPI() == null) {
            plugin.getLogger().warning("Dynmap marker api not ready, skipping...");
            return;
        }
        shopMarkerSet.getMarkers().forEach(GenericMarker::deleteMarker);
        for(UUID shopOwner : Shop.getPlugin().getShopHandler().getShopOwnerUUIDs()){
            for (AbstractShop shop : Shop.getPlugin().getShopHandler().getShops(shopOwner)) {

                String preview = markerPreview;
                preview = ShopMessage.formatMessage(preview, shop, null, false);

                Marker marker = shopMarkerSet.createMarker(UtilMethods.getCleanLocation(shop.getSignLocation(), true),
                        preview
                        , shop.getSignLocation().getWorld().getName()
                        , shop.getSignLocation().getBlockX()
                        , shop.getSignLocation().getBlockY()
                        , shop.getSignLocation().getBlockZ()
                        , api.getMarkerAPI().getMarkerIcon("chest"), false);

                String desc = markerDescription;
                desc = ShopMessage.formatMessage(desc, shop, null, false);

                marker.setDescription(desc);
            }
        }
    }

    public void deleteMarkers() {
        if(!enabled)
            return;
        shopMarkerSet.deleteMarkerSet();
    }

    private void readConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("dynmap-marker.enabled");
        markerName = config.getString("dynmap-marker.name");
        markerPreview = config.getString("dynmap-marker.preview");
        markerDescription = config.getString("dynmap-marker.description");
    }
}
