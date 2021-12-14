package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class EconomyUtils {

    //check to see if the player has enough funds to take out [amount]
    //return false if they do not
    public static boolean hasSufficientFunds(OfflinePlayer player, Inventory inventory, double amount) {
        switch (Shop.getPlugin().getCurrencyType()) {
            case VAULT:
                double balance = Shop.getPlugin().getEconomy().getBalance(player);
                return (balance >= amount);
            case ITEM:
                ItemStack currency = Shop.getPlugin().getItemCurrency().clone();
                currency.setAmount(1);
                int stock = InventoryUtils.getAmount(inventory, currency);
                return (stock >= amount);
            case EXPERIENCE:
                int exp = getExperience(player);
                return (exp > amount);
            default:
                return false;
        }
    }

    //check to see if the player has enough space to accept the funds to deposit [amount]
    //return false if they do not
    public static boolean canAcceptFunds(OfflinePlayer player, Inventory inventory, double amount) {
        switch (Shop.getPlugin().getCurrencyType()) {
            case VAULT:
            case EXPERIENCE:
                return true;
            case ITEM:
                ItemStack currency = Shop.getPlugin().getItemCurrency().clone();
                currency.setAmount((int) amount);

                return InventoryUtils.hasRoom(inventory, currency, player);
            default:
                return false;
        }
    }

    //gets the current funds of the player
    public static double getFunds(OfflinePlayer player, Inventory inventory){
        switch (Shop.getPlugin().getCurrencyType()) {
            case VAULT:
                double balance = Shop.getPlugin().getEconomy().getBalance(player);
                return balance;
            case EXPERIENCE:
                return getExperience(player);
            case ITEM:
                ItemStack currency = Shop.getPlugin().getItemCurrency().clone();
                currency.setAmount(1);
                int balanceInt = InventoryUtils.getAmount(inventory, currency);
                return balanceInt;
            default:
                return 0;
        }
    }

    //removes [amount] of funds from the player
    //return false if the player did not have sufficient funds or if something went wrong
    public static boolean removeFunds(OfflinePlayer player, Inventory inventory, double amount){
        switch (Shop.getPlugin().getCurrencyType()) {
            case VAULT:
                EconomyResponse response = Shop.getPlugin().getEconomy().withdrawPlayer(player, amount);
                if(response.transactionSuccess())
                    return true;
            case EXPERIENCE:
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
                    onlinePlayer.setTotalExperience(onlinePlayer.getTotalExperience() - (int)amount);
                    return true;
                } else {
                    PlayerExperience expData = PlayerExperience.loadFromFile(player);
                    if (expData != null) {
                        expData.removeExperienceAmount((int)amount);
                        return true;
                    }
                    else{
                        return false;
                    }
                }
            case ITEM:
                ItemStack currency = Shop.getPlugin().getItemCurrency().clone();
                currency.setAmount((int)amount);
                int unremoved = InventoryUtils.removeItem(inventory, currency, player);
                if(unremoved > 0){
                    currency.setAmount(((int)amount) - unremoved);
                    InventoryUtils.addItem(inventory, currency, player);
                    return false;
                }
                return true;
            default:
                return false;
        }
    }

    //adds [amount] of funds to the player
    //return false if the player did not have enough room for items or if something went wrong
    public static boolean addFunds(OfflinePlayer player, Inventory inventory, double amount){
        switch (Shop.getPlugin().getCurrencyType()) {
            case VAULT:
                EconomyResponse response = Shop.getPlugin().getEconomy().depositPlayer(player, amount);
                if(response.transactionSuccess())
                    return true;
            case EXPERIENCE:
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
                    onlinePlayer.setTotalExperience(onlinePlayer.getTotalExperience() + (int)amount);
                    return true;
                } else {
                    PlayerExperience expData = PlayerExperience.loadFromFile(player);
                    if (expData != null) {
                        expData.addExperienceAmount((int)amount);
                        return true;
                    }
                    else{
                        return false;
                    }
                }
            case ITEM:
                ItemStack currency = Shop.getPlugin().getItemCurrency().clone();
                currency.setAmount((int)amount);
                int unadded = InventoryUtils.addItem(inventory, currency, player);
                if(unadded > 0){
                    currency.setAmount(((int)amount) - unadded);
                    InventoryUtils.removeItem(inventory, currency, player);
                    return false;
                }
                return true;
            default:
                return false;
        }
    }

    private static int getExperience(OfflinePlayer player){
        if (player.getPlayer() != null) {
            return player.getPlayer().getTotalExperience();
        } else {
            PlayerExperience expData = PlayerExperience.loadFromFile(player);
            if (expData != null) {
                return expData.getExperience();
            }
        }
        return 0;
    }
}
