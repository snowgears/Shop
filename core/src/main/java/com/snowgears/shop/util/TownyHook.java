package com.snowgears.shop.util;

import com.palmergames.bukkit.towny.utils.ShopPlotUtil;
import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TownyHook {

    public static final String PLUGIN_NAME = "Towny";

    public static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
    }

    public static boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
    }


    public static boolean canCreateShop(Player player, Location location) {
        if (!Shop.getPlugin().hookTowny()) {
            return true;
        }
        if (player.isOp() || (Shop.getPlugin().usePerms() && player.hasPermission("shop.operator"))) {
            return true;
        }
        try {
//            if (!TownyAPI.getInstance().isWilderness(player.getLocation())) {
//                Town town = TownyAPI.getInstance().getTownBlock(player.getLocation()).getTown();
//                Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
//                if (!resident.getTown().equals(town)) {
//                    return false;
//                }
//            }
            //this is what the Towny API said to use specifically for Shop developers
            if(!ShopPlotUtil.doesPlayerHaveAbilityToEditShopPlot(player, location)) {
                return false;
            }
        } catch (Exception | NoClassDefFoundError ignore) {
        }
        return true;
    }
}