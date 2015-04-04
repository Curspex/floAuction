package com.flobi.floAuction;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.floAuction.utility.CArrayList;
import com.flobi.floAuction.utility.functions;
import com.flobi.floAuction.utility.items;

public class AuctionBid
{
    private Auction auction;
    private String bidderName;
    private long bidAmount;
    private long maxBidAmount;
    private String error;
    private String[] args;
    private double reserve;
    
    public AuctionBid(final Auction auction, final Player player, final String[] inputArgs) {
        this.bidAmount = 0L;
        this.maxBidAmount = 0L;
        this.reserve = 0.0;
        this.auction = auction;
        this.bidderName = player.getName();
        this.args = inputArgs;
        if (!this.validateBidder()) {
            return;
        }
        if (!this.parseArgs()) {
            return;
        }
        if (!this.reserveBidFunds()) {
            return;
        }
    }
    
    private boolean reserveBidFunds() {
        long amountToReserve = 0L;
        long previousSealedReserve = 0L;
        final AuctionBid currentBid = this.auction.getCurrentBid();
        for (int i = 0; i < this.auction.sealedBids.size(); ++i) {
            if (this.auction.sealedBids.get(i).getBidder().equalsIgnoreCase(this.getBidder())) {
                previousSealedReserve += this.auction.sealedBids.get(i).getBidAmount();
                this.auction.sealedBids.remove(i);
                --i;
            }
        }
        if (currentBid != null && currentBid.getBidder().equalsIgnoreCase(this.bidderName)) {
            if (this.maxBidAmount <= currentBid.getMaxBidAmount() + previousSealedReserve) {
                return true;
            }
            amountToReserve = this.maxBidAmount - currentBid.getMaxBidAmount() - previousSealedReserve;
        }
        else {
            amountToReserve = this.maxBidAmount;
        }
        if (functions.withdrawPlayer(this.bidderName, amountToReserve)) {
            this.reserve = functions.getUnsafeMoney(amountToReserve);
            return true;
        }
        this.error = "bid-fail-cant-allocate-funds";
        return false;
    }
    
    public void cancelBid() {
        if (this.auction.sealed) {
            this.auction.sealedBids.add(this);
            AuctionParticipant.addParticipant(this.getBidder(), this.auction.getScope());
        }
        else {
            functions.depositPlayer(this.bidderName, this.reserve);
            this.reserve = 0.0;
        }
    }
    
    public void winBid() {
        Double unsafeBidAmount = functions.getUnsafeMoney(this.bidAmount);
        Double taxes = 0.0;
        double taxPercent = AuctionConfig.getDouble("auction-end-tax-percent", this.auction.getScope());
        final ItemStack typeStack = this.auction.getLotType();
        for (final Map.Entry<String, String> entry : AuctionConfig.getStringStringMap("taxed-items", this.auction.getScope()).entrySet()) {
            if (items.isSameItem(typeStack, entry.getKey())) {
                if (entry.getValue().endsWith("%")) {
                    try {
                        taxPercent = Double.valueOf(entry.getValue().substring(0, entry.getValue().length() - 1));
                    }
                    catch (Exception ex) {}
                    break;
                }
                break;
            }
        }
        if (taxPercent > 0.0) {
            taxes = unsafeBidAmount * (taxPercent / 100.0);
            this.auction.extractedPostTax = taxes;
            this.auction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-end-tax" }), this.auction.getOwner(), this.auction);
            unsafeBidAmount -= taxes;
            final String taxDestinationUser = AuctionConfig.getString("deposit-tax-to-user", this.auction.getScope());
            if (!taxDestinationUser.isEmpty()) {
                floAuction.econ.depositPlayer(taxDestinationUser, (double)taxes);
            }
        }
        floAuction.econ.depositPlayer(this.auction.getOwner(), (double)unsafeBidAmount);
        floAuction.econ.depositPlayer(this.bidderName, this.reserve - unsafeBidAmount - taxes);
        this.reserve = 0.0;
    }
    
