package com.snowgears.shop.hook;

import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Optional;

public class BluemapHookListener implements Listener {

    private Shop plugin;

    private boolean enabled;
    private String markerIcon;
    private String markerLabel;
    private int markerMinDistance;
    private int markerMaxDistance;


    //docs say to use BlueMapAPI.getInstance() at time of to always have the current valid API instance

    public BluemapHookListener(Shop plugin){
        this.plugin = plugin;
        readConfig();

        //reload the shop plugin after 2 minutes of booting to make all current markers (BlueMap API loads async)
        if(enabled){
            new BukkitRunnable() {
                @Override
                public void run() {
                    BlueMapAPI.getInstance().ifPresent(api -> {
                        Shop.getPlugin().reload();
                    });
                }
            }.runTaskLater(this.plugin, 20*120);
        }
    }

    public void updateMarker(AbstractShop shop){
        if(!enabled)
            return;

        BlueMapAPI.getInstance().ifPresent(api -> {
            final String shopDetails = ShopMessage.formatMessage(markerLabel, shop, null, false);

            MarkerSet markerSet = getMarkerSet(api, shop);
            if(markerSet != null){
                String markerID = UtilMethods.getCleanLocation(shop.getSignLocation(), true);

                //adjust the markers so the icons line up nice with the actual chests on bluemap
                int x = shop.getSignLocation().getBlockX();
                int z = shop.getSignLocation().getBlockZ();
                switch (shop.getFacing()){
                    case NORTH:
                        x++;
                        z++;
                        break;
                    case EAST:
                        z++;
                        break;
                    case WEST:
                        x++;
                        break;
                    default:
                        break;
                }

                POIMarker marker = POIMarker.toBuilder()
                        .label(shopDetails)
                        .icon(markerIcon, 0, 0)
                        .position(x, shop.getSignLocation().getBlockY()+1, z)
                        .minDistance(markerMinDistance)
                        .maxDistance(markerMaxDistance)
                        .build();

                markerSet.getMarkers().put(markerID, marker);
            }
        });
    }

    public void removeMarker(AbstractShop shop){
        if(!enabled)
            return;

        BlueMapAPI.getInstance().ifPresent(api -> {
            MarkerSet markerSet = getMarkerSet(api, shop);
            if(markerSet != null){
                String markerID = UtilMethods.getCleanLocation(shop.getSignLocation(), true);
                markerSet.getMarkers().remove(markerID);
            }
        });
    }

    @EventHandler
    public void onShopDestroy(PlayerDestroyShopEvent event){
        if(!event.isCancelled()){
            removeMarker(event.getShop());
        }
    }



//    private void updateMarkers() {
//        if(!enabled)
//            return;
//        Optional<BlueMapAPI> optionalApi = BlueMapAPI.getInstance();
//
//        try {
//            BlueMapAPI.getInstance().ifPresent(api -> {
//
//                //create a marker set to populate with markers
//                MarkerSet markerSet = MarkerSet.builder()
//                        .label("Shops")
//                        .build();
//
//                //for every shop, create a marker
//                for (UUID shopOwner : plugin.getShopHandler().getShopOwnerUUIDs()) {
//                    for (AbstractShop shop : plugin.getShopHandler().getShops(shopOwner)) {
//                        //create a marker
//                        POIMarker marker = POIMarker.toBuilder()
//                                .label(this.markerLabel)
//                                .position(shop.getSignLocation().getBlockX(), shop.getSignLocation().getBlockY(), shop.getSignLocation().getBlockZ())
//                                .maxDistance(1000)
//                                .build();
//
//                        String markerID = UtilMethods.getCleanLocation(shop.getSignLocation(), true);
//                        markerSet.getMarkers().put(markerID, marker);
//                    }
//                }
//
//
//                api.getWorld(player.getWorld()).ifPresent(world -> {
//                    for (BlueMapMap map : world.getMaps()) {
//                        map.getMarkerSets().put("my-marker-set-id", markerSet);
//                    }
//                });
//            });
//
//        } catch(IOException e){
//            plugin.getLogger().warning("Bluemap marker api not ready, skipping...");
//            return;
//        }
//
////        shopMarkerSet.getMarkers().forEach(GenericMarker::deleteMarker);
////        for(UUID shopOwner : Shop.getPlugin().getShopHandler().getShopOwnerUUIDs()){
////            for (AbstractShop shop : Shop.getPlugin().getShopHandler().getShops(shopOwner)) {
////
////                String preview = markerPreview;
////                preview = ShopMessage.formatMessage(preview, shop, null, false);
////
////                Marker marker = shopMarkerSet.createMarker(UtilMethods.getCleanLocation(shop.getSignLocation(), true),
////                        preview
////                        , shop.getSignLocation().getWorld().getName()
////                        , shop.getSignLocation().getBlockX()
////                        , shop.getSignLocation().getBlockY()
////                        , shop.getSignLocation().getBlockZ()
////                        , api.getMarkerAPI().getMarkerIcon("chest"), false);
////
////                String desc = markerDescription;
////                desc = ShopMessage.formatMessage(desc, shop, null, false);
////
////                marker.setDescription(desc);
////            }
////        }
//    }

//    public void deleteMarkers() {
//        shopMarkerSet.deleteMarkerSet();
//    }

    private MarkerSet getMarkerSet(BlueMapAPI api, AbstractShop shop) {
        String markerSetID = "shop-markers-"+shop.getSignLocation().getWorld().getUID();

        Optional<BlueMapWorld> world = api.getWorld(shop.getSignLocation().getWorld());
        if(world.isPresent()) {
            for (BlueMapMap map : world.get().getMaps()) {
                if (map.getMarkerSets().containsKey(markerSetID)) {
                    MarkerSet markerSet = map.getMarkerSets().get(markerSetID);
                    return markerSet;
                }
            }

            //if there was no markerset returned, create one
            MarkerSet markerSet = MarkerSet.builder()
                    .label("Shops")
                    .build();

            //apply it to all maps
            for (BlueMapMap map : world.get().getMaps()) {
                map.getMarkerSets().put(markerSetID, markerSet);
            }
            return markerSet;
        }
        return null;
    }

    private void readConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("bluemap-marker.enabled");
        markerIcon = config.getString("bluemap-marker.icon");
        markerLabel = config.getString("bluemap-marker.label");
        markerMinDistance = config.getInt("bluemap-marker.minDistance");
        markerMaxDistance = config.getInt("bluemap-marker.maxDistance");
    }
}
