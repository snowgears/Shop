package com.snowgears.shop.listener;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.SellShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.event.PlayerResizeShopEvent;
import com.snowgears.shop.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;


public class MiscListener implements Listener {

    public Shop plugin = Shop.getPlugin();

    public MiscListener(Shop instance) {
        plugin = instance;
    }

    //prevent emptying of bucket when player clicks on shop sign
    //also prevent when emptying on display item itself
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block b = event.getBlockClicked();

        if (b.getBlockData() instanceof WallSign) {
            AbstractShop shop = plugin.getShopHandler().getShop(b.getLocation());
            if (shop != null)
                event.setCancelled(true);
        }
        Block blockToFill = event.getBlockClicked().getRelative(event.getBlockFace());
        AbstractShop shop = plugin.getShopHandler().getShopByChest(blockToFill.getRelative(BlockFace.DOWN));
        if (shop != null)
            event.setCancelled(true);
    }

    //player places a sign on a chest and creates an initial shop with no item
    //this method calls PlayerCreateShopEvent
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopCreation(SignChangeEvent event) {
        final Block b = event.getBlock();
        final Player player = event.getPlayer();

        if(!(b.getState() instanceof Sign))
            return;

        BlockFace signDirection = null;
        Block chest = null;
        if(b.getBlockData() instanceof WallSign) {
            signDirection = ((WallSign) b.getBlockData()).getFacing();
            chest = b.getRelative(signDirection.getOppositeFace());
        }
        else if(b.getBlockData() instanceof Rotatable){ //regular sign post
            signDirection = ((Rotatable) b.getBlockData()).getRotation();
            //adjust the sign direction to cordinal direction if its not already one
            if( signDirection.toString().indexOf('_') != -1) {
                String adjustedDirString = signDirection.toString().substring(0, signDirection.toString().indexOf('_'));
                signDirection = BlockFace.valueOf(adjustedDirString);
            }
            chest = b.getRelative(signDirection.getOppositeFace());
        }
        else
            return;

        double price = 0;
        double priceCombo = 0;
        int amount;
        ShopType type;
        boolean isAdmin = false;
        if (plugin.getShopHandler().isChest(chest)) {
            final Sign signBlock = (Sign) b.getState();
            if (event.getLine(0).toLowerCase().contains(ShopMessage.getCreationWord("SHOP").toLowerCase())) {

                int numberOfShops = plugin.getShopHandler().getNumberOfShops(player);
                int buildPermissionNumber = plugin.getShopListener().getBuildLimit(player);

                if (plugin.usePerms() && !player.isOp() && !player.hasPermission("shop.operator")) {
                    if (numberOfShops >= buildPermissionNumber) {
                        event.setCancelled(true);
                        AbstractShop tempShop = new SellShop(null, player.getUniqueId(), 0, 0, false);
                        player.sendMessage(ShopMessage.getMessage("permission", "buildLimit", tempShop, player));
                        return;
                    }
                }

                if(plugin.getWorldBlacklist().contains(b.getLocation().getWorld().getName())){
                    if (!(player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator")))) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "worldBlacklist", null, player));
                        event.setCancelled(true);
                        return;
                    }
                }

                boolean canCreateShopInRegion = true;
                try {
                    canCreateShopInRegion = WorldGuardHook.canCreateShop(player, b.getLocation());
                } catch(NoClassDefFoundError e) {}
                
                if(Shop.getPlugin().towny()) {
                	if(!com.palmergames.bukkit.towny.utils.ShopPlotUtil.doesPlayerHaveAbilityToEditShopPlot(player, b.getLocation())) { 
	                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "regionRestriction", null, player));
	                    event.setCancelled(true);
	                    return;
                	}
                }
                if(!canCreateShopInRegion){
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "regionRestriction", null, player));
                    event.setCancelled(true);
                    return;
                }

                try {
                    String line2 = UtilMethods.cleanNumberText(event.getLine(1));
                    amount = Integer.parseInt(line2);
                    if (amount < 1) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line2", null, player));
                        return;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "line2", null, player));
                    return;
                }

                //change default shop type based on permissions
                type = ShopType.SELL;
                if(plugin.usePerms()){
                    if(!player.hasPermission("shop.create.sell")) {
                        type = ShopType.BUY;
                        if(!player.hasPermission("shop.create.buy"))
                            type = ShopType.BARTER;
                    }
                }

                if (event.getLine(3).toLowerCase().contains(ShopMessage.getCreationWord("SELL")))
                    type = ShopType.SELL;
                else if (event.getLine(3).toLowerCase().contains(ShopMessage.getCreationWord("BUY")))
                    type = ShopType.BUY;
                else if (event.getLine(3).toLowerCase().contains(ShopMessage.getCreationWord("BARTER")))
                    type = ShopType.BARTER;
                else if (event.getLine(3).toLowerCase().contains(ShopMessage.getCreationWord("GAMBLE")))
                    type = ShopType.GAMBLE;
                else if (event.getLine(3).toLowerCase().contains(ShopMessage.getCreationWord("COMBO")))
                    type = ShopType.COMBO;

                if(plugin.useVault()){
                    try {
                        String line3 = UtilMethods.cleanNumberText(event.getLine(2));

                        String[] multiplePrices = line3.split(" ");
                        if(multiplePrices.length > 1){
                            if(multiplePrices[0].contains("."))
                                price = Double.parseDouble(multiplePrices[0]);
                            else
                                price = Integer.parseInt(multiplePrices[0]);

                            if(multiplePrices[1].contains("."))
                                priceCombo = Double.parseDouble(multiplePrices[1]);
                            else
                                priceCombo = Integer.parseInt(multiplePrices[1]);
                        }
                        else{
                            if(line3.contains("."))
                                price = Double.parseDouble(line3);
                            else
                                price = Integer.parseInt(line3);
                        }

                    } catch (NumberFormatException e){
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                        return;
                    }
                }
                else{
                    try {
                        String line3 = UtilMethods.cleanNumberText(event.getLine(2));

                        String[] multiplePrices = line3.split(" ");
                        if(multiplePrices.length > 1){
                            price = Integer.parseInt(multiplePrices[0]);
                            priceCombo = Integer.parseInt(multiplePrices[1]);
                        }
                        else{
                            price = Integer.parseInt(line3);
                        }
                    } catch (NumberFormatException e){
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                        return;
                    }
                }
                //only allow price to be zero if the type is selling
                //if (price < 0 || (price == 0 && !(type == ShopType.SELL))) {
                if (price < 0 || (price == 0 && type == ShopType.BARTER)) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                    return;
                }

                String playerMessage = null;
                AbstractShop tempShop = new SellShop(null, player.getUniqueId(), 0, 0, false);

                if (plugin.usePerms()) {
                    if (!(player.hasPermission("shop.create."+type.toString().toLowerCase()) || player.hasPermission("shop.create")))
                        playerMessage = ShopMessage.getMessage("permission", "create", tempShop, player);
                }

                if (type == ShopType.GAMBLE) {
                    isAdmin = true;
                }

                //if players must pay to create shops, check that they have enough money first
                double cost = plugin.getCreationCost();
                if(cost > 0){
                    if(!EconomyUtils.hasSufficientFunds(player, player.getInventory(), cost)){
                        playerMessage = ShopMessage.getMessage("interactionIssue", "createInsufficientFunds", tempShop, player);
                    }
                }

                if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    playerMessage = null;
                }

                //prevent players (even if they are OP) from creating a shop on a double chest with another player
                AbstractShop existingShop = plugin.getShopHandler().getShopByChest(chest);
                if(existingShop != null && !existingShop.isAdmin()){
                    if(!existingShop.getOwnerUUID().equals(player.getUniqueId())){
                        playerMessage = ShopMessage.getMessage("interactionIssue", "createOtherPlayer", null, player);
                    }
                }

                if (playerMessage != null) {
                    player.sendMessage(playerMessage);
                    event.setCancelled(true);
                    return;
                }

                if (event.getLine(3).toLowerCase().contains(ShopMessage.getCreationWord("ADMIN"))) {
                    if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator")))
                        isAdmin = true;
                }

                //make sure that the sign is in front of the chest, unless it is a shulker box
                if(chest.getState() instanceof Directional && chest.getState() instanceof Container) {
                    Directional chestDirectional = (Directional) chest.getState();
                    if (chestDirectional.getFacing() == signDirection && chest.getRelative(signDirection).getLocation().equals(signBlock.getLocation())) {
                        //chest.getRelative(sign.getFacing()).setType(Material.LEGACY_WALL_SIGN);
                    } else {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "direction", null, player));
                        return;
                    }
                } else {
                    existingShop = plugin.getShopHandler().getShopByChest(chest);
                    if(existingShop != null){
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "direction", null, player));
                        return;
                    }
                    else{
                        //chest.getRelative(sign.getFacing()).setType(Material.LEGACY_WALL_SIGN);
                    }
                }

                if (!(b.getBlockData() instanceof WallSign)) {
                    if(!b.getType().toString().contains("_SIGN")){
                        return;
                    }
                    String wallSignString = b.getType().toString().replaceAll("_SIGN", "_WALL_SIGN");
                    b.setType(Material.valueOf(wallSignString));

                    Directional wallSignData = (Directional) b.getBlockData();
                    wallSignData.setFacing(signDirection);
                    b.setBlockData(wallSignData);
                }
                signBlock.update();

                final AbstractShop shop = AbstractShop.create(signBlock.getLocation(), player.getUniqueId(), price, priceCombo, amount, isAdmin, type);

                PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
                plugin.getServer().getPluginManager().callEvent(e);

                if(e.isCancelled())
                    return;

                if(type == ShopType.GAMBLE){
                    shop.setItemStack(plugin.getGambleDisplayItem());
                    shop.setAmount(1);
                    plugin.getShopHandler().addShop(shop);
                    shop.getDisplay().setType(DisplayType.LARGE_ITEM);

                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "create", shop, player));
                    plugin.getTransactionListener().sendEffects(true, player, shop);
                    plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                    return;
                }

                plugin.getShopHandler().addShop(shop);
                shop.updateSign();

                player.sendMessage(ShopMessage.getMessage(type.toString(), "initialize", shop, player));
                if (type == ShopType.BUY && plugin.allowCreativeSelection()){
                    player.sendMessage(ShopMessage.getMessage(type.toString(), "initializeAlt", shop, player));
                }

                //give player a limited amount of time to finish creating the shop until it is deleted
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        //the shop has still not been initialized with an item from a player
                        if (!shop.isInitialized()) {
                            plugin.getShopHandler().removeShop(shop);
                            if (b.getBlockData() instanceof WallSign) {
                                Sign sign = (Sign) b.getState();
                                sign.setLine(0, ChatColor.RED + "SHOP CLOSED");
                                sign.setLine(1, ChatColor.GRAY + "CREATION TIMEOUT");
                                sign.setLine(2, "");
                                sign.setLine(3, "");
                                sign.update(true);
                                plugin.getCreativeSelectionListener().removePlayerData(player);
                            }
                        }
                    }
                }, 1200L); //1 minute
            }
        }
    }

    //this method calls PlayerInitializeShopEvent
    @EventHandler (ignoreCancelled = true)
    public void onPreShopSignClick(PlayerInteractEvent event) {
        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                return; // off hand packet, ignore.
            }
        } catch (NoSuchMethodError error) {}
        final Player player = event.getPlayer();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            final Block clicked = event.getClickedBlock();

            if (clicked.getBlockData() instanceof WallSign) {
                AbstractShop shop = plugin.getShopHandler().getShop(clicked.getLocation());
                if (shop == null) {
                    return;
                } else if (shop.isInitialized()) {
                    return;
                }
                if (!player.getUniqueId().equals(shop.getOwnerUUID())) {
                    //do not allow non operators to initialize other player's shops
                    if((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "initialize", null, player));
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }

                if (player.getItemInHand().getType() == Material.AIR) {
                    return;
                }

                if(Shop.getPlugin().getDisplayType() != DisplayType.NONE) {
                    //make sure there is room above the shop for the display
                    Block aboveShop = shop.getChestLocation().getBlock().getRelative(BlockFace.UP);
                    if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "displayRoom", null, player));
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }

                //if players must pay to create shops, remove money first
                double cost = plugin.getCreationCost();
                if(cost > 0 && !shop.isAdmin()){
                    boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
                    if(!removed){
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "createInsufficientFunds", shop, player));
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }

                ItemStack shopItem = player.getItemInHand();
                if (shop.getItemStack() == null) {

                    PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                    Bukkit.getServer().getPluginManager().callEvent(e);

                    if(e.isCancelled())
                        return;

                    if(shop.getItemStack() == null)
                        shop.setItemStack(shopItem);
                    if (shop.getType() == ShopType.BARTER) {
                        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "initializeInfo", shop, player));
                        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "initializeBarter", shop, player));
                        if(plugin.allowCreativeSelection())
                            player.sendMessage(ShopMessage.getMessage("BUY", "initializeAlt", shop, player));
                    }
                    else {
                        shop.getDisplay().spawn();
                        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "create", shop, player));
                        plugin.getTransactionListener().sendEffects(true, player, shop);
                        plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                    }
                } else if (shop.getSecondaryItemStack() == null) {
                    if (!(InventoryUtils.itemstacksAreSimilar(shop.getItemStack(), shopItem))) {

                        PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                        Bukkit.getServer().getPluginManager().callEvent(e);

                        if(e.isCancelled())
                            return;

                        if(shop.getSecondaryItemStack() == null)
                            shop.setSecondaryItemStack(shopItem);
                        shop.getDisplay().spawn();
                        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "create", shop, player));
                        plugin.getTransactionListener().sendEffects(true, player, shop);
                        plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                    } else {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "sameItem", null, player));
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    //player destroys shop, call PlayerDestroyShopEvent or PlayerResizeShopEvent
    @EventHandler(priority = EventPriority.HIGHEST)
    public void shopDestroy(BlockBreakEvent event) {
        if (event.isCancelled())
            return;

        Block b = event.getBlock();
        Player player = event.getPlayer();

        if (b.getBlockData() instanceof WallSign) {
            AbstractShop shop = plugin.getShopHandler().getShop(b.getLocation());
            if (shop == null)
                return;
            else if (!shop.isInitialized()) {
                event.setCancelled(true);
                return;
            }
            //player trying to break their own shop
            if (shop.getOwnerName().equals(player.getName())) {
                if (plugin.usePerms() && !(player.hasPermission("shop.destroy") || player.hasPermission("shop.operator"))) {
                    event.setCancelled(true);
                    player.sendMessage(ShopMessage.getMessage("permission", "destroy", shop, player));
                    return;
                }

                //if players must pay to create shops, remove money first
                double cost = plugin.getDestructionCost();
                if(cost > 0){
                    boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
                    if(!removed){
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "destroyInsufficientFunds", shop, player));
                        return;
                    }
                }

                PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
                plugin.getServer().getPluginManager().callEvent(e);
                if (e.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }

                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "destroy", shop, player));
                shop.delete();
                plugin.getShopHandler().saveShops(shop.getOwnerUUID());

                return;
            }
            //player trying to break other players shop
            else {
                if (player.isOp() || (plugin.usePerms() && (player.hasPermission("shop.operator") || player.hasPermission("shop.destroy.other")))) {
                    PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
                    plugin.getServer().getPluginManager().callEvent(e);

                    if (e.isCancelled()) {
                        event.setCancelled(true);
                        return;
                    }

                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "opDestroy", shop, player));
                    shop.delete();
                    plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                } else
                    event.setCancelled(true);
            }
        } else if (plugin.getShopHandler().isChest(b)) {

            AbstractShop shop = plugin.getShopHandler().getShopByChest(b);
            if (shop == null)
                return;

            InventoryHolder ih = ((InventoryHolder)b.getState()).getInventory().getHolder();

            if (ih instanceof DoubleChest) {
                if(shop.getOwnerUUID().equals(player.getUniqueId()) || player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))){

                    //the broken block was the initial chest with the sign
                    if(shop.getChestLocation().equals(b.getLocation())){
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "destroyChest", null, player));
                        event.setCancelled(true);
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                    }
                    else {
                        PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, b.getLocation(), false);
                        Bukkit.getPluginManager().callEvent(e);

                        if(e.isCancelled()){
                            event.setCancelled(true);
                            return;
                        }
                        return;
                    }
                }
                else
                    event.setCancelled(true);
            }
            else{
                if(shop.getOwnerUUID().equals(player.getUniqueId()) || player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "destroyChest", null, player));
                    plugin.getTransactionListener().sendEffects(false, player, shop);
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onShopExpansion(BlockPlaceEvent event) {
        Block b = event.getBlockPlaced();
        Player player = event.getPlayer();

        if (plugin.getShopHandler().isChest(b)) {
            ArrayList<BlockFace> doubleChestFaces = new ArrayList<BlockFace>();
            doubleChestFaces.add(BlockFace.NORTH);
            doubleChestFaces.add(BlockFace.EAST);
            doubleChestFaces.add(BlockFace.SOUTH);
            doubleChestFaces.add(BlockFace.WEST);

            //find out if the player placed a chest next to an already active shop
            AbstractShop shop = plugin.getShopHandler().getShopNearBlock(b);
            if (shop == null || (b.getType() != shop.getChestLocation().getBlock().getType()))
                return;
            else if(b.getType() == Material.ENDER_CHEST)
                return;

            Block shopChestBlock = shop.getChestLocation().getBlock();

            //prevent placing the chest behind current shop (changing its direction)
            if(shopChestBlock.getRelative(shop.getFacing().getOppositeFace()).getLocation().equals(b.getLocation())){
                event.setCancelled(true);
                return;
            }

         //   DirectionalContainer chest = (DirectionalContainer) b.getState().getData();
            BlockFace chestFacing = UtilMethods.getDirectionOfChest(b);

            //prevent placing the chest next to the shop but facing the opposite direction (changing its direction)
            if(chestFacing == shop.getFacing().getOppositeFace()){
                event.setCancelled(true);
                return;
            }

            //owner is trying to
            if (shop.getOwnerName().equals(player.getName())) {
                PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, b.getLocation(), true);
                Bukkit.getPluginManager().callEvent(e);

                if(e.isCancelled()){
                    event.setCancelled(true);
                    return;
                }
                return;
            }
            //other player is trying to
            else {
                if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, b.getLocation(), true);
                    Bukkit.getPluginManager().callEvent(e);

                    if(e.isCancelled()){
                        event.setCancelled(true);
                        return;
                    }

                } else
                    event.setCancelled(true);
            }
        }
    }

