package com.flobi.floAuction;

import org.bukkit.entity.*;
import org.bukkit.command.*;
import java.util.*;
import com.flobi.floAuction.utility.*;
import java.text.*;
import org.bukkit.enchantments.*;
import org.bukkit.inventory.*;
import org.bukkit.*;

public class AuctionMessageManager extends MessageManager
{
    private static Map<String, Map<String, String>> replacementDefaults;
    
    static {
        AuctionMessageManager.replacementDefaults = new HashMap<String, Map<String, String>>();
    }
    
    public AuctionMessageManager() {
        final Map<String, String> aReplacments = new HashMap<String, String>();
        aReplacments.put("%A1", "-");
        aReplacments.put("%A2", "-");
        aReplacments.put("%A3", "-");
        aReplacments.put("%A4", "-");
        aReplacments.put("%A5", "-");
        aReplacments.put("%A6", "-");
        aReplacments.put("%A7", "-");
        aReplacments.put("%A8", "-");
        aReplacments.put("%A9", "-");
        AuctionMessageManager.replacementDefaults.put("a", aReplacments);
        final Map<String, String> bReplacments = new HashMap<String, String>();
        bReplacments.put("%B1", "-");
        bReplacments.put("%B2", "-");
        bReplacments.put("%B3", "-");
        bReplacments.put("%B4", "-");
        AuctionMessageManager.replacementDefaults.put("b", bReplacments);
        final Map<String, String> lReplacments = new HashMap<String, String>();
        lReplacments.put("%L1", "-");
        lReplacments.put("%L2", "-");
        lReplacments.put("%L3", "-");
        lReplacments.put("%L4", "-");
        lReplacments.put("%L5", "-");
        lReplacments.put("%L6", "-");
        lReplacments.put("%L7", "-");
        AuctionMessageManager.replacementDefaults.put("l", lReplacments);
        final Map<String, String> pReplacments = new HashMap<String, String>();
        pReplacments.put("%P1", "-");
        pReplacments.put("%P2", "-");
        pReplacments.put("%P3", "-");
        pReplacments.put("%P4", "-");
        pReplacments.put("%P5", "-");
        pReplacments.put("%P6", "-");
        pReplacments.put("%P7", "-");
        pReplacments.put("%P8", "-");
        pReplacments.put("%P9", "-");
        pReplacments.put("%P0", "-");
        AuctionMessageManager.replacementDefaults.put("p", pReplacments);
        final Map<String, String> sReplacments = new HashMap<String, String>();
        sReplacments.put("%S1", "-");
        sReplacments.put("%S2", "-");
        sReplacments.put("%S3", "-");
        sReplacments.put("%S4", "-");
        AuctionMessageManager.replacementDefaults.put("s", sReplacments);
    }
    
    @Override
    public void sendPlayerMessage(final List<String> messageKeys, final String playerName, final Auction auction) {
        CommandSender recipient = null;
        if (playerName == null) {
            recipient = (CommandSender)Bukkit.getConsoleSender();
        }
        else {
            recipient = (CommandSender)Bukkit.getPlayer(playerName);
        }
        AuctionScope auctionScope = null;
        if (auction != null) {
            auctionScope = auction.getScope();
        }
        if (auctionScope == null && recipient instanceof Player) {
            auctionScope = AuctionScope.getPlayerScope((Player)recipient);
        }
        this.sendMessage(messageKeys, recipient, auctionScope, false);
    }
    
    @Override
    public void sendPlayerMessage(final List<String> messageKeys, final String playerName, AuctionScope auctionScope) {
        CommandSender recipient = null;
        if (playerName == null) {
            recipient = (CommandSender)Bukkit.getConsoleSender();
        }
        else {
            recipient = (CommandSender)Bukkit.getPlayer(playerName);
        }
        if (auctionScope == null && recipient instanceof Player) {
            auctionScope = AuctionScope.getPlayerScope((Player)recipient);
        }
        this.sendMessage(messageKeys, recipient, auctionScope, false);
    }
    
