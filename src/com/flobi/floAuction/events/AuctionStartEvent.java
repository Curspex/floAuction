package com.flobi.floAuction.events;

import org.bukkit.event.*;
import org.bukkit.entity.*;
import com.flobi.floAuction.*;

public class AuctionStartEvent extends Event implements Cancellable
{
    private static final HandlerList handlers;
    private boolean cancelled;
    private Player player;
    private Auction auction;
    
    static {
        handlers = new HandlerList();
    }
    
    public AuctionStartEvent(final Player player, final Auction auction) {
        this.player = player;
        this.auction = auction;
        this.cancelled = false;
    }
    
    public HandlerList getHandlers() {
        return AuctionStartEvent.handlers;
    }
    
    public boolean isCancelled() {
        return this.cancelled;
    }
    
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    public Player getPlayer() {
        return this.player;
    }
    
    public Auction getAuction() {
        return this.auction;
    }
}
