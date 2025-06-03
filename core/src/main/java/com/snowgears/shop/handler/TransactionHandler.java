package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import com.snowgears.shop.hook.WorldGuardHook;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import com.snowgears.shop.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;


public class TransactionHandler {

    private Shop plugin;
    private HashMap<Location, UUID> shopMessageCooldown = new HashMap<>(); //shop location, shop owner

    public TransactionHandler(Shop instance) {
        plugin = instance;
    }

    public void executeTransactionFromEvent(PlayerInteractEvent event, AbstractShop shop, boolean fullStackOrder){
        Player player = event.getPlayer();

        if(shop.isPerformingTransaction()) {
            ShopMessage.sendMessage("interactionIssue", "useShopAlreadyInUse", player, shop);
            event.setCancelled(true);
            return;
        }

        boolean canUseShopInRegion = true;
        try {
            canUseShopInRegion = WorldGuardHook.canUseShop(player, shop.getSignLocation());
        } catch(NoClassDefFoundError e) {}

        //check that player can use the shop if it is in a WorldGuard region
        if(!canUseShopInRegion){
            ShopMessage.sendMessage("interactionIssue", "regionRestriction", player, shop);
            event.setCancelled(true);
            return;
        }

        //delete shop if it does not have a chest attached to it
        if(!(plugin.getShopHandler().isChest(shop.getChestLocation().getBlock()))){
            plugin.getShopLogger().warning("Deleting Shop because chest does not exist! " + shop);
            shop.delete();
            return;
        }

        //do not allow gamble shops to have full stack orders
        if(shop.getType() == ShopType.GAMBLE)
            fullStackOrder = false;

        //player did not click their own shop
        if (!shop.getOwnerName().equals(player.getName()) || Shop.getPlugin().getDebug_allowUseOwnShop()) {

            if (plugin.usePerms() && !(player.hasPermission("shop.use."+shop.getType().toString().toLowerCase()) || player.hasPermission("shop.use"))) {
                if (!player.hasPermission("shop.operator")) {
                    ShopMessage.sendMessage("permission", "use", player, shop);
                    return;
                }
            }
            //for COMBO shops, shops can execute either a BUY or a SELL depending on the side of sign that was clicked
            if(shop.getType() == ShopType.COMBO){
                int clickedSide = UtilMethods.calculateSideFromClickedSign(player, event.getClickedBlock());
                //clicked left side of sign
                if(clickedSide >= 0){
                    if(plugin.inverseComboShops())
                        executeTransactionSequence(player, shop, ShopType.SELL, fullStackOrder);
                    else
                        executeTransactionSequence(player, shop, ShopType.BUY, fullStackOrder);
                }
                //clicked right side of sign
                else{
                    if(plugin.inverseComboShops())
                        executeTransactionSequence(player, shop, ShopType.BUY, fullStackOrder);
                    else
                        executeTransactionSequence(player, shop, ShopType.SELL, fullStackOrder);
                }
            }
            else {
                executeTransactionSequence(player, shop, shop.getType(), fullStackOrder);
            }
        } else {
            ShopMessage.sendMessage("interactionIssue", "useOwnShop", player, shop);
            shop.sendEffects(false, player);
        }
        event.setCancelled(true);
    }

    private void executeTransactionSequence(Player player, AbstractShop shop, ShopType actionType, boolean fullStackOrder){
        Transaction transaction = new Transaction(player, shop, actionType);

        // Set the desired purchase amount if we are a full stack order
        if (fullStackOrder) {
            // Use either 64 (Minecraft's standard "full stack") or the shop's amount, whichever is larger
            // This ensures players get 64 items when buying from shops with smaller default amounts
            // But still get the full amount from shops configured to sell larger quantities
            transaction.negotiatePurchase(Math.max(shop.getAmount(), 64));
        } else {
            transaction.negotiatePurchase();
        }

        // Verify the transaction is possible
        TransactionError issue = transaction.verify();
        // If it is possible, go ahead and execute it, extra check just in case there is an issue (shouldn't ever happen, but who knows)
        if (issue == TransactionError.NONE) { issue = transaction.execute(); }

        // If there was an issue with the transaction, send the error message and bail out early
        if (issue != TransactionError.NONE) {
            //there was an issue when checking transaction, send reason to player
            this.sendErrorMessage(player, shop, actionType, transaction);
            return;
        }

        //the transaction has finished and the exchange event has not been cancelled
        sendExchangeMessagesAndLog(shop, player, actionType, transaction);
        shop.sendEffects(true, player);
        //make sure to update the shop sign, but only if the sign lines use a variable that requires a refresh (like stock that is dynamically updated)
        if(shop.getSignLinesRequireRefresh()){
            plugin.getShopLogger().trace("[TransactionHandler.executeTransactionSequence] updateSign");
            shop.updateSign();
        }
    }

