package com.flobi.floAuction;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.flobi.floAuction.utility.CArrayList;

public class AuctionProhibition
{
    private Plugin prohibiterPlugin;
    private String playerName;
    private String enableMessage;
    private String reminderMessage;
    private String disableMessage;
    private static ArrayList<AuctionProhibition> involuntarilyDisabledUsers;
    
    static {
        AuctionProhibition.involuntarilyDisabledUsers = new ArrayList<AuctionProhibition>();
    }
    
    private static AuctionProhibition getProhibition(final Plugin prohibiterPlugin, final String playerName) {
        for (int i = 0; i < AuctionProhibition.involuntarilyDisabledUsers.size(); ++i) {
            final AuctionProhibition auctionProhibition = AuctionProhibition.involuntarilyDisabledUsers.get(i);
            if (auctionProhibition.playerName.equalsIgnoreCase(playerName) && auctionProhibition.prohibiterPlugin.equals(prohibiterPlugin)) {
                return auctionProhibition;
            }
        }
        return null;
    }
    
    private static AuctionProhibition getProhibition(final String playerName) {
        for (int i = 0; i < AuctionProhibition.involuntarilyDisabledUsers.size(); ++i) {
            final AuctionProhibition auctionProhibition = AuctionProhibition.involuntarilyDisabledUsers.get(i);
            if (auctionProhibition.playerName.equalsIgnoreCase(playerName)) {
                return auctionProhibition;
            }
        }
        return null;
    }
    
    public static boolean isOnProhibition(final String playerName, final boolean sendReminderMessage) {
        final AuctionProhibition auctionProhibition = getProhibition(playerName);
        if (auctionProhibition != null) {
            if (sendReminderMessage) {
                final Player player = Bukkit.getPlayer(playerName);
                if (player == null) {
                    return true;
                }
                if (auctionProhibition.reminderMessage == null) {
                    floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "remote-plugin-prohibition-reminder" }), playerName, (AuctionScope)null);
                }
                else {
                    player.sendMessage(auctionProhibition.reminderMessage);
                }
            }
            return true;
        }
        return false;
    }
    
    public static boolean isOnProhibition(final Plugin prohibiterPlugin, final String playerName, final boolean sendReminderMessage) {
        final AuctionProhibition auctionProhibition = getProhibition(prohibiterPlugin, playerName);
        if (auctionProhibition != null) {
            if (sendReminderMessage) {
                final Player player = Bukkit.getPlayer(playerName);
                if (player == null) {
                    return true;
                }
                if (auctionProhibition.reminderMessage == null) {
                    floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "remote-plugin-prohibition-reminder" }), playerName, (AuctionScope)null);
                }
                else {
                    player.sendMessage(auctionProhibition.reminderMessage);
                }
            }
            return true;
        }
        return false;
    }
    
    public static boolean prohibitPlayer(final Plugin prohibiterPlugin, final String playerName) {
        return prohibitPlayer(prohibiterPlugin, playerName, null, null, null);
    }
    
    public static boolean prohibitPlayer(final Plugin prohibiterPlugin, final String playerName, final String enableMessage, final String reminderMessage, final String disableMessage) {
        if (AuctionParticipant.isParticipating(playerName)) {
            return false;
        }
        if (isOnProhibition(prohibiterPlugin, playerName, false)) {
            return true;
        }
        if (getProhibition(playerName) != null) {
            prohibitPlayer(prohibiterPlugin, playerName, disableMessage, reminderMessage, enableMessage);
            return true;
        }
        prohibitPlayer(prohibiterPlugin, playerName, disableMessage, reminderMessage, enableMessage);
        final Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return true;
        }
        if (enableMessage == null) {
            floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "remote-plugin-prohibition-enabled" }), playerName, (AuctionScope)null);
        }
        else {
            player.sendMessage(enableMessage);
        }
        return true;
    }
    
    public static void removeProhibition(final Plugin prohibiterPlugin, final String playerName) {
        final Player player = Bukkit.getPlayer(playerName);
        for (int i = 0; i < AuctionProhibition.involuntarilyDisabledUsers.size(); ++i) {
            final AuctionProhibition auctionProhibition = AuctionProhibition.involuntarilyDisabledUsers.get(i);
            if (auctionProhibition.playerName.equalsIgnoreCase(playerName) && auctionProhibition.prohibiterPlugin.equals(prohibiterPlugin)) {
                if (player != null) {
                    if (auctionProhibition.disableMessage == null) {
                        floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "remote-plugin-prohibition-disabled" }), playerName, (AuctionScope)null);
                    }
                    else {
                        player.sendMessage(auctionProhibition.disableMessage);
                    }
                }
                AuctionProhibition.involuntarilyDisabledUsers.remove(i);
                --i;
            }
        }
        final AuctionProhibition auctionProhibition2 = getProhibition(playerName);
        if (auctionProhibition2 != null && player != null) {
            if (auctionProhibition2.enableMessage == null) {
                floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "remote-plugin-prohibition-enabled" }), playerName, (AuctionScope)null);
            }
            else {
                player.sendMessage(auctionProhibition2.enableMessage);
            }
        }
    }
    
    private AuctionProhibition(final Plugin prohibiterPlugin, final String playerName, final String enableMessage, final String reminderMessage, final String disableMessage) {
        this.prohibiterPlugin = null;
        this.playerName = null;
        this.enableMessage = null;
        this.reminderMessage = null;
        this.disableMessage = null;
        this.prohibiterPlugin = prohibiterPlugin;
        this.playerName = playerName;
        this.enableMessage = enableMessage;
        this.reminderMessage = reminderMessage;
        this.disableMessage = disableMessage;
    }
}
