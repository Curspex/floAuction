package com.flobi.floAuction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.flobi.floAuction.utility.CArrayList;

public class AuctionParticipant
{
    private String playerName;
    private AuctionScope auctionScope;
    private Location lastKnownGoodLocation;
    private boolean sentEscapeWarning;
    private boolean sentArenaWarning;
    
    public static boolean checkLocation(final String playerName) {
        final AuctionParticipant participant = getParticipant(playerName);
        return participant == null || participant.auctionScope.equals(AuctionScope.getPlayerScope(Bukkit.getPlayer(playerName)));
    }
    
    public static boolean checkLocation(final String playerName, final Location location) {
        final AuctionParticipant participant = getParticipant(playerName);
        return participant == null || participant.auctionScope.equals(AuctionScope.getLocationScope(location));
    }
    
    public static void forceLocation(final String playerName, final Location locationForGaze) {
        final AuctionParticipant participant = getParticipant(playerName);
        if (participant == null) {
            return;
        }
        if (!participant.isParticipating()) {
            return;
        }
        final Player player = Bukkit.getPlayer(playerName);
        final Location location = player.getLocation();
        if (locationForGaze != null) {
            location.setDirection(new Vector(0, 0, 0));
            location.setPitch(locationForGaze.getPitch());
            location.setYaw(locationForGaze.getYaw());
        }
        if (!checkLocation(playerName)) {
            player.teleport(participant.lastKnownGoodLocation);
            participant.sendEscapeWarning();
            return;
        }
        if (floAuction.plugin.arenaManager.isInArena(player)) {
            player.teleport(participant.lastKnownGoodLocation);
            participant.sendArenaWarning();
            return;
        }
        participant.lastKnownGoodLocation = location;
    }
    
    public static boolean checkTeleportLocation(final String playerName, final Location location) {
        final AuctionParticipant participant = getParticipant(playerName);
        if (participant == null) {
            return true;
        }
        if (!participant.isParticipating()) {
            return true;
        }
        if (!checkLocation(playerName, location)) {
            participant.sendEscapeWarning();
            return false;
        }
        if (floAuction.plugin.arenaManager.isInArena(Bukkit.getPlayer(playerName))) {
            participant.sendArenaWarning();
            return false;
        }
        return true;
    }
    
    private void sendArenaWarning() {
        if (this.sentArenaWarning) {
            return;
        }
        floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "arena-warning" }), this.playerName, (AuctionScope)null);
        this.sentArenaWarning = true;
    }
    
    private void sendEscapeWarning() {
        if (this.sentEscapeWarning) {
            return;
        }
        floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "auctionscope-escape-warning" }), this.playerName, (AuctionScope)null);
        this.sentEscapeWarning = true;
    }
    
    public static boolean isParticipating(final String playerName) {
        boolean participating = false;
        for (int i = 0; i < floAuction.auctionParticipants.size(); ++i) {
            final AuctionParticipant participant = floAuction.auctionParticipants.get(i);
            if (participant.isParticipating() && playerName.equalsIgnoreCase(participant.playerName)) {
                participating = true;
            }
        }
        return participating;
    }
    
    public static void addParticipant(final String playerName, final AuctionScope auctionScope) {
        final Player player = Bukkit.getPlayer(playerName);
        if (getParticipant(playerName) == null) {
            final AuctionParticipant participant = new AuctionParticipant(playerName, auctionScope);
            participant.lastKnownGoodLocation = player.getLocation();
            floAuction.auctionParticipants.add(participant);
            participant.isParticipating();
        }
    }
    
    private static AuctionParticipant getParticipant(final String playerName) {
        for (int i = 0; i < floAuction.auctionParticipants.size(); ++i) {
            final AuctionParticipant participant = floAuction.auctionParticipants.get(i);
            if (playerName.equalsIgnoreCase(participant.playerName)) {
                return participant;
            }
        }
        return null;
    }
    
    private AuctionParticipant(final String playerName, final AuctionScope auctionScope) {
        this.playerName = null;
        this.auctionScope = null;
        this.lastKnownGoodLocation = null;
        this.sentEscapeWarning = false;
        this.sentArenaWarning = false;
        this.playerName = playerName;
        this.auctionScope = auctionScope;
    }
    
    public boolean isParticipating() {
        boolean participating = false;
        final Auction scopeAuction = this.auctionScope.getActiveAuction();
        if (scopeAuction != null) {
            if (scopeAuction.getOwner().equalsIgnoreCase(this.playerName)) {
                participating = true;
            }
            if (scopeAuction.getCurrentBid() != null && scopeAuction.getCurrentBid().getBidder().equalsIgnoreCase(this.playerName)) {
                participating = true;
            }
            for (int i = 0; i < scopeAuction.sealedBids.size(); ++i) {
                if (scopeAuction.sealedBids.get(i).getBidder().equalsIgnoreCase(this.playerName)) {
                    participating = true;
                }
            }
        }
        for (int i = 0; i < this.auctionScope.getAuctionQueueLength(); ++i) {
            final Auction queuedAuction = this.auctionScope.getAuctionQueue().get(i);
            if (queuedAuction != null) {
                if (queuedAuction.getOwner().equalsIgnoreCase(this.playerName)) {
                    participating = true;
                }
                if (queuedAuction.getCurrentBid() != null && queuedAuction.getCurrentBid().getBidder().equalsIgnoreCase(this.playerName)) {
                    participating = true;
                }
            }
        }
        if (!participating) {
            floAuction.auctionParticipants.remove(this);
        }
        return participating;
    }
}
