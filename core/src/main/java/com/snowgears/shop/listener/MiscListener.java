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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;


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
        final org.bukkit.material.Sign sign = (org.bukkit.material.Sign) b.getState().getData(); //TODO for some reason this has thrown cast errors

        if (sign.isWallSign()) {
            chest = b.getRelative(sign.getAttachedFace());
        }
        else {
            chest = b.getRelative(sign.getFacing().getOppositeFace());
        }
        signDirection = sign.getFacing();

        int amount = 0 ;
        ShopType type = null;
        boolean isAdmin = false;
        if (plugin.getShopHandler().isChest(chest)) {
            final Sign signBlock = (Sign) b.getState();
            if (event.getLine(0).toLowerCase().contains(ShopMessage.getCreationWord("SHOP").toLowerCase())) {

                if (!plugin.getShopCreationUtil().shopCanBeCreated(player, chest)) {
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
                if (pricePair == null) {
                    event.setCancelled(true);
                    return;
                }

                System.out.println("MiscListener 1 -> "+b.getType().toString());
                AbstractShop shop = plugin.getShopCreationUtil().createShop(player, chest, b, pricePair, amount, isAdmin, type, signDirection, false);
                if (shop == null) {
                    event.setCancelled(true);
                    return;
                }
                System.out.println("MiscListener 2 -> "+b.getType().toString());

                String message = ShopMessage.getMessage(type.toString(), "initialize", shop, player);
                if (message != null && !message.isEmpty())
                    player.sendMessage(message);
                if (plugin.allowCreativeSelection() && (type == ShopType.BUY || type == ShopType.COMBO)) {
                    message = ShopMessage.getMessage(type.toString(), "initializeAlt", shop, player);
                    if (message != null && !message.isEmpty())
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
                                plugin.getCreativeSelectionListener().removePlayerFromCreativeSelection(player);
                            }
                        }
                    }
                }, 1200L); //1 minute
            }
        }
    }

    public ShopCreationProcess getShopCreationProcess(Player player){
        return playerChatCreationSteps.get(player.getUniqueId());
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

            if (clicked.getType() == Material.WALL_SIGN) {

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
                        if (currentProcess != null && currentProcess.getStep() == ShopCreationProcess.ChatCreationStep.BARTER_ITEM) {
                            plugin.getCreativeSelectionListener().putPlayerInCreativeSelection(player, clicked.getLocation(), false);
                            event.setCancelled(true);
                            return;
                        }
                        else if (currentProcess == null && player.isSneaking()){
                            Long lastCreatedProcess = lastChatCreation.get(player.getUniqueId());
                            //if the player has created a new process in the last 5 seconds, block them from creating another
                            if(lastCreatedProcess != null && (new Date().getTime() - lastCreatedProcess) < 5000) {
                                String message = ShopMessage.getMessage("interactionIssue", "createCooldown", null, player);
                                if (message != null && !message.isEmpty())
                                    player.sendMessage(message);
                                event.setCancelled(true);
                                return;
                            }

                            if(!plugin.getShopCreationUtil().shopCanBeCreated(player, clicked))
                                return;
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
                    if (currentProcess != null && currentProcess.getStep() == ShopCreationProcess.ChatCreationStep.BARTER_ITEM) {
                        if (!plugin.getShopCreationUtil().itemsCanBeInitialized(player, currentProcess.getItemStack(), event.getItem())) {
                            event.setCancelled(true);
                            return;
                        }
                        currentProcess.setBarterItemStack(event.getItem());

                        String message = ShopMessage.getUnformattedMessage(currentProcess.getShopType().toString(), "createHitChestBarterAmount");
                        message = ShopMessage.formatMessage(message, currentProcess, player);
                        if (message != null && !message.isEmpty())
                            player.sendMessage(message);
                        event.setCancelled(true);
                        return;
                    }
                }

                if(!player.isSneaking())
                    return;

                Long lastCreatedProcess = lastChatCreation.get(player.getUniqueId());
                //if the player has created a new process in the last 5 seconds, block them from creating another
                if(lastCreatedProcess != null && (new Date().getTime() - lastCreatedProcess) < 5000) {
                    String message = ShopMessage.getMessage("interactionIssue", "createCooldown", null, player);
                    if (message != null && !message.isEmpty())
                        player.sendMessage(message);
                    event.setCancelled(true);
                    return;
                }

                //dont let players create shops via chest on shops that already exist
                //TODO come back to this and allow players to create double chest shops via chest creation method
                AbstractShop existingShop = plugin.getShopHandler().getShopByChest(clicked);
                if (existingShop != null) {
                    return;
                }

                if(!plugin.getShopCreationUtil().shopCanBeCreated(player, clicked))
                    return;

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
                String message = ShopMessage.getUnformattedMessage("createHitChest", null);
                message = ShopMessage.formatMessage(message, process, player);
                if(message != null && !message.isEmpty()) {
                    player.sendMessage(message);
                }
                if((!plugin.usePerms() && player.isOp()) || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    String adminMessage = ShopMessage.getUnformattedMessage("adminCreateHitChest", null);
                    adminMessage = ShopMessage.formatMessage(adminMessage, process, player);
                    if (adminMessage != null && !adminMessage.isEmpty())
                        player.sendMessage(adminMessage);
                }

                //give player a limited amount of time to finish creating the shop until it is deleted
                final UUID originalProcessUUID = process.getUniqueID();
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        //the shop has still not been initialized with an item from a player
                        ShopCreationProcess process = playerChatCreationSteps.get(player.getUniqueId());
                        if (process != null && process.getUniqueID().equals(originalProcessUUID)) {
                            playerChatCreationSteps.remove(player.getUniqueId());
                            plugin.getCreativeSelectionListener().removePlayerFromCreativeSelection(player);

                            String message = ShopMessage.getUnformattedMessage("interactionIssue", "createHitChestTimeout");
                            message = ShopMessage.formatMessage(message, process, player);
                            if(message != null && !message.isEmpty())
                                event.getPlayer().sendMessage(message);
                        }
                    }
                }, 1200); //1 minute
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        if(playerChatCreationSteps.containsKey(player.getUniqueId())){
            ShopCreationProcess process = playerChatCreationSteps.get(player.getUniqueId());
            switch (process.getStep()){
                case SHOP_TYPE:
                    ShopType type = plugin.getShopCreationUtil().getShopType(event.getMessage());
                    if(type == null){
                        playerChatCreationSteps.remove(player.getUniqueId());
                        return;
                    }
                    boolean isAdmin = plugin.getShopCreationUtil().getShopIsAdmin(event.getMessage());
                    process.setShopType(type);
                    process.setAdmin(isAdmin);
                    event.setCancelled(true);

                    String message;
                    if(type == ShopType.GAMBLE){
                        message = ShopMessage.getUnformattedMessage(type.toString(), "createHitChestPrice");
                        message = ShopMessage.formatMessage(message, process, player);
                        if(message != null && !message.isEmpty())
                            player.sendMessage(message);
                    }
                    else {
                        message = ShopMessage.getUnformattedMessage(type.toString(), "createHitChestAmount");
                        message = ShopMessage.formatMessage(message, process, player);
                        if (message != null && !message.isEmpty())
                            player.sendMessage(message);
                    }
                    break;
                case ITEM_AMOUNT:
                    int amount = 0;
                    try {
                        String textAmt = UtilMethods.cleanNumberText(event.getMessage());
                        amount = Integer.parseInt(textAmt);
                        if (amount < 1) {
                            player.sendMessage(ShopMessage.getMessage("interactionIssue", "line2", null, player));
                            event.setCancelled(true);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line2", null, player));
                        //event.setCancelled(true);
                        //instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
                        playerChatCreationSteps.remove(player.getUniqueId());
                        return;
                    }
                    process.setItemAmount(amount);
                    event.setCancelled(true);

                    if(process.getShopType() == ShopType.BARTER){
                        message = ShopMessage.getUnformattedMessage(process.getShopType().toString(), "createHitChest");
                        message = ShopMessage.formatMessage(message, process, player);
                        if (message != null && !message.isEmpty())
                            event.getPlayer().sendMessage(message);

                        if (plugin.allowCreativeSelection()) {
                            message = ShopMessage.getMessage(process.getShopType().toString(), "initializeBarterAlt", null, player);
                            if (message != null && !message.isEmpty())
                                player.sendMessage(message);
                        }
                    }
                    else {
                        message = ShopMessage.getUnformattedMessage(process.getShopType().toString(), "createHitChestPrice");
                        message = ShopMessage.formatMessage(message, process, player);
                        if (message != null && !message.isEmpty())
                            event.getPlayer().sendMessage(message);
                    }
                    break;
                case ITEM_PRICE:
                    double price = plugin.getShopCreationUtil().getShopPrice(player, event.getMessage(), process.getShopType());
                    if(price == -1){
                        //event.setCancelled(true);
                        //instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
                        playerChatCreationSteps.remove(player.getUniqueId());
                        return;
                    }
                    process.setPrice(price);
                    event.setCancelled(true);

                    if(process.getStep() == ShopCreationProcess.ChatCreationStep.FINISHED){
                        process.createShop(player);
                        playerChatCreationSteps.remove(player.getUniqueId());
                    }
                    else if(process.getStep() == ShopCreationProcess.ChatCreationStep.ITEM_PRICE_COMBO){
                        message = ShopMessage.getUnformattedMessage(process.getShopType().toString(), "createHitChestPriceCombo");
                        message = ShopMessage.formatMessage(message, process, player);
                        if(message != null && !message.isEmpty())
                            event.getPlayer().sendMessage(message);
                    }
                    break;
                case ITEM_PRICE_COMBO:
                    double priceCombo = plugin.getShopCreationUtil().getShopPriceCombo(player, event.getMessage(), process.getShopType());
                    if(priceCombo == -1){
                        //event.setCancelled(true);
                        //instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
                        playerChatCreationSteps.remove(player.getUniqueId());
                        return;
                    }
                    process.setPriceCombo(priceCombo);
                    event.setCancelled(true);

                    if(process.getStep() == ShopCreationProcess.ChatCreationStep.FINISHED){
                        process.createShop(player);
                        playerChatCreationSteps.remove(player.getUniqueId());
                    }
                    break;
                case BARTER_ITEM_AMOUNT:
                    int barterAmount = 0;
                    try {
                        String textAmt = UtilMethods.cleanNumberText(event.getMessage());
                        barterAmount = Integer.parseInt(textAmt);
                        if (barterAmount < 1) {
                            player.sendMessage(ShopMessage.getMessage("interactionIssue", "line2", null, player));
                            event.setCancelled(true);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line2", null, player));
                        //event.setCancelled(true);
                        //instead of cancelling the chat event, just let them know what they typed wasnt a number and break them out of the creation process so they aren't chat locked
                        playerChatCreationSteps.remove(player.getUniqueId());
                        return;
                    }
                    process.setPrice(barterAmount);
                    event.setCancelled(true);

                    if(process.getStep() == ShopCreationProcess.ChatCreationStep.FINISHED) {
                        process.createShop(player);
                        playerChatCreationSteps.remove(player.getUniqueId());
                    }
                    break;
                default:
                    event.setCancelled(true); //TODO maybe remove this
                    break;
            }
        }
    }

    //player destroys shop, call PlayerDestroyShopEvent or PlayerResizeShopEvent
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void shopDestroy(BlockBreakEvent event) {

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
            if(plugin.getDestroyShopRequiresSneak()){
                if(!player.isSneaking()){
                    event.setCancelled(true);
                    shop.updateSign();
                    return;
                }
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


                String message = ShopMessage.getMessage(shop.getType().toString(), "destroy", shop, player);
                if(message != null && !message.isEmpty())
                    player.sendMessage(message);
                shop.delete();
                plugin.getShopHandler().saveShops(shop.getOwnerUUID());

                return;
            }
            //player trying to break other players shop
            else {
                boolean isRegionOwner = false;
                //check if the player is a world guard region owner
                if (Shop.getPlugin().hookWorldGuard()) {
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
                        plugin.getTransactionHelper().sendEffects(false, player, shop);
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
                    plugin.getTransactionHelper().sendEffects(false, player, shop);
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