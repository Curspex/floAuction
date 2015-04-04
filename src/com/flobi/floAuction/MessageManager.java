package com.flobi.floAuction;

import java.util.List;

public abstract class MessageManager
{
    public abstract void sendPlayerMessage(final List<String> p0, final String p1, final Auction p2);
    
    public abstract void sendPlayerMessage(final List<String> p0, final String p1, final AuctionScope p2);
    
    public abstract void broadcastAuctionMessage(final List<String> p0, final Auction p1);
    
    public abstract void broadcastAuctionScopeMessage(final List<String> p0, final AuctionScope p1);
}