    private Boolean validateBidder() {
        if (this.bidderName == null) {
            this.error = "bid-fail-no-bidder";
            return false;
        }
        if (AuctionProhibition.isOnProhibition(this.bidderName, false)) {
            this.error = "remote-plugin-prohibition-reminder";
            return false;
        }
        if (!AuctionParticipant.checkLocation(this.bidderName)) {
            this.error = "bid-fail-outside-auctionhouse";
            return false;
        }
        if (this.bidderName.equalsIgnoreCase(this.auction.getOwner()) && !AuctionConfig.getBoolean("allow-bid-on-own-auction", this.auction.getScope())) {
            this.error = "bid-fail-is-auction-owner";
            return false;
        }
        return true;
    }
    
    private Boolean parseArgs() {
        if (!this.parseArgBid()) {
            return false;
        }
        if (!this.parseArgMaxBid()) {
            return false;
        }
        return true;
    }
    
    public Boolean raiseOwnBid(final AuctionBid otherBid) {
        if (!this.bidderName.equalsIgnoreCase(otherBid.bidderName)) {
            return false;
        }
        this.reserve += otherBid.reserve;
        otherBid.reserve = 0.0;
        this.maxBidAmount = Math.max(this.maxBidAmount, otherBid.maxBidAmount);
        otherBid.maxBidAmount = this.maxBidAmount;
        if (this.bidAmount > otherBid.bidAmount) {
            return true;
        }
        otherBid.reserve = this.reserve;
        this.reserve = 0.0;
        return false;
    }
    
    public Boolean raiseBid(final Long newBidAmount) {
        if (newBidAmount <= this.maxBidAmount && newBidAmount >= this.bidAmount) {
            this.bidAmount = newBidAmount;
            return true;
        }
        return false;
    }
    
    private Boolean parseArgBid() {
        if (this.args.length > 0) {
            if (this.args[0].isEmpty() || !this.args[0].matches(floAuction.decimalRegex)) {
                this.error = "parse-error-invalid-bid";
                return false;
            }
            this.bidAmount = functions.getSafeMoney(Double.parseDouble(this.args[0]));
            
            if(bidAmount > floAuction.econ.getBalance(this.bidderName))
            {
            	this.error = "bid-fail-cant-allocate-funds";
            	return false;
            }
            
            if (this.bidAmount == 0L) {
                this.error = "parse-error-invalid-bid";
                return false;
            }
        }
        else {
            if (this.auction.sealed || !AuctionConfig.getBoolean("allow-auto-bid", this.auction.getScope())) {
                this.error = "bid-fail-bid-required";
                return false;
            }
            this.bidAmount = 0L;
        }
        if (this.bidAmount == 0L) {
            final AuctionBid currentBid = this.auction.getCurrentBid();
            if (currentBid == null) {
                this.bidAmount = this.auction.getStartingBid();
                if (this.bidAmount == 0L) {
                    this.bidAmount = this.auction.getMinBidIncrement();
                }
            }
            else if (currentBid.getBidder().equalsIgnoreCase(this.bidderName)) {
                this.bidAmount = currentBid.bidAmount;
            }
            else {
                this.bidAmount = currentBid.getBidAmount() + this.auction.getMinBidIncrement();
            }
        }
        if (this.bidAmount <= 0L) {
            this.error = "parse-error-invalid-bid";
            return false;
        }
        return true;
    }
    
    private Boolean parseArgMaxBid() {
        if (!AuctionConfig.getBoolean("allow-max-bids", this.auction.getScope()) || this.auction.sealed) {
            this.maxBidAmount = this.bidAmount;
            return true;
        }
        if (this.args.length > 1) {
            if (this.args[1].isEmpty() || !this.args[1].matches(floAuction.decimalRegex)) {
                this.error = "parse-error-invalid-max-bid";
                return false;
            }
            this.maxBidAmount = functions.getSafeMoney(Double.parseDouble(this.args[1]));
        }
        this.maxBidAmount = Math.max(this.bidAmount, this.maxBidAmount);
        if (this.maxBidAmount <= 0L) {
            this.error = "parse-error-invalid-max-bid";
            return false;
        }
        return true;
    }
    
    public String getError() {
        return this.error;
    }
    
    public String getBidder() {
        return this.bidderName;
    }
    
    public String getBidderDisplayName() {
        final Player bidderPlayer = Bukkit.getPlayer(this.bidderName);
        if (bidderPlayer != null) {
            return bidderPlayer.getDisplayName();
        }
        return this.bidderName;
    }
    
    public long getBidAmount() {
        return this.bidAmount;
    }
    
    public long getMaxBidAmount() {
        return this.maxBidAmount;
    }
}
