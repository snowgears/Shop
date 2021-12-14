package com.snowgears.shop.shop;

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

public class SellShop extends AbstractShop {

    public SellShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
        super(signLoc, player, pri, amt, admin, facing);

        this.type = ShopType.SELL;
        this.signLines = ShopMessage.getSignLines(this, this.type);
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {

        this.isPerformingTransaction = true;
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
                boolean hasFunds = EconomyUtils.hasSufficientFunds(player, player.getInventory(), this.getPrice());
                if (!hasFunds)
                    issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
            } else {
                //remove currency from player
                EconomyUtils.removeFunds(player, player.getInventory(), this.getPrice());
            }
        }

        if(issue == null) {
            //check if shop has enough room to accept currency
            if (!isAdmin()) {
                if (isCheck) {
                    boolean hasRoom = EconomyUtils.canAcceptFunds(this.getOwner(), this.getInventory(), this.getPrice());
                    if (!hasRoom)
                        issue = TransactionError.INVENTORY_FULL_SHOP;
                } else {
                    //add currency to shop
                    EconomyUtils.addFunds(this.getOwner(), this.getInventory(), this.getPrice());
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
            this.isPerformingTransaction = false;
            return issue;
        }

        //if there are no issues with the test/check transaction
        if(issue == null && isCheck){

            PlayerExchangeShopEvent e = new PlayerExchangeShopEvent(player, this);
            Bukkit.getPluginManager().callEvent(e);

            if(e.isCancelled()) {
                this.isPerformingTransaction = false;
                return TransactionError.CANCELLED;
            }

            //run the transaction again without the check clause
            return executeTransaction(orders, player, false, transactionType);
        }
        this.isPerformingTransaction = false;
        setGuiIcon();
        return TransactionError.NONE;
    }
}
