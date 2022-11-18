package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ComboShop;
import com.snowgears.shop.shop.ShopType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Transaction {

    private Player player;
    private AbstractShop shop;
    private ShopType transactionType;
    private boolean isCheck;
    private ItemStack itemStack;
    private ItemStack secondaryItemStack;
    private double price;
    private TransactionError error;

    public Transaction(Player player, AbstractShop shop, ShopType transactionType) {
        this.player = player;
        this.shop = shop;
        this.transactionType = transactionType;
        this.isCheck = true;
        this.itemStack = shop.getItemStack();
        if(shop.getType() == ShopType.BARTER){
            this.secondaryItemStack = shop.getSecondaryItemStack();
        }
        if(shop instanceof ComboShop && transactionType == ShopType.SELL) {
            this.price = ((ComboShop)shop).getPriceSell();
        }
        else{
            this.price = shop.getPrice();
        }
        this.error = null;
    }

    public Player getPlayer() {
        return player;
    }

    public AbstractShop getShop() {
        return shop;
    }

    public ShopType getType() {
        return transactionType;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void passCheck(){
        isCheck = false;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public ItemStack getSecondaryItemStack(){
        return secondaryItemStack;
    }

    public boolean setAmountCalculatePrice(int primaryItemAmount){
        if(shop.getType() == ShopType.BARTER){
            float crossProductX = ((float)shop.getSecondaryItemStack().getAmount() / (float)shop.getItemStack().getAmount()) * (float)primaryItemAmount;
            int secondaryItemAmount = (int)Math.floor(crossProductX);
            this.price = secondaryItemAmount;
            this.secondaryItemStack.setAmount(secondaryItemAmount);
            this.itemStack.setAmount(primaryItemAmount);
            return (primaryItemAmount > 0 && secondaryItemAmount > 0);
        }
//        else if(shop instanceof ComboShop && transactionType == ShopType.SELL) {
//            float percentage = (float) primaryItemAmount / (float) itemStack.getAmount();
//            this.itemStack.setAmount(primaryItemAmount);
//            this.price = ((ComboShop)shop).getPriceSell();
//        }
        else{
            float percentage = (float) primaryItemAmount / (float) itemStack.getAmount();
            this.itemStack.setAmount(primaryItemAmount);
            this.price = price * percentage;
            if(Shop.getPlugin().getCurrencyType() == CurrencyType.ITEM){
                return this.price >= 1;
            }
            return true;
        }
    }

    public boolean setSecondaryAmountCalculatePrice(int secondaryItemAmount){
        float crossProductX = (float)secondaryItemAmount / ((float)shop.getSecondaryItemStack().getAmount() / (float)shop.getItemStack().getAmount());
        int primaryItemAmount = (int)Math.floor(crossProductX);
        //System.out.println("Max amount of primary items that equals: "+crossProductX);
        this.price = secondaryItemAmount;
        this.secondaryItemStack.setAmount(secondaryItemAmount);
        this.itemStack.setAmount(primaryItemAmount);
        return (primaryItemAmount > 0 && secondaryItemAmount > 0);
    }

    public int setPriceCalculateAmount(double price){
        double percentage = price / this.price;
        this.price = price;
        int amount = (int) (itemStack.getAmount() * percentage);
        this.itemStack.setAmount(amount);
        return amount;
    }

    public float getPricePerItem(){
        float pricePer = (float)this.getPrice() / (float)this.getItemStack().getAmount();
        return pricePer;
    }

    public TransactionError getError(){
        return error;
    }

    public double getPrice(){
        return price;
    }

    public void setError(TransactionError error){
        this.error = error;
    }
}
