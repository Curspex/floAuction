package com.flobi.floAuction;

import com.flobi.floAuction.utility.CArrayList;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class AuctionScope
{
    private Auction activeAuction;
    private List<Auction> otherPluginsAuctions;
    private String scopeId;
    private String name;
    private String type;
    private ArrayList<Auction> auctionQueue;
    private long lastAuctionDestroyTime;
    private List<String> worlds;
    private Location minHouseLocation;
    private Location maxHouseLocation;
    private String regionId;
    private boolean locationChecked;
    private ConfigurationSection config;
    private ConfigurationSection textConfig;
    public static List<String> auctionScopesOrder;
    public static Map<String, AuctionScope> auctionScopes;
    private static WorldGuardPlugin worldGuardPlugin;
    
    static {
        AuctionScope.auctionScopesOrder = new ArrayList<String>();
        AuctionScope.auctionScopes = new HashMap<String, AuctionScope>();
        AuctionScope.worldGuardPlugin = null;
    }
    
    private AuctionScope(final String scopeId, final ConfigurationSection config, final ConfigurationSection textConfig) {
        this.activeAuction = null;
        this.otherPluginsAuctions = null;
        this.scopeId = null;
        this.name = null;
        this.type = null;
        this.auctionQueue = new ArrayList<Auction>();
        this.lastAuctionDestroyTime = 0L;
        this.worlds = null;
        this.minHouseLocation = null;
        this.maxHouseLocation = null;
        this.regionId = null;
        this.locationChecked = false;
        this.config = null;
        this.textConfig = null;
        this.scopeId = scopeId;
        this.name = config.getString("name");
        if (this.name == null) {
            this.name = scopeId;
        }
        this.type = config.getString("type");
        this.config = config;
        this.textConfig = textConfig;
    }
    
    private boolean scopeLocationIsValid() {
        if (this.locationChecked) {
            return this.worlds != null || this.minHouseLocation != null || this.maxHouseLocation != null || this.regionId != null;
        }
        if (this.type.equalsIgnoreCase("worlds")) {
            this.worlds = (List<String>)this.config.getStringList("worlds");
        }
        else if (this.type.equalsIgnoreCase("house")) {
            final String world = this.config.getString("house-world");
            if (world == null || world.isEmpty()) {
                this.minHouseLocation = null;
                this.maxHouseLocation = null;
            }
            else {
                this.minHouseLocation = new Location(Bukkit.getWorld(world), this.config.getDouble("house-min-x"), this.config.getDouble("house-min-y"), this.config.getDouble("house-min-z"));
                this.maxHouseLocation = new Location(Bukkit.getWorld(world), this.config.getDouble("house-max-x"), this.config.getDouble("house-max-y"), this.config.getDouble("house-max-z"));
            }
        }
        else if (this.type.equalsIgnoreCase("worldguardregion")) {
            if (AuctionScope.worldGuardPlugin == null) {
                final Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
                if (plugin != null && plugin instanceof WorldGuardPlugin) {
                    AuctionScope.worldGuardPlugin = (WorldGuardPlugin)plugin;
                }
            }
            this.regionId = this.config.getString("region-id");
        }
        this.locationChecked = true;
        return this.worlds != null || this.minHouseLocation != null || this.maxHouseLocation != null || this.regionId != null;
    }
    
    public Auction getActiveAuction() {
        return this.activeAuction;
    }
    
    public int getAuctionQueueLength() {
        return this.auctionQueue.size();
    }
    
    public void setActiveAuction(final Auction auction) {
        if (this.activeAuction != null && auction == null) {
            this.lastAuctionDestroyTime = System.currentTimeMillis();
            checkAuctionQueue();
        }
        this.activeAuction = auction;
    }
    
    public void queueAuction(final Auction auctionToQueue) {
        final String playerName = auctionToQueue.getOwner();
        final MessageManager messageManager = auctionToQueue.messageManager;
        if (this.activeAuction == null) {
            if (Math.max(AuctionConfig.getInt("max-auction-queue-length", this), 1) <= this.auctionQueue.size()) {
                messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-queue-fail-full" }), playerName, auctionToQueue);
                return;
            }
        }
        else {
            if (AuctionConfig.getInt("max-auction-queue-length", this) <= 0) {
                messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-auction-exists" }), playerName, auctionToQueue);
                return;
            }
            if (this.activeAuction.getOwner().equalsIgnoreCase(playerName)) {
                messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-queue-fail-current-auction" }), playerName, auctionToQueue);
                return;
            }
            if (AuctionConfig.getInt("max-auction-queue-length", this) <= this.auctionQueue.size()) {
                messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-queue-fail-full" }), playerName, auctionToQueue);
                return;
            }
        }
        for (int i = 0; i < this.auctionQueue.size(); ++i) {
            if (this.auctionQueue.get(i) != null) {
                final Auction queuedAuction = this.auctionQueue.get(i);
                if (queuedAuction.getOwner().equalsIgnoreCase(playerName)) {
                    messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-queue-fail-in-queue" }), playerName, auctionToQueue);
                    return;
                }
            }
        }
        if ((this.auctionQueue.size() == 0 && System.currentTimeMillis() - this.lastAuctionDestroyTime >= AuctionConfig.getInt("min-auction-interval-secs", this) * 1000) || auctionToQueue.isValid()) {
            this.auctionQueue.add(auctionToQueue);
            AuctionParticipant.addParticipant(playerName, this);
            checkAuctionQueue();
            if (this.auctionQueue.contains(auctionToQueue)) {
                messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-queue-enter" }), playerName, auctionToQueue);
            }
        }
    }
    
    private void checkThisAuctionQueue() {
        if (this.activeAuction != null) {
            return;
        }
        if (System.currentTimeMillis() - this.lastAuctionDestroyTime < AuctionConfig.getInt("min-auction-interval-secs", this) * 1000) {
            return;
        }
        if (this.auctionQueue.size() == 0) {
            return;
        }
        final Auction auction = this.auctionQueue.remove(0);
        if (auction == null) {
            return;
        }
        final MessageManager messageManager = auction.messageManager;
        final String playerName = auction.getOwner();
        final Player player = Bukkit.getPlayer(playerName);
        if (player == null || !player.isOnline()) {
            return;
        }
        if (AuctionProhibition.isOnProhibition(auction.getOwner(), false)) {
            messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "remote-plugin-prohibition-reminder" }), playerName, auction);
            return;
        }
        if (!AuctionConfig.getBoolean("allow-gamemode-creative", this) && player.getGameMode() == GameMode.CREATIVE) {
            messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-gamemode-creative" }), playerName, auction);
            return;
        }
        if (!floAuction.perms.has(player, "auction.start")) {
            messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-permissions" }), playerName, auction);
            return;
        }
        if (!auction.isValid()) {
            return;
        }
        this.activeAuction = auction;
        if (!auction.start()) {
            this.activeAuction = null;
        }
    }
    
    private boolean isPlayerInScope(final Player player) {
        return player != null && this.isLocationInScope(player.getLocation());
    }
    
    private boolean isLocationInScope(final Location location) {
        if (location == null) {
            return false;
        }
        final World world = location.getWorld();
        if (world == null) {
            return false;
        }
        final String worldName = world.getName();
        if (!this.scopeLocationIsValid()) {
            return false;
        }
        if (this.type.equalsIgnoreCase("worlds")) {
            for (int i = 0; i < this.worlds.size(); ++i) {
                if (this.worlds.get(i).equalsIgnoreCase(worldName) || this.worlds.get(i).equalsIgnoreCase("*")) {
                    return true;
                }
            }
        }
        else {
            if (this.type.equalsIgnoreCase("house")) {
                return this.minHouseLocation != null && this.maxHouseLocation != null && location.getWorld().equals(this.minHouseLocation.getWorld()) && location.getX() <= Math.max(this.minHouseLocation.getX(), this.maxHouseLocation.getX()) && location.getX() >= Math.min(this.minHouseLocation.getX(), this.maxHouseLocation.getX()) && location.getZ() <= Math.max(this.minHouseLocation.getZ(), this.maxHouseLocation.getZ()) && location.getZ() >= Math.min(this.minHouseLocation.getZ(), this.maxHouseLocation.getZ()) && location.getY() <= Math.max(this.minHouseLocation.getY(), this.maxHouseLocation.getY()) && location.getY() >= Math.min(this.minHouseLocation.getY(), this.maxHouseLocation.getY());
            }
            if (this.type.equalsIgnoreCase("worldguardregion")) {
                if (AuctionScope.worldGuardPlugin == null) {
                    return false;
                }
                final RegionManager regionManager = AuctionScope.worldGuardPlugin.getRegionManager(location.getWorld());
                final ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(location);
                for (final ProtectedRegion region : applicableRegions) {
                    if (region.getId().equalsIgnoreCase(this.regionId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public ConfigurationSection getConfig() {
        return this.config.getConfigurationSection("config");
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getScopeId() {
        return this.scopeId;
    }
    
    public ConfigurationSection getTextConfig() {
        return this.textConfig;
    }
    
    public ArrayList<Auction> getAuctionQueue() {
        return this.auctionQueue;
    }
    
    public static void checkAuctionQueue() {
        for (final Map.Entry<String, AuctionScope> auctionScopesEntry : AuctionScope.auctionScopes.entrySet()) {
            auctionScopesEntry.getValue().checkThisAuctionQueue();
        }
    }
    
    public int getQueuePosition(final String playerName) {
        for (int t = 0; t < this.auctionQueue.size(); ++t) {
            final Auction auction = this.auctionQueue.get(t);
            if (auction.getOwner().equalsIgnoreCase(playerName)) {
                return t + 1;
            }
        }
        return 0;
    }
    
    public static AuctionScope getPlayerScope(final Player player) {
        if (player == null) {
            return null;
        }
        for (int i = 0; i < AuctionScope.auctionScopesOrder.size(); ++i) {
            final String auctionScopeId = AuctionScope.auctionScopesOrder.get(i);
            final AuctionScope auctionScope = AuctionScope.auctionScopes.get(auctionScopeId);
            if (auctionScope.isPlayerInScope(player)) {
                return auctionScope;
            }
        }
        return null;
    }
    
    public static AuctionScope getLocationScope(final Location location) {
        if (location == null) {
            return null;
        }
        for (int i = 0; i < AuctionScope.auctionScopesOrder.size(); ++i) {
            final String auctionScopeId = AuctionScope.auctionScopesOrder.get(i);
            final AuctionScope auctionScope = AuctionScope.auctionScopes.get(auctionScopeId);
            if (auctionScope.isLocationInScope(location)) {
                return auctionScope;
            }
        }
        return null;
    }
    
    public static void setupScopeList(final ConfigurationSection auctionScopesConfig, final File dataFolder) {
        AuctionScope.auctionScopes.clear();
        AuctionScope.auctionScopesOrder.clear();
        if (auctionScopesConfig != null) {
            for (final String scopeName : auctionScopesConfig.getKeys(false)) {
                AuctionScope.auctionScopesOrder.add(scopeName);
                final ConfigurationSection auctionScopeConfig = auctionScopesConfig.getConfigurationSection(scopeName);
                final File scopeTextConfigFile = new File(dataFolder, "language-" + scopeName + ".yml");
                YamlConfiguration scopeTextConfig = null;
                if (scopeTextConfigFile.exists()) {
                    scopeTextConfig = YamlConfiguration.loadConfiguration(scopeTextConfigFile);
                }
                final AuctionScope auctionScope = new AuctionScope(scopeName, auctionScopeConfig, (ConfigurationSection)scopeTextConfig);
                AuctionScope.auctionScopes.put(scopeName, auctionScope);
            }
        }
    }
    
    public static void cancelAllAuctions() {
        for (final Map.Entry<String, AuctionScope> auctionScopesEntry : AuctionScope.auctionScopes.entrySet()) {
            final AuctionScope auctionScope = auctionScopesEntry.getValue();
            auctionScope.auctionQueue.clear();
            if (auctionScope.activeAuction != null) {
                auctionScope.activeAuction.cancel();
            }
        }
    }
    
    public static boolean areAuctionsRunning() {
        for (final Map.Entry<String, AuctionScope> auctionScopesEntry : AuctionScope.auctionScopes.entrySet()) {
            final AuctionScope auctionScope = auctionScopesEntry.getValue();
            if (auctionScope.getActiveAuction() != null || auctionScope.getAuctionQueueLength() > 0) {
                return true;
            }
        }
        return false;
    }
    
    public int getOtherPluginsAuctionsLength() {
        if (this.otherPluginsAuctions == null) {
            return 0;
        }
        return this.otherPluginsAuctions.size();
    }
    
    public static void sendFairwellMessages() {
        final Iterator<String> playerIterator = floAuction.playerScopeCache.keySet().iterator();
        while (playerIterator.hasNext()) {
            final String playerName = playerIterator.next();
            if (!AuctionParticipant.isParticipating(playerName)) {
                final Player player = Bukkit.getPlayer(playerName);
                if (player == null || !player.isOnline()) {
                    continue;
                }
                final String oldScopeId = floAuction.playerScopeCache.get(playerName);
                final AuctionScope oldScope = AuctionScope.auctionScopes.get(oldScopeId);
                final AuctionScope playerScope = getPlayerScope(player);
                String playerScopeId = null;
                if (playerScope != null) {
                    playerScopeId = playerScope.getScopeId();
                }
                if (playerScopeId != null && !playerScopeId.isEmpty() && playerScopeId.equalsIgnoreCase(oldScopeId)) {
                    continue;
                }
                floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "auctionscope-fairwell" }), playerName, oldScope);
                playerIterator.remove();
                floAuction.playerScopeCache.remove(playerName);
            }
        }
    }

    public static void sendWelcomeMessages() {

    	for(Player player : Bukkit.getOnlinePlayers())
    	{
    		sendWelcomeMessage(player, false);
    	}
    }

    public static void sendWelcomeMessage(final Player player, final boolean isOnJoin) {
        String welcomeMessageKey = "auctionscope-welcome";
        if (isOnJoin) {
            welcomeMessageKey = String.valueOf(welcomeMessageKey) + "-onjoin";
        }
        final String playerName = player.getName();
        if (!AuctionParticipant.isParticipating(playerName)) {
            final AuctionScope playerScope = getPlayerScope(player);
            final String oldScopeId = floAuction.playerScopeCache.get(playerName);
            if (playerScope == null) {
                if (oldScopeId != null) {
                    floAuction.playerScopeCache.remove(playerName);
                }
            }
            else if (oldScopeId == null || oldScopeId.isEmpty() || !oldScopeId.equalsIgnoreCase(playerScope.getScopeId())) {
                floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { welcomeMessageKey }), playerName, playerScope);
                floAuction.playerScopeCache.put(playerName, playerScope.getScopeId());
            }
        }
    }
}
