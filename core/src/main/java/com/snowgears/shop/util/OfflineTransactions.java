package com.snowgears.shop.util;

import com.snowgears.shop.Shop;

import java.util.UUID;

public class OfflineTransactions {

    private UUID playerUUID;
    private long lastPlayed;
    private boolean isCalculating;
    private int numTransactions;

    public OfflineTransactions(UUID playerUUID, long lastPlayed){
        this.playerUUID = playerUUID;
        this.lastPlayed = lastPlayed;
        calculate();
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
}
