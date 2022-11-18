package com.snowgears.shop.shop;

import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SellShop extends AbstractShop {

    public SellShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
        super(signLoc, player, pri, amt, admin, facing);

        this.type = ShopType.SELL;
        this.signLines = ShopMessage.getSignLines(this, this.type);
    }

    @Override
    public TransactionError executeTransaction(Transaction transaction) {

        this.isPerformingTransaction = true;

        Player player = transaction.getPlayer();
        ItemStack itemStack = transaction.getItemStack();

        //check if shop has enough items
        if (!isAdmin()) {
            if(transaction.isCheck()) {
                int shopItems = InventoryUtils.getAmount(this.getInventory(), itemStack);
                if (shopItems < itemStack.getAmount()) {
                    transaction.setError(TransactionError.INSUFFICIENT_FUNDS_SHOP);
                }
            }
            else {
                //remove items from shop
                InventoryUtils.removeItem(this.getInventory(), itemStack, this.getOwner());
            }
        }

        if(transaction.getError() == null) {
            if (transaction.isCheck()) {
                //check if player has enough currency
                boolean hasFunds = EconomyUtils.hasSufficientFunds(player, player.getInventory(), transaction.getPrice());
                if (!hasFunds) {
                    transaction.setError(TransactionError.INSUFFICIENT_FUNDS_PLAYER);
                }
            } else {
//                System.out.println("executeTransaction on sell shop. Economy is "+Shop.getPlugin().getCurrencyType().toString());
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
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), itemStack, player);
                if (!hasRoom)
                    transaction.setError(TransactionError.INVENTORY_FULL_PLAYER);
            } else {
                //add items to player's inventory
                InventoryUtils.addItem(player.getInventory(), itemStack, player);
            }
        }

        player.updateInventory();

        if(transaction.getError() != null){
            this.isPerformingTransaction = false;
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

            //run the transaction again after passing the first check
            transaction.passCheck();
            return executeTransaction(transaction);
        }
        this.isPerformingTransaction = false;
        setGuiIcon();
        transaction.setError(TransactionError.NONE);
        return TransactionError.NONE;
    }
}
