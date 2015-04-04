package com.flobi.floAuction.events;

import org.bukkit.event.*;
import com.flobi.floAuction.*;

public class AuctionEndEvent extends Event implements Cancellable
{
    private static final HandlerList handlers;
    private boolean cancelled;
    private Auction auction;
    
    static {
        handlers = new HandlerList();
    }
    
    public AuctionEndEvent(final Auction auction, final boolean cancelled) {
        this.auction = auction;
        this.cancelled = cancelled;
    }
    
    public HandlerList getHandlers() {
        return AuctionEndEvent.handlers;
    }
    
    public boolean isCancelled() {
        return this.cancelled;
    }
    
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    public Auction getAuction() {
        return this.auction;
    }
}
