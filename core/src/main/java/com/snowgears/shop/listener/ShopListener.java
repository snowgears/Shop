package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayTagOption;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.PlayerExperience;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.WorldGuardHook;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;


public class ShopListener implements Listener {

    private Shop plugin = Shop.getPlugin();
    private HashMap<String, Integer> shopBuildLimits = new HashMap<String, Integer>();

    public ShopListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        if(plugin.usePerms()){
            Player player = event.getPlayer();
            int buildPermissionNumber = -1;
            for(PermissionAttachmentInfo permInfo : player.getEffectivePermissions()){
                if(permInfo.getPermission().contains("shop.buildlimit.")){
                    try {
                        int tempNum = Integer.parseInt(permInfo.getPermission().substring(permInfo.getPermission().lastIndexOf(".") + 1));
                        if(tempNum > buildPermissionNumber)
                            buildPermissionNumber = tempNum;
                    } catch (Exception e) {}
                }
            }
            if(buildPermissionNumber == -1)
                shopBuildLimits.put(player.getName(), 10000);
            else
                shopBuildLimits.put(player.getName(), buildPermissionNumber);
        }
    }

    public int getBuildLimit(Player player){
        if(shopBuildLimits.get(player.getName()) != null)
            return shopBuildLimits.get(player.getName());
        return Integer.MAX_VALUE;
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onDisplayChange(PlayerInteractEvent event){
        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                return; // off hand version, ignore.
            }
        } catch (NoSuchMethodError error) {}

        if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
            if(event.getClickedBlock().getType() == Material.WALL_SIGN){
                AbstractShop shop = plugin.getShopHandler().getShop(event.getClickedBlock().getLocation());
                if (shop == null || !shop.isInitialized())
                    return;
                Player player = event.getPlayer();

                //player clicked another player's shop sign
                if (!shop.getOwnerName().equals(player.getName())) {
                    if(!player.isSneaking())
                        return;

                    //player has permission to change another player's shop display
                    if(player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                        shop.getDisplay().cycleType(player);
                        event.setCancelled(true);
                        return;
                    }
                //player clicked own shop sign
                } else {
                    if(plugin.usePerms() && !player.hasPermission("shop.setdisplay"))
                        return;

                    shop.getDisplay().cycleType(player);
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopOpen(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (plugin.getShopHandler().isChest(event.getClickedBlock())) {
                try {
                    if (event.getHand() == EquipmentSlot.OFF_HAND) {
                        return; // off hand version, ignore.
                    }
                } catch (NoSuchMethodError error) {}

                Player player = event.getPlayer();
                AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getClickedBlock());
                if (shop == null)
                    return;

                boolean canUseShopInRegion = true;
                try {
                    canUseShopInRegion = WorldGuardHook.canUseShop(player, shop.getSignLocation());
                } catch(NoClassDefFoundError e) {}

                //check that player can use the shop if it is in a WorldGuard region
                if(!canUseShopInRegion){
                    String message = ShopMessage.getMessage("interactionIssue", "regionRestriction", null, player);
                    if(message != null && !message.isEmpty())
                        player.sendMessage(message);
                    event.setCancelled(true);
                    return;
                }

                if((!plugin.getShopHandler().isChest(shop.getChestLocation().getBlock())) || !(shop.getSignLocation().getBlock().getType() == Material.WALL_SIGN)){
                    shop.delete();
                    return;
                }

                if(shop.getChestLocation().getBlock().getType() == Material.ENDER_CHEST) {
                    if(player.isSneaking()){
                        shop.printSalesInfo(player);
                        event.setCancelled(true);
                    }
                    return;
                }

                //player is sneaking and clicks a chest of a shop
                if(player.isSneaking()){
                    //don't print sales info and cancel event if player is holding a sign (may be trying to place directly onto chest)
                    if(player.getInventory().getItemInHand().getType() != Material.SIGN) {
                        shop.printSalesInfo(player);
                        event.setCancelled(true);
                        if(plugin.getDisplayTagOption() == DisplayTagOption.RIGHT_CLICK_CHEST){
                            shop.getDisplay().showDisplayTags(player);
                        }
                        return;
                    }
                }
                //non-owner is trying to open shop
                if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
                    if ((plugin.usePerms() && player.hasPermission("shop.operator")) || player.isOp()) {
                        if (shop.isAdmin()) {
                            if (shop.getType() == ShopType.GAMBLE) {
                                //allow gamble shops to be opened by operators
                                return;
                            }
                            event.setCancelled(true);
                            shop.printSalesInfo(player);
                        } else {
                            String message = ShopMessage.getMessage(shop.getType().toString(), "opOpen", shop, player);
                            if(message != null && !message.isEmpty())
                                player.sendMessage(message);

                        }
                    } else {
                        event.setCancelled(true);
                        shop.printSalesInfo(player);
                        if(plugin.getDisplayTagOption() == DisplayTagOption.RIGHT_CLICK_CHEST){
                            shop.getDisplay().showDisplayTags(player);
                        }
                        //player.sendMessage(ChatColor.RED + "You do not have access to open this shop.");
                    }
                }
            }
        }
    }

    //NOT SURE WHY I WAS REFRESHING GAMBLE ITEM ON CLOSE?