    @Override
    public void broadcastAuctionMessage(final List<String> messageKeys, final Auction auction) {
        if (auction == null) {
            return;
        }
        final AuctionScope auctionScope = auction.getScope();
        this.sendMessage(messageKeys, null, auctionScope, true);
    }
    
    @Override
    public void broadcastAuctionScopeMessage(final List<String> messageKeys, final AuctionScope auctionScope) {
        this.sendMessage(messageKeys, null, auctionScope, true);
    }
    
    private void sendMessage(final List<String> messageKeys, final CommandSender sender, final AuctionScope auctionScope, final boolean fullBroadcast) {
        Auction auction = null;
        Player player = null;
        if (auctionScope != null) {
            auction = auctionScope.getActiveAuction();
        }
        if (sender != null) {
            if (sender instanceof Player) {
                player = (Player)sender;
                if (!fullBroadcast && floAuction.getVoluntarilyDisabledUsers().indexOf(player.getName()) != -1) {
                    return;
                }
            }
            else if (!fullBroadcast && floAuction.getVoluntarilyDisabledUsers().indexOf("*console*") != -1) {
                return;
            }
        }
        final List<String> messages = this.parseMessages(messageKeys, auctionScope, auction, player, fullBroadcast);
        if (fullBroadcast) {
            broadcastMessage(messages, auctionScope);
        }
        else if (player != null) {
            for (final String message : messages) {
                player.sendMessage(message);
                floAuction.log(player.getName(), message, auctionScope);
            }
        }
        else if (sender != null) {
            final ConsoleCommandSender console = Bukkit.getConsoleSender();
            for (final String message2 : messages) {
                console.sendMessage(ChatColor.stripColor(message2));
                floAuction.log("CONSOLE", message2, auctionScope);
            }
        }
        else {
            for (final String message : messages) {
                floAuction.log("NO TARGET!", message, auctionScope);
            }
        }
    }
    
    private static void broadcastMessage(final List<String> messages, final AuctionScope auctionScope) {
        for(Player player : Bukkit.getOnlinePlayers())
        {
            if (!floAuction.getVoluntarilyDisabledUsers().contains(player.getName())) {
                if (auctionScope == null || auctionScope.equals(AuctionScope.getPlayerScope(player))) {
                    for (final String message : messages) {
                        player.sendMessage(message);
                    }
                }
            }
        }
        if (auctionScope == null && floAuction.getVoluntarilyDisabledUsers().indexOf("*console*") == -1) {
            for (String message2 : messages) {
                message2 = ChatColor.stripColor(message2);
                Bukkit.getConsoleSender().sendMessage(message2);
            }
        }
        for (String message2 : messages) {
            message2 = ChatColor.stripColor(message2);
            floAuction.log("BROADCAST", message2, auctionScope);
        }
    }
    
