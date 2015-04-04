package com.flobi.floAuction.utility;

import java.text.DecimalFormat;

import net.milkbowl.vault.economy.EconomyResponse;

import com.flobi.floAuction.AuctionConfig;
import com.flobi.floAuction.AuctionScope;
import com.flobi.floAuction.floAuction;

public class functions
{
    public static String formatTime(final int seconds, final AuctionScope auctionScope) {
        String returnTime = "-";
        if (seconds >= 60) {
            returnTime = AuctionConfig.getLanguageString("time-format-minsec", auctionScope);
            returnTime = returnTime.replace("%s", Integer.toString(seconds % 60));
            returnTime = returnTime.replace("%m", Integer.toString((seconds - seconds % 60) / 60));
        }
        else {
            returnTime = AuctionConfig.getLanguageString("time-format-seconly", auctionScope);
            returnTime = returnTime.replace("%s", Integer.toString(seconds));
        }
        return returnTime;
    }
    
    public static String removeUselessDecimal(String number) {
        if (number.endsWith(".0")) {
            number = number.replace(".0", "");
        }
        return number;
    }
    
    public static String[] mergeInputArgs(final String playerName, final String[] inputArgs, final boolean validateArgs) {
        String[] resultArgs = null;
        if (floAuction.userSavedInputArgs.get(playerName) == null) {
            resultArgs = new String[] { "this", removeUselessDecimal(Double.toString(AuctionConfig.getDouble("default-starting-bid", null))), removeUselessDecimal(Double.toString(AuctionConfig.getDouble("default-bid-increment", null))), Integer.toString(AuctionConfig.getInt("default-auction-time", null)), "0" };
        }
        else {
            resultArgs = floAuction.userSavedInputArgs.get(playerName).clone();
        }
        if (resultArgs.length < 5) {
            final String[] tmp = resultArgs.clone();
            resultArgs = new String[] { tmp[0], tmp[1], tmp[2], tmp[3], "0" };
        }
        String[] processArgs = inputArgs;
        if (processArgs.length > 0 && (processArgs[0].equalsIgnoreCase("start") || processArgs[0].equalsIgnoreCase("s") || processArgs[0].equalsIgnoreCase("prep") || processArgs[0].equalsIgnoreCase("p"))) {
            processArgs = new String[inputArgs.length - 1];
            System.arraycopy(inputArgs, 1, processArgs, 0, inputArgs.length - 1);
        }
        if (processArgs.length > 0) {
            if (!processArgs[0].equalsIgnoreCase("-")) {
                resultArgs[0] = processArgs[0];
            }
            if (validateArgs && !resultArgs[0].equalsIgnoreCase("this") && !resultArgs[0].equalsIgnoreCase("hand") && !resultArgs[0].equalsIgnoreCase("all") && !resultArgs[0].matches("[0-9]{1,7}")) {
                floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-quantity" }), playerName, (AuctionScope)null);
                return null;
            }
            if (processArgs.length > 1) {
                if (!processArgs[1].equalsIgnoreCase("-")) {
                    resultArgs[1] = processArgs[1];
                }
                if (validateArgs && (resultArgs[1].isEmpty() || !resultArgs[1].matches(floAuction.decimalRegex))) {
                    floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-starting-bid" }), playerName, (AuctionScope)null);
                    return null;
                }
                if (processArgs.length > 2) {
                    if (!processArgs[2].equalsIgnoreCase("-")) {
                        resultArgs[2] = processArgs[2];
                    }
                    if (validateArgs && (resultArgs[2].isEmpty() || !resultArgs[2].matches(floAuction.decimalRegex))) {
                        floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-max-bid" }), playerName, (AuctionScope)null);
                        return null;
                    }
                    if (processArgs.length > 3) {
                        if (!processArgs[3].equalsIgnoreCase("-")) {
                            resultArgs[3] = processArgs[3];
                        }
                        if (validateArgs && !resultArgs[3].matches("[0-9]{1,7}")) {
                            floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-time" }), playerName, (AuctionScope)null);
                            return null;
                        }
                        if (processArgs.length > 4) {
                            if (!processArgs[4].equalsIgnoreCase("-")) {
                                resultArgs[4] = processArgs[4];
                            }
                            if (validateArgs && (resultArgs[4].isEmpty() || !resultArgs[4].matches(floAuction.decimalRegex))) {
                                floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "parse-error-invalid-buynow" }), playerName, (AuctionScope)null);
                                return null;
                            }
                        }
                    }
                }
            }
        }
        return resultArgs;
    }
    
    public static String formatAmount(final long safeMoney) {
        return formatAmount(getUnsafeMoney(safeMoney));
    }
    
    public static String formatAmount(final double unsafeMoney) {
        if (floAuction.econ == null) {
            return "-";
        }
        if (!floAuction.econ.isEnabled()) {
            return "-";
        }
        return floAuction.econ.format(unsafeMoney);
    }
    
    public static boolean withdrawPlayer(final String playerName, final long safeMoney) {
        return withdrawPlayer(playerName, getUnsafeMoney(safeMoney));
    }
    
    public static boolean withdrawPlayer(final String playerName, final double unsafeMoney) {
        final EconomyResponse receipt = floAuction.econ.withdrawPlayer(playerName, unsafeMoney);
        return receipt.transactionSuccess();
    }
    
    public static boolean depositPlayer(final String playerName, final double unsafeMoney) {
        final EconomyResponse receipt = floAuction.econ.depositPlayer(playerName, unsafeMoney);
        return receipt.transactionSuccess();
    }
    
    public static long getSafeMoney(final Double money) {
        final DecimalFormat twoDForm = new DecimalFormat("#");
        return Long.valueOf(twoDForm.format(money * Math.pow(10.0, floAuction.decimalPlaces)));
    }
    
    public static double getUnsafeMoney(final long money) {
        return money / Math.pow(10.0, floAuction.decimalPlaces);
    }
}
