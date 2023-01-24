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

    @Override
    public TransactionError executeTransaction(Transaction transaction) {

        this.isPerformingTransaction = true;

        Player player = transaction.getPlayer();
        ItemStack is = transaction.getItemStack();
        ItemStack is2 = transaction.getSecondaryItemStack();

        //check if shop has enough items
        if (!this.isAdmin()) {
            if(transaction.isCheck()) {
                int shopItems = InventoryUtils.getAmount(this.getInventory(), is);
                if (shopItems < is.getAmount()) {
                    transaction.setError(TransactionError.INSUFFICIENT_FUNDS_SHOP);
                }
            }
            else {
                //remove items from shop
                InventoryUtils.removeItem(this.getInventory(), is, this.getOwner());
            }
        }

        if(transaction.getError() == null) {
            if(transaction.isCheck()) {
                //check if player has enough barter items
                int playerItems = InventoryUtils.getAmount(player.getInventory(), is2);
                if (playerItems < is2.getAmount()) {
                    transaction.setError(TransactionError.INSUFFICIENT_FUNDS_PLAYER);
                }
            }
            else {
                //remove barter items from player
                InventoryUtils.removeItem(player.getInventory(), is2, player);
            }
        }

        if(transaction.getError() == null) {
            //check if shop has enough room to accept barter items
            if (!this.isAdmin()) {
                if(transaction.isCheck()) {
                    boolean hasRoom = InventoryUtils.hasRoom(this.getInventory(), is2, this.getOwner());
                    if (!hasRoom) {
                        transaction.setError(TransactionError.INVENTORY_FULL_SHOP);
                    }
                }
                else {
                    //add barter items to shop
                    InventoryUtils.addItem(this.getInventory(), is2, this.getOwner());
                }
            }
        }

        if(transaction.getError() == null) {
            if(transaction.isCheck()) {
                //check if player has enough room to accept items
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), is, player);
                if (!hasRoom) {
                    transaction.setError(TransactionError.INVENTORY_FULL_PLAYER);
                }
            }
            else {
                //add items to player's inventory
                InventoryUtils.addItem(player.getInventory(), is, player);
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

        //if the shop is connected to an ender inventory, save the contents as needed
        if(!isAdmin && this.chestLocation != null && this.chestLocation.getBlock().getType() == Material.ENDER_CHEST){
            Shop.getPlugin().getEnderChestHandler().saveInventory(this.getOwner());
        }

        this.isPerformingTransaction = false;
        setGuiIcon();
        transaction.setError(TransactionError.NONE);
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
