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
//                System.out.println("hasSufficientFunds is "+(exp > amount));
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
//                System.out.println("getFunds is "+getExperience(player));
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
                return false;
            case EXPERIENCE:
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
//                    System.out.println("removeFunds before: "+onlinePlayer.getTotalExperience());
                    setTotalExperience(onlinePlayer, getTotalExperience(onlinePlayer) - (int)amount);
//                    System.out.println("removeFunds after: "+onlinePlayer.getTotalExperience());
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
//                    System.out.println("addFunds before: "+onlinePlayer.getTotalExperience());
                    setTotalExperience(onlinePlayer, getTotalExperience(onlinePlayer) + (int)amount);
//                    System.out.println("addFunds after: "+onlinePlayer.getTotalExperience());
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
            return getTotalExperience(player.getPlayer());
        } else {
            PlayerExperience expData = PlayerExperience.loadFromFile(player);
            if (expData != null) {
                return expData.getExperience();
            }
        }
        return 0;
    }

    public static int getTotalExperience(int level) {
        int xp = 0;

        if (level >= 0 && level <= 15) {
            xp = (int) Math.round(Math.pow(level, 2) + 6 * level);
        } else if (level > 15 && level <= 30) {
            xp = (int) Math.round((2.5 * Math.pow(level, 2) - 40.5 * level + 360));
        } else if (level > 30) {
            xp = (int) Math.round(((4.5 * Math.pow(level, 2) - 162.5 * level + 2220)));
        }
        return xp;
    }

    public static int getTotalExperience(Player player) {
        return Math.round(player.getExp() * player.getExpToLevel()) + getTotalExperience(player.getLevel());
    }

    public static void setTotalExperience(Player player, int amount) {
        int level = 0;
        int xp = 0;
        float a = 0;
        float b = 0;
        float c = -amount;

        if (amount > getTotalExperience(0) && amount <= getTotalExperience(15)) {
            a = 1;
            b = 6;
        } else if (amount > getTotalExperience(15) && amount <= getTotalExperience(30)) {
            a = 2.5f;
            b = -40.5f;
            c += 360;
        } else if (amount > getTotalExperience(30)) {
            a = 4.5f;
            b = -162.5f;
            c += 2220;
        }
        level = (int) Math.floor((-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a));
        xp = amount - getTotalExperience(level);
        player.setLevel(level);
        player.setExp(0);
        player.giveExp(xp);
    }
}
