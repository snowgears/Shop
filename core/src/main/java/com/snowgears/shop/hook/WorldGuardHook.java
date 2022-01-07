package com.snowgears.shop.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldGuardHook {

    public static final String PLUGIN_NAME = "WorldGuard";
    // This flag got originally registered by WorldGuard itself, but this is no longer the case. Other plugins are
    // supposed to register it themselves. One such plugin is for example ChestShop. To not rely on other plugins for
    // registering this flag, we will register it ourselves if no other plugin has registered it yet.
    private static final String FLAG_ALLOW_SHOP = "allow-shop";

    public static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
    }

    public static boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
    }

    // Note: WorldGuard only allows registering flags before it got enabled.
    public static void registerAllowShopFlag() {
        if (getPlugin() == null) return; // WorldGuard is not loaded
            Internal.registerAllowShopFlag();
    }

    // Separate class that gets only accessed if WorldGuard is present. Avoids class loading issues.
    private static class Internal {

        public static void registerAllowShopFlag() {
            System.out.println("[Shop] Registering WorldGuard flag '" + FLAG_ALLOW_SHOP + "'.");
            try {
                StateFlag allowShopFlag = new StateFlag(FLAG_ALLOW_SHOP, false);
                WorldGuard.getInstance().getFlagRegistry().register(allowShopFlag);
            } catch (FlagConflictException | IllegalStateException e) {
                // Another plugin has probably already registered this flag,
                // or this plugin got hard reloaded by some plugin manager plugin.
                System.out.println("[Shop] [SEVERE] Couldn't register WorldGuard flag '" + FLAG_ALLOW_SHOP + "': " + e.getMessage());
            }
        }

        public static boolean isShopAllowed(Plugin worldGuardPlugin, Player player, Location loc) {
            assert worldGuardPlugin instanceof WorldGuardPlugin && worldGuardPlugin.isEnabled() && player != null && loc != null;
            WorldGuardPlugin wgPlugin = (WorldGuardPlugin) worldGuardPlugin;
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();

            // Check if shop flag is set:
            boolean allowShopFlag = false; // false if unset or disallowed

            // Get shop flag:
            Flag<?> shopFlag = WorldGuard.getInstance().getFlagRegistry().get(FLAG_ALLOW_SHOP);
            if (shopFlag != null) {
                // Check if shop flag is set:
                if (shopFlag instanceof StateFlag) {
                    allowShopFlag = query.testState(BukkitAdapter.adapt(loc), wgPlugin.wrapPlayer(player), (StateFlag) shopFlag);
                } else if (shopFlag instanceof BooleanFlag) {
                    // Value might be null:
                    Boolean shopFlagValue = query.queryValue(BukkitAdapter.adapt(loc), wgPlugin.wrapPlayer(player), (BooleanFlag) shopFlag);
                    allowShopFlag = (Boolean.TRUE.equals(shopFlagValue));
                } else {
                    // Unknown flag type, assume unset.
                }
            } else {
                // Shop flag doesn't exist, assume unset.
            }

            if (Shop.getPlugin().hookWorldGuard()) {
                // Allow shops ONLY in regions with the shop flag set:
                return allowShopFlag;
            } else {
                // Allow shops in regions where the shop flag is set OR the player can build:
                return (allowShopFlag || query.testState(BukkitAdapter.adapt(loc), wgPlugin.wrapPlayer(player), Flags.BUILD));
            }
        }

        private Internal() {
        }
    }

    public static boolean canCreateShop(Player player, Location location) {
        if (!Shop.getPlugin().hookWorldGuard()) {
            return true;
        }
        if (player.isOp() || (Shop.getPlugin().usePerms() && player.hasPermission("shop.operator"))) {
            return true;
        }
        try {
            Plugin wgPlugin = getPlugin();
            if (wgPlugin == null || !wgPlugin.isEnabled()) return true;
            return Internal.isShopAllowed(wgPlugin, player, location);
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

    public static boolean isRegionOwner(Player player, Location location) {
        if (!Shop.getPlugin().hookWorldGuard()) {
            return false;
        }
        try {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(localPlayer.getWorld());
            BlockVector3 vLoc = BlockVector3.at(location.getX(), location.getY(), location.getZ());
            if(regions == null)
                return false;
            if(regions.getApplicableRegions(vLoc).isOwnerOfAll(localPlayer)){
                return true;
            }
        } catch (NoClassDefFoundError ignore) {
        }
        return false;
    }
}