    private void sendErrorMessage(Player player, AbstractShop shop, ShopType actionType, Transaction transaction) {
        String message = null;
        switch (transaction.getError()) {
            case INSUFFICIENT_FUNDS_SHOP:
                if (!shop.isAdmin()) {
                    Player owner = shop.getOwner().getPlayer();
                    //the shop owner is online
                    if (owner != null && notifyOwner(shop)) {
                        ShopGuiHandler.GuiIcon guiIcon = plugin.getGuiHandler().getIconFromOption(player, PlayerSettings.Option.NOTIFICATION_STOCK);

                        if (guiIcon != null && guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_STOCK_ON) {
                            ShopMessage.sendMessage(actionType.toString(), "ownerNoStock", owner, shop);
                        }
                    }
                }
                message = ShopMessage.getUnformattedMessage(actionType.toString(), "shopNoStock");
                break;
            case INSUFFICIENT_FUNDS_PLAYER:
                message = ShopMessage.getUnformattedMessage(actionType.toString(), "playerNoStock");
                break;
            case INVENTORY_FULL_SHOP:
                if (!shop.isAdmin()) {
                    Player owner = shop.getOwner().getPlayer();
                    //the shop owner is online
                    if (owner != null && notifyOwner(shop)) {
                        ShopGuiHandler.GuiIcon guiIcon = plugin.getGuiHandler().getIconFromOption(player, PlayerSettings.Option.NOTIFICATION_STOCK);

                        if (guiIcon != null && guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_STOCK_ON) {
                            ShopMessage.sendMessage(actionType.toString(), "ownerNoSpace", owner, shop);
                        }
                    }
                }
                message = ShopMessage.getUnformattedMessage(actionType.toString(), "shopNoSpace");
                break;
            case INVENTORY_FULL_PLAYER:
                message = ShopMessage.getUnformattedMessage(actionType.toString(), "playerNoSpace");
                break;
        }

        // Since there was an error during the transaction, send the message, then exit the transaction early.
        if (message != null && !message.isEmpty())
            ShopMessage.sendMessage(message, player, shop);
        shop.sendEffects(false, player);
    }

    private void sendExchangeMessagesAndLog(AbstractShop shop, Player player, ShopType transactionType, Transaction transaction) {

        double price = transaction.getPrice();
        String message = ShopMessage.getMessageFromOrders(transactionType, "user", price, transaction.getAmount());

        ShopGuiHandler.GuiIcon guiIcon = plugin.getGuiHandler().getIconFromOption(player, PlayerSettings.Option.NOTIFICATION_SALE_USER);
        if(guiIcon != null && guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_USER_ON) {
            if(message != null && !message.isEmpty()) {
                ShopMessage.sendMessage(message, player, shop);
            }
        }

        Player owner = Bukkit.getPlayer(shop.getOwnerUUID());
        if ((owner != null) && (!shop.isAdmin())) {
            message = ShopMessage.getMessageFromOrders(transactionType, "owner", price, transaction.getAmount());

            guiIcon = plugin.getGuiHandler().getIconFromOption(owner, PlayerSettings.Option.NOTIFICATION_SALE_OWNER);
            if(guiIcon != null && guiIcon == ShopGuiHandler.GuiIcon.SETTINGS_NOTIFY_OWNER_ON) {
                if(message != null && !message.isEmpty())
                    ShopMessage.sendMessage(message, owner, player, shop);
            }
        }

        int amount = transaction.getAmount();

        plugin.getLogHandler().logTransaction(player, shop, transactionType, price, amount);
    }

    private boolean notifyOwner(final AbstractShop shop){
        if(shop.isAdmin())
            return false;
        if(shopMessageCooldown.containsKey(shop.getSignLocation()))
            return false;
        else{
            shopMessageCooldown.put(shop.getSignLocation(), shop.getOwnerUUID());

            plugin.getFoliaLib().getScheduler().runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if(shop != null){
                        if(shopMessageCooldown.containsKey(shop.getSignLocation())){
                            shopMessageCooldown.remove(shop.getSignLocation());
                        }
                    }
                    //TODO if shop is null, should you clear the entire cooldown list so that that location isn't messed up?
                }
            }, 2400); //make cooldown 2 minutes
        }
        return true;
    }
}