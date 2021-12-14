package com.snowgears.shop.event;

import com.snowgears.shop.shop.AbstractShop;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerCreateShopEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private AbstractShop shop;
    private boolean cancelled;

    public PlayerCreateShopEvent(Player p, AbstractShop s) {
        player = p;
        shop = s;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean set) {
        cancelled = set;
    }
}
