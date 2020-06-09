package com.snowgears.shop.util;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.snowgears.shop.Shop;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook {
    private static final StateFlag ENABLE_SHOP;

    static {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        StateFlag flag = new StateFlag("allow-shop", false);
        try {
            registry.register(flag);
        } catch (FlagConflictException | IllegalStateException e) {
        	Flag<?> before = registry.get("allow-shop");
        	if (before instanceof StateFlag) {
            flag = (StateFlag) before;
        	}else { 
        	flag = new StateFlag("shop-area", false);
        	before = registry.get("shop-area");
        	if (before instanceof StateFlag) {
                flag = (StateFlag) before;
            	}
        	}
        }
        ENABLE_SHOP = flag;
    }

    public static boolean canCreateShop(Player player, Location location) {
        if (!Shop.getPlugin().hookWorldGuard()) {
            return true;
        }
        if (player.isOp() || (Shop.getPlugin().usePerms() && player.hasPermission("shop.operator"))) {
            return true;
        }
        try {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            BlockVector3 vLoc = BlockVector3.at(location.getX(), location.getY(), location.getZ());
            RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(localPlayer.getWorld());
            return regions == null || regions.getApplicableRegions(vLoc).queryState(localPlayer, ENABLE_SHOP) == StateFlag.State.ALLOW;
        } catch (Exception | NoClassDefFoundError ignore) {
        }
        return true;
    }

    public static boolean canUseShop(Player player, Location location) {
        if (!Shop.getPlugin().hookWorldGuard()) {
            return true;
        }
        if (player.isOp() || (Shop.getPlugin().usePerms() && player.hasPermission("shop.operator"))) {
            return true;
        }
        try {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(localPlayer.getWorld());
            BlockVector3 vLoc = BlockVector3.at(location.getX(), location.getY(), location.getZ());
            return regions == null || regions.getApplicableRegions(vLoc).queryState(localPlayer, Flags.USE) != StateFlag.State.DENY;
        } catch (NoClassDefFoundError ignore) {
        }
        return true;
    }
}
