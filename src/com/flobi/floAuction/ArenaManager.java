package com.flobi.floAuction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.api.PVPArenaAPI;
import net.slipcor.pvparena.events.PAJoinEvent;

import com.flobi.floAuction.utility.CArrayList;
import com.garbagemule.MobArena.MobArena;
import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;

public class ArenaManager implements Listener
{
	
    private MobArena mobArena;
    private PVPArena pVPArena;
    private War war;
    
    public ArenaManager(MobArena mobArena, PVPArena pVPArena, War war)
    {
    	this.mobArena = mobArena;
    	this.pVPArena = pVPArena;
    	this.war = war;
    }
    
    
    public void loadArenaListeners(final floAuction plugin) {
        final PluginManager pluginManager = Bukkit.getPluginManager();
        if (this.mobArena == null) {
            this.mobArena = (MobArena)pluginManager.getPlugin("MobArena");
        }
        if (this.mobArena != null) {
            pluginManager.registerEvents(new Listener() {
                @EventHandler
                public void onMAPlayerJoin(final ArenaPlayerJoinEvent event) {
                    if (event.isCancelled()) {
                        return;
                    }
                    final Player player = event.getPlayer();
                    if (player == null) {
                        return;
                    }
                    final String playerName = player.getName();
                    if (!AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)) && AuctionParticipant.isParticipating(playerName)) {
                        floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "arena-warning" }), playerName, (AuctionScope)null);
                        event.setCancelled(true);
                    }
                }
            }, plugin);
        }
        if (this.pVPArena == null) {
            this.pVPArena = (PVPArena)pluginManager.getPlugin("pvparena");
        }
        if (this.pVPArena != null) {
            pluginManager.registerEvents(this,plugin);
        }
    }
                @EventHandler
                public void onPAPlayerJoin(final PAJoinEvent event) {
                    if (event.isCancelled()) {
                        return;
                    }
                    final Player player = event.getPlayer();
                    if (player == null) {
                        return;
                    }
                    final String playerName = player.getName();
                    if (!AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player)) && AuctionParticipant.isParticipating(playerName)) {
                        floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "arena-warning" }), playerName, (AuctionScope)null);
                        event.setCancelled(true);
                    }
                }

    
    public void loadArenaPlugins() {
        final PluginManager pluginManager = Bukkit.getPluginManager();
        if (this.mobArena == null) {
            this.mobArena = (MobArena)pluginManager.getPlugin("MobArena");
        }
        if (this.pVPArena == null) {
            this.pVPArena = (PVPArena)pluginManager.getPlugin("pvparena");
        }
        if (this.war == null) {
            this.war = (War)pluginManager.getPlugin("MobDungeon");
        }
        if (this.mobArena != null && !this.mobArena.isEnabled()) {
            this.mobArena = null;
        }
        if (this.pVPArena != null && !this.pVPArena.isEnabled()) {
            this.pVPArena = null;
        }
        if (this.war != null && !this.war.isEnabled()) {
            this.war = null;
        }
    }
    
    public void unloadArenaPlugins() {
        this.mobArena = null;
        this.pVPArena = null;
        this.war = null;
    }
    
    public boolean isInArena(final Player player) {
        if (player == null) {
            return false;
        }
        if (AuctionConfig.getBoolean("allow-arenas", AuctionScope.getPlayerScope(player))) {
            return false;
        }
        loadArenaPlugins();
        return (this.mobArena != null && this.mobArena.getArenaMaster() != null && this.mobArena.getArenaMaster().getArenaWithPlayer(player) != null) || (this.pVPArena != null && !PVPArenaAPI.getArenaName(player).equals("")) || (this.war != null && Warzone.getZoneByLocation(player) != null);
    }
    
    public boolean isInArena(final Location location) {
        if (location == null) {
            return false;
        }
        if (AuctionConfig.getBoolean("allow-arenas", AuctionScope.getLocationScope(location))) {
            return false;
        }
        loadArenaPlugins();
        return (this.mobArena != null && this.mobArena.getArenaMaster() != null && this.mobArena.getArenaMaster().getArenaAtLocation(location) != null) || (this.pVPArena != null && !PVPArenaAPI.getArenaNameByLocation(location).equals("")) || (this.war != null && Warzone.getZoneByLocation(location) != null);
    }
}
