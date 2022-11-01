
package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.hook.WorldGuardHook;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ComboShop;
import com.snowgears.shop.shop.ShopType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;


public class TransactionHelper {

    private Shop plugin = Shop.getPlugin();
    private HashMap<Location, UUID> shopMessageCooldown = new HashMap<>(); //shop location, shop owner
//    private Logger exchangeLogger;

    public TransactionHelper(Shop instance) {
        plugin = instance;
//        initializeLogger(); //TODO
    }

    //TODO will need to update ender chest contents at the end of every transaction involving an ender chest

    public void executeTransactionFromEvent(PlayerInteractEvent event, AbstractShop shop, boolean fullStackOrder){
        Player player = event.getPlayer();

        if(shop.isPerformingTransaction()) {
            String message = ShopMessage.getMessage("interactionIssue", "useShopAlreadyInUse", shop, player);
            if(message != null && !message.isEmpty())
                player.sendMessage(message);
            event.setCancelled(true);
            return;
        }

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

        //delete shop if it does not have a chest attached to it
        if(!(plugin.getShopHandler().isChest(shop.getChestLocation().getBlock()))){
            shop.delete();
            return;
        }

        //do not allow gamble shops to have full stack orders
        if(shop.getType() == ShopType.GAMBLE)
            fullStackOrder = false;

        //player did not click their own shop
        if (!shop.getOwnerName().equals(player.getName())) {

            if (plugin.usePerms() && !(player.hasPermission("shop.use."+shop.getType().toString().toLowerCase()) || player.hasPermission("shop.use"))) {
                if (!player.hasPermission("shop.operator")) {
                    String message = ShopMessage.getMessage("permission", "use", shop, player);
                    if(message != null && !message.isEmpty())
                        player.sendMessage(message);
                    return;
                }
            }
            //for COMBO shops, shops can execute either a BUY or a SELL depending on the side of sign that was clicked
            if(shop.getType() == ShopType.COMBO){
                int clickedSide = UtilMethods.calculateSideFromClickedSign(player, event.getClickedBlock());
                //clicked left side of sign
                if(clickedSide >= 0){
                    if(plugin.inverseComboShops())
                        executeTransaction(player, shop, ShopType.SELL, fullStackOrder);
                    else
                        executeTransaction(player, shop, ShopType.BUY, fullStackOrder);
                }
                //clicked right side of sign
                else{
                    if(plugin.inverseComboShops())
                        executeTransaction(player, shop, ShopType.BUY, fullStackOrder);
                    else
                        executeTransaction(player, shop, ShopType.SELL, fullStackOrder);
                }
            }
            else {
                executeTransaction(player, shop, shop.getType(), fullStackOrder);
            }
        } else {
            String message = ShopMessage.getMessage("interactionIssue", "useOwnShop", shop, player);
            if(message != null && !message.isEmpty())
                player.sendMessage(message);
            sendEffects(false, player, shop);
        }
        event.setCancelled(true);
    }

    private void executeTransaction(Player player, AbstractShop shop, ShopType actionType, boolean fullStackOrder){

        int orderNum = 0;
        int orderSizeMax = 1;
        //full stack order max will be a stack of 64
        if(fullStackOrder){
            if(shop.getType() == ShopType.BARTER){
                orderSizeMax = 64 / shop.getItemStack().getAmount();
                if(orderSizeMax < 1)
                    orderSizeMax = 1;
            }
            else {
                orderSizeMax = 64 / shop.getAmount();
                if (orderSizeMax < 1)
                    orderSizeMax = 1;
            }
        }

        //loop through, submitting transactions up to the order max or until an issue occurs
        TransactionError issue = TransactionError.NONE;
        while(orderNum < orderSizeMax && issue == TransactionError.NONE) {
            orderNum++;
            //System.out.println("MaxOrders - "+orderSizeMax+", OrderNum - "+orderNum+", issue - "+issue.toString());
            issue = shop.executeTransaction(player, true, actionType);

            //while loop fail safe
            if(orderNum > 100)
                return;

            //there was an issue when checking transaction, send reason to player
            if (issue != TransactionError.NONE) {
                String message = null;
                switch (issue) {
                    case INSUFFICIENT_FUNDS_SHOP:
                        if (!shop.isAdmin()) {
                            Player owner = shop.getOwner().getPlayer();
                            //the shop owner is online
                            if (owner != null && notifyOwner(shop)) {
                                ShopGuiHandler.GuiIcon guiIcon = plugin.getGuiHandler().getIconFromOption(player, PlayerSettings.Option.NOTIFICATION_STOCK);

                                if (guiIcon != null && guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_STOCK_ON) {
                                    String ownerMessage = ShopMessage.getMessage(actionType.toString(), "ownerNoStock", shop, owner);
                                    if (ownerMessage != null && !ownerMessage.isEmpty())
                                        owner.sendMessage(ownerMessage);
                                }
                            }
                        }
                        message = ShopMessage.getMessage(actionType.toString(), "shopNoStock", shop, player);
                        break;
                    case INSUFFICIENT_FUNDS_PLAYER:
                        message = ShopMessage.getMessage(actionType.toString(), "playerNoStock", shop, player);
                        break;
                    case INVENTORY_FULL_SHOP:
                        if (!shop.isAdmin()) {
                            Player owner = shop.getOwner().getPlayer();
                            //the shop owner is online
                            if (owner != null && notifyOwner(shop)) {
                                ShopGuiHandler.GuiIcon guiIcon = plugin.getGuiHandler().getIconFromOption(player, PlayerSettings.Option.NOTIFICATION_STOCK);

                                if (guiIcon != null && guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_STOCK_ON) {
                                    String ownerMessage = ShopMessage.getMessage(actionType.toString(), "ownerNoSpace", shop, owner);
                                    if (ownerMessage != null && !ownerMessage.isEmpty())
                                        owner.sendMessage(ownerMessage);
                                }
                            }
                        }
                        message = ShopMessage.getMessage(actionType.toString(), "shopNoSpace", shop, player);
                        break;
                    case INVENTORY_FULL_PLAYER:
                        message = ShopMessage.getMessage(actionType.toString(), "playerNoSpace", shop, player);
                        break;
                }
                if(orderNum < 2) {
                    if (message != null && !message.isEmpty())
                        player.sendMessage(message);
                    sendEffects(false, player, shop);
                    return;
                }
                //if orderNum >= 2, that means the player successfully transacted so we will show success messages below
            }
        }

        //TODO update enderchest shop inventory?

        //this cleans up the while loop logic. If an order is on 2 and fails, they got 1 order. etc
        if(issue != TransactionError.NONE)
            orderNum = orderNum - 1;


        //the transaction has finished and the exchange event has not been cancelled
        sendExchangeMessagesAndLog(shop, player, actionType, orderNum);
        sendEffects(true, player, shop);
        //make sure to update the shop sign, but only if the sign lines use a variable that requires a refresh (like stock that is dynamically updated)
        if(shop.getSignLinesRequireRefresh())
            shop.updateSign();
    }

