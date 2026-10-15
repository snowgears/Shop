package com.snowgears.shop.hook;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import com.snowgears.shop.Shop;
import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.event.PlayerResizeShopEvent;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.event.PlayerOpenShopEvent;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimTransferEvent;


public class GriefPreventionHookListener implements Listener {

    private static final String OTHERS_SHOPS_MESSAGE = "claimOtherPlayerShop";
    private static final String ADMIN_SHOPS_MESSAGE = "claimAdminShop";

    private GriefPrevention gpPlugin = null;
    private Shop plugin = null;

    public GriefPreventionHookListener() {
        plugin = Shop.getPlugin();
        if (checkPluginEnabled()) {
            Plugin p = Bukkit.getPluginManager().getPlugin("GriefPrevention");
            if (p instanceof GriefPrevention) {
                gpPlugin = (GriefPrevention) p;
            }
        }
    }

    // Creating, destroying and resizing a shop needs build permission in every claim its blocks are in.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShopCreate(PlayerCreateShopEvent e) {
        if (!checkPluginEnabledAndVarSet())
            return;
        denyWithoutBuild(e, e.getPlayer(), e.getShop());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShopResize(PlayerResizeShopEvent e) {
        if (!checkPluginEnabledAndVarSet())
            return;
        String denialMsg = denial(e.getPlayer(), e.getLocation(), ClaimPermission.Build);
        if (denialMsg != null) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(denialMsg);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShopDestroy(PlayerDestroyShopEvent e) {
        if (!checkPluginEnabledAndVarSet())
            return;
        denyWithoutBuild(e, e.getPlayer(), e.getShop());
    }

    // Lets players with container trust in the chest's claim open another player's shop chest.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShopOpen(PlayerOpenShopEvent e) {
        if (!checkPluginEnabledAndVarSet())
            return;
        if (e.getTarget() != PlayerOpenShopEvent.OpenTarget.CHEST || e.getMode() == PlayerOpenShopEvent.OpenMode.OPEN_CONTAINER)
            return;
        Claim claim = gpPlugin.dataStore.getClaimAt(e.getShop().getChestLocation(), false, null);
        // ClaimPermission.Inventory is container trust
        if (claim != null && claim.checkPermission(e.getPlayer(), ClaimPermission.Inventory, null) == null) {
            e.setMode(PlayerOpenShopEvent.OpenMode.OPEN_CONTAINER);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClaimCreation(ClaimCreatedEvent e) {
        if (!checkPluginEnabledAndVarSet())
            return;
        refuseClaimOverOthersShops(e, verifyShopsWithinClaim(e.getClaim(), null), e.getCreator());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClaimResize(ClaimResizeEvent e) {
        if (!checkPluginEnabledAndVarSet())
            return;
        refuseClaimOverOthersShops(e, verifyShopsWithinClaim(e.getTo(), e.getFrom()), e.getModifier());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClaimTransfer(ClaimTransferEvent e) {
        if (!checkPluginEnabledAndVarSet())
            return;
        refuseClaimOverOthersShops(e, verifyShopsWithinTransferredClaim(e.getClaim(), e.getNewOwner()), null);
    }

    private void denyWithoutBuild(Cancellable e, Player player, AbstractShop shop) {
        String denialMsg = denial(player, shop.getChestLocation(), ClaimPermission.Build);
        if (denialMsg == null)
            denialMsg = denial(player, shop.getSignLocation(), ClaimPermission.Build);
        if (denialMsg != null) {
            e.setCancelled(true);
            player.sendMessage(denialMsg);
        }
    }

    /**
     * GriefPrevention's denial message for the player at the location, or null if the location is unclaimed or the
     * player has the permission there.
     */
    private @Nullable String denial(Player player, Location location, ClaimPermission permission) {
        Claim claim = gpPlugin.dataStore.getClaimAt(location, false, null);
        if (claim == null)
            return null;
        // checkPermission returns null when the permission is granted
        Supplier<String> denial = claim.checkPermission(player, permission, null);
        return denial == null ? null : denial.get();
    }

    /**
     * Cancels a claim change refused with the {@code interactionIssue} message {@code result}, unless the actor is the
     * console or a shop operator.
     */
    private void refuseClaimOverOthersShops(Cancellable e, @Nullable String result, @Nullable CommandSender actor) {
        if (result == null || (actor != null && (!(actor instanceof Player) || isShopOperator(actor))))
            return;
        e.setCancelled(true);
        if (actor != null) {
            ShopMessage.sendMessage("interactionIssue", result, (Player) actor, null);
        } else {
            String message = ShopMessage.getUnformattedMessage("interactionIssue", result);
            plugin.getLogger().info("Refused GriefPrevention claim change: "
                    + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', String.valueOf(message))));
        }
    }

    private boolean isShopOperator(CommandSender sender) {
        return plugin.usePerms() ? sender.hasPermission("shop.operator") : sender.isOp();
    }

    /**
     * Checks the shops the claim covers that {@code previous} did not: each owner must be able to build in the claim,
     * and admin shops may only be in admin claims.
     *
     * @return the {@code interactionIssue} message key, or null if every such shop may be in the claim
     */
    private @Nullable String verifyShopsWithinClaim(Claim c, @Nullable Claim previous) {
        for (AbstractShop s : shopsWithin(c)) {
            if (previous != null && covers(previous, s))
                continue;
            UUID owner = s.getOwnerUUID();
            if (owner.equals(plugin.getShopHandler().getAdminUUID())) {
                if (!c.isAdminClaim())
                    return ADMIN_SHOPS_MESSAGE;
            } else if (c.checkPermission(owner, ClaimPermission.Build, null) != null) {
                return OTHERS_SHOPS_MESSAGE;
            }
        }
        return null;
    }

    // GP's Claim copy constructor sets both corners to the greater one, so the transfer is checked on the claim itself.
    private @Nullable String verifyShopsWithinTransferredClaim(Claim c, @Nullable UUID newOwner) {
        for (AbstractShop s : shopsWithin(c)) {
            UUID owner = s.getOwnerUUID();
            if (owner.equals(plugin.getShopHandler().getAdminUUID())) {
                if (newOwner != null)
                    return ADMIN_SHOPS_MESSAGE;
            } else if (!owner.equals(newOwner) &&
                    (owner.equals(c.getOwnerID()) || c.checkPermission(owner, ClaimPermission.Build, null) != null)) {
                return OTHERS_SHOPS_MESSAGE;
            }
        }
        return null;
    }

    private List<AbstractShop> shopsWithin(Claim c) {
        Location lesser = c.getLesserBoundaryCorner();
        Location greater = c.getGreaterBoundaryCorner();
        Location center = lesser.clone().add(greater.clone().subtract(lesser).multiply(0.5));
        int chunkRadius = Math.max(greater.getBlockX() - lesser.getBlockX(), greater.getBlockZ() - lesser.getBlockZ()) / 32 + 1;
        List<AbstractShop> shops = plugin.getShopHandler().getShopsNearLocation(center, chunkRadius);
        shops.removeIf(s -> !covers(c, s));
        return shops;
    }

    private boolean covers(Claim c, AbstractShop s) {
        return c.contains(s.getChestLocation(), true, false) || c.contains(s.getSignLocation(), true, false);
    }

    private boolean checkPluginEnabledAndVarSet() {
        if (checkPluginEnabled() && this.gpPlugin != null) {
            return true;
        }
        return false;
    }

    private boolean checkPluginEnabled() {
        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") != null &&
                Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            return true;
        }
        return false;
    }
}