//    //allow players to place blocks that are occupied by large item displays
//    @EventHandler
//    public void onBlockPlaceAttempt(PlayerInteractEvent event) {
//        try {
//            if (event.getHand() == EquipmentSlot.OFF_HAND) {
//                return; // off hand packet, ignore.
//            }
//        } catch (NoSuchMethodError error) {}
//
//        if(plugin.getDisplayType() != DisplayType.LARGE_ITEM)
//            return;
//        final Player player = event.getPlayer();
//
//        if (event.isCancelled())
//            return;
//
//        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
//            if(plugin.getShopHandler().isChest(event.getClickedBlock()))
//                return;
//            if(player.getItemInHand().getType().isBlock()){
//                Block toChange = event.getClickedBlock().getRelative(event.getBlockFace());
//                if(toChange.getType() != Material.AIR)
//                    return;
//                BlockFace[] directions = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
//                Block[] checks = {toChange, toChange.getRelative(BlockFace.DOWN)};
//                for(Block check : checks) {
//                    for (BlockFace dir : directions) {
//                        Block b = check.getRelative(dir);
//                        if (plugin.getShopHandler().isChest(b)) {
//                            AbstractShop shop = plugin.getShopHandler().getShopByChest(b);
//                            if (shop != null) {
//                                if (player.getUniqueId().equals(shop.getOwnerUUID()) || player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
//                                    toChange.setType(player.getItemInHand().getType());
//                                    event.setCancelled(true);
//                                    if (player.getGameMode() == GameMode.SURVIVAL) {
//                                        ItemStack hand = player.getItemInHand();
//                                        hand.setAmount(hand.getAmount() - 1);
//                                        if (hand.getAmount() == 0)
//                                            hand.setType(Material.AIR);
//                                        event.getPlayer().setItemInHand(hand);
//                                    }
//                                }
//                                return;
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
}