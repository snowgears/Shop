package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class GambleShop extends AbstractShop {

    private ItemStack gambleItem;

    public GambleShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
        super(signLoc, player, pri, amt, admin, facing);

        this.isAdmin = true;
        this.type = ShopType.GAMBLE;
        this.signLines = ShopMessage.getSignLines(this, this.type);
        setGambleItem();
        this.setAmount(this.gambleItem.getAmount());
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(Transaction transaction) {

        this.isPerformingTransaction = true;

        Player player = transaction.getPlayer();

        //set gamble item to random in the first check
        if (transaction.isCheck()) {
            setGambleItem();
        }

        //commented out because shop type is always admin
        //check if shop has enough items
//        if (!isAdmin()) {
//            if(isCheck) {
//                int shopItems = InventoryUtils.getAmount(this.getInventory(), gambleItem);
//                if (shopItems < gambleItem.getAmount()) {
//                    issue = TransactionError.INSUFFICIENT_FUNDS_SHOP;
//                }
//            }
//            else {
//                //remove items from shop
//                InventoryUtils.removeItem(this.getInventory(), gambleItem, this.getOwner());
//            }
//        }

        if(transaction.getError() == null) {
            if (transaction.isCheck()) {
                //check if player has enough currency
                boolean hasFunds = EconomyUtils.hasSufficientFunds(player, player.getInventory(), transaction.getPrice());
                if (!hasFunds) {
                    transaction.setError(TransactionError.INSUFFICIENT_FUNDS_PLAYER);
                }
            } else {
                //remove currency from player
                EconomyUtils.removeFunds(player, player.getInventory(), transaction.getPrice());
            }
        }

        //commented out because shop type is always admin
//        if(issue == null) {
//            //check if shop has enough room to accept currency
//            if (!isAdmin()) {
//                if (isCheck) {
//                    boolean hasRoom = EconomyUtils.canAcceptFunds(this.getOwner(), this.getInventory(), this.getPrice());
//                    if (!hasRoom)
//                        issue = TransactionError.INVENTORY_FULL_SHOP;
//                } else {
//                    //add currency to shop
//                    EconomyUtils.addFunds(this.getOwner(), this.getInventory(), this.getPrice());
//                }
//            }
//        }

        if(transaction.getError() == null) {
            if (transaction.isCheck()) {
                //System.out.println("[Shop] checking inventory of player. "+gambleItem.getType().toString()+" (x"+gambleItem.getAmount()+")");
                //check if player has enough room to accept items
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), gambleItem, player);
                if (!hasRoom)
                    transaction.setError(TransactionError.INVENTORY_FULL_PLAYER);
            } else {
                //add items to player's inventory
                InventoryUtils.addItem(player.getInventory(), gambleItem, player);
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

            //run the transaction again after passing checks
            transaction.passCheck();
            return executeTransaction(transaction);
        }

        this.shuffleGambleItem(player);

        //set isPerformaingTransaction after shuffling is done
        //this.isPerformingTransaction = false;
        //setGuiIcon();
        transaction.setError(TransactionError.NONE);
        return TransactionError.NONE;
    }

    public void shuffleGambleItem(Player player){

        this.setItemStack(gambleItem.clone());
        this.setAmount(gambleItem.getAmount());
        final DisplayType initialDisplayType = this.getDisplay().getType();
        this.getDisplay().setType(DisplayType.ITEM, false);
        setGambleItem();
        this.getDisplay().spawn(player); //TODO maybe only show what item player got to the player themselves???

        new BukkitRunnable() {
            @Override
            public void run() {
                setItemStack(Shop.getPlugin().getGambleDisplayItem());
                if(initialDisplayType == null) {
                    display.setType(Shop.getPlugin().getDisplayType(), false);
                    getDisplay().spawn(player); //TODO maybe only show what item player got to the player themselves???
                }
                else {
                    display.setType(initialDisplayType, false);
                    getDisplay().spawn(player); //TODO maybe only show what item player got to the player themselves???
                }
                isPerformingTransaction = false;
            }
        }.runTaskLater(Shop.getPlugin(), 20);
    }

    public void setGambleItem(){
        this.gambleItem = Shop.getPlugin().getDisplayListener().getRandomItem(this);
    }

    public ItemStack getGambleItem(){
        return gambleItem;
    }
}
