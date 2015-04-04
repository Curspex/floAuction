package com.flobi.floAuction.events;

import org.bukkit.event.*;
import org.bukkit.entity.*;
import com.flobi.floAuction.*;

public class AuctionBidEvent extends Event implements Cancellable
{
    private static final HandlerList handlers;
    private boolean cancelled;
    private Player player;
    private Auction auction;
    private double bidAmount;
    private double hiddenMaxBid;
    private boolean isBuy;
    
    static {
        handlers = new HandlerList();
    }
    
    public AuctionBidEvent(final Player player, final Auction auction, final double bidAmount, final double hiddenMaxBid, final boolean isBuy) {
        this.player = player;
        this.auction = auction;
        this.bidAmount = bidAmount;
        this.hiddenMaxBid = hiddenMaxBid;
        this.isBuy = isBuy;
        this.cancelled = false;
    }
    
    public HandlerList getHandlers() {
        return AuctionBidEvent.handlers;
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
    
    public double getBidAmount() {
        return this.bidAmount;
    }
    
    public double getHiddenMaxBid() {
        return this.hiddenMaxBid;
    }
    
    public boolean getIsBuy() {
        return this.isBuy;
    }
}
