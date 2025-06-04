package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayTagOption;
import com.snowgears.shop.hook.WorldGuardHook;
import com.snowgears.shop.listener.CreativeSelectionListener;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class ShopListener implements Listener {

    private Shop plugin;
    private HashMap<UUID, Integer> shopBuildLimits = new HashMap<UUID, Integer>();
    private HashMap<UUID, OfflineTransactions> transactionsWhileOffline = new HashMap<>();
    private HashMap<UUID, Long> playerLastShopTeleport = new HashMap<>();

    public ShopListener(Shop instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        plugin.getFoliaLib().getScheduler().runLater(() -> {
            recalculateShopPerms(event.getPlayer());
        }, 5);
    }

    public void recalculateShopPerms(Player player){
        int buildPermissionNumber = -1;
        if(plugin.usePerms()){
            Set<PermissionAttachmentInfo> permissions = player.getEffectivePermissions();
            //calculate base buildlimit permission first (highest number)
            for(PermissionAttachmentInfo permInfo : permissions){
                if(permInfo.getPermission().contains("shop.buildlimit.")){
                    try {
                        int tempNum = Integer.parseInt(permInfo.getPermission().substring(permInfo.getPermission().lastIndexOf(".") + 1));
                        if(tempNum > buildPermissionNumber) {
                            buildPermissionNumber = tempNum;
                        }
                    } catch (NumberFormatException e) {}
                }
            }
        }
        // Check if we had a matching permission
        if(buildPermissionNumber == -1) {
            shopBuildLimits.put(player.getUniqueId(), 10000);
        } else {
            shopBuildLimits.put(player.getUniqueId(), buildPermissionNumber);
        }
    }

    public int getBuildLimit(Player player){
        boolean hasCachedBuildLimit = shopBuildLimits.containsKey(player.getUniqueId());
        // Check if we need to calculate for the first time
        // or if the players permissions have changed
        if (!hasCachedBuildLimit) {
            recalculateShopPerms(player);
            return shopBuildLimits.get(player.getUniqueId());
        }
        int cachedBuildLimit = shopBuildLimits.get(player.getUniqueId());
        if (!player.hasPermission("shop.buildlimit." + cachedBuildLimit)) {
            recalculateShopPerms(player);
            return shopBuildLimits.get(player.getUniqueId());
        }
        // We have the latest build limit permissions for the user
        return cachedBuildLimit;
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOW)
    public void onShopSignClick(PlayerInteractEvent event) {
        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                return; // off hand version, ignore.
            }
        } catch (NoSuchMethodError error) {}
        Player player = event.getPlayer();

        //player clicked the sign of a shop
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (CompatibilityUtil.isWallSignBlock(event.getClickedBlock())) {
                AbstractShop shop = plugin.getShopHandler().getShop(event.getClickedBlock().getLocation());
                if (shop == null || !shop.isInitialized())
                    return;

                boolean actionPerformed;
                if(player.isSneaking()) {
                    if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
                        actionPerformed = shop.executeClickAction(event, ShopClickType.SHIFT_RIGHT_CLICK_SIGN);
                    else
                        actionPerformed = shop.executeClickAction(event, ShopClickType.SHIFT_LEFT_CLICK_SIGN);
                }
                else{
                    if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
                        actionPerformed = shop.executeClickAction(event, ShopClickType.RIGHT_CLICK_SIGN);
                    else
                        actionPerformed = shop.executeClickAction(event, ShopClickType.LEFT_CLICK_SIGN);
                }
                if (actionPerformed)
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopChestClick(PlayerInteractEvent event) {
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
                    ShopMessage.sendMessage("interactionIssue", "regionRestriction", player, null);
                    event.setCancelled(true);
                    return;
                }

                if((!plugin.getShopHandler().isChest(shop.getChestLocation().getBlock())) || !CompatibilityUtil.isWallSignBlock(shop.getSignLocation().getBlock())){
                    plugin.getShopLogger().warning("Deleting Shop because chest does not exist, or sign is not exist! " + shop);
                    shop.delete();
                    return;
                }

                //TODO maybe put this back in to the AbstractShop action method
//                if(shop.getChestLocation().getBlock().getType() == Material.ENDER_CHEST) {
//                    if(player.isSneaking()){
//                        shop.printSalesInfo(player);
//                        event.setCancelled(true);
//                    }
//                    return;
//                }

                //player is sneaking and clicks a chest of a shop
                if(player.isSneaking()){
                    //don't execute the action and cancel event if player is holding a sign (may be trying to place directly onto chest)
                    if(!Tag.SIGNS.isTagged(player.getInventory().getItemInMainHand().getType())) {

                        boolean actionPerformed = shop.executeClickAction(event, ShopClickType.SHIFT_RIGHT_CLICK_CHEST);

                        if(plugin.getDisplayTagOption() == DisplayTagOption.RIGHT_CLICK_CHEST){
                            shop.getDisplay().showDisplayTags(player);
                        }

                        if (actionPerformed) {
                            event.setCancelled(true);
                            // Stop processing since we cancelled the event, if no action was performed, continue with logic below
                            return;
                        }
                    }
                }
                //non-owner is trying to open shop
                if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
                    if ((plugin.usePerms() && player.hasPermission("shop.operator")) || (!plugin.usePerms() && player.isOp())) {
                        if (shop.isAdmin()) {
                            if (shop.getType() == ShopType.GAMBLE) {
                                //allow gamble shops to be opened by operators
                                return;
                            }
                            event.setCancelled(true);

                            shop.executeClickAction(event, ShopClickType.RIGHT_CLICK_CHEST);
                            //we are cancelling this event regardless so no need to check if the action was performed

                        } else {
                            ShopMessage.sendMessage(shop.getType().toString(), "opOpen", player, shop);
                        }
                    } else {
                        event.setCancelled(true);

                        shop.executeClickAction(event, ShopClickType.RIGHT_CLICK_CHEST);
                        //we are cancelling this event regardless so no need to check if the action was performed

                        if(plugin.getDisplayTagOption() == DisplayTagOption.RIGHT_CLICK_CHEST){
                            shop.getDisplay().showDisplayTags(player);
                        }
                    }
                }
            }
        }
        else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (plugin.getShopHandler().isChest(event.getClickedBlock())) {
                try {
                    if (event.getHand() == EquipmentSlot.OFF_HAND) {
                        return; // off hand version, ignore.
                    }
                } catch (NoSuchMethodError error) {
                }

                Player player = event.getPlayer();
                AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getClickedBlock());
                if (shop == null)
                    return;

                boolean actionPerformed;
                if (player.isSneaking()) {
                    actionPerformed = shop.executeClickAction(event, ShopClickType.SHIFT_LEFT_CLICK_CHEST);
                } else {
                    actionPerformed = shop.executeClickAction(event, ShopClickType.LEFT_CLICK_CHEST);
                }
                if (actionPerformed)
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        //save all potential shop blocks (for sake of time during explosion)
        Iterator<Block> blockIterator = event.blockList().iterator();
        AbstractShop shop = null;
        while (blockIterator.hasNext()) {

            Block block = blockIterator.next();
            if (Tag.WALL_SIGNS.isTagged(block.getType())) {
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

//    @EventHandler
//    public void onCloseEnderChest(InventoryCloseEvent event){
//        if(event.getPlayer() instanceof Player) {
//            Player player = (Player)event.getPlayer();
//            if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
//                if(plugin.useEnderChests()) {
//                    plugin.getEnderChestHandler().saveInventory(player, event.getInventory());
//                }
//            }
//        }
//    }

    @EventHandler
    public void onLogin(PlayerJoinEvent event){
        //delete all shops from players that have not played in X amount of hours (if configured)
        if(plugin.getHoursOfflineToRemoveShops() != 0){
            for(OfflinePlayer offlinePlayer : plugin.getShopHandler().getShopOwners()){
                if(offlinePlayer.getName() != null) {
                    long msSinceLastPlayed = System.currentTimeMillis() - offlinePlayer.getLastPlayed();
                    long hoursSinceLastPlayed = TimeUnit.MILLISECONDS.toHours(msSinceLastPlayed);

                    if (hoursSinceLastPlayed >= plugin.getHoursOfflineToRemoveShops()) {
                        boolean deletedShop = false;
                        for (AbstractShop shop : plugin.getShopHandler().getShops(offlinePlayer.getUniqueId())) {
                            plugin.getShopLogger().notice("Deleting Shop because player " + offlinePlayer.getName() + " has not logged in within the required " + (int) hoursSinceLastPlayed + " hours! " + shop);
                            shop.delete();
                            deletedShop = true;
                        }
                        if (deletedShop) { plugin.getShopHandler().saveShops(offlinePlayer.getUniqueId(), true); }
                    }
                }
            }
        }
        final Player player = event.getPlayer();

        //final Inventory inv = plugin.getEnderChestHandler().getInventory(player);

        plugin.getFoliaLib().getScheduler().runLater(() -> {
            if(plugin.getCurrencyType() == CurrencyType.EXPERIENCE) {
                PlayerExperience exp = PlayerExperience.loadFromFile(player);
                if(exp != null){
                    exp.apply();
                }
            }
//                if(plugin.useEnderChests() && inv != null){
//                    player.getEnderChest().setContents(inv.getContents());
//                    plugin.getEnderChestHandler().saveInventory(player, inv);
//                }

            plugin.getShopHandler().clearShopDisplaysNearPlayer(player);
            // Force process shop displays on login - ignore movement threshold
            plugin.getShopHandler().forceProcessShopDisplaysNearPlayer(player);
        }, 20);


        //setup a repeating task that checks if async sql calculations are still running, if they are done, send messages and cancel task
        OfflineTransactions offlineTransactions = transactionsWhileOffline.get(player.getUniqueId());
        if(offlineTransactions != null) {
            BukkitRunnable runnable = new BukkitRunnable() {
                public void run() {
                    if (transactionsWhileOffline.containsKey(player.getUniqueId())) {
                        if (offlineTransactions != null && !offlineTransactions.isCalculating()) {
                            //only display the message if some transactions happened while they were offline
                            if(offlineTransactions.getNumTransactions() > 0) {
                                List<String> messageList = ShopMessage.getUnformattedMessageList("offline", "summary");
                                for (String message : messageList) {
                                    ShopMessage.sendMessage(message, player, offlineTransactions);
                                }
                            }
                            transactionsWhileOffline.remove(player.getUniqueId());
                        }
                    }
                }
            };
            WrappedTask task = plugin.getFoliaLib().getScheduler().runTimer(runnable, 1, 20);
            // Let it attempt to run for 5 seconds before cancelling
            plugin.getFoliaLib().getScheduler().runLater(() -> {
                plugin.getFoliaLib().getScheduler().cancelTask(task);
            }, 100);
        }
    }

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event){
        OfflinePlayer player = Bukkit.getOfflinePlayer(event.getUniqueId());
        long lastPlayed = player.getLastPlayed();

        //create an object that will calculate offline transactions (if sql is being used)
        if(plugin.getLogHandler().isEnabled() && plugin.offlinePurchaseNotificationsEnabled()) {
            OfflineTransactions offlineTransactions = new OfflineTransactions(player.getUniqueId(), lastPlayed);
            transactionsWhileOffline.put(event.getUniqueId(), offlineTransactions);
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event){
        Player player = event.getPlayer();
        
        // Clear shop displays and connection cache for this player
        plugin.getShopHandler().clearShopDisplaysNearPlayer(player);
        
        if(plugin.getCurrencyType() == CurrencyType.EXPERIENCE) {
            //this automatically saves to file
            new PlayerExperience(player);
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event){
        final Player player = event.getPlayer();
        
        // Skip shop display processing if player is in creative selection mode
        CreativeSelectionListener creativeModeListener = plugin.getCreativeSelectionListener();
        if (creativeModeListener != null && creativeModeListener.isPlayerInCreativeSelection(player)) {
            plugin.getShopLogger().debug("Skipping shop display refresh for " + player.getName() + " (in creative selection)");
            return;
        }
        
        // Immediate attempt right after teleport
        plugin.getShopHandler().forceProcessShopDisplaysNearPlayer(player);
        
        // Staggered display updates after teleport
        // First delayed attempt - wait for chunks to load
        plugin.getFoliaLib().getScheduler().runLater(() -> {
            if (player.isOnline()) {
                // Check again inside the delayed task in case player entered selection during the delay
                if (creativeModeListener != null && creativeModeListener.isPlayerInCreativeSelection(player)) {
                    plugin.getShopLogger().debug("Skipping delayed shop display refresh for " + player.getName() + " (in creative selection)");
                    return;
                }
                plugin.getShopLogger().debug("First display refresh for " + player.getName() + " after teleport");
                plugin.getShopHandler().forceProcessShopDisplaysNearPlayer(player);
            }
        }, 5); // 5 ticks (250ms) delay
        
        // Second attempt - for completeness
        plugin.getFoliaLib().getScheduler().runLater(() -> {
            if (player.isOnline()) {
                // Check again inside the delayed task in case player entered selection during the delay
                if (creativeModeListener != null && creativeModeListener.isPlayerInCreativeSelection(player)) {
                    plugin.getShopLogger().debug("Skipping delayed shop display refresh for " + player.getName() + " (in creative selection)");
                    return;
                }
                plugin.getShopLogger().debug("Second display refresh for " + player.getName() + " after teleport");
                plugin.getShopHandler().forceProcessShopDisplaysNearPlayer(player);
            }
        }, 15); // 750ms delay
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event){
        plugin.getShopHandler().processUnloadedShopsInChunk(event.getChunk());
        
        // Also rebuild shop displays for any players near this chunk
        // This ensures displays reappear after chunk unload/load cycles
        plugin.getShopHandler().rebuildDisplaysInChunk(event.getChunk());
    }

    public int getTeleportCooldownRemaining(Player player){
        if(plugin.getTeleportCooldown() <= 0)
            return 0;
        Long lastTeleport = playerLastShopTeleport.get(player.getUniqueId());
        if(lastTeleport != null) {
            long secondsSinceLastTeleport = (System.currentTimeMillis() - lastTeleport) / 1000;
            int secondsLeft = (int)plugin.getTeleportCooldown() - (int)secondsSinceLastTeleport;
            if(secondsLeft <= 0)
                return 0;
            else
                return secondsLeft;
        }
        return 0;
    }

    public void addTeleportCooldown(Player player){
        playerLastShopTeleport.put(player.getUniqueId(), System.currentTimeMillis());
    }
}