//    @EventHandler
//    public void onShopClose(InventoryCloseEvent event) {
//        InventoryHolder holder = event.getInventory().getHolder();
//        if(holder != null && holder instanceof Chest) {
//            Chest chest = (Chest) holder;
//            AbstractShop shop = plugin.getShopHandler().getShopByChest(chest.getBlock());
//            if(shop == null)
//                return;
//            if(shop.getType() == ShopType.GAMBLE){
//                ((GambleShop)shop).shuffleGambleItem();
//            }
//        }
//    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        //save all potential shop blocks (for sake of time during explosion)
        Iterator<Block> blockIterator = event.blockList().iterator();
        AbstractShop shop = null;
        while (blockIterator.hasNext()) {

            Block block = blockIterator.next();
            if (block.getType() == Material.WALL_SIGN) {
                shop = plugin.getShopHandler().getShop(block.getLocation());
            } else if (plugin.getShopHandler().isChest(block)) {
                shop = plugin.getShopHandler().getShopByChest(block);
            }

            if (shop != null) {
                blockIterator.remove();
            }
        }
    }

    //THIS CODE IS BEING TEMPORARILY SUSPENDED
    //it causes a lot of timing issues but may be needed for checking signs falling off of chests
//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void signDetachCheck(BlockPhysicsEvent event) {
//        Block b = event.getBlock();
//        if (b.getBlockData() instanceof WallSign) {
//            if(plugin.getShopHandler() != null) {
//                AbstractShop shop = plugin.getShopHandler().getShop(b.getLocation());
//                if (shop != null) {
//                    event.setCancelled(true);
//                }
//            }
//        }
//    }

    @EventHandler
    public void onShopExpansion(BlockPlaceEvent event) {
        Block b = event.getBlockPlaced();
        Player player = event.getPlayer();

        if(b.getType() == Material.HOPPER){
            AbstractShop shop = plugin.getShopHandler().getShopByChest(b.getRelative(BlockFace.UP));
            if(shop != null){
                if(!player.isOp() && !shop.getOwnerUUID().equals(player.getUniqueId())){
                    event.setCancelled(true);
                }
            }
        }
    }

        //REMOVING AND REPLACING WITH CHECK FOR PLACING HOPPERS (was slowing down servers with many hoppers)
    //prevent hoppers from stealing inventory from shops
//    @EventHandler (priority = EventPriority.HIGHEST)
//    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
//        AbstractShop shop = null;
//        if(event.getSource().getHolder() instanceof Chest){
//            Chest container = (Chest) event.getSource().getHolder();
//            shop = plugin.getShopHandler().getShopByChest(container.getBlock());
//        }
//        else if(event.getSource().getHolder() instanceof DoubleChest){
//            DoubleChest container = (DoubleChest) event.getSource().getHolder();
//            shop = plugin.getShopHandler().getShopByChest(container.getLocation().getBlock());
//        }
//        else if(event.getSource().getHolder() instanceof ShulkerBox){
//            ShulkerBox container = (ShulkerBox) event.getSource().getHolder();
//            shop = plugin.getShopHandler().getShopByChest(container.getBlock());
//        }
//
//        if(shop != null){
//            if(event.getDestination().getType() != InventoryType.PLAYER)
//                event.setCancelled(true);
//        }
//    }


    //===================================================================================//
    //              ENDER CHEST HANDLING EVENTS
    //===================================================================================//

    @EventHandler
    public void onCloseEnderChest(InventoryCloseEvent event){
        if(event.getPlayer() instanceof Player) {
            Player player = (Player)event.getPlayer();
            if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
                if(plugin.useEnderChests()) {
                    plugin.getEnderChestHandler().saveInventory(player, event.getInventory());
                }
            }
        }
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event){
        //delete all shops from players that have not played in X amount of hours (if configured)
        if(plugin.getHoursOfflineToRemoveShops() != 0){
            for(OfflinePlayer offlinePlayer : plugin.getShopHandler().getShopOwners()){
                if(offlinePlayer.getName() != null) {
                    long msSinceLastPlayed = System.currentTimeMillis() - offlinePlayer.getLastPlayed();
                    long hoursSinceLastPlayed = TimeUnit.MILLISECONDS.toHours(msSinceLastPlayed);

                    if (hoursSinceLastPlayed >= plugin.getHoursOfflineToRemoveShops()) {
                        for (AbstractShop shop : plugin.getShopHandler().getShops(offlinePlayer.getUniqueId())) {
                            shop.delete();
                        }
                        plugin.getShopHandler().saveShops(offlinePlayer.getUniqueId());
                    }
                }
            }
        }
        final Player player = event.getPlayer();

        final Inventory inv = plugin.getEnderChestHandler().getInventory(player);

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                if(plugin.getCurrencyType() == CurrencyType.EXPERIENCE) {
                    PlayerExperience exp = PlayerExperience.loadFromFile(player);
                    if(exp != null){
                        exp.apply();
                    }
                }
                if(plugin.useEnderChests() && inv != null){
                    player.getEnderChest().setContents(inv.getContents());
                    plugin.getEnderChestHandler().saveInventory(player, inv);
                }

                plugin.getShopHandler().clearShopDisplaysNearPlayer(player);
                plugin.getShopHandler().processShopDisplaysNearPlayer(player);
            }
        }, 2L);
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event){
        if(plugin.getCurrencyType() == CurrencyType.EXPERIENCE) {
            //this automatically saves to file
            new PlayerExperience(event.getPlayer());
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event){
        plugin.getShopHandler().processShopDisplaysNearPlayer(event.getPlayer());
        //if(!event.getTo().getWorld().getUID().equals(event.getFrom().getWorld().getUID())) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    plugin.getShopHandler().processShopDisplaysNearPlayer(event.getPlayer());
                }
            }, 5L);
        //}
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event){
        plugin.getShopHandler().processUnloadedShopsInChunk(event.getChunk());
    }
}