package com.flobi.floAuction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.floAuction.events.AuctionBidEvent;
import com.flobi.floAuction.events.AuctionEndEvent;
import com.flobi.floAuction.events.AuctionStartEvent;
import com.flobi.floAuction.utility.CArrayList;
import com.flobi.floAuction.utility.functions;
import com.flobi.floAuction.utility.items;

public class Auction
{
    protected floAuction plugin;
    private String[] args;
    private String ownerName;
    private AuctionScope scope;
    public double extractedPreTax;
    public double extractedPostTax;
    private long startingBid;
    private long minBidIncrement;
    private long buyNow;
    private int quantity;
    private int time;
    private boolean active;
    private AuctionLot lot;
    private AuctionBid currentBid;
    public ArrayList<AuctionBid> sealedBids;
    public boolean sealed;
    public long nextTickTime;
    private int countdown;
    private int countdownTimer;
    public MessageManager messageManager;
    
    
    public AuctionScope getScope() {
        return this.scope;
    }
    
    public Auction(final floAuction plugin, final Player auctionOwner, final String[] inputArgs, final AuctionScope scope, final boolean sealed, final MessageManager messageManager) {
        this.extractedPreTax = 0.0;
        this.extractedPostTax = 0.0;
        this.startingBid = 0L;
        this.minBidIncrement = 0L;
        this.buyNow = 0L;
        this.quantity = 0;
        this.time = 0;
        this.active = false;
        this.currentBid = null;
        this.sealedBids = new ArrayList<AuctionBid>();
        this.sealed = false;
        this.nextTickTime = 0L;
        this.countdown = 0;
        this.countdownTimer = 0;
        this.messageManager = null;
        this.ownerName = auctionOwner.getName();
        this.args = functions.mergeInputArgs(auctionOwner.getName(), inputArgs, false);
        this.plugin = plugin;
        this.scope = scope;
        this.sealed = sealed;
        this.messageManager = messageManager;
    }
    
