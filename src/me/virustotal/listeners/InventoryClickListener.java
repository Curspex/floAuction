package me.virustotal.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.flobi.floAuction.floAuction;

public class InventoryClickListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void invClick(InventoryClickEvent event)
	{
		
		if (event.getInventory() == null) return;
		
		//if (event.getClick() == null) return;
		
		if (!event.getInventory().getTitle().equalsIgnoreCase(floAuction.guiQueueName)) return;
		
		event.setResult(Result.DENY);
		event.setCancelled(true);
		//event.getCurrentItem().setAmount(0);
		if (event.getCursor() != null)
			event.getCursor().setAmount(0);
		Bukkit.getScheduler().runTaskLater(floAuction.plugin, () -> ((Player) event.getWhoClicked()).updateInventory() , 2);

	}

}