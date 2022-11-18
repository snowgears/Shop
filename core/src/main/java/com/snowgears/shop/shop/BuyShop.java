package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BuyShop extends AbstractShop {

    public BuyShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
        super(signLoc, player, pri, amt, admin, facing);

        this.type = ShopType.BUY;
        this.signLines = ShopMessage.getSignLines(this, this.type);
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(Transaction transaction) {

        this.isPerformingTransaction = true;

        Player player = transaction.getPlayer();
        ItemStack is = transaction.getItemStack();

        //check if player has enough items
        if(transaction.isCheck()) {
            int playerItems = InventoryUtils.getAmount(player.getInventory(), is);
            if (playerItems < is.getAmount()) {
                transaction.setError(TransactionError.INSUFFICIENT_FUNDS_PLAYER);
            }
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
                    if (!hasFunds) {
                        transaction.setError(TransactionError.INSUFFICIENT_FUNDS_SHOP);
                    }
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
                if (!hasRoom) {
                    transaction.setError(TransactionError.INVENTORY_FULL_PLAYER);
                }
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
                    if (!shopHasRoom) {
                        transaction.setError(TransactionError.INVENTORY_FULL_SHOP);
                    }
                }
                else{
                    //add items to shop's inventory
                    InventoryUtils.addItem(this.getInventory(), is, this.getOwner());
                }
            }
        }

        player.updateInventory();

        if(transaction.getError() != null){
            isPerformingTransaction = false;
            return transaction.getError();
        }

        //if there are no issues with the test/check transaction
        if(transaction.getError() == null && transaction.isCheck()){

            PlayerExchangeShopEvent e = new PlayerExchangeShopEvent(player, this);
            Bukkit.getPluginManager().callEvent(e);

            if(e.isCancelled()) {
                this.isPerformingTransaction = false;
                transaction.setError(TransactionError.CANCELLED);
                return TransactionError.CANCELLED;
            }

            //run the transaction again after passing checks
            transaction.passCheck();
            return executeTransaction(transaction);
        }
        this.isPerformingTransaction = false;
        setGuiIcon();
        transaction.setError(TransactionError.NONE);
        return TransactionError.NONE;
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
                if(stock == 0 && Shop.getPlugin().getAllowPartialSales()){
                    double pricePer = this.getPrice() / this.getItemStack().getAmount();
                    if(funds >= pricePer){
                        stock = 1;
                    }
                }
            }
        }
        return stock;
    }
}
