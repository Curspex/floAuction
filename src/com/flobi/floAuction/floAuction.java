package com.flobi.floAuction;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.FileUtil;

import com.flobi.floAuction.utility.CArrayList;
import com.flobi.floAuction.utility.functions;

public class floAuction extends JavaPlugin implements Listener
{
    private static final Logger log;
    public static int decimalPlaces;
    public static String decimalRegex;
    public static boolean loadedDecimalFromVault;
    private static File auctionLog;
    private static boolean suspendAllAuctions;
    public static boolean useWhatIsIt;
    public static List<AuctionParticipant> auctionParticipants;
    public static Map<String, String[]> userSavedInputArgs;
    public static FileConfiguration config;
    public static FileConfiguration textConfig;
    private static File dataFolder;
    private static int queueTimer;
    static floAuction plugin;
    private static int playerScopeCheckTimer;
    static Map<String, String> playerScopeCache;
    private static ArrayList<AuctionLot> orphanLots;
    private static ArrayList<String> voluntarilyDisabledUsers;
    private static ArrayList<String> suspendedUsers;
    private static MessageManager messageManager;
    public static Economy econ;
    public static Permission perms;
    public static Chat chat;
    public ArenaManager arenaManager;
    
    static {
        log = Logger.getLogger("Minecraft");
        floAuction.decimalPlaces = 0;
        floAuction.decimalRegex = "^[0-9]{0,13}(\\.[0-9]{1," + floAuction.decimalPlaces + "})?$";
        floAuction.loadedDecimalFromVault = false;
        floAuction.auctionLog = null;
        floAuction.suspendAllAuctions = false;
        floAuction.useWhatIsIt = true;
        floAuction.auctionParticipants = new ArrayList<AuctionParticipant>();
        floAuction.userSavedInputArgs = new HashMap<String, String[]>();
        floAuction.config = null;
        floAuction.textConfig = null;
        floAuction.playerScopeCache = new HashMap<String, String>();
        floAuction.orphanLots = new ArrayList<AuctionLot>();
        floAuction.voluntarilyDisabledUsers = new ArrayList<String>();
        floAuction.suspendedUsers = new ArrayList<String>();
        floAuction.messageManager = new AuctionMessageManager();
        floAuction.econ = null;
        floAuction.perms = null;
        floAuction.chat = null;
    }
    
    public static void saveOrphanLot(final AuctionLot auctionLot) {
        floAuction.orphanLots.add(auctionLot);
        saveObject(floAuction.orphanLots, "orphanLots.ser");
    }
    
    private static void saveObject(final Object object, final String filename) {
        final File saveFile = new File(floAuction.dataFolder, filename);
        try {
            if (saveFile.exists()) {
                saveFile.delete();
            }
            final FileOutputStream file = new FileOutputStream(saveFile.getAbsolutePath());
            final OutputStream buffer = new BufferedOutputStream(file);
            final ObjectOutput output = new ObjectOutputStream(buffer);
            try {
                output.writeObject(object);
            }
            finally {
                output.close();
            }
            output.close();
        }
        catch (IOException ex) {}
    }
    
    private static ArrayList<String> loadArrayListString(final String filename) throws ClassNotFoundException, IOException {
        final File saveFile = new File(floAuction.dataFolder, filename);
        ArrayList<String> importedObjects = new ArrayList<String>();
        try {
            final InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            final InputStream buffer = new BufferedInputStream(file);
            final ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (ArrayList<String>)input.readObject();
            input.close();
        }
        finally {}
        return importedObjects;
    }
    
    private static Map<String, String[]> loadMapStringStringArray(final String filename) throws IOException, ClassNotFoundException {
        final File saveFile = new File(floAuction.dataFolder, filename);
        Map<String, String[]> importedObjects = new HashMap<String, String[]>();
        try {
            final InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            final InputStream buffer = new BufferedInputStream(file);
            final ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (Map<String, String[]>)input.readObject();
            input.close();
        }
        finally {}
        return importedObjects;
    }
    
