package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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

    @Override
    public TransactionError executeTransaction(Transaction transaction) {
        this.isPerformingTransaction = true;
        if(transaction.getType() == ShopType.SELL){
            executeSellTransaction(transaction);
        }
        else{
            executeBuyTransaction(transaction);
        }

        //if the shop is connected to an ender inventory, save the contents as needed
        if(!isAdmin && this.chestLocation != null && this.chestLocation.getBlock().getType() == Material.ENDER_CHEST){
            Shop.getPlugin().getEnderChestHandler().saveInventory(this.getOwner());
        }

        this.isPerformingTransaction = false;
        setGuiIcon();
        return transaction.getError();
    }

    private TransactionError executeSellTransaction(Transaction transaction){

        Player player = transaction.getPlayer();
        ItemStack is = transaction.getItemStack();

        //check if shop has enough items
        if (!isAdmin()) {
            if(transaction.isCheck()) {
                int shopItems = InventoryUtils.getAmount(this.getInventory(), is);
                if (shopItems < is.getAmount())
                    transaction.setError(TransactionError.INSUFFICIENT_FUNDS_SHOP);
            }
            else {
                //remove items from shop
                InventoryUtils.removeItem(this.getInventory(), is, this.getOwner());
            }
        }

        if(transaction.getError() == null) {
            if (transaction.isCheck()) {
                //check if player has enough currency
                boolean hasFunds = EconomyUtils.hasSufficientFunds(player, player.getInventory(), transaction.getPrice());
                if (!hasFunds)
                    transaction.setError(TransactionError.INSUFFICIENT_FUNDS_PLAYER);
            } else {
                //remove currency from player
                EconomyUtils.removeFunds(player, player.getInventory(), transaction.getPrice());
            }
        }

        if(transaction.getError() == null) {
            //check if shop has enough room to accept currency
            if (!isAdmin()) {
                if (transaction.isCheck()) {
                    boolean hasRoom = EconomyUtils.canAcceptFunds(this.getOwner(), this.getInventory(), transaction.getPrice());
                    if (!hasRoom)
                        transaction.setError(TransactionError.INVENTORY_FULL_SHOP);
                } else {
                    //add currency to shop
                    EconomyUtils.addFunds(this.getOwner(), this.getInventory(), transaction.getPrice());
                }
            }
        }

        if(transaction.getError() == null) {
            if (transaction.isCheck()) {
                //check if player has enough room to accept items
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), is, player);
                if (!hasRoom)
                    transaction.setError(TransactionError.INVENTORY_FULL_PLAYER);
            } else {
                //add items to player's inventory
                InventoryUtils.addItem(player.getInventory(), is, player);
            }
        }

        player.updateInventory();

        if(transaction.getError() != null){
            return transaction.getError();
        }

        //if there are no issues with the test/check transaction
        if(transaction.getError() == null && transaction.isCheck()){

            PlayerExchangeShopEvent e = new PlayerExchangeShopEvent(player, this);
            Bukkit.getPluginManager().callEvent(e);

            if(e.isCancelled()) {
                transaction.setError(TransactionError.CANCELLED);
                return TransactionError.CANCELLED;
            }

            //run the transaction again after passing checks
            transaction.passCheck();
            return executeSellTransaction(transaction);
        }
        transaction.setError(TransactionError.NONE);
        return TransactionError.NONE;
    }

    private TransactionError executeBuyTransaction(Transaction transaction){

        Player player = transaction.getPlayer();
        ItemStack is = transaction.getItemStack();

        //check if player has enough items
        if(transaction.isCheck()) {
            int playerItems = InventoryUtils.getAmount(player.getInventory(), is);
            if (playerItems < is.getAmount())
                transaction.setError(TransactionError.INSUFFICIENT_FUNDS_PLAYER);
        }
        else {
            //remove items from player
            InventoryUtils.removeItem(player.getInventory(), is, player);
        }

        if(transaction.getError() == null) {
            //check if shop has enough currency
            if(!this.isAdmin()) {
                if(transaction.isCheck()) {
                    boolean hasFunds = EconomyUtils.hasSufficientFunds(this.getOwner(), this.getInventory(), transaction.getPrice());
                    if (!hasFunds)
                        transaction.setError(TransactionError.INSUFFICIENT_FUNDS_SHOP);
                }
                else {
                    EconomyUtils.removeFunds(this.getOwner(), this.getInventory(), transaction.getPrice());
                }
            }
        }

        if(transaction.getError() == null) {
            if(transaction.isCheck()) {
                //check if player has enough room to accept currency
                boolean hasRoom = EconomyUtils.canAcceptFunds(player, player.getInventory(), transaction.getPrice());
                if (!hasRoom)
                    transaction.setError(TransactionError.INVENTORY_FULL_PLAYER);
            }
            else {
                //add currency to player
                EconomyUtils.addFunds(player, player.getInventory(), transaction.getPrice());
            }
        }

        if(transaction.getError() == null) {
            //check if shop has enough room to accept items
            if(!this.isAdmin()) {
                if(transaction.isCheck()) {
                    boolean shopHasRoom = InventoryUtils.hasRoom(this.getInventory(), is, this.getOwner());
                    if (!shopHasRoom)
                        transaction.setError(TransactionError.INVENTORY_FULL_SHOP);
                }
                else{
                    //add items to shop's inventory
                    InventoryUtils.addItem(this.getInventory(), is, this.getOwner());
                }
            }
        }

        player.updateInventory();

        if(transaction.getError() != null){
            return transaction.getError();
        }

        //if there are no issues with the test/check transaction
        if(transaction.getError() == null && transaction.isCheck()){

            PlayerExchangeShopEvent e = new PlayerExchangeShopEvent(player, this);
            Bukkit.getPluginManager().callEvent(e);

            if(e.isCancelled()) {
                transaction.setError(TransactionError.CANCELLED);
                return TransactionError.CANCELLED;
            }

            //run the transaction again after passing checks
            transaction.passCheck();
            return executeBuyTransaction(transaction);
        }
        transaction.setError(TransactionError.NONE);
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

    @Override
    protected int calculateStock(){
        if(this.isAdmin) {
            stock = Integer.MAX_VALUE;
        }
        else {
            double funds = EconomyUtils.getFunds(this.getOwner(), this.getInventory());
            if (this.getPrice() == 0)
                stock = Integer.MAX_VALUE;
            else{
                stock = (int)(funds / this.getPrice());
            }
        }
        return stock;
    }
}
