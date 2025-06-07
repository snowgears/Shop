package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.event.PlayerResizeShopEvent;
import com.snowgears.shop.hook.WorldGuardHook;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;


public class MiscListener implements Listener {

    public Shop plugin;
    private HashMap<UUID, ShopCreationProcess> playerChatCreationSteps = new HashMap<>();
    private HashMap<UUID, Long> lastChatCreation = new HashMap<>();

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

        if (b.getType().toString().contains("WALL_SIGN")) {
            AbstractShop shop = plugin.getShopHandler().getShop(b.getLocation());
            if (shop != null)
                event.setCancelled(true);
        }
        Block blockToFill = event.getBlockClicked().getRelative(event.getBlockFace());
        AbstractShop shop = plugin.getShopHandler().getShopByChest(blockToFill.getRelative(BlockFace.DOWN));
        if (shop != null)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopCreation(SignChangeEvent event) {
        final Block b = event.getBlock();
        final Player player = event.getPlayer();

        if(!plugin.getAllowCreationMethodSign())
            return;

        if(!(b.getState() instanceof Sign))
            return;

        BlockFace signDirection = null;
        Block chest = null;
        if (!MCVersion.atLeast("1.13")) {
            throw new UnsupportedOperationException("Sign creation is not supported in legacy versions");
        }
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

        int amount = 0 ;
        ShopType type = null;
        boolean isAdmin = false;
        if (plugin.getShopHandler().isChest(chest)) {
            final Sign signBlock = (Sign) b.getState();
            if (event.getLine(0).toLowerCase().contains(ShopMessage.getCreationWord("SHOP").toLowerCase())) {

                if(!plugin.getShopCreationUtil().shopCanBeCreated(player, chest)){
                    cancelShopCreationProcess(player);
                    event.setCancelled(true);
                    return;
                }

                try {
                    String line2 = UtilMethods.cleanNumberText(event.getLine(1));
                    amount = Integer.parseInt(line2);
                    if (amount < 1) {
                        ShopMessage.sendMessage("interactionIssue", "line2", player, null);
                        ShopMessage.sendMessage("interactionIssue", "createCancel", player, null);
                        cancelShopCreationProcess(player);
                        event.setCancelled(true);
                        return;
                    }
                } catch (NumberFormatException e) {
                    ShopMessage.sendMessage("interactionIssue", "line2", player, null);
                    ShopMessage.sendMessage("interactionIssue", "createCancel", player, null);
                    cancelShopCreationProcess(player);
                    event.setCancelled(true);
                    return;
                }

                //change default shop type based on permissions
                //TODO I dont like this. I would rather throw an error for permissions
//                type = ShopType.SELL;
//                if (plugin.usePerms()) {
//                    if (!player.hasPermission("shop.create.sell")) {
//                        type = ShopType.BUY;
//                        if (!player.hasPermission("shop.create.buy"))
//                            type = ShopType.BARTER;
//                    }
//                }

                type = plugin.getShopCreationUtil().getShopType(event.getLine(3));
                isAdmin = plugin.getShopCreationUtil().getShopIsAdmin(event.getLine(3));

                if(type == null)
                    type = ShopType.SELL;

                PricePair pricePair = plugin.getShopCreationUtil().getShopPricePair(player, event.getLine(2), type);
                if(pricePair == null){
                    event.setCancelled(true);
                    return;
                }

                AbstractShop shop = plugin.getShopCreationUtil().createShop(player, chest, signBlock.getBlock(), pricePair, amount, isAdmin, type, signDirection, false);
                if(shop == null) {
                    event.setCancelled(true);
                    return;
                }

                ShopCreationProcess process = new ShopCreationProcess(player, chest, signDirection);
                process.setStep(ShopCreationProcess.ChatCreationStep.SIGN_ITEM);
                playerChatCreationSteps.put(player.getUniqueId(), process);

                process.displayFloatingText(type.toString(), "initialize");
                if (plugin.allowCreativeSelection() && (type == ShopType.BUY || type == ShopType.COMBO)) {
                    ShopMessage.sendMessage(type.toString(), "initializeAlt", player, shop);
                }

                //give player a limited amount of time to finish creating the shop until it is deleted
                plugin.getFoliaLib().getScheduler().runLater(() -> {
                    //the shop has still not been initialized with an item from a player
                    if (!shop.isInitialized()) {
                        plugin.getShopHandler().removeShop(shop);
                        if (b.getBlockData() instanceof WallSign) {
                            String[] lines = ShopMessage.getTimeoutSignLines(shop);
                            Sign sign = (Sign) b.getState();
                            sign.setLine(0, lines[0]);
                            sign.setLine(1, lines[1]);
                            sign.setLine(2, lines[2]);
                            sign.setLine(3, lines[3]);
                            sign.update(true);
                            cancelShopCreationProcess(player);
                        }
                    }
                }, 30 * 20); // 30 seconds * 20 ticks
            }
        }
    }