    private static ArrayList<AuctionLot> loadArrayListAuctionLot(final String filename) {
        final File saveFile = new File(floAuction.dataFolder, filename);
        ArrayList<AuctionLot> importedObjects = new ArrayList<AuctionLot>();
        try {
            final InputStream file = new FileInputStream(saveFile.getAbsolutePath());
            final InputStream buffer = new BufferedInputStream(file);
            final ObjectInput input = new ObjectInputStream(buffer);
            importedObjects = (ArrayList<AuctionLot>)input.readObject();
            input.close();
        }
        catch (IOException ex) {}
        catch (ClassNotFoundException ex2) {}
        return importedObjects;
    }
    
    public static void killOrphan(final Player player) {
        if (floAuction.orphanLots != null && floAuction.orphanLots.size() > 0) {
            final Iterator<AuctionLot> iter = floAuction.orphanLots.iterator();
            while (iter.hasNext()) {
                final AuctionLot lot = iter.next();
                if (lot.getOwner().equalsIgnoreCase(player.getName())) {
                    lot.cancelLot();
                    iter.remove();
                }
            }
            saveObject(floAuction.orphanLots, "orphanLots.ser");
        }
    }
    
    public void onEnable() {
        floAuction.dataFolder = this.getDataFolder();
        floAuction.plugin = this;
        floAuction.auctionLog = new File(floAuction.dataFolder, "auctions.log");
        loadConfig();
        if (Bukkit.getPluginManager().getPlugin("WhatIsIt") == null) {
            if (!floAuction.config.getBoolean("allow-inferior-item-name-logic")) {
                this.logToBukkit("plugin-disabled-no-whatisit", Level.SEVERE);
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
            this.logToBukkit("recommended-whatisit", Level.WARNING);
            floAuction.useWhatIsIt = false;
        }
        else {
            floAuction.useWhatIsIt = true;
        }
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            this.logToBukkit("plugin-disabled-no-vault", Level.SEVERE);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.setupEconomy();
        this.setupPermissions();
        this.setupChat();
        if (floAuction.econ == null) {
            this.logToBukkit("plugin-disabled-no-economy", Level.SEVERE);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.arenaManager = new ArenaManager(null, null, null);
        arenaManager.loadArenaListeners(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }
            @EventHandler
            public void playerJoin(final PlayerJoinEvent event) {
                final Player player = event.getPlayer();
                floAuction.killOrphan(player);
                AuctionScope.sendWelcomeMessage(player, true);
            }
            
            @EventHandler
            public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
                AuctionParticipant.forceLocation(event.getPlayer().getName(), null);
            }
            
            @EventHandler
            public void onPlayerChangedGameMode(final PlayerGameModeChangeEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                final Player player = event.getPlayer();
                final String playerName = player.getName();
                final AuctionScope playerScope = AuctionScope.getPlayerScope(player);
                final Auction playerAuction = floAuction.getPlayerAuction(player);
                if (AuctionConfig.getBoolean("allow-gamemode-change", playerScope) || playerAuction == null) {
                    return;
                }
                if (AuctionParticipant.isParticipating(playerName)) {
                    event.setCancelled(true);
                    floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "gamemodechange-fail-participating" }), playerName, (AuctionScope)null);
                }
            }
            
            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerPreprocessCommand(final PlayerCommandPreprocessEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                final Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                final String playerName = player.getName();
                final String message = event.getMessage();
                if (message == null || message.isEmpty()) {
                    return;
                }
                final AuctionScope playerScope = AuctionScope.getPlayerScope(player);
                List<String> disabledCommands = AuctionConfig.getStringList("disabled-commands-inscope", playerScope);
                for (int i = 0; i < disabledCommands.size(); ++i) {
                    final String disabledCommand = disabledCommands.get(i);
                    if (!disabledCommand.isEmpty()) {
                        if (message.toLowerCase().startsWith(disabledCommand.toLowerCase())) {
                            event.setCancelled(true);
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "disabled-command-inscope" }), playerName, (AuctionScope)null);
                            return;
                        }
                    }
                }
                if (playerScope == null) {
                    return;
                }
                if (!AuctionParticipant.isParticipating(player.getName())) {
                    return;
                }
                disabledCommands = AuctionConfig.getStringList("disabled-commands-participating", playerScope);
                for (int i = 0; i < disabledCommands.size(); ++i) {
                    final String disabledCommand = disabledCommands.get(i);
                    if (!disabledCommand.isEmpty()) {
                        if (message.toLowerCase().startsWith(disabledCommand.toLowerCase())) {
                            event.setCancelled(true);
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "disabled-command-participating" }), playerName, (AuctionScope)null);
                            return;
                        }
                    }
                }
            }
            
            @EventHandler
            public void onPlayerMove(final PlayerMoveEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                AuctionParticipant.forceLocation(event.getPlayer().getName(), event.getTo());
            }
            
            @EventHandler
            public void onPlayerTeleport(final PlayerTeleportEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                if (!AuctionParticipant.checkTeleportLocation(event.getPlayer().getName(), event.getTo())) {
                    event.setCancelled(true);
                }
            }
            
            @EventHandler
            public void onPlayerPortalEvent(final PlayerPortalEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                if (!AuctionParticipant.checkTeleportLocation(event.getPlayer().getName(), event.getTo())) {
                    event.setCancelled(true);
                }
  

        final BukkitScheduler bukkitScheduler = this.getServer().getScheduler();
        if (floAuction.queueTimer > 0) {
            bukkitScheduler.cancelTask(floAuction.queueTimer);
        }
        floAuction.queueTimer = bukkitScheduler.scheduleSyncRepeatingTask(this, (Runnable)new Runnable() {
            @Override
            public void run() {
                AuctionScope.checkAuctionQueue();
            }
        }, 20L, 20L);
        final long playerScopeCheckInterval = floAuction.config.getLong("auctionscope-change-check-interval");
        if (floAuction.playerScopeCheckTimer > 0) {
            bukkitScheduler.cancelTask(floAuction.playerScopeCheckTimer);
        }
        if (playerScopeCheckInterval > 0L) {
            floAuction.playerScopeCheckTimer = bukkitScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    AuctionScope.sendFairwellMessages();
                    AuctionScope.sendWelcomeMessages();
                }
            }, playerScopeCheckInterval, playerScopeCheckInterval);
        }
        floAuction.orphanLots = loadArrayListAuctionLot("orphanLots.ser");
        try {
		floAuction.voluntarilyDisabledUsers = loadArrayListString("voluntarilyDisabledUsers.ser");
        floAuction.suspendedUsers = loadArrayListString("suspendedUsers.ser");
        floAuction.userSavedInputArgs = loadMapStringStringArray("userSavedInputArgs.ser");
        } catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "plugin-enabled" }), null, (AuctionScope)null);
    }
    
    private static void loadConfig() {
        File configFile = new File(floAuction.dataFolder, "config.yml");
        InputStream defConfigStream = floAuction.plugin.getResource("config.yml");
        File textConfigFile = new File(floAuction.dataFolder, "language.yml");
        InputStream defTextConfigStream = floAuction.plugin.getResource("language.yml");
        YamlConfiguration defConfig = null;
        YamlConfiguration defTextConfig = null;
        floAuction.config = null;
        floAuction.config = (FileConfiguration)YamlConfiguration.loadConfiguration(configFile);
        if (defConfigStream != null) {
            defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            defConfigStream = null;
        }
        if (defConfig != null) {
            floAuction.config.setDefaults(defConfig);
        }
        floAuction.textConfig = null;
        if (floAuction.config.contains("suppress-auction-start-info")) {
            FileUtil.copy(configFile, new File(floAuction.dataFolder, "config.v2-backup.yml"));
            FileUtil.copy(textConfigFile, new File(floAuction.dataFolder, "language.v2-backup.yml"));
            final String houseWorld = floAuction.config.getString("auctionhouse-world");
            if (houseWorld != null && !houseWorld.isEmpty()) {
                final YamlConfiguration house = new YamlConfiguration();
                house.set("name", (Object)"Auction House");
                house.set("type", (Object)"house");
                house.set("house-world", (Object)houseWorld);
                house.set("house-min-x", floAuction.config.get("auctionhouse-min-x"));
                house.set("house-min-y", floAuction.config.get("auctionhouse-min-y"));
                house.set("house-min-z", floAuction.config.get("auctionhouse-min-z"));
                house.set("house-max-x", floAuction.config.get("auctionhouse-max-x"));
                house.set("house-max-y", floAuction.config.get("auctionhouse-max-y"));
                house.set("house-max-z", floAuction.config.get("auctionhouse-max-z"));
                final YamlConfiguration scopes = new YamlConfiguration();
                scopes.set("house", (Object)house);
                floAuction.config.set("auction-scopes", (Object)scopes);
            }
            floAuction.config.set("disabled-commands-participating", floAuction.config.get("disabled-commands"));
            floAuction.textConfig = (FileConfiguration)new YamlConfiguration();
        }
        else {
            floAuction.textConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(textConfigFile);
        }
        if (defTextConfigStream != null) {
            defTextConfig = YamlConfiguration.loadConfiguration(defTextConfigStream);
            defTextConfigStream = null;
        }
        if (defTextConfig != null) {
            floAuction.textConfig.setDefaults(defTextConfig);
        }
        final FileConfiguration cleanConfig = (FileConfiguration)new YamlConfiguration();
        final Map<String, Object> configValues = (Map<String, Object>)floAuction.config.getDefaults().getValues(false);
        for (final Map.Entry<String, Object> configEntry : configValues.entrySet()) {
            cleanConfig.set((String)configEntry.getKey(), floAuction.config.get((String)configEntry.getKey()));
        }
        floAuction.config = cleanConfig;
        try {
            floAuction.config.save(configFile);
        }
        catch (IOException ex) {
            floAuction.log.severe("Cannot save config.yml");
        }
        defConfig = null;
        configFile = null;
        if (floAuction.textConfig.contains("plogin-reload-fail-permissions")) {
            floAuction.textConfig.set("plugin-reload-fail-permissions", floAuction.textConfig.get("plogin-reload-fail-permissions"));
        }
        final FileConfiguration cleanTextConfig = (FileConfiguration)new YamlConfiguration();
        final Map<String, Object> textConfigValues = (Map<String, Object>)floAuction.textConfig.getDefaults().getValues(false);
        for (final Map.Entry<String, Object> textConfigEntry : textConfigValues.entrySet()) {
            cleanTextConfig.set((String)textConfigEntry.getKey(), floAuction.textConfig.get((String)textConfigEntry.getKey()));
        }
        floAuction.textConfig = cleanTextConfig;
        if (floAuction.textConfig.getString("bid-fail-under-starting-bid") != null && floAuction.textConfig.getString("bid-fail-under-starting-bid").equals("&6The bidding must start at %A8.")) {
            floAuction.textConfig.set("bid-fail-under-starting-bid", (Object)"&6The bidding must start at %A4.");
        }
        try {
            floAuction.textConfig.save(textConfigFile);
        }
        catch (IOException ex2) {
            floAuction.log.severe("Cannot save language.yml");
        }
        defTextConfig = null;
        textConfigFile = null;
        AuctionScope.setupScopeList(floAuction.config.getConfigurationSection("auction-scopes"), floAuction.dataFolder);
    }
    
    public void onDisable() {
        AuctionScope.cancelAllAuctions();
        this.getServer().getScheduler().cancelTask(floAuction.queueTimer);
        floAuction.plugin = null;
        this.logToBukkit("plugin-disabled", Level.INFO);
        floAuction.auctionLog = null;
    }
    
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (!floAuction.loadedDecimalFromVault && floAuction.econ.isEnabled()) {
            floAuction.loadedDecimalFromVault = true;
            floAuction.decimalPlaces = Math.max(floAuction.econ.fractionalDigits(), 0);
            floAuction.config.set("decimal-places", (Object)floAuction.decimalPlaces);
            if (floAuction.decimalPlaces < 1) {
                floAuction.decimalRegex = "^[0-9]{1,13}$";
            }
            else if (floAuction.decimalPlaces == 1) {
                floAuction.decimalRegex = "^[0-9]{0,13}(\\.[0-9])?$";
            }
            else {
                floAuction.decimalRegex = "^[0-9]{0,13}(\\.[0-9]{1," + floAuction.decimalPlaces + "})?$";
            }
        }
        Player player = null;
        Auction auction = null;
        AuctionScope userScope = null;
        String playerName = null;
        if (sender instanceof Player) {
            player = (Player)sender;
            playerName = player.getName();
            userScope = AuctionScope.getPlayerScope(player);
            if (userScope != null) {
                auction = userScope.getActiveAuction();
            }
        }
        if ((cmd.getName().equalsIgnoreCase("auction") || cmd.getName().equalsIgnoreCase("auc")) && args.length > 0 && args[0].equalsIgnoreCase("on")) {
            final int index = getVoluntarilyDisabledUsers().indexOf(playerName);
            if (index != -1) {
                getVoluntarilyDisabledUsers().remove(index);
            }
            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-enabled" }), playerName, (AuctionScope)null);
            saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
            return true;
        }
        if (getVoluntarilyDisabledUsers().contains(playerName)) {
            getVoluntarilyDisabledUsers().remove(getVoluntarilyDisabledUsers().indexOf(playerName));
            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-disabled" }), playerName, (AuctionScope)null);
            getVoluntarilyDisabledUsers().add(playerName);
            saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("auc") || cmd.getName().equalsIgnoreCase("auction") || cmd.getName().equalsIgnoreCase("sauc") || cmd.getName().equalsIgnoreCase("sealedauction")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (player != null && !floAuction.perms.has(player, "auction.admin")) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "plugin-reload-fail-permissions" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (AuctionScope.areAuctionsRunning()) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "plugin-reload-fail-auctions-running" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    loadConfig();
                    floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "plugin-reloaded" }), playerName, (AuctionScope)null);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("resume")) {
                    if (args.length == 1) {
                        if (player != null && !floAuction.perms.has(player, "auction.admin")) {
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "unsuspension-fail-permissions" }), playerName, (AuctionScope)null);
                            return true;
                        }
                        floAuction.suspendAllAuctions = false;
                        floAuction.messageManager.broadcastAuctionScopeMessage(new CArrayList<String>(new String[] { "unsuspension-global" }), null);
                        return true;
                    }
                    else {
                        if (player != null && !floAuction.perms.has(player, "auction.admin")) {
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "unsuspension-fail-permissions" }), playerName, (AuctionScope)null);
                            return true;
                        }
                        if (!floAuction.suspendedUsers.contains(args[1].toLowerCase())) {
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "unsuspension-user-fail-not-suspended" }), playerName, (AuctionScope)null);
                            return true;
                        }
                        floAuction.suspendedUsers.remove(args[1].toLowerCase());
                        saveObject(floAuction.suspendedUsers, "suspendedUsers.ser");
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "unsuspension-user" }), args[1], (AuctionScope)null);
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "unsuspension-user-success" }), playerName, (AuctionScope)null);
                        return true;
                    }
                }
                else if (args[0].equalsIgnoreCase("suspend")) {
                    if (player != null && !floAuction.perms.has(player, "auction.admin")) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "suspension-fail-permissions" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (args.length <= 1) {
                        floAuction.suspendAllAuctions = true;
                        AuctionScope.cancelAllAuctions();
                        floAuction.messageManager.broadcastAuctionScopeMessage(new CArrayList<String>(new String[] { "suspension-global" }), null);
                        return true;
                    }
                    if (floAuction.suspendedUsers.contains(args[1].toLowerCase())) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "suspension-user-fail-already-suspended" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    final Player playerToSuspend = this.getServer().getPlayer(args[1]);
                    if (playerToSuspend == null || !playerToSuspend.isOnline()) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "suspension-user-fail-is-offline" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (floAuction.perms.has(playerToSuspend, "auction.admin")) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "suspension-user-fail-is-admin" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    floAuction.suspendedUsers.add(args[1].toLowerCase());
                    saveObject(floAuction.suspendedUsers, "suspendedUsers.ser");
                    floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "suspension-user" }), playerToSuspend.getName(), (AuctionScope)null);
                    floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "suspension-user-success" }), playerName, (AuctionScope)null);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("s") || args[0].equalsIgnoreCase("this") || args[0].equalsIgnoreCase("hand") || args[0].equalsIgnoreCase("all") || args[0].matches("[0-9]+")) {
                    if (floAuction.suspendAllAuctions) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "suspension-global" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (player != null && floAuction.suspendedUsers.contains(playerName.toLowerCase())) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "suspension-user" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (player == null) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-console" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode() == GameMode.CREATIVE) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-gamemode-creative" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (userScope == null) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-no-scope" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (!floAuction.perms.has(player, "auction.start")) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-permissions" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (!AuctionConfig.getBoolean("allow-sealed-auctions", userScope) && !AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-no-auctions-allowed" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (cmd.getName().equalsIgnoreCase("sealedauction") || cmd.getName().equalsIgnoreCase("sauc")) {
                        if (AuctionConfig.getBoolean("allow-sealed-auctions", userScope)) {
                            userScope.queueAuction(new Auction(this, player, args, userScope, true, floAuction.messageManager));
                        }
                        else {
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-no-sealed-auctions" }), playerName, (AuctionScope)null);
                        }
                    }
                    else if (AuctionConfig.getBoolean("allow-unsealed-auctions", userScope)) {
                        userScope.queueAuction(new Auction(this, player, args, userScope, false, floAuction.messageManager));
                    }
                    else {
                        userScope.queueAuction(new Auction(this, player, args, userScope, true, floAuction.messageManager));
                    }
                    return true;
                }
                else if (args[0].equalsIgnoreCase("prep") || args[0].equalsIgnoreCase("p")) {
                    if (player == null) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-console" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (!floAuction.perms.has(player, "auction.start")) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-permissions" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    final String[] mergedArgs = functions.mergeInputArgs(playerName, args, true);
                    if (mergedArgs != null) {
                        floAuction.userSavedInputArgs.put(playerName, mergedArgs);
                        saveObject(floAuction.userSavedInputArgs, "userSavedInputArgs.ser");
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "prep-save-success" }), playerName, (AuctionScope)null);
                    }
                    return true;
                }
                else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("c")) {
                    if (userScope == null) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-no-scope" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (userScope.getActiveAuction() == null && userScope.getAuctionQueueLength() == 0) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-no-auction-exists" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    final ArrayList<Auction> auctionQueue = userScope.getAuctionQueue();
                    for (int i = 0; i < auctionQueue.size(); ++i) {
                        if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
                            auctionQueue.remove(i);
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-cancel-queued" }), playerName, (AuctionScope)null);
                            return true;
                        }
                    }
                    if (auction == null) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-no-auction-exists" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (player == null || player.getName().equalsIgnoreCase(auction.getOwner()) || floAuction.perms.has(player, "auction.admin")) {
                        if (AuctionConfig.getInt("cancel-prevention-seconds", userScope) > auction.getRemainingTime() || AuctionConfig.getDouble("cancel-prevention-percent", userScope) > auction.getRemainingTime() / auction.getTotalTime() * 100.0) {
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-cancel-prevention" }), playerName, (AuctionScope)null);
                        }
                        else {
                            auction.cancel();
                        }
                    }
                    else {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-not-owner-cancel" }), playerName, (AuctionScope)null);
                    }
                    return true;
                }
                else if (args[0].equalsIgnoreCase("confiscate") || args[0].equalsIgnoreCase("impound")) {
                    if (auction == null) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-no-auction-exists" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (player == null) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "confiscate-fail-console" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (!floAuction.perms.has(player, "auction.admin")) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "confiscate-fail-permissions" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (playerName.equalsIgnoreCase(auction.getOwner())) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "confiscate-fail-self" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    auction.confiscate(player);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("e")) {
                    if (auction == null) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-no-auction-exists" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (!AuctionConfig.getBoolean("allow-early-end", userScope)) {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-no-early-end" }), playerName, (AuctionScope)null);
                        return true;
                    }
                    if (player.getName().equalsIgnoreCase(auction.getOwner())) {
                        auction.end();
                    }
                    else {
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-fail-not-owner-end" }), playerName, (AuctionScope)null);
                    }
                    return true;
                }
                else {
                    if (args[0].equalsIgnoreCase("stfu") || args[0].equalsIgnoreCase("ignore") || args[0].equalsIgnoreCase("quiet") || args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("silent") || args[0].equalsIgnoreCase("silence")) {
                        if (getVoluntarilyDisabledUsers().indexOf(playerName) == -1) {
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-disabled" }), playerName, (AuctionScope)null);
                            getVoluntarilyDisabledUsers().add(playerName);
                            saveObject(getVoluntarilyDisabledUsers(), "voluntarilyDisabledUsers.ser");
                        }
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i")) {
                        if (auction == null) {
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-info-no-auction" }), playerName, (AuctionScope)null);
                            return true;
                        }
                        auction.info(sender, false);
                        return true;
                    }
                    else if (args[0].equalsIgnoreCase("queue") || args[0].equalsIgnoreCase("q")) {
                        final ArrayList<Auction> auctionQueue = userScope.getAuctionQueue();
                        if (auctionQueue.isEmpty()) {
                            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-queue-status-not-in-queue" }), playerName, (AuctionScope)null);
                            return true;
                        }
                        for (int i = 0; i < auctionQueue.size(); ++i) {
                            if (auctionQueue.get(i).getOwner().equalsIgnoreCase(playerName)) {
                                floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-queue-status-in-queue" }), playerName, (AuctionScope)null);
                                return true;
                            }
                        }
                        floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-queue-status-not-in-queue" }), playerName, (AuctionScope)null);
                        return true;
                    }
                }
            }
            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "auction-help" }), playerName, (AuctionScope)null);
            return true;
        }
        if (!cmd.getName().equalsIgnoreCase("bid")) {
            return false;
        }
        if (floAuction.suspendAllAuctions) {
            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "suspension-global" }), playerName, (AuctionScope)null);
            return true;
        }
        if (player != null && floAuction.suspendedUsers.contains(playerName.toLowerCase())) {
            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "suspension-user" }), playerName, (AuctionScope)null);
            return true;
        }
        if (player == null) {
            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "bid-fail-console" }), playerName, (AuctionScope)null);
            return true;
        }
        if (!AuctionConfig.getBoolean("allow-gamemode-creative", userScope) && player.getGameMode().equals((Object)GameMode.CREATIVE)) {
            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "bid-fail-gamemode-creative" }), playerName, (AuctionScope)null);
            return true;
        }
        if (!floAuction.perms.has(player, "auction.bid")) {
            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "bid-fail-permissions" }), playerName, (AuctionScope)null);
            return true;
        }
        if (auction == null) {
            floAuction.messageManager.sendPlayerMessage(new CArrayList<String>(new String[] { "bid-fail-no-auction" }), playerName, (AuctionScope)null);
            return true;
        }
        auction.Bid(player, args);
        return true;
    }
    
    static void log(final String playerName, final String message, final AuctionScope auctionScope) {
        if (AuctionConfig.getBoolean("log-auctions", auctionScope)) {
            String scopeId = null;
            BufferedWriter out = null;
            try {
                if (floAuction.auctionLog == null || !floAuction.auctionLog.exists()) {
                    floAuction.auctionLog.createNewFile();
                    floAuction.auctionLog.setWritable(true);
                }
                out = new BufferedWriter(new FileWriter(floAuction.auctionLog.getAbsolutePath(), true));
                if (auctionScope == null) {
                    scopeId = "NOSCOPE";
                }
                else {
                    scopeId = auctionScope.getScopeId();
                }
                out.append((CharSequence)(String.valueOf(new Date().toString()) + " (" + playerName + ", " + scopeId + "): " + ChatColor.stripColor(message) + "\n"));
                out.close();
            }
            catch (IOException ex) {}
        }
    }
    
    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        final RegisteredServiceProvider<Economy> rsp = (RegisteredServiceProvider<Economy>)Bukkit.getServicesManager().getRegistration((Class)Economy.class);
        if (rsp == null) {
            return false;
        }
        floAuction.econ = (Economy)rsp.getProvider();
        return floAuction.econ != null;
    }
    
    private boolean setupChat() {
        final RegisteredServiceProvider<Chat> rsp = (RegisteredServiceProvider<Chat>)Bukkit.getServicesManager().getRegistration((Class)Chat.class);
        if (rsp == null) {
            return false;
        }
        floAuction.chat = (Chat)rsp.getProvider();
        return floAuction.chat != null;
    }
    
    private boolean setupPermissions() {
        final RegisteredServiceProvider<Permission> rsp = (RegisteredServiceProvider<Permission>)Bukkit.getServicesManager().getRegistration((Class)Permission.class);
        floAuction.perms = (Permission)rsp.getProvider();
        return floAuction.perms != null;
    }
    
    public static Auction getPlayerAuction(final String playerName) {
        if (playerName == null) {
            return null;
        }
        return getPlayerAuction(Bukkit.getPlayer(playerName));
    }
    
    public static Auction getPlayerAuction(final Player player) {
        if (player == null) {
            return null;
        }
        final AuctionScope auctionScope = AuctionScope.getPlayerScope(player);
        if (auctionScope == null) {
            return null;
        }
        return auctionScope.getActiveAuction();
    }
    
    public static ArrayList<String> getVoluntarilyDisabledUsers() {
        return floAuction.voluntarilyDisabledUsers;
    }
    
    private static String chatPrepClean(String message, final AuctionScope auctionScope) {
        message = String.valueOf(AuctionConfig.getLanguageString("chat-prefix", auctionScope)) + message;
        message = ChatColor.translateAlternateColorCodes('&', message);
        message = ChatColor.stripColor(message);
        return message;
    }
    
    public static MessageManager getMessageManager() {
        return floAuction.messageManager;
    }
    
    private void logToBukkit(final String key, final Level level) {
        List<String> messageList = AuctionConfig.getLanguageStringList(key, null);
        String originalMessage = null;
        if (messageList == null || messageList.size() == 0) {
            originalMessage = AuctionConfig.getLanguageString(key, null);
            if (originalMessage != null && originalMessage.length() != 0) {
                messageList = Arrays.asList(originalMessage.split("(\r?\n|\r)"));
            }
        }
        for (final String messageListItem : messageList) {
            floAuction.log.log(level, chatPrepClean(messageListItem, null));
        }
    }
}