    private void sendExchangeMessagesAndLog(AbstractShop shop, Player player, ShopType transactionType, int orders) {

        double price = getPriceFromOrders(shop, transactionType, orders);
        String message = getMessageFromOrders(shop, player, transactionType, "user", price, orders);

        ShopGuiHandler.GuiIcon guiIcon = plugin.getGuiHandler().getIconFromOption(player, PlayerSettings.Option.NOTIFICATION_SALE_USER);
        if(guiIcon != null && guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_USER_ON) {
            if(message != null && !message.isEmpty())
                player.sendMessage(message);
        }

        Player owner = Bukkit.getPlayer(shop.getOwnerName());
        if ((owner != null) && (!shop.isAdmin())) {
            message = getMessageFromOrders(shop, player, transactionType, "owner", price, orders);

            guiIcon = plugin.getGuiHandler().getIconFromOption(owner, PlayerSettings.Option.NOTIFICATION_SALE_OWNER);
            if(guiIcon != null && guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_OWNER_ON) {
                if(message != null && !message.isEmpty())
                    owner.sendMessage(message);
            }
        }

        plugin.getLogHandler().logTransaction(player, shop, transactionType, price, (shop.getAmount()*orders));
//        if(shop.getType() == ShopType.GAMBLE)
//            shop.shuffleGambleItem();
    }

    private String getMessageFromOrders(AbstractShop shop, Player player, ShopType transactionType, String subKey, double price, int orders){
        String message = ShopMessage.getUnformattedMessage(transactionType.toString(), subKey);
        String priceStr = Shop.getPlugin().getPriceString(price, false);
        message = message.replace("[price]", ""+priceStr);

        if(shop.getItemStack() != null) {
            int amount = shop.getItemStack().getAmount() * orders;
            message = message.replace("[item amount]", "" + amount);
        }
        if(shop.getSecondaryItemStack() != null) {
            int amount = shop.getSecondaryItemStack().getAmount() * orders;
            message = message.replace("[barter item amount]", "" + amount);
        }
        message = ShopMessage.formatMessage(message, shop, player, false);
        return message;
    }

    private double getPriceFromOrders(AbstractShop shop, ShopType transactionType, int orders){
        double price;
        if(shop.getType() == ShopType.COMBO && transactionType == ShopType.SELL){
            price = (((ComboShop)shop).getPriceSell() * orders);
        }
        else{
            price = shop.getPrice() * orders;
        }
        return price;
    }

    public void sendEffects(boolean success, Player player, AbstractShop shop){
        try {
            //only send effects to player if server is above MC 1.8 (when OFF_HAND was introduced)
            if(EquipmentSlot.OFF_HAND == EquipmentSlot.OFF_HAND) {
                if (success) {
                    if (plugin.playSounds()) {
                        try {
                            player.playSound(shop.getSignLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                        } catch (NoSuchFieldError e) {}
                    }
                    if (plugin.playEffects())
                        player.getWorld().playEffect(shop.getChestLocation(), Effect.STEP_SOUND, Material.EMERALD_BLOCK);
                } else {
                    if (plugin.playSounds())
                        player.playSound(shop.getSignLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                    if (plugin.playEffects())
                        player.getWorld().playEffect(shop.getChestLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
                }
            }
        } catch (Error e){
        } catch (Exception e) {}
    }

    private boolean notifyOwner(final AbstractShop shop){
        if(shop.isAdmin())
            return false;
        if(shopMessageCooldown.containsKey(shop.getSignLocation()))
            return false;
        else{
            shopMessageCooldown.put(shop.getSignLocation(), shop.getOwnerUUID());

            new BukkitRunnable() {
                @Override
                public void run() {
                    if(shop != null){
                        if(shopMessageCooldown.containsKey(shop.getSignLocation())){
                            shopMessageCooldown.remove(shop.getSignLocation());
                        }
                    }
                    //TODO if shop is null, should you clear the entire cooldown list so that that location isn't messed up?
                }
            }.runTaskLater(this.plugin, 2400); //make cooldown 2 minutes
        }
        return true;
    }
}