    public ShopCreationProcess getShopCreationProcess(Player player){
        return playerChatCreationSteps.get(player.getUniqueId());
    }

    public void removeShopCreationProcess(Player player){
        playerChatCreationSteps.remove(player.getUniqueId());
    }

    public void cancelShopCreationProcess(Player player){
        ShopCreationProcess process = this.getShopCreationProcess(player);
        if (process != null) {
            process.display.removeDisplayEntities(player, true);
            playerChatCreationSteps.remove(player.getUniqueId());
            // Send message that the creation was cancelled
            ShopMessage.sendMessage("interactionIssue", "createCancel", player, null);
        }

        // Remove player from creative selection if they are in it!
        // The Bukkit API can only change player game modes in a sync task, not an async task
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                plugin.getCreativeSelectionListener().removePlayerFromCreativeSelection(player);
            }
        }, 1);
    }

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

            if (clicked.getType().toString().contains("WALL_SIGN")) {

                if(!plugin.getAllowCreationMethodSign())
                    return;

                AbstractShop shop = plugin.getShopHandler().getShop(clicked.getLocation());
                if (shop == null) {
                    return;
                } else if (shop.isInitialized()) {
                    return;
                }

                //creative selection listener will handle if item is null
                if(event.getItem() != null && event.getItem().getType() != Material.AIR){

                    boolean initializedShop;
                    if(shop.getType() == ShopType.BARTER && shop.getItemStack() != null && shop.getSecondaryItemStack() == null)
                        initializedShop = plugin.getShopCreationUtil().initializeShop(shop, player, shop.getItemStack(), event.getItem());
                    else
                        initializedShop = plugin.getShopCreationUtil().initializeShop(shop, player, event.getItem(), null);

                    if(initializedShop){
                        plugin.getShopCreationUtil().sendCreationSuccess(player, shop);
                        plugin.getLogHandler().logAction(player, shop, ShopActionType.INIT);
                    }
                }
                event.setCancelled(true); //cancel event regardless
                Shop.getPlugin().getShopLogger().trace("[MiscListener.onPreShopSignClick] updateSign");
                shop.updateSign();
            }
            else if(plugin.getShopHandler().isChest(clicked)){

                if(!plugin.getAllowCreationMethodChest())
                    return;

                //TODO also protect the chest if its in the middle of a chat creation process

                if(event.getItem() == null || event.getItem().getType() == Material.AIR){
                    if(plugin.allowCreativeSelection()) {
                        //TODO this section needs to check if the current step is to get the barter item
                        ShopCreationProcess currentProcess = playerChatCreationSteps.get(player.getUniqueId());
                        // Check if last created process is within 80ms, if so, cancel the event
                        Long lastCreatedProcess = lastChatCreation.get(player.getUniqueId());
                        if (lastCreatedProcess != null && (new Date().getTime() - lastCreatedProcess) < 80) {
                            event.setCancelled(true);
                            return;
                        }
                        if (currentProcess != null && currentProcess.getStep() == ShopCreationProcess.ChatCreationStep.BARTER_ITEM) {   
                            plugin.getCreativeSelectionListener().putPlayerInCreativeSelection(player, clicked.getLocation(), false);
                            event.setCancelled(true);
                            return;
                        }
                        else if (currentProcess == null && player.isSneaking()){
                            // Long lastCreatedProcess = lastChatCreation.get(player.getUniqueId());
                            //if the player has created a new process in the last 5 seconds, block them from creating another
                            if(lastCreatedProcess != null && (new Date().getTime() - lastCreatedProcess) < plugin.getDebug_shopCreateCooldown()) {
                                ShopMessage.sendMessage("interactionIssue", "createCooldown", player, null);
                                event.setCancelled(true);
                                return;
                            }

                            if(!plugin.getShopCreationUtil().shopCanBeCreated(player, clicked)){
                                event.setCancelled(true);
                                return;
                            }
                            BlockFace signFacing = plugin.getShopCreationUtil().calculateBlockFaceForSign(player, clicked, event.getBlockFace());
                            if(signFacing == null) {
                                event.setCancelled(true);
                                return;
                            }

                            ShopCreationProcess process = new ShopCreationProcess(player, clicked, signFacing);
                            playerChatCreationSteps.put(player.getUniqueId(), process);
                            lastChatCreation.put(player.getUniqueId(), new Date().getTime());
                            plugin.getCreativeSelectionListener().putPlayerInCreativeSelection(player, clicked.getLocation(), false);
                            event.setCancelled(true);
                            return;
                        }
                    }
                    else{
                        return;
                    }
                }
                else {
                    ShopCreationProcess currentProcess = playerChatCreationSteps.get(player.getUniqueId());
                    plugin.getShopLogger().debug("Current Shop Creation Process: " + currentProcess);
                    if (currentProcess != null && currentProcess.getStep() == ShopCreationProcess.ChatCreationStep.BARTER_ITEM) {
                        if (!plugin.getShopCreationUtil().itemsCanBeInitialized(player, currentProcess.getItemStack(), event.getItem())) {
                            event.setCancelled(true);
                            return;
                        }
                        currentProcess.setBarterItemStack(event.getItem());
                        currentProcess.displayFloatingText(currentProcess.getShopType().toString(), "createHitChestBarterAmount");
                        event.setCancelled(true);
                        return;
                    }
                }

                if(!player.isSneaking())
                    return;

                Long lastCreatedProcess = lastChatCreation.get(player.getUniqueId());
                if(lastCreatedProcess != null) {
                    //if the player has created a new process in the last 5 seconds, block them from creating another
                    long diff = (new Date().getTime() - lastCreatedProcess);
                    if (diff < plugin.getDebug_shopCreateCooldown()) {
                        ShopMessage.sendMessage("interactionIssue", "createCooldown", player, null);
                        event.setCancelled(true);
                        return;
                    }
                }
                // Cleanup the last process if needed and assume the player wants to start a new process
                ShopCreationProcess lastProcess = playerChatCreationSteps.get(player.getUniqueId());
                if (lastProcess != null) {
                    lastProcess.cleanup(); // removes floating displays
                }

                //dont let players create shops via chest on shops that already exist
                //TODO come back to this and allow players to create double chest shops via chest creation method
                AbstractShop existingShop = plugin.getShopHandler().getShopByChest(clicked);
                if (existingShop != null) {
                    return;
                }

                if(!plugin.getShopCreationUtil().shopCanBeCreated(player, clicked)){
                    event.setCancelled(true);
                    return;
                }

                event.setCancelled(true);
                BlockFace signFacing = plugin.getShopCreationUtil().calculateBlockFaceForSign(player, clicked, event.getBlockFace());
                if(signFacing == null)
                    return;

                //since player is creating a shop via clicking a chest with an item, create a new object to track the steps of that process
                ShopCreationProcess process = new ShopCreationProcess(player, clicked, signFacing);
                process.setItemStack(event.getItem());
                playerChatCreationSteps.put(player.getUniqueId(), process);
                lastChatCreation.put(player.getUniqueId(), new Date().getTime());

                //send player text prompts after they have clicked the chest with the item they want to create a shop with
                ShopMessage.sendMessage("initialCreateInstruction", null, process, player);
                process.displayFloatingText("createHitChest", null);
                List<String> autocomplete = new ArrayList<>();
                Arrays.asList(ShopType.values()).forEach((shopType -> autocomplete.add(shopType.toString().toLowerCase())));
                
                if (MCVersion.atLeast("1.17")) {
                    player.setCustomChatCompletions(autocomplete);
                }
                if((!plugin.usePerms() && player.isOp()) || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    ShopMessage.sendMessage("adminCreateHitChest", null, process, player);
                }

                //give player a limited amount of time to finish creating the shop until it is deleted
                final UUID originalProcessUUID = process.getUniqueID();
                plugin.getFoliaLib().getScheduler().runLater(() -> {
                    //the shop has still not been initialized with an item from a player
                    ShopCreationProcess currentProcess = playerChatCreationSteps.get(player.getUniqueId());
                    if (currentProcess != null && currentProcess.getUniqueID().equals(originalProcessUUID)) {
                        currentProcess.cleanup();
                        playerChatCreationSteps.remove(player.getUniqueId());
                        plugin.getCreativeSelectionListener().removePlayerFromCreativeSelection(player);
                        ShopMessage.sendMessage("interactionIssue", "createHitChestTimeout", currentProcess, player);
                    }
                }, 30 * 20); // 30 seconds * 20 ticks
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        if(playerChatCreationSteps.containsKey(player.getUniqueId())){
            ShopCreationProcess process = playerChatCreationSteps.get(player.getUniqueId());
            plugin.getShopLogger().debug("Shop Creation Process: " + process.getStep() + " Player " + player.getName() + " input: " + event.getMessage(), true);
            switch (process.getStep()){
                case SHOP_TYPE:
                    ShopType type = plugin.getShopCreationUtil().getShopType(event.getMessage());
                    if(type == null){
                        process.cleanup();
                        playerChatCreationSteps.remove(player.getUniqueId());
                        return;
                    }
                    boolean isAdmin = plugin.getShopCreationUtil().getShopIsAdmin(event.getMessage());
                    process.setShopType(type);
                    process.setAdmin(isAdmin);
                    event.setCancelled(true);

                    if(type == ShopType.GAMBLE){ ShopMessage.sendMessage(type.toString(), "createHitChestPrice", process, player); }
                    else {
                        process.displayFloatingText(type.toString(), "createHitChestAmount");
                    }
                    break;
                case ITEM_AMOUNT:
                    int amount = 0;
                    try {
                        String textAmt = UtilMethods.cleanNumberText(event.getMessage());
                        amount = Integer.parseInt(textAmt);
                        if (amount < 1) {
                            ShopMessage.sendMessage("interactionIssue", "line2", player, null);
                            ShopMessage.sendMessage("interactionIssue", "createCancel", player, null);
                            event.setCancelled(true);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        ShopMessage.sendMessage("interactionIssue", "line2", player, null);
                        ShopMessage.sendMessage("interactionIssue", "createCancel", player, null);
                        process.cleanup();
                        //event.setCancelled(true);
                        //instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
                        playerChatCreationSteps.remove(player.getUniqueId());
                        return;
                    }
                    process.setItemAmount(amount);
                    event.setCancelled(true);

                    if(process.getShopType() == ShopType.BARTER){
                        process.displayFloatingText(process.getShopType().toString(), "createHitChest");
                        if (plugin.allowCreativeSelection()) {
                            // ToDo: Allow multiple floating displays, or combine this text, up to you
                            ShopMessage.sendMessage(process.getShopType().toString(), "initializeBarterAlt", player, null);
                        }
                    }
                    else {
                        process.displayFloatingText(process.getShopType().toString(), "createHitChestPrice");
                    }
                    break;
                case ITEM_PRICE:
                    double price = plugin.getShopCreationUtil().getShopPrice(player, event.getMessage(), process.getShopType());
                    if(price == -1){
                        //event.setCancelled(true);
                        //instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
                        process.cleanup();
                        playerChatCreationSteps.remove(player.getUniqueId());
                        return;
                    }
                    process.setPrice(price);
                    event.setCancelled(true);

                    if(process.getStep() == ShopCreationProcess.ChatCreationStep.ITEM_PRICE_COMBO){
                        process.displayFloatingText(process.getShopType().toString(), "createHitChestPriceCombo");
                        return;
                    }
                    if(process.getStep() == ShopCreationProcess.ChatCreationStep.FINISHED){
                        process.createShop(player);
                        process.cleanup();
                        playerChatCreationSteps.remove(player.getUniqueId());
                    }
                    break;
                case ITEM_PRICE_COMBO:
                    double priceCombo = plugin.getShopCreationUtil().getShopPriceCombo(player, event.getMessage(), process.getShopType());
                    if(priceCombo == -1){
                        //event.setCancelled(true);
                        //instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
                        process.cleanup();
                        playerChatCreationSteps.remove(player.getUniqueId());
                        return;
                    }
                    process.setPriceCombo(priceCombo);
                    event.setCancelled(true);

                    if(process.getStep() == ShopCreationProcess.ChatCreationStep.FINISHED){
                        process.createShop(player);
                        process.cleanup();
                        playerChatCreationSteps.remove(player.getUniqueId());
                    }
                    break;
                case BARTER_ITEM_AMOUNT:
                    int barterAmount = 0;
                    try {
                        String textAmt = UtilMethods.cleanNumberText(event.getMessage());
                        barterAmount = Integer.parseInt(textAmt);
                        if (barterAmount < 1) {
                            ShopMessage.sendMessage("interactionIssue", "line2", player, null);
                            ShopMessage.sendMessage("interactionIssue", "createCancel", player, null);
                            event.setCancelled(true);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        ShopMessage.sendMessage("interactionIssue", "line2", player, null);
                        ShopMessage.sendMessage("interactionIssue", "createCancel", player, null);
                        //event.setCancelled(true);
                        //instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
                        process.cleanup();
                        playerChatCreationSteps.remove(player.getUniqueId());
                        return;
                    }
                    process.setPrice(barterAmount);
                    event.setCancelled(true);

                    if(process.getStep() == ShopCreationProcess.ChatCreationStep.FINISHED) {
                        process.createShop(player);
                        process.cleanup();
                        playerChatCreationSteps.remove(player.getUniqueId());
                    }
                    break;
                // ITEM, BARTER_ITEM, or FINISHED
                default:
                    // If the user chatted and we were not in one of the earlier steps, cancel the creation process
                    // This will happen if the user was meant to select an ITEM or BARTER_ITEM, and exited the window
                    // without selecting their item to buy.
                    // This prevents chat from being locked for the player
                    process.cleanup();
                    this.cancelShopCreationProcess(player);
                    break;
            }
        }
    }

    //player destroys shop, call PlayerDestroyShopEvent or PlayerResizeShopEvent
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void shopDestroy(BlockBreakEvent event) {

        Block b = event.getBlock();
        Player player = event.getPlayer();

        if (b.getBlockData() instanceof WallSign) {
            AbstractShop shop = plugin.getShopHandler().getShop(b.getLocation());
            if (shop == null)
                return;
            // Disable dropping sign if its fake
            if(shop.isFakeSign()){
                event.setDropItems(false);
            }
            if (!shop.isInitialized()) {
                event.setCancelled(true);
                return;
            }

            if(plugin.getDestroyShopRequiresSneak()){
                if(!player.isSneaking()){
                    event.setCancelled(true);
                    Shop.getPlugin().getShopLogger().trace("[MiscListener.shopDestroy : getDestroyShopRequiresSneak] updateSign");
                    shop.updateSign();
                    return;
                }
            }
            //player trying to break their own shop
            if (shop.getOwnerName().equals(player.getName())) {
                if (plugin.usePerms() && !(player.hasPermission("shop.destroy") || player.hasPermission("shop.operator"))) {
                    event.setCancelled(true);
                    ShopMessage.sendMessage("permission", "destroy", player, shop);
                    return;
                }

                //if players must pay to create shops, remove money first
                double cost = plugin.getDestructionCost();
                if(cost > 0){
                    // Check for funds
                    if (!EconomyUtils.hasSufficientFunds(player, player.getInventory(), cost)){
                        ShopMessage.sendMessage("interactionIssue", "destroyInsufficientFunds", player, shop);
                        event.setCancelled(true);
                        return;
                    }
                    // Remove funds
                    boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
                    if(!removed){
                        ShopMessage.sendMessage("interactionIssue", "destroyInsufficientFunds", player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }

                PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
                plugin.getServer().getPluginManager().callEvent(e);
                if (e.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }

                plugin.getLogHandler().logAction(player, shop, ShopActionType.DESTROY);

                if(shop.isFakeSign()){
                    event.setDropItems(false);
                }

                if((!shop.isAdmin()) && plugin.returnCreationCost() && plugin.getCreationCost() > 0) {
                    if (plugin.getCurrencyType() != CurrencyType.ITEM) {
                        EconomyUtils.addFunds(shop.getOwner(),player.getInventory(), plugin.getCreationCost());
                    } else {
                        ItemStack currencyDrop = plugin.getItemCurrency().clone();
                        currencyDrop.setAmount((int) plugin.getCreationCost());
                        shop.getChestLocation().getWorld().dropItemNaturally(shop.getChestLocation(), currencyDrop);
                    }
                }


                ShopMessage.sendMessage(shop.getType().toString(), "destroy", player, shop);
                // We already log on ShopActionType.DESTROY in the Log Handler, so don't log the shop destroy reason
                shop.delete();
                plugin.getShopHandler().saveShops(shop.getOwnerUUID(), true);

                return;
            }
            //player trying to break other players shop
            else {
                boolean isRegionOwner = false;
                //check if the player is a world guard region owner
                if (Shop.getPlugin().worldGuardExists()) {
                    isRegionOwner = WorldGuardHook.isRegionOwner(player, shop.getSignLocation());
                }
                if (isRegionOwner || player.isOp() || (plugin.usePerms() && (player.hasPermission("shop.operator") || player.hasPermission("shop.destroy.other")))) {
                    PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
                    plugin.getServer().getPluginManager().callEvent(e);

                    if (e.isCancelled()) {
                        event.setCancelled(true);
                        return;
                    }

                    plugin.getLogHandler().logAction(player, shop, ShopActionType.DESTROY);

                    if(shop.isFakeSign()){
                        event.setDropItems(false);
                    }

                    ShopMessage.sendMessage(shop.getType().toString(), "opDestroy", player, shop);
                    shop.delete();
                    plugin.getShopHandler().saveShops(shop.getOwnerUUID(), true);
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
                        ShopMessage.sendMessage("interactionIssue", "destroyChest", player, shop);
                        event.setCancelled(true);
                        shop.sendEffects(false, player);
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
                    ShopMessage.sendMessage("interactionIssue", "destroyChest", player, shop);
                    shop.sendEffects(false, player);
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBreakBlockUnderShop(BlockBreakEvent event){
       //if the block under a chest has been broken, check that its a shop chest
        if(DisplayUtil.isChest(event.getBlock().getRelative(BlockFace.UP).getType())){
            AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getBlock().getRelative(BlockFace.UP));
            if(shop != null){
                //if it is a shop chest, don't allow it to be broken unless its by the owner or someone with permission
                Player player = event.getPlayer();
                if(!(shop.getOwnerUUID().equals(player.getUniqueId()) || player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator")))){
                    event.setCancelled(true);
                }
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
            AbstractShop shop = plugin.getShopHandler().getShopTouchingBlock(b);
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