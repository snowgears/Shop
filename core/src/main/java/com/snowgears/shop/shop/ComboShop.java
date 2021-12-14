package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.TransactionError;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ComboShop extends AbstractShop {

    private double priceBuy;
    private double priceSell;

    public ComboShop(Location signLoc, UUID player, double pri, double priSell, int amt, Boolean admin, BlockFace facing) {
        super(signLoc, player, pri, amt, admin, facing);

        this.type = ShopType.COMBO;
        this.signLines = ShopMessage.getSignLines(this, this.type);
        this.priceBuy = pri;
        this.priceSell = priSell;
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {
        this.isPerformingTransaction = true;
        TransactionError issue;
        if(transactionType == ShopType.SELL){
            issue = executeSellTransaction(orders, player, isCheck);
        }
        else{
            issue = executeBuyTransaction(orders, player, isCheck);
        }
        this.isPerformingTransaction = false;
        setGuiIcon();
        return issue;
    }

    private TransactionError executeSellTransaction(int orders, Player player, boolean isCheck){
        TransactionError issue = null;

        ItemStack is = this.getItemStack();

        //check if shop has enough items
        if (!isAdmin()) {
            if(isCheck) {
                int shopItems = InventoryUtils.getAmount(this.getInventory(), is);
                if (shopItems < is.getAmount())
                    issue = TransactionError.INSUFFICIENT_FUNDS_SHOP;
            }
            else {
                //remove items from shop
                InventoryUtils.removeItem(this.getInventory(), is, this.getOwner());
            }
        }

        if(issue == null) {
            if (isCheck) {
                //check if player has enough currency
                boolean hasFunds = EconomyUtils.hasSufficientFunds(player, player.getInventory(), this.getPriceSell());
                if (!hasFunds)
                    issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
            } else {
                //remove currency from player
                EconomyUtils.removeFunds(player, player.getInventory(), this.getPriceSell());
            }
        }

        if(issue == null) {
            //check if shop has enough room to accept currency
            if (!isAdmin()) {
                if (isCheck) {
                    boolean hasRoom = EconomyUtils.canAcceptFunds(this.getOwner(), this.getInventory(), this.getPriceSell());
                    if (!hasRoom)
                        issue = TransactionError.INVENTORY_FULL_SHOP;
                } else {
                    //add currency to shop
                    EconomyUtils.addFunds(this.getOwner(), this.getInventory(), this.getPriceSell());
                }
            }
        }

        if(issue == null) {
            if (isCheck) {
                //check if player has enough room to accept items
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), is, player);
                if (!hasRoom)
                    issue = TransactionError.INVENTORY_FULL_PLAYER;
            } else {
                //add items to player's inventory
                InventoryUtils.addItem(player.getInventory(), is, player);
            }
        }

        player.updateInventory();

        if(issue != null){
            return issue;
        }

        //if there are no issues with the test/check transaction
        if(issue == null && isCheck){

            PlayerExchangeShopEvent e = new PlayerExchangeShopEvent(player, this);
            Bukkit.getPluginManager().callEvent(e);

            if(e.isCancelled())
                return TransactionError.CANCELLED;

            //run the transaction again without the check clause
            return executeSellTransaction(orders, player, false);
        }
        return TransactionError.NONE;
    }

    private TransactionError executeBuyTransaction(int orders, Player player, boolean isCheck){
        TransactionError issue = null;

        ItemStack is = this.getItemStack();

        //check if player has enough items
        if(isCheck) {
            int playerItems = InventoryUtils.getAmount(player.getInventory(), is);
            if (playerItems < is.getAmount())
                return TransactionError.INSUFFICIENT_FUNDS_PLAYER;
        }
        else {
            //remove items from player
            InventoryUtils.removeItem(player.getInventory(), is, player);
        }

        if(issue == null) {
            //check if shop has enough currency
            if(!this.isAdmin()) {
                if(isCheck) {
                    boolean hasFunds = EconomyUtils.hasSufficientFunds(this.getOwner(), this.getInventory(), this.getPrice());
                    if (!hasFunds)
                        return TransactionError.INSUFFICIENT_FUNDS_SHOP;
                }
                else {
                    EconomyUtils.removeFunds(this.getOwner(), this.getInventory(), this.getPrice());
                }
            }
        }

        if(issue == null) {
            if(isCheck) {
                //check if player has enough room to accept currency
                boolean hasRoom = EconomyUtils.canAcceptFunds(player, player.getInventory(), this.getPrice());
                if (!hasRoom)
                    return TransactionError.INVENTORY_FULL_PLAYER;
            }
            else {
                //add currency to player
                EconomyUtils.addFunds(player, player.getInventory(), this.getPrice());
            }
        }

        if(issue == null) {
            //check if shop has enough room to accept items
            if(!this.isAdmin()) {
                if(isCheck) {
                    boolean shopHasRoom = InventoryUtils.hasRoom(this.getInventory(), is, this.getOwner());
                    if (!shopHasRoom)
                        return TransactionError.INVENTORY_FULL_SHOP;
                }
                else{
                    //add items to shop's inventory
                    InventoryUtils.addItem(this.getInventory(), is, this.getOwner());
                }
            }
        }

        player.updateInventory();

        if(issue != null){
            return issue;
        }

        //if there are no issues with the test/check transaction
        if(issue == null && isCheck){

            PlayerExchangeShopEvent e = new PlayerExchangeShopEvent(player, this);
            Bukkit.getPluginManager().callEvent(e);

            if(e.isCancelled())
                return TransactionError.CANCELLED;

            //run the transaction again without the check clause
            return executeBuyTransaction(orders, player, false);
        }
        return TransactionError.NONE;
    }

    public String getPriceSellString() {
        return Shop.getPlugin().getPriceString(this.priceSell, false);
    }

    public String getPriceSellPerItemString() {
        double pricePer = this.getPriceSell() / this.getAmount();
        return Shop.getPlugin().getPriceString(pricePer, true);
    }

    public String getPriceComboString() {
        return Shop.getPlugin().getPriceComboString(this.price, this.priceSell, false);
    }

    public double getPriceSell(){
        return priceSell;
    }
}
