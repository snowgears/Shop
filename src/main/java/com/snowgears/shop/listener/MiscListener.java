package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.event.PlayerResizeShopEvent;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.SellShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
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
import org.bukkit.material.MaterialData;

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

        if (b.getType() == Material.WALL_SIGN) {
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
        final org.bukkit.material.Sign sign = (org.bukkit.material.Sign) b.getState().getData(); //TODO for some reason this has thrown cast errors

        if (sign.isWallSign())
            chest = b.getRelative(sign.getAttachedFace());
        else
            chest = b.getRelative(sign.getFacing().getOppositeFace());

        double price = 0;
        double priceCombo = 0;
        int amount = 0 ;
        ShopType type = null;
        boolean isAdmin = false;
        if (plugin.getShopHandler().isChest(chest)) {
            //System.out.println("Chest can be a shop chest.");
            final Sign signBlock = (Sign) b.getState();
            if (event.getLine(0).toLowerCase().contains(ShopMessage.getCreationWord("SHOP").toLowerCase())) {

                int numberOfShops = plugin.getShopHandler().getNumberOfShops(player);
                int buildPermissionNumber = plugin.getShopListener().getBuildLimit(player);

                if (plugin.usePerms() && !player.isOp() && !player.hasPermission("shop.operator")) {
                    if (numberOfShops >= buildPermissionNumber) {
                        event.setCancelled(true);
                        AbstractShop tempShop = new SellShop(null, player.getUniqueId(), 0, 0, false, signDirection);
                        player.sendMessage(ShopMessage.getMessage("permission", "buildLimit", tempShop, player));
                        return;
                    }
                }

                if (plugin.getWorldBlacklist().contains(b.getLocation().getWorld().getName())) {
                    if (!(player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator")))) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "worldBlacklist", null, player));
                        event.setCancelled(true);
                        return;
                    }
                }

                //do a check for the WorldGuard region (optional hook)
                boolean canCreateShopInRegion = true;
                try {
                    if(plugin.hookWorldGuard()) {
                        canCreateShopInRegion = WorldGuardHook.canCreateShop(player, b.getLocation());
                    }
                } catch (NoClassDefFoundError e) {
                    //tried to hook world guard but it was not registered
                    e.printStackTrace();
                }

                //do a check for the Towny region (optional hook)
                try {
                    if(plugin.hookTowny()) {
                        canCreateShopInRegion = TownyHook.canCreateShop(player, b.getLocation());
                    }
                } catch (NoClassDefFoundError e) {
                    //tried to hook towny but it was not registered
                    e.printStackTrace();
                }

                if (!canCreateShopInRegion) {
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
                if (plugin.usePerms()) {
                    if (!player.hasPermission("shop.create.sell")) {
                        type = ShopType.BUY;
                        if (!player.hasPermission("shop.create.buy"))
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

                if (plugin.getCurrencyType() == CurrencyType.VAULT) {
                    try {
                        int multiplyValue = UtilMethods.getMultiplyValue(event.getLine(2));
                        String line3 = UtilMethods.cleanNumberText(event.getLine(2));

                        String[] multiplePrices = line3.split(" ");
                        if (multiplePrices.length > 1) {
                            if (multiplePrices[0].contains("."))
                                price = Double.parseDouble(multiplePrices[0]);
                            else
                                price = Integer.parseInt(multiplePrices[0]);

                            if (multiplePrices[1].contains("."))
                                priceCombo = Double.parseDouble(multiplePrices[1]);
                            else
                                priceCombo = Integer.parseInt(multiplePrices[1]);
                        } else {
                            if (line3.contains("."))
                                price = Double.parseDouble(line3);
                            else
                                price = Integer.parseInt(line3);
                        }

                        price *= multiplyValue;
                        priceCombo *= multiplyValue;

                    } catch (NumberFormatException e) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                        return;
                    }
                } else {
                    try {
                        String line3 = UtilMethods.cleanNumberText(event.getLine(2));

                        String[] multiplePrices = line3.split(" ");
                        if (multiplePrices.length > 1) {
                            price = Integer.parseInt(multiplePrices[0]);
                            priceCombo = Integer.parseInt(multiplePrices[1]);
                        } else {
                            price = Integer.parseInt(line3);
                        }
                    } catch (NumberFormatException e) {
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
                final AbstractShop shop = AbstractShop.create(signBlock.getLocation(), player.getUniqueId(), price, priceCombo, amount, isAdmin, type, signDirection);

                if (plugin.usePerms()) {
                    if (!(player.hasPermission("shop.create." + type.toString().toLowerCase()) || player.hasPermission("shop.create")))
                        playerMessage = ShopMessage.getMessage("permission", "create", shop, player);
                }

                if (type == ShopType.GAMBLE) {
                    isAdmin = true;
                    shop.setAdmin(true);
                    if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                        playerMessage = ShopMessage.getMessage("permission", "create", shop, player);
                    }
                }

                //if players must pay to create shops, check that they have enough money first
                double cost = plugin.getCreationCost();
                if (cost > 0) {
                    if (!EconomyUtils.hasSufficientFunds(player, player.getInventory(), cost)) {
                        playerMessage = ShopMessage.getMessage("interactionIssue", "createInsufficientFunds", shop, player);
                    }
                }

                if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    playerMessage = null;
                }

                //prevent players (even if they are OP) from creating a shop on a double chest with another player
                AbstractShop existingShop = plugin.getShopHandler().getShopByChest(chest);
                if (existingShop != null && !existingShop.isAdmin()) {
                    if (!existingShop.getOwnerUUID().equals(player.getUniqueId())) {
                        playerMessage = ShopMessage.getMessage("interactionIssue", "createOtherPlayer", null, player);
                    }
                }

                if (playerMessage != null) {
                    if(!playerMessage.isEmpty())
                        player.sendMessage(playerMessage);
                    event.setCancelled(true);
                    return;
                }

                if (event.getLine(3).toLowerCase().contains(ShopMessage.getCreationWord("ADMIN"))) {
                    if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator")))
                        isAdmin = true;
                }

                //removed all the direction checking code. just make sure its a container
                //make sure that the sign is in front of the chest, unless it is a shulker box
                if (chest.getState() instanceof Container || (plugin.useEnderChests() && chest.getType() == Material.ENDER_CHEST)) {
                    //System.out.println("Chest of shop was a container.");
                    existingShop = plugin.getShopHandler().getShopByChest(chest);
                    if (existingShop != null) {
                        //if the block they are adding a sign to is already a shop, do not let them
                        if (chest.getLocation().equals(existingShop.getChestLocation())) {
                            String message = ShopMessage.getMessage("interactionIssue", "createOtherPlayer", null, player);
                            if(message != null && !message.isEmpty())
                                player.sendMessage(message);
                            return;
                        }
                    }


                    if (!(b.getType() == Material.WALL_SIGN)) {
                        if (!b.getType().toString().contains("_SIGN")) {
                            return;
                        }
                        String wallSignString = b.getType().toString().replaceAll("_SIGN", "_WALL_SIGN");
                        b.setType(Material.valueOf(wallSignString));

                        Directional wallSignData = (Directional) b.getState().getData();
                        wallSignData.setFacing(signDirection);
                        //b.setBlockData(wallSignData);
                        b.getState().setData((MaterialData)wallSignData);
                        b.getState().update();
                    }
                    signBlock.update();

                    shop.setAdmin(isAdmin);
                    shop.load();

                    PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
                    plugin.getServer().getPluginManager().callEvent(e);

                    if (e.isCancelled())
                        return;

                    if (type == ShopType.GAMBLE) {
                        shop.setItemStack(plugin.getGambleDisplayItem());
                        shop.setAmount(1);
                        plugin.getShopHandler().addShop(shop);
                        shop.getDisplay().setType(DisplayType.LARGE_ITEM, false);
                        shop.getDisplay().spawn(null);
                        shop.updateSign();

                        String message = ShopMessage.getMessage(shop.getType().toString(), "create", shop, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                        plugin.getTransactionListener().sendEffects(true, player, shop);
                        plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                        return;
                    }

                    plugin.getShopHandler().addShop(shop);
                    shop.updateSign();

                    String message = ShopMessage.getMessage(type.toString(), "initialize", shop, player);
                    if(message != null && !message.isEmpty())
                        player.sendMessage(message);
                    if (type == ShopType.BUY && plugin.allowCreativeSelection()) {
                        message = ShopMessage.getMessage(type.toString(), "initializeAlt", shop, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                    }

                    //give player a limited amount of time to finish creating the shop until it is deleted
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        public void run() {
                            //the shop has still not been initialized with an item from a player
                            if (!shop.isInitialized()) {
                                plugin.getShopHandler().removeShop(shop);
                                if (b.getType() == Material.WALL_SIGN) {
                                    String[] lines = ShopMessage.getTimeoutSignLines(shop);
                                    Sign sign = (Sign) b.getState();
                                    sign.setLine(0, lines[0]);
                                    sign.setLine(1, lines[1]);
                                    sign.setLine(2, lines[2]);
                                    sign.setLine(3, lines[3]);
                                    sign.update(true);
                                    plugin.getCreativeSelectionListener().removePlayerData(player);
                                }
                            }
                        }
                    }, 1200L); //1 minute
                }
            }
        }
    }

    //this method calls PlayerInitializeShopEvent
    @EventHandler
    public void onPreShopSignClick(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }
        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                return; // off hand version, ignore.
            }
        } catch (NoSuchMethodError error) {}
        final Player player = event.getPlayer();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            final Block clicked = event.getClickedBlock();

            if (clicked.getType() == Material.WALL_SIGN) {
                AbstractShop shop = plugin.getShopHandler().getShop(clicked.getLocation());
                if (shop == null) {
                    return;
                } else if (shop.isInitialized()) {
                    return;
                }
                if (!player.getUniqueId().equals(shop.getOwnerUUID())) {
                    //do not allow non operators to initialize other player's shops
                    if((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
                        String message = ShopMessage.getMessage("interactionIssue", "initialize", null, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }

                if (player.getInventory().getItemInHand().getType() == Material.AIR) {
                    return;
                }

                if(Shop.getPlugin().getDisplayType() != DisplayType.NONE) {
                    //make sure there is room above the shop for the display
                    Block aboveShop = shop.getChestLocation().getBlock().getRelative(BlockFace.UP);
                    if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                        if(plugin.forceDisplayToNoneIfBlocked()){
                            shop.getDisplay().setType(DisplayType.NONE, false);
                            shop.getDisplay().spawn(null);
                            shop.updateSign();
                        }
                        else {
                            String message = ShopMessage.getMessage("interactionIssue", "displayRoom", null, player);
                            if(message != null && !message.isEmpty())
                                player.sendMessage(message);
                            plugin.getTransactionListener().sendEffects(false, player, shop);
                            event.setCancelled(true);
                            return;
                        }
                    }
                }

                //if players must pay to create shops, remove money first
                double cost = plugin.getCreationCost();
                if(cost > 0 && !shop.isAdmin()){
                    boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
                    if(!removed){
                        String message = ShopMessage.getMessage("interactionIssue", "createInsufficientFunds", shop, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }

                ItemStack shopItem = player.getInventory().getItemInHand();

                if (shop.getItemStack() == null) {

                    PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                    Bukkit.getServer().getPluginManager().callEvent(e);

                    if(e.isCancelled())
                        return;

                    if(shop.getItemStack() == null)
                        shop.setItemStack(shopItem);
                    if (shop.getType() == ShopType.BARTER) {
                        String message = ShopMessage.getMessage(shop.getType().toString(), "initializeInfo", shop, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                        message = ShopMessage.getMessage(shop.getType().toString(), "initializeBarter", shop, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                        if(plugin.allowCreativeSelection()) {
                            message = ShopMessage.getMessage("BUY", "initializeAlt", shop, player);
                            if(message != null && !message.isEmpty())
                                player.sendMessage(message);
                        }
                    }
                    else {
                        shop.getDisplay().spawn(null);
                        shop.updateSign();
                        String message = ShopMessage.getMessage(shop.getType().toString(), "create", shop, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
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
                        shop.getDisplay().spawn(null);
                        shop.updateSign();
                        String message = ShopMessage.getMessage(shop.getType().toString(), "create", shop, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                        plugin.getTransactionListener().sendEffects(true, player, shop);
                        plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                    } else {
                        String message = ShopMessage.getMessage("interactionIssue", "sameItem", null, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
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
    @EventHandler(priority = EventPriority.HIGH)
    public void shopDestroy(BlockBreakEvent event) {
        //refresh the shop display even if the event was cancelled as sometimes items can bug out and fall through chest
        if (event.isCancelled()) {
            Block b = event.getBlock();
            if (b.getType() == Material.WALL_SIGN) {
                AbstractShop shop = plugin.getShopHandler().getShop(b.getLocation());
                if (shop != null) {
                    if (shop.getDisplay().getType() == DisplayType.ITEM) {
                        shop.getDisplay().spawn(null);
                        shop.updateSign();
                    }
                }
            }
        }

        Block b = event.getBlock();
        Player player = event.getPlayer();

        if (b.getType() == Material.WALL_SIGN) {
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
                    String message = ShopMessage.getMessage("permission", "destroy", shop, player);
                    if(message != null && !message.isEmpty())
                        player.sendMessage(message);
                    return;
                }

                //if players must pay to create shops, remove money first
                double cost = plugin.getDestructionCost();
                if(cost > 0){
                    boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
                    if(!removed){
                        String message = ShopMessage.getMessage("interactionIssue", "destroyInsufficientFunds", shop, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                        return;
                    }
                }

                PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
                plugin.getServer().getPluginManager().callEvent(e);
                if (e.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }
                else{
                    if((!shop.isAdmin()) && plugin.returnCreationCost() && plugin.getCreationCost() > 0) {
                        if (plugin.getCurrencyType() != CurrencyType.ITEM) {
                            EconomyUtils.addFunds(shop.getOwner(),player.getInventory(), plugin.getCreationCost());
                        } else {
                            ItemStack currencyDrop = plugin.getItemCurrency().clone();
                            currencyDrop.setAmount((int) plugin.getCreationCost());
                            shop.getChestLocation().getWorld().dropItemNaturally(shop.getChestLocation(), currencyDrop);
                        }
                    }
                }

                String message = ShopMessage.getMessage(shop.getType().toString(), "destroy", shop, player);
                if(message != null && !message.isEmpty())
                    player.sendMessage(message);
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

                    String message = ShopMessage.getMessage(shop.getType().toString(), "opDestroy", shop, player);
                    if(message != null && !message.isEmpty())
                        player.sendMessage(message);
                    shop.delete();
                    plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                } else
                    event.setCancelled(true);
            }
        } else if (plugin.getShopHandler().isChest(b)) {

            AbstractShop shop = plugin.getShopHandler().getShopByChest(b);
            if (shop == null) {
                return;
            }

            InventoryHolder ih = ((InventoryHolder)b.getState()).getInventory().getHolder();

            if (ih instanceof DoubleChest) {
                if(shop.getOwnerUUID().equals(player.getUniqueId()) || player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))){

                    //the broken block was the initial chest with the sign
                    if(shop.getChestLocation().equals(b.getLocation())){
                        String message = ShopMessage.getMessage("interactionIssue", "destroyChest", null, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
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
                    String message = ShopMessage.getMessage("interactionIssue", "destroyChest", null, player);
                    if(message != null && !message.isEmpty())
                        player.sendMessage(message);
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
            ArrayList<BlockFace> doubleChestFaces = new ArrayList<>();
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

            //owner is trying to
            if (shop.getOwnerUUID().equals(player.getUniqueId())) {
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
//                return; // off hand version, ignore.
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