package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.ShopType;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OfflineTransactions {

    private UUID playerUUID;
    private long lastPlayed;
    private boolean isCalculating;
    private int numTransactions;
    private double totalProfit;
    private double totalSpent;
    private Map<ItemStack, Integer> itemsBought;
    private Map<ItemStack, Integer> itemsSold;

    private List<String> txStrings = new ArrayList<>();

    public OfflineTransactions(UUID playerUUID, long lastPlayed){
        this.playerUUID = playerUUID;
        this.lastPlayed = lastPlayed;
        calculate();
    }

    public void addTx(Location location, ShopType transactionType, double price, OfflinePlayer purchaser, int amount, ItemStack itemSold, ItemStack barterItem) {
        // load message and perform initial formatting
        String formattedMessage = ShopMessage.getMessageFromOrders(transactionType, "owner", price, amount);
        // Add rest of the formatting
        PlaceholderContext context = new PlaceholderContext();
        context.setOfflinePlayer(purchaser);
        context.setItem(itemSold);
        context.setBarterItem(barterItem);
        context.setLocation(location);
        formattedMessage = ShopMessage.format("• " + formattedMessage, context).toLegacyText();
        txStrings.add(formattedMessage);
    }

    public String getTransactionsLore(){
        return String.join("\n", txStrings);
    }

    private void calculate(){
        isCalculating = true;
        Shop.getPlugin().getLogHandler().calculateOfflineTransactions(this);
    }

    public UUID getPlayerUUID(){
        return playerUUID;
    }

    public long getLastPlayed(){
        return lastPlayed;
    }

    public boolean isCalculating(){
        return isCalculating;
    }
    public void setIsCalculating(boolean isCalculating){
        this.isCalculating = isCalculating;
    }
    public int getNumTransactions(){
        return numTransactions;
    }
    public void setNumTransactions(int numTransactions){
        this.numTransactions = numTransactions;
    }
    public double getTotalProfit(){
        return totalProfit;
    }
    public void setTotalProfit(double totalProfit){
        this.totalProfit = totalProfit;
    }
    public double getTotalSpent(){
        return totalSpent;
    }
    public void setTotalSpent(double totalSpent){
        this.totalSpent = totalSpent;
    }
    public Map<ItemStack, Integer> getItemsBought(){
        return itemsBought;
    }
    public void setItemsBought(Map<ItemStack, Integer> itemsBought){
        this.itemsBought = itemsBought;
    }
    public Map<ItemStack, Integer> getItemsSold(){
        return itemsSold;
    }
    public void setItemsSold(Map<ItemStack, Integer> itemsSold){
        this.itemsSold = itemsSold;
    }
}
