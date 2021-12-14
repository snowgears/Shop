package com.snowgears.shop.shop;

import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.TransactionError;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

import static com.snowgears.shop.shop.BarterShop.BarterType.EXPERIENCE;
import static com.snowgears.shop.shop.BarterShop.BarterType.ITEM;

public class BarterShop extends AbstractShop {

    private ItemStack originalItem;
    private BarterType barterType;

    public BarterShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
        super(signLoc, player, pri, amt, admin, facing);

        this.type = ShopType.BARTER;
        this.barterType = ITEM;
        this.signLines = ShopMessage.getSignLines(this, this.type);
    }

    @Override
    public void setItemStack(ItemStack is) {
        super.setItemStack(is);
        if(originalItem == null){
            originalItem = is.clone();
        }
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {

        this.isPerformingTransaction = true;
        TransactionError issue = null;

        ItemStack is = this.getItemStack();
        ItemStack is2 = this.getSecondaryItemStack();

        //check if shop has enough items
        if (!this.isAdmin()) {
            if(isCheck) {
                int shopItems = InventoryUtils.getAmount(this.getInventory(), is);
                if (shopItems < is.getAmount()) {
                    this.isPerformingTransaction = false;
                    return TransactionError.INSUFFICIENT_FUNDS_SHOP;
                }
            }
            else {
                //remove items from shop
                InventoryUtils.removeItem(this.getInventory(), is, this.getOwner());
            }
        }

        if(issue == null) {
            if(isCheck) {
                //check if player has enough barter items
                int playerItems = InventoryUtils.getAmount(player.getInventory(), is2);
                if (playerItems < is2.getAmount()) {
                    this.isPerformingTransaction = false;
                    return TransactionError.INSUFFICIENT_FUNDS_PLAYER;

                }
            }
            else {
                //remove barter items from player
                InventoryUtils.removeItem(player.getInventory(), is2, player);
            }
        }

        if(issue == null) {
            //check if shop has enough room to accept barter items
            if (!this.isAdmin()) {
                if(isCheck) {
                    boolean hasRoom = InventoryUtils.hasRoom(this.getInventory(), is2, this.getOwner());
                    if (!hasRoom) {
                        this.isPerformingTransaction = false;
                        return TransactionError.INVENTORY_FULL_SHOP;
                    }
                }
                else {
                    //add barter items to shop
                    InventoryUtils.addItem(this.getInventory(), is2, this.getOwner());
                }
            }
        }

        if(issue == null) {
            if(isCheck) {
                //check if player has enough room to accept items
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), is, player);
                if (!hasRoom) {
                    this.isPerformingTransaction = false;
                    return TransactionError.INVENTORY_FULL_PLAYER;
                }
            }
            else {
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

    @Override
    public boolean isInitialized() {
        return (item != null && secondaryItem != null);
    }

    public enum BarterType{
        ITEM, EXPERIENCE
    }

    public void cycleBarterType(){
        //if shops are already using experience as the main currency, don't allow barter shops to barter experience (that would be a sell shop)
        if(Shop.getPlugin().getCurrencyType() == CurrencyType.EXPERIENCE)
            return;

        if(this.barterType == ITEM){
            this.barterType = EXPERIENCE;
        }
        else if(this.barterType == EXPERIENCE){
            this.barterType = ITEM;
        }
    }
}
