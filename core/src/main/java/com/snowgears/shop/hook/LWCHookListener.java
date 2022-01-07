package com.snowgears.shop.hook;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class LWCHookListener implements Listener {

    private Shop plugin;
    private LWC lwc;

    public LWCHookListener(Shop instance) {
        plugin = instance;
        lwc = ((LWCPlugin) plugin.getServer().getPluginManager().getPlugin("LWC")).getLWC();
    }

    //because LWC events do not use the Spigot event system, they apparently cannot be listened for
    //this is dumb, but in order to not hard code all scenarios where an LWCProtectionRegisterEvent could be registered, we are just gonna remove protections on shops when their chests are clicked
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChestClick(PlayerInteractEvent event){
        if(event.getClickedBlock() != null && plugin.getShopHandler().isChest(event.getClickedBlock())){
            AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getClickedBlock());
            if(shop != null){
                Protection protection = lwc.findProtection(event.getClickedBlock());
                if(protection != null) {
                    protection.remove();
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(PlayerCreateShopEvent event){
        if(event.getShop().getChestLocation() != null){
            Protection protection = lwc.findProtection(event.getShop().getChestLocation());
            if(protection != null) {
                //if the owner of the existing LWC protection is NOT the player creating the shop
                if(!event.getPlayer().isOp() && !protection.getBukkitOwner().getUniqueId().equals(event.getPlayer().getUniqueId())) {
                    event.setCancelled(true);
                    String message = ShopMessage.getMessage("interactionIssue", "createOtherPlayer", event.getShop(), event.getPlayer());
                    if(message != null && !message.isEmpty())
                        event.getPlayer().sendMessage(message);
                }
            }
        }
    }

}