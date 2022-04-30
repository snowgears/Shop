package com.snowgears.shop.hook;

import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class BluemapHookListener implements Listener {

    private Shop plugin;

    private boolean enabled;
    private String markerName;
    private String markerPreview;
    private String markerDescription;

    //this is the section from the config file I will put back once I go add this BlueMap integration
//    # If you use BlueMap, this controls how shops show up as markers #
//            #bluemap-marker:
//            #   enabled: true
//            #   type: "extrude"
//            #   label: "shop"
//            #   detail: "Item: [item](x[item amount])<br />Owner: [owner]<br />Type: [shop type]<br />Price: [price]<br />Stock: [stock]<br />---------------<br />[location]<br />"
//            #   shapeMinY: 66
//            #   shapeMaxY: 88
//            #   depthTest: true
//            #   lineWidth: 3
//            #   lineColor: '{ "r": 0, "g": 255, "b": 0, "a": 1 }'
//            #   fillColor: '{ "r": 0, "g": 200, "b": 0, "a": 0.2 }'
//            #   minDistance: 10.0
//            #   maxDistance: 500.0

    //docs say to use BlueMapAPI.getInstance() at time of to always have the current valid API instance

    public BluemapHookListener(Shop plugin){
        this.plugin = plugin;
        readConfig();

        new BukkitRunnable() {
            @Override
            public void run() {
                updateMarkers();
            }
        }.runTaskTimer(plugin, 1, 20 * 120 * 60);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShopRemoved(PlayerDestroyShopEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, this::updateMarkers);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShopCreated(PlayerInitializeShopEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, this::updateMarkers);
    }

    private MarkerSet getMarkerSet(MarkerAPI markerApi){
        try{
            markerApi.load();
            Optional<MarkerSet> optionalMarkerSet = markerApi.getMarkerSet("shop");
            MarkerSet markerSet;
            if (optionalMarkerSet.isPresent()) {
                markerSet = optionalMarkerSet.get();
            } else {
                markerSet = markerApi.createMarkerSet("shop");
            }
            return markerSet;
        } catch(IOException e){
            plugin.getLogger().warning("Bluemap marker api not ready, skipping...");
        }
        return null;
    }

    private void updateMarkers() {
        if(!enabled)
            return;
        Optional<BlueMapAPI> optionalApi = BlueMapAPI.getInstance();

        try {
            if (optionalApi.isPresent()) {
                MarkerAPI markerApi = optionalApi.get().getMarkerAPI();
                MarkerSet markerSet = this.getMarkerSet(markerApi);
                if(markerSet == null)
                    return;




            }
        } catch(IOException e){
            plugin.getLogger().warning("Bluemap marker api not ready, skipping...");
            return;
        }

//        shopMarkerSet.getMarkers().forEach(GenericMarker::deleteMarker);
//        for(UUID shopOwner : Shop.getPlugin().getShopHandler().getShopOwnerUUIDs()){
//            for (AbstractShop shop : Shop.getPlugin().getShopHandler().getShops(shopOwner)) {
//
//                String preview = markerPreview;
//                preview = ShopMessage.formatMessage(preview, shop, null, false);
//
//                Marker marker = shopMarkerSet.createMarker(UtilMethods.getCleanLocation(shop.getSignLocation(), true),
//                        preview
//                        , shop.getSignLocation().getWorld().getName()
//                        , shop.getSignLocation().getBlockX()
//                        , shop.getSignLocation().getBlockY()
//                        , shop.getSignLocation().getBlockZ()
//                        , api.getMarkerAPI().getMarkerIcon("chest"), false);
//
//                String desc = markerDescription;
//                desc = ShopMessage.formatMessage(desc, shop, null, false);
//
//                marker.setDescription(desc);
//            }
//        }
    }

//    public void deleteMarkers() {
//        shopMarkerSet.deleteMarkerSet();
//    }

    private void readConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("dynmap-marker.enabled");
        markerName = config.getString("dynmap-marker.name");
        markerPreview = config.getString("dynmap-marker.preview");
        markerDescription = config.getString("dynmap-marker.description");
    }
}