    public Boolean start() {
        final Player owner = Bukkit.getPlayer(this.ownerName);
        if (plugin.arenaManager.isInArena(owner)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-arena" }), this.ownerName, this);
            return false;
        }
        final ItemStack typeStack = this.lot.getTypeStack();
        double preAuctionTax = AuctionConfig.getDouble("auction-start-tax", this.scope);
        final List<String> bannedItems = AuctionConfig.getStringList("banned-items", this.scope);
        for (int i = 0; i < bannedItems.size(); ++i) {
            if (items.isSameItem(typeStack, bannedItems.get(i))) {
                this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-banned" }), this.ownerName, this);
                return false;
            }
        }
        final Map<String, String> taxedItems = AuctionConfig.getStringStringMap("taxed-items", this.scope);
        if (taxedItems != null) {
            for (final Map.Entry<String, String> entry : taxedItems.entrySet()) {
                if (items.isSameItem(typeStack, entry.getKey())) {
                    final String itemTax = entry.getValue();
                    if (itemTax.endsWith("a")) {
                        try {
                            preAuctionTax = Double.valueOf(itemTax.substring(0, itemTax.length() - 1));
                        }
                        catch (Exception e) {
                            preAuctionTax = AuctionConfig.getDouble("auction-start-tax", this.scope);
                        }
                        break;
                    }
                    if (!itemTax.endsWith("%")) {
                        try {
                            preAuctionTax = Double.valueOf(itemTax);
                            preAuctionTax *= this.quantity;
                        }
                        catch (Exception e) {
                            preAuctionTax = AuctionConfig.getDouble("auction-start-tax", this.scope);
                        }
                        break;
                    }
                    break;
                }
            }
        }
        if (preAuctionTax > 0.0 && !floAuction.econ.has(this.ownerName, preAuctionTax)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-start-tax" }), this.ownerName, this);
            return false;
        }
        if (!this.lot.addItems(this.quantity, true)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-insufficient-supply" }), this.ownerName, this);
            return false;
        }
        if (preAuctionTax > 0.0 && floAuction.econ.has(this.ownerName, preAuctionTax)) {
            floAuction.econ.withdrawPlayer(this.ownerName, preAuctionTax);
            this.extractedPreTax = preAuctionTax;
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-start-tax" }), this.ownerName, this);
            final String taxDestinationUser = AuctionConfig.getString("deposit-tax-to-user", this.scope);
            if (!taxDestinationUser.isEmpty()) {
                floAuction.econ.depositPlayer(taxDestinationUser, preAuctionTax);
            }
        }
        if (this.buyNow < this.getStartingBid()) {
            this.buyNow = 0L;
        }
        final AuctionStartEvent auctionStartEvent = new AuctionStartEvent(owner, this);
        Bukkit.getServer().getPluginManager().callEvent(auctionStartEvent);
        if (auctionStartEvent.isCancelled()) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-blocked-by-other-plugin" }), this.ownerName, this);
        }
        else {
            this.active = true;
            this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { "auction-start" }), this);
            this.countdown = this.time;
            this.countdownTimer = this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, (Runnable)new Runnable() {
                @Override
                public void run() {
                    if (Auction.this.nextTickTime > System.currentTimeMillis()) {
                        return;
                    }
                    Auction.this.nextTickTime += 1000L;
                    final Auction val$thisAuction = Auction.this;
                    Auction.access$1(val$thisAuction, val$thisAuction.countdown - 1);
                    if (Auction.this.countdown <= 0) {
                        Auction.this.end();
                        return;
                    }
                    if (!AuctionConfig.getBoolean("suppress-countdown", Auction.this.scope)) {
                        if (Auction.this.countdown < 4) {
                            Auction.this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { "timer-countdown-notification" }), Auction.this);
                            return;
                        }
                        if (Auction.this.time >= 20 && Auction.this.countdown == Auction.this.time / 2) {
                            Auction.this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { "timer-countdown-notification" }), Auction.this);
                        }
                    }
                }
            }, 1L, 1L);
            this.nextTickTime = System.currentTimeMillis() + 1000L;
            this.info(null, true);
        }
        return this.active;
    }
    
    public void info(final CommandSender sender, final boolean fullBroadcast) {
        final List<String> messageKeys = new ArrayList<String>();
        String playerName = null;
        if (sender instanceof Player) {
            playerName = ((Player)sender).getName();
        }
        final ItemStack itemType = this.getLotType();
        Map<Enchantment, Integer> enchantments = (Map<Enchantment, Integer>)itemType.getEnchantments();
        if (enchantments == null || enchantments.size() == 0) {
            enchantments = items.getStoredEnchantments(itemType);
        }
        if (!this.active) {
            if (sender instanceof Player) {
                this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-info-no-auction" }), playerName, this);
            }
            return;
        }
        messageKeys.add("auction-info");
        if (fullBroadcast) {
            this.messageManager.broadcastAuctionMessage(messageKeys, this);
        }
        else {
            this.messageManager.sendPlayerMessage(messageKeys, playerName, this);
        }
    }
    
    public void cancel() {
        Bukkit.getServer().getPluginManager().callEvent(new AuctionEndEvent(this, true));
        this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { "auction-cancel" }), this);
        if (this.lot != null) {
            this.lot.cancelLot();
        }
        if (this.currentBid != null) {
            this.currentBid.cancelBid();
        }
        this.dispose();
    }
    
    public void confiscate(final Player authority) {
        Bukkit.getServer().getPluginManager().callEvent(new AuctionEndEvent(this, true));
        this.ownerName = authority.getName();
        this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { "confiscate-success" }), this);
        if (this.lot != null) {
            this.lot.winLot(authority.getName());
        }
        if (this.currentBid != null) {
            this.currentBid.cancelBid();
        }
        this.dispose();
    }
    
    public void end() {
        final AuctionEndEvent auctionEndEvent = new AuctionEndEvent(this, false);
        Bukkit.getServer().getPluginManager().callEvent(auctionEndEvent);
        if (auctionEndEvent.isCancelled()) {
            this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { "auction-cancel" }), this);
            if (this.lot != null) {
                this.lot.cancelLot();
            }
            if (this.currentBid != null) {
                this.currentBid.cancelBid();
            }
        }
        else if (this.currentBid == null || this.lot == null) {
            this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { "auction-end-nobids" }), this);
            if (this.lot != null) {
                this.lot.cancelLot();
            }
            if (this.currentBid != null) {
                this.currentBid.cancelBid();
            }
        }
        else {
            this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { "auction-end" }), this);
            this.lot.winLot(this.currentBid.getBidder());
            this.currentBid.winBid();
        }
        this.dispose();
    }
    
    private void dispose() {
        this.plugin.getServer().getScheduler().cancelTask(this.countdownTimer);
        this.sealed = false;
        for (int i = 0; i < this.sealedBids.size(); ++i) {
            this.sealedBids.get(i).cancelBid();
        }
        this.scope.setActiveAuction(null);
    }
    
    public Boolean isValid() {
        if (!this.isValidOwner()) {
            return false;
        }
        if (!this.parseHeldItem()) {
            return false;
        }
        if (!this.parseArgs()) {
            return false;
        }
        if (!this.isValidAmount()) {
            return false;
        }
        if (!this.isValidStartingBid()) {
            return false;
        }
        if (!this.isValidIncrement()) {
            return false;
        }
        if (!this.isValidTime()) {
            return false;
        }
        if (!this.isValidBuyNow()) {
            return false;
        }
        return true;
    }
    
    public void Bid(final Player bidder, final String[] inputArgs) {
    	
        if (bidder == null) {
            return;
        }
      
        if(inputArgs.length == 0)
        {
        	
        }
    	
        
        final String playerName = bidder.getName();
        if (plugin.arenaManager.isInArena(bidder)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "bid-fail-arena" }), playerName, this);
            return;
        }
        if (AuctionConfig.getBoolean("allow-buynow", this.scope) && inputArgs.length > 0 && inputArgs[0].equalsIgnoreCase("buy")) {
            if (this.buyNow == 0L || (this.currentBid != null && this.currentBid.getBidAmount() >= this.buyNow)) {
                this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "bid-fail-buynow-expired" }), playerName, this);
            }
            else {
                inputArgs[0] = Double.toString(functions.getUnsafeMoney(this.buyNow));
                if (inputArgs[0].endsWith(".0")) {
                    inputArgs[0] = inputArgs[0].substring(0, inputArgs[0].length() - 2);
                }
                final AuctionBid bid = new AuctionBid(this, bidder, inputArgs);
                if (bid.getError() != null) {
                    this.failBid(bid, bid.getError());
                    return;
                }
                if (this.currentBid != null) {
                    bid.raiseOwnBid(this.currentBid);
                }
                final AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, functions.getUnsafeMoney(bid.getBidAmount()), functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
                Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent);
                if (auctionBidEvent.isCancelled()) {
                    this.failBid(bid, "bid-fail-blocked-by-other-plugin");
                }
                else {
                    this.setNewBid(bid, null);
                    this.end();
                }
            }
            return;
        }
        final AuctionBid bid = new AuctionBid(this, bidder, inputArgs);
        if (bid.getError() != null) {
            this.failBid(bid, bid.getError());
            return;
        }
        if (this.currentBid == null) {
            if (bid.getBidAmount() < this.getStartingBid()) {
                this.failBid(bid, "bid-fail-under-starting-bid");
                return;
            }
            final AuctionBidEvent auctionBidEvent = new AuctionBidEvent(bidder, this, functions.getUnsafeMoney(bid.getBidAmount()), functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
            Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent);
            if (auctionBidEvent.isCancelled()) {
                this.failBid(bid, "bid-fail-blocked-by-other-plugin");
            }
            else {
                this.setNewBid(bid, "bid-success-no-challenger");
            }
        }
        else {
            final long previousBidAmount = this.currentBid.getBidAmount();
            final long previousMaxBidAmount = this.currentBid.getMaxBidAmount();
            if (this.currentBid.getBidder().equals(bidder.getName())) {
                if (bid.raiseOwnBid(this.currentBid)) {
                    final AuctionBidEvent auctionBidEvent2 = new AuctionBidEvent(bidder, this, functions.getUnsafeMoney(bid.getBidAmount()), functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
                    Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent2);
                    if (auctionBidEvent2.isCancelled()) {
                        this.failBid(bid, "bid-fail-blocked-by-other-plugin");
                    }
                    else {
                        this.setNewBid(bid, "bid-success-update-own-bid");
                    }
                }
                else if (previousMaxBidAmount < this.currentBid.getMaxBidAmount()) {
                    this.failBid(bid, "bid-success-update-own-maxbid");
                }
                else {
                    this.failBid(bid, "bid-fail-already-current-bidder");
                }
                return;
            }
            AuctionBid winner = null;
            AuctionBid loser = null;
            if (AuctionConfig.getBoolean("use-old-bid-logic", this.scope)) {
                if (bid.getMaxBidAmount() > this.currentBid.getMaxBidAmount()) {
                    winner = bid;
                    loser = this.currentBid;
                }
                else {
                    winner = this.currentBid;
                    loser = bid;
                }
                winner.raiseBid(Math.max(winner.getBidAmount(), Math.min(winner.getMaxBidAmount(), loser.getBidAmount() + this.minBidIncrement)));
            }
            else {
                long baseBid = 0L;
                if (bid.getBidAmount() >= this.currentBid.getBidAmount() + this.minBidIncrement) {
                    baseBid = bid.getBidAmount();
                }
                else {
                    baseBid = this.currentBid.getBidAmount() + this.minBidIncrement;
                }
                final Integer prevSteps = (int)Math.floor((this.currentBid.getMaxBidAmount() - baseBid + this.minBidIncrement) / this.minBidIncrement / 2.0);
                final Integer newSteps = (int)Math.floor((bid.getMaxBidAmount() - baseBid) / this.minBidIncrement / 2.0);
                if (newSteps >= prevSteps) {
                    winner = bid;
                    winner.raiseBid(baseBid + Math.max(0, prevSteps) * this.minBidIncrement * 2L);
                    loser = this.currentBid;
                }
                else {
                    winner = this.currentBid;
                    winner.raiseBid(baseBid + Math.max(0, newSteps + 1) * this.minBidIncrement * 2L - this.minBidIncrement);
                    loser = bid;
                }
            }
            if (previousBidAmount <= winner.getBidAmount()) {
                if (winner.equals(bid)) {
                    final AuctionBidEvent auctionBidEvent3 = new AuctionBidEvent(bidder, this, functions.getUnsafeMoney(bid.getBidAmount()), functions.getUnsafeMoney(bid.getMaxBidAmount()), true);
                    Bukkit.getServer().getPluginManager().callEvent(auctionBidEvent3);
                    if (auctionBidEvent3.isCancelled()) {
                        this.failBid(bid, "bid-fail-blocked-by-other-plugin");
                    }
                    else {
                        this.setNewBid(bid, "bid-success-outbid");
                    }
                }
                else if (previousBidAmount < winner.getBidAmount()) {
                    if (!this.sealed && !AuctionConfig.getBoolean("broadcast-bid-updates", this.scope)) {
                        this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { "bid-auto-outbid" }), this);
                    }
                    this.failBid(bid, "bid-fail-auto-outbid");
                }
                else {
                    if (!this.sealed) {
                        this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "bid-fail-too-low" }), bid.getBidder(), this);
                    }
                    this.failBid(bid, null);
                }
            }
            else {
                this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "bid-fail-too-low" }), bid.getBidder(), this);
                this.failBid(bid, null);//not previously here
            }
        }
    }
    
    private void failBid(final AuctionBid attemptedBid, final String reason) {
        attemptedBid.cancelBid();
        if (this.sealed && (attemptedBid.getError() == null || attemptedBid.getError().isEmpty())) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "bid-success-sealed" }), attemptedBid.getBidder(), this);
        }
        else {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { reason }), attemptedBid.getBidder(), this);
        }
    }
    
    private void setNewBid(final AuctionBid newBid, final String reason) {
        final AuctionBid prevBid = this.currentBid;
        if (AuctionConfig.getBoolean("expire-buynow-at-first-bid", this.scope)) {
            this.buyNow = 0L;
        }
        if (this.currentBid != null) {
            this.currentBid.cancelBid();
        }
        this.currentBid = newBid;
        if (this.sealed) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "bid-success-sealed" }), newBid.getBidder(), this);
        }
        else if (AuctionConfig.getBoolean("broadcast-bid-updates", this.scope)) {
            this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { reason }), this);
        }
        else {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { reason }), newBid.getBidder(), this);
            if (prevBid != null && newBid.getBidder().equalsIgnoreCase(prevBid.getBidder())) {
                this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { reason }), prevBid.getBidder(), this);
            }
        }
        AuctionParticipant.addParticipant(newBid.getBidder(), this.scope);
        if (this.currentBid.getBidAmount() >= this.buyNow) {
            this.buyNow = 0L;
        }
        if (!this.sealed && AuctionConfig.getBoolean("anti-snipe", this.scope) && this.getRemainingTime() <= AuctionConfig.getInt("anti-snipe-prevention-seconds", this.scope)) {
            this.addToRemainingTime(AuctionConfig.getInt("anti-snipe-prevention-seconds", this.scope));
            this.messageManager.broadcastAuctionMessage(new CArrayList<String>(new String[] { "anti-snipe-time-added" }), this);
        }
    }
    
    private Boolean parseHeldItem() {
        final Player owner = Bukkit.getPlayer(this.ownerName);
        if (this.lot != null) {
            return true;
        }
        final ItemStack heldItem = owner.getItemInHand();
        if (heldItem == null || heldItem.getAmount() == 0) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-hand-is-empty" }), this.ownerName, this);
            return false;
        }
        this.lot = new AuctionLot(heldItem, this.ownerName);
        final ItemStack itemType = this.lot.getTypeStack();
        if (!AuctionConfig.getBoolean("allow-damaged-items", this.scope) && itemType.getType().getMaxDurability() > 0 && itemType.getDurability() > 0) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-damaged-item" }), this.ownerName, this);
            this.lot = null;
            return false;
        }
        String displayName = items.getDisplayName(itemType);
        if (displayName == null) {
            displayName = "";
        }
        if (!displayName.isEmpty() && !AuctionConfig.getBoolean("allow-renamed-items", this.scope)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-renamed-item" }), this.ownerName, this);
            this.lot = null;
            return false;
        }
        final String[] lore = items.getLore(heldItem);
        final List<String> bannedLore = AuctionConfig.getStringList("banned-lore", this.scope);
        if (lore != null && bannedLore != null) {
            for (int i = 0; i < bannedLore.size(); ++i) {
                for (int j = 0; j < lore.length; ++j) {
                    if (lore[j].toLowerCase().contains(bannedLore.get(i).toLowerCase())) {
                        this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-banned-lore" }), this.ownerName, this);
                        this.lot = null;
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    private Boolean parseArgs() {
        if (!this.parseArgAmount()) {
            return false;
        }
        if (!this.parseArgStartingBid()) {
            return false;
        }
        if (!this.parseArgIncrement()) {
            return false;
        }
        if (!this.parseArgTime()) {
            return false;
        }
        if (!this.parseArgBuyNow()) {
            return false;
        }
        return true;
    }
    
    private Boolean isValidOwner() {
        if (this.ownerName == null) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-invalid-owner" }), null, this);
            return false;
        }
        return true;
    }
    
    private Boolean isValidAmount() {
        if (this.quantity <= 0) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-quantity-too-low" }), this.ownerName, this);
            return false;
        }
        if (!items.hasAmount(this.ownerName, this.quantity, this.lot.getTypeStack())) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-insufficient-supply" }), this.ownerName, this);
            return false;
        }
        return true;
    }
    
    private Boolean isValidStartingBid() {
        if (this.startingBid < 0L) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-starting-bid-too-low" }), this.ownerName, this);
            return false;
        }
        if (this.startingBid > AuctionConfig.getSafeMoneyFromDouble("max-starting-bid", this.scope)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-starting-bid-too-high" }), this.ownerName, this);
            return false;
        }
        return true;
    }
    
    private Boolean isValidIncrement() {
        if (this.getMinBidIncrement() < AuctionConfig.getSafeMoneyFromDouble("min-bid-increment", this.scope)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-increment-too-low" }), this.ownerName, this);
            return false;
        }
        if (this.getMinBidIncrement() > AuctionConfig.getSafeMoneyFromDouble("max-bid-increment", this.scope)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-increment-too-high" }), this.ownerName, this);
            return false;
        }
        return true;
    }
    
    private Boolean isValidBuyNow() {
        if (this.getBuyNow() < 0L) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-buynow-too-low" }), this.ownerName, this);
            return false;
        }
        if (this.getBuyNow() > AuctionConfig.getSafeMoneyFromDouble("max-buynow", this.scope)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-buynow-too-high" }), this.ownerName, this);
            return false;
        }
        return true;
    }
    
    private Boolean isValidTime() {
        if (this.time < AuctionConfig.getInt("min-auction-time", this.scope)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-time-too-low" }), this.ownerName, this);
            return false;
        }
        if (this.time > AuctionConfig.getInt("max-auction-time", this.scope)) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-time-too-high" }), this.ownerName, this);
            return false;
        }
        return true;
    }
    
    private Boolean parseArgAmount() {
        if (this.quantity > 0) {
            return true;
        }
        final ItemStack lotType = this.lot.getTypeStack();
        if (this.args.length > 0) {
            if (this.args[0].equalsIgnoreCase("this") || this.args[0].equalsIgnoreCase("hand")) {
                this.quantity = lotType.getAmount();
            }
            else if (this.args[0].equalsIgnoreCase("all")) {
                this.quantity = items.getAmount(this.ownerName, lotType);
            }
            else {
                if (!this.args[0].matches("[0-9]{1,7}")) {
                    this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-quantity" }), this.ownerName, this);
                    return false;
                }
                this.quantity = Integer.parseInt(this.args[0]);
            }
        }
        else {
            this.quantity = lotType.getAmount();
        }
        if (this.quantity < 0) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-quantity" }), this.ownerName, this);
            return false;
        }
        return true;
    }
    
    private Boolean parseArgStartingBid() {
        if (this.startingBid > 0L) {
            return true;
        }
        if (this.args.length > 1) {
            if (this.args[1].isEmpty() || !this.args[1].matches(floAuction.decimalRegex)) {
                this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-starting-bid" }), this.ownerName, this);
                return false;
            }
            this.startingBid = functions.getSafeMoney(Double.parseDouble(this.args[1]));
        }
        else {
            this.startingBid = AuctionConfig.getSafeMoneyFromDouble("default-starting-bid", this.scope);
        }
        if (this.startingBid < 0L) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-starting-bid" }), this.ownerName, this);
            return false;
        }
        return true;
    }
    
    private Boolean parseArgIncrement() {
        if (this.minBidIncrement > 0L) {
            return true;
        }
        if (this.args.length > 2) {
            if (this.args[2].isEmpty() || !this.args[2].matches(floAuction.decimalRegex)) {
                this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-bid-increment" }), this.ownerName, this);
                return false;
            }
            this.minBidIncrement = functions.getSafeMoney(Double.parseDouble(this.args[2]));
        }
        else {
            this.minBidIncrement = AuctionConfig.getSafeMoneyFromDouble("default-bid-increment", this.scope);
        }
        if (this.minBidIncrement < 0L) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-bid-increment" }), this.ownerName, this);
            return false;
        }
        return true;
    }
    
    private Boolean parseArgTime() {
        if (this.time > 0) {
            return true;
        }
        if (this.args.length > 3) {
            if (!this.args[3].matches("[0-9]{1,7}")) {
                this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-time" }), this.ownerName, this);
                return false;
            }
            this.time = Integer.parseInt(this.args[3]);
        }
        else {
            this.time = AuctionConfig.getInt("default-auction-time", this.scope);
        }
        if (this.time < 0) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-time" }), this.ownerName, this);
            return false;
        }
        return true;
    }
    
    private Boolean parseArgBuyNow() {
        if (this.sealed || !AuctionConfig.getBoolean("allow-buynow", this.scope)) {
            this.buyNow = 0L;
            return true;
        }
        if (this.getBuyNow() > 0L) {
            return true;
        }
        if (this.args.length > 4) {
            if (this.args[4].isEmpty() || !this.args[4].matches(floAuction.decimalRegex)) {
                this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-buynow" }), this.ownerName, this);
                return false;
            }
            this.buyNow = functions.getSafeMoney(Double.parseDouble(this.args[4]));
        }
        else {
            this.buyNow = 0L;
        }
        if (this.getBuyNow() < 0L) {
            this.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-buynow" }), this.ownerName, this);
            return false;
        }
        return true;
    }
    
    public long getMinBidIncrement() {
        return this.minBidIncrement;
    }
    
    public ItemStack getLotType() {
        if (this.lot == null) {
            return null;
        }
        return this.lot.getTypeStack();
    }
    
    public int getLotQuantity() {
        if (this.lot == null) {
            return 0;
        }
        return this.lot.getQuantity();
    }
    
    public long getStartingBid() {
        long effectiveStartingBid = this.startingBid;
        if (effectiveStartingBid == 0L) {
            effectiveStartingBid = this.minBidIncrement;
        }
        return effectiveStartingBid;
    }
    
    public AuctionBid getCurrentBid() {
        return this.currentBid;
    }
    
    public String getOwner() {
        return this.ownerName;
    }
    
    public int getRemainingTime() {
        return this.countdown;
    }
    
    public int getTotalTime() {
        return this.time;
    }
    
    public int addToRemainingTime(final int secondsToAdd) {
        return this.countdown += secondsToAdd;
    }
    
    public long getBuyNow() {
        return this.buyNow;
    }
    
    public String getOwnerDisplayName() {
        final Player ownerPlayer = Bukkit.getPlayer(this.ownerName);
        if (ownerPlayer != null) {
            return ownerPlayer.getDisplayName();
        }
        return this.ownerName;
    }
    
    static void access$1(final Auction auction, final int countdown) {
        auction.countdown = countdown;
    }
}