    private static String chatPrep(String message, final AuctionScope auctionScope) {
        message = String.valueOf(ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("chat-prefix", auctionScope))) + message;
        return message;
    }
    
    private List<String> parseMessages(final List<String> messageKeys, final AuctionScope auctionScope, final Auction auction, final Player player, final boolean isBroadcast) {
        final List<String> messageList = new ArrayList<String>();
        for (int l = 0; l < messageKeys.size(); ++l) {
            final String messageKey = messageKeys.get(l);
            if (messageKey != null) {
                List<String> partialMessageList = AuctionConfig.getLanguageStringList(messageKey, auctionScope);
                if (partialMessageList == null || partialMessageList.size() == 0) {
                    String originalMessage = null;
                    originalMessage = AuctionConfig.getLanguageString(messageKey, auctionScope);
                    if (originalMessage == null) {
                        continue;
                    }
                    if (originalMessage.length() == 0) {
                        continue;
                    }
                    partialMessageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
                }
                messageList.addAll(partialMessageList);
            }
        }
        return this.parseMessageTokens(messageList, auctionScope, auction, player, isBroadcast);
    }
    
    private List<String> parseMessageTokens(final List<String> messageList, final AuctionScope auctionScope, Auction auction, final Player player, final boolean isBroadcast) {
        final List<String> newMessageList = new ArrayList<String>();
        final Map<String, String> replacements = new HashMap<String, String>();
        ItemStack lot = null;
        if (auction == null && auctionScope != null) {
            auction = auctionScope.getActiveAuction();
        }
        int l = 0;
        while (l < messageList.size()) {
            final String message = messageList.get(l);
            if (message.length() > 0 && message.contains("%A")) {
                replacements.putAll(AuctionMessageManager.replacementDefaults.get("a"));
                if (auction != null) {
                    replacements.put("%A1", auction.getOwner());
                    replacements.put("%A2", auction.getOwnerDisplayName());
                    replacements.put("%A3", Integer.toString(auction.getLotQuantity()));
                    if (auction.getStartingBid() == 0L) {
                        replacements.put("%A4", functions.formatAmount(auction.getMinBidIncrement()));
                    }
                    else {
                        replacements.put("%A4", functions.formatAmount(auction.getStartingBid()));
                    }
                    replacements.put("%A5", functions.formatAmount(auction.getMinBidIncrement()));
                    replacements.put("%A6", functions.formatAmount(auction.getBuyNow()));
                    replacements.put("%A7", functions.formatTime(auction.getRemainingTime(), auctionScope));
                    replacements.put("%A8", functions.formatAmount(auction.extractedPreTax));
                    replacements.put("%A9", functions.formatAmount(auction.extractedPostTax));
                    break;
                }
                break;
            }
            else {
                ++l;
            }
        }
        l = 0;
        while (l < messageList.size()) {
            final String message = messageList.get(l);
            if (message.length() > 0 && message.contains("%B")) {
                replacements.putAll(AuctionMessageManager.replacementDefaults.get("b"));
                if (auction == null) {
                    break;
                }
                final AuctionBid currentBid = auction.getCurrentBid();
                if (currentBid != null) {
                    replacements.put("%B1", currentBid.getBidder());
                    replacements.put("%B2", currentBid.getBidderDisplayName());
                    replacements.put("%B3", functions.formatAmount(currentBid.getBidAmount()));
                    replacements.put("%B4", functions.formatAmount(auction.getStartingBid()));
                    break;
                }
                final String bidderName = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-bidder-noone", auctionScope));
                final String startingBid = functions.formatAmount(auction.getStartingBid());
                replacements.put("%B1", bidderName);
                replacements.put("%B2", bidderName);
                replacements.put("%B3", startingBid);
                replacements.put("%B4", startingBid);
                break;
            }
            else {
                ++l;
            }
        }
        l = 0;
        while (l < messageList.size()) {
            final String message = messageList.get(l);
            if (message.length() > 0 && message.contains("%L")) {
                replacements.putAll(AuctionMessageManager.replacementDefaults.get("l"));
                if (auction == null) {
                    break;
                }
                lot = auction.getLotType();
                if (lot == null) {
                    break;
                }
                replacements.put("%L1", items.getItemName(lot));
                replacements.put("%L2", items.getDisplayName(lot));
                if (replacements.get("%L2") == null || replacements.get("%L2").isEmpty()) {
                    replacements.put("%L2", replacements.get("%L1"));
                }
                if (items.getFireworkPower(lot) != null) {
                    replacements.put("%L3", Integer.toString(items.getFireworkPower(lot)));
                }
                if (items.getBookAuthor(lot) != null) {
                    replacements.put("%L4", items.getBookAuthor(lot));
                }
                if (items.getBookTitle(lot) != null) {
                    replacements.put("%L5", items.getBookTitle(lot));
                }
                if (lot.getType().getMaxDurability() > 0) {
                    final DecimalFormat decimalFormat = new DecimalFormat("#%");
                    replacements.put("%L6", decimalFormat.format(1.0 - lot.getDurability() / lot.getType().getMaxDurability()));
                }
                Map<Enchantment, Integer> enchantments = (Map<Enchantment, Integer>)lot.getEnchantments();
                if (enchantments == null || enchantments.size() == 0) {
                    enchantments = items.getStoredEnchantments(lot);
                }
                if (enchantments != null) {
                    String enchantmentList = "";
                    final String enchantmentSeparator = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-enchantment-separator", auctionScope));
                    for (final Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
                        if (!enchantmentList.isEmpty()) {
                            enchantmentList = String.valueOf(enchantmentList) + enchantmentSeparator;
                        }
                        enchantmentList = String.valueOf(enchantmentList) + items.getEnchantmentName(enchantment);
                    }
                    if (enchantmentList.isEmpty()) {
                        enchantmentList = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-enchantment-none", auctionScope));
                    }
                    replacements.put("%L7", enchantmentList);
                    break;
                }
                break;
            }
            else {
                ++l;
            }
        }
        l = 0;
        while (l < messageList.size()) {
            final String message = messageList.get(l);
            if (message.length() > 0 && message.contains("%P")) {
                replacements.putAll(AuctionMessageManager.replacementDefaults.get("p"));
                if (player != null) {
                    final String playerName = player.getName();
                    final String[] defaultStartArgs = functions.mergeInputArgs(playerName, new String[0], false);
                    if (defaultStartArgs[0].equalsIgnoreCase("this") || defaultStartArgs[0].equalsIgnoreCase("hand")) {
                        replacements.put("%P1", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-amount-in-hand", auctionScope)));
                    }
                    else if (defaultStartArgs[0].equalsIgnoreCase("all")) {
                        replacements.put("%P1", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-all-of-this-kind", auctionScope)));
                    }
                    else {
                        replacements.put("%P1", ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("prep-qty-of-this-kind", auctionScope)));
                    }
                    replacements.put("%P2", defaultStartArgs[0]);
                    replacements.put("%P3", functions.formatAmount(Double.parseDouble(defaultStartArgs[1])));
                    replacements.put("%P4", defaultStartArgs[1]);
                    replacements.put("%P5", functions.formatAmount(Double.parseDouble(defaultStartArgs[2])));
                    replacements.put("%P6", defaultStartArgs[2]);
                    replacements.put("%P7", functions.formatTime(Integer.parseInt(defaultStartArgs[3]), auctionScope));
                    replacements.put("%P8", defaultStartArgs[3]);
                    replacements.put("%P9", functions.formatAmount(Double.parseDouble(defaultStartArgs[4])));
                    replacements.put("%P0", defaultStartArgs[4]);
                    break;
                }
                break;
            }
            else {
                ++l;
            }
        }
        l = 0;
        while (l < messageList.size()) {
            final String message = messageList.get(l);
            if (message.length() > 0 && message.contains("%S")) {
                replacements.putAll(AuctionMessageManager.replacementDefaults.get("s"));
                if (auctionScope != null) {
                    if (player != null) {
                        replacements.put("%S1", Integer.toString(auctionScope.getQueuePosition(player.getName())));
                    }
                    replacements.put("%S2", Integer.toString(auctionScope.getAuctionQueueLength()));
                    replacements.put("%S3", auctionScope.getName());
                    replacements.put("%S4", auctionScope.getScopeId());
                    break;
                }
                break;
            }
            else {
                ++l;
            }
        }
        final Map<String, Boolean> conditionals = new HashMap<String, Boolean>();
        for (int i = 0; i < messageList.size(); ++i) {
            final String message2 = messageList.get(i);
            if (message2.length() > 0 && (message2.contains("%C") || message2.contains("%N"))) {
                conditionals.put("1", player != null && floAuction.perms.has(player, "auction.admin"));
                conditionals.put("2", player != null && floAuction.perms.has(player, "auction.start"));
                conditionals.put("3", player != null && floAuction.perms.has(player, "auction.bid"));
                conditionals.put("4", lot != null && lot.getEnchantments() != null && lot.getEnchantments().size() > 0);
                conditionals.put("5", lot != null && lot.getEnchantments() != null && lot.getEnchantments().size() > 0);
                conditionals.put("6", auction != null && auction.sealed);
                conditionals.put("7", auction != null && !auction.sealed && auction.getCurrentBid() != null);
                conditionals.put("8", isBroadcast);
                conditionals.put("9", lot != null && items.getBookTitle(lot) != null && !items.getBookTitle(lot).isEmpty());
                conditionals.put("0", lot != null && items.getBookAuthor(lot) != null && !items.getBookAuthor(lot).isEmpty());
                conditionals.put("A", lot != null && items.getLore(lot) != null && items.getLore(lot).length > 0);
                conditionals.put("B", lot != null && lot.getType().getMaxDurability() > 0 && lot.getDurability() > 0);
                conditionals.put("C", lot != null && (lot.getType() == Material.FIREWORK || lot.getType() == Material.FIREWORK_CHARGE));
                conditionals.put("D", auction != null && auction.getBuyNow() != 0L);
                conditionals.put("E", lot != null && ((lot.getEnchantments() != null && lot.getEnchantments().size() > 0) || (items.getStoredEnchantments(lot) != null && items.getStoredEnchantments(lot).size() > 0)));
                conditionals.put("F", AuctionConfig.getBoolean("allow-max-bids", auctionScope));
                conditionals.put("G", AuctionConfig.getBoolean("allow-buynow", auctionScope));
                conditionals.put("H", AuctionConfig.getBoolean("allow-auto-bid", auctionScope));
                conditionals.put("I", AuctionConfig.getBoolean("allow-early-end", auctionScope));
                conditionals.put("J", AuctionConfig.getInt("cancel-prevention-percent", auctionScope) < 100);
                conditionals.put("K", AuctionConfig.getBoolean("allow-unsealed-auctions", auctionScope));
                conditionals.put("L", AuctionConfig.getBoolean("allow-sealed-auctions", auctionScope));
                conditionals.put("M", conditionals.get("K") || conditionals.get("L"));
                conditionals.put("N", auctionScope != null && auctionScope.getActiveAuction() != null);
                conditionals.put("O", auctionScope != null && auctionScope.getAuctionQueueLength() > 0);
                break;
            }
        }
        for (int i = 0; i < messageList.size(); ++i) {
            String message2 = ChatColor.translateAlternateColorCodes('&', (String)messageList.get(i));
            if (message2.length() > 0) {
                if (message2.contains("%C") || message2.contains("%N")) {
                    for (final Map.Entry<String, Boolean> conditional : conditionals.entrySet()) {
                        if (message2.length() > 0) {
                            message2 = this.parseConditionals(message2, conditional.getKey(), conditional.getValue());
                        }
                    }
                }
                for (final Map.Entry<String, String> replacementEntry : replacements.entrySet()) {
                    message2 = message2.replace(replacementEntry.getKey(), replacementEntry.getValue());
                }
                if (message2.contains("%R")) {
                    if (message2.contains("%R1")) {
                        if (lot != null) {
                            Map<Enchantment, Integer> enchantments2 = (Map<Enchantment, Integer>)lot.getEnchantments();
                            if (enchantments2 == null) {
                                enchantments2 = items.getStoredEnchantments(lot);
                            }
                            else {
                                final Map<Enchantment, Integer> storedEnchantments = items.getStoredEnchantments(lot);
                                if (storedEnchantments != null) {
                                    enchantments2.putAll(storedEnchantments);
                                }
                            }
                            if (enchantments2 != null && enchantments2.size() > 0) {
                                for (final Map.Entry<Enchantment, Integer> enchantmentEntry : enchantments2.entrySet()) {
                                    if (message2.length() > 0) {
                                        newMessageList.add(chatPrep(message2, auctionScope).replace("%R1", items.getEnchantmentName(enchantmentEntry)));
                                    }
                                }
                            }
                        }
                    }
                    else if (message2.contains("%R2")) {
                        final FireworkEffect[] payloads = items.getFireworkEffects(lot);
                        if (payloads != null && payloads.length > 0) {
                            for (int j = 0; j < payloads.length; ++j) {
                                final FireworkEffect payload = payloads[j];
                                String payloadAspects = "";
                                final String payloadSeparator = ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("auction-info-payload-separator", auctionScope));
                                final FireworkEffect.Type type = payload.getType();
                                if (type != null) {
                                    if (!payloadAspects.isEmpty()) {
                                        payloadAspects = String.valueOf(payloadAspects) + payloadSeparator;
                                    }
                                    final String fireworkShape = AuctionConfig.getLanguageString("firework-shapes." + type.toString(), auctionScope);
                                    if (fireworkShape == null) {
                                        payloadAspects = String.valueOf(payloadAspects) + type.toString();
                                    }
                                    else {
                                        payloadAspects = String.valueOf(payloadAspects) + ChatColor.translateAlternateColorCodes('&', fireworkShape);
                                    }
                                }
                                final List<Color> colors = (List<Color>)payload.getColors();
                                for (int k = 0; k < colors.size(); ++k) {
                                    if (!payloadAspects.isEmpty()) {
                                        payloadAspects = String.valueOf(payloadAspects) + payloadSeparator;
                                    }
                                    final Color color = colors.get(k);
                                    final String colorRGB = color.toString().replace("Color:[rgb0x", "").replace("]", "");
                                    final String fireworkColor = AuctionConfig.getLanguageString("firework-colors." + colorRGB, auctionScope);
                                    if (fireworkColor == null) {
                                        payloadAspects = String.valueOf(payloadAspects) + "#" + colorRGB;
                                    }
                                    else {
                                        payloadAspects = String.valueOf(payloadAspects) + ChatColor.translateAlternateColorCodes('&', fireworkColor);
                                    }
                                }
                                if (payload.hasFlicker()) {
                                    if (!payloadAspects.isEmpty()) {
                                        payloadAspects = String.valueOf(payloadAspects) + payloadSeparator;
                                    }
                                    payloadAspects = String.valueOf(payloadAspects) + ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("firework-twinkle", auctionScope));
                                }
                                if (payload.hasTrail()) {
                                    if (!payloadAspects.isEmpty()) {
                                        payloadAspects = String.valueOf(payloadAspects) + payloadSeparator;
                                    }
                                    payloadAspects = String.valueOf(payloadAspects) + ChatColor.translateAlternateColorCodes('&', AuctionConfig.getLanguageString("firework-trail", auctionScope));
                                }
                                if (message2.length() > 0) {
                                    newMessageList.add(chatPrep(message2, auctionScope).replace("%R2", payloadAspects));
                                }
                            }
                        }
                    }
                    else if (message2.contains("%R3") && auction != null) {
                        final String[] lore = items.getLore(lot);
                        for (int j = 0; j < lore.length; ++j) {
                            if (message2.length() > 0) {
                                newMessageList.add(chatPrep(message2, auctionScope).replace("%R3", lore[j]));
                            }
                        }
                    }
                }
                else if (message2.length() > 0) {
                    newMessageList.add(chatPrep(message2, auctionScope));
                }
            }
        }
        return newMessageList;
    }
    
    private String parseConditionals(String message, final String conditionalNumber, final boolean condition) {
        message = this.parseConditional(message, "%C" + conditionalNumber, condition);
        message = this.parseConditional(message, "%N" + conditionalNumber, !condition);
        return message;
    }
    
    private String parseConditional(String message, final String conditionalKey, final boolean condition) {
        if (!message.contains(conditionalKey)) {
            return message;
        }
        if (condition) {
            message = message.replace(conditionalKey, "");
        }
        else {
            final String[] parts = message.split(conditionalKey);
            message = "";
            for (int t = 0; t < parts.length; ++t) {
                if (t % 2 == 0) {
                    message = String.valueOf(message) + parts[t];
                }
            }
        }
        return message;
    }
}
