package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.display.DisplayType;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class GambleShop extends AbstractShop {

    private ItemStack gambleItem;

    public GambleShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
        super(signLoc, player, pri, amt, admin, facing);

        this.isAdmin = true;
        this.type = ShopType.GAMBLE;
        this.signLines = ShopMessage.getSignLines(this, this.type);
        this.gambleItem = Shop.getPlugin().getDisplayListener().getRandomItem(this);
        this.setAmount(this.gambleItem.getAmount());
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(Player player, boolean isCheck, ShopType transactionType) {

        this.isPerformingTransaction = true;
        TransactionError issue = null;

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

        if(issue == null) {
            if (isCheck) {
                //check if player has enough currency
                boolean hasFunds = EconomyUtils.hasSufficientFunds(player, player.getInventory(), this.getPrice());
                if (!hasFunds) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
                }
            } else {
                //remove currency from player
                EconomyUtils.removeFunds(player, player.getInventory(), this.getPrice());
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

        if(issue == null) {
            if (isCheck) {
                //System.out.println("[Shop] checking inventory of player. "+gambleItem.getType().toString()+" (x"+gambleItem.getAmount()+")");
                //check if player has enough room to accept items
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), gambleItem, player);
                if (!hasRoom)
                    issue = TransactionError.INVENTORY_FULL_PLAYER;
            } else {
                //add items to player's inventory
                InventoryUtils.addItem(player.getInventory(), gambleItem, player);
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
            return executeTransaction(player, false, transactionType);
        }

        this.shuffleGambleItem(player);

        //set isPerformaingTransaction after shuffling is done
        //this.isPerformingTransaction = false;
        //setGuiIcon();
        return TransactionError.NONE;
    }

    public void shuffleGambleItem(Player player){

        this.setItemStack(gambleItem.clone());
        this.setAmount(gambleItem.getAmount());
        final DisplayType initialDisplayType = this.getDisplay().getType();
        this.getDisplay().setType(DisplayType.ITEM, false);
        this.gambleItem = Shop.getPlugin().getDisplayListener().getRandomItem(this);
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

    public ItemStack getGambleItem(){
        return gambleItem;
    }
}
