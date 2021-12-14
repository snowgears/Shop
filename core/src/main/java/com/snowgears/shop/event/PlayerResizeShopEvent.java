package com.snowgears.shop.event;

import com.snowgears.shop.shop.AbstractShop;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerResizeShopEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private AbstractShop shop;
    private Location location;
    private boolean isExpansion;
    private boolean cancelled;

    public PlayerResizeShopEvent(Player p, AbstractShop s, Location location, boolean isExpansion) {
        player = p;
        shop = s;
        this.location = location;
        this.isExpansion = isExpansion;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public AbstractShop getShop() {
        return shop;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isExpansion() {
        return isExpansion;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean set) {
        cancelled = set;
    }
}
