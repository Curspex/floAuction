package com.flobi.floAuction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.flobi.floAuction.utility.CArrayList;
import com.flobi.floAuction.utility.items;

public class AuctionLot implements Serializable
{
    private static final long serialVersionUID = -1764290458703647129L;
    private String ownerName;
    private int quantity;
    private int lotTypeId;
    private short lotDurability;
    private Map<Integer, Integer> lotEnchantments;
    private Map<Integer, Integer> storedEnchantments;
    private int sourceStackQuantity;
    private String displayName;
    private String bookAuthor;
    private String bookTitle;
    private String[] bookPages;
    private Integer repairCost;
    private String headOwner;
    private Integer power;
    private FireworkEffect[] effects;
    private String[] lore;
    private String itemSerialized;
    
    public AuctionLot(final ItemStack lotType, final String lotOwner) {
        this.quantity = 0;
        this.sourceStackQuantity = 0;
        this.displayName = "";
        this.bookAuthor = "";
        this.bookTitle = "";
        this.bookPages = null;
        this.repairCost = null;
        this.headOwner = null;
        this.power = 0;
        this.effects = null;
        this.lore = null;
        this.itemSerialized = null;
        this.ownerName = lotOwner;
        this.setLotType(lotType);
    }
    
    public boolean addItems(final int addQuantity, final boolean removeFromOwner) {
        if (removeFromOwner) {
            if (!items.hasAmount(this.ownerName, addQuantity, this.getTypeStack())) {
                return false;
            }
            items.remove(this.ownerName, addQuantity, this.getTypeStack());
        }
        this.quantity += addQuantity;
        return true;
    }
    
    public void winLot(final String winnerName) {
        this.giveLot(winnerName);
    }
    
    public void cancelLot() {
        this.giveLot(this.ownerName);
    }
    
    private void giveLot(final String playerName) {
        this.ownerName = playerName;
        if (this.quantity == 0) {
            return;
        }
        final ItemStack lotTypeLock = this.getTypeStack();
        final Player player = Bukkit.getPlayer(playerName);
        final int maxStackSize = lotTypeLock.getType().getMaxStackSize();
        if (player != null && player.isOnline()) {
            int amountToGive = 0;
            if (items.hasSpace(player, this.quantity, lotTypeLock)) {
                amountToGive = this.quantity;
            }
            else {
                amountToGive = items.getSpaceForItem(player, lotTypeLock);
            }
            final ItemStack typeStack = this.getTypeStack();
            if (amountToGive > 0) {
                floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "lot-give" }), playerName, (AuctionScope)null);
            }
            while (amountToGive > 0) {
                final ItemStack givingItems = lotTypeLock.clone();
                givingItems.setAmount(Math.min(maxStackSize, amountToGive));
                this.quantity -= givingItems.getAmount();
                items.saferItemGive(player.getInventory(), givingItems);
                amountToGive -= maxStackSize;
            }
            if (this.quantity > 0) {
                while (this.quantity > 0) {
                    final ItemStack cloneStack = typeStack.clone();
                    cloneStack.setAmount(Math.min(this.quantity, items.getMaxStackSize(typeStack)));
                    this.quantity -= cloneStack.getAmount();
                    final Item drop = player.getWorld().dropItemNaturally(player.getLocation(), cloneStack);
                    drop.setItemStack(cloneStack);
                }
                floAuction.getMessageManager().sendPlayerMessage(new CArrayList<String>(new String[] { "lot-drop" }), playerName, (AuctionScope)null);
            }
        }
        else {
            final AuctionLot orphanLot = new AuctionLot(lotTypeLock, playerName);
            orphanLot.addItems(this.quantity, false);
            this.quantity = 0;
            floAuction.saveOrphanLot(orphanLot);
        }
    }
    
    public ItemStack getTypeStack() {
        ItemStack lotTypeLock = null;
        if (this.itemSerialized != null) {
            final FileConfiguration tmpconfig = new YamlConfiguration();
            try {
                tmpconfig.loadFromString(this.itemSerialized);
                if (tmpconfig.isItemStack("itemstack")) {
                    return tmpconfig.getItemStack("itemstack");
                }
            }
            catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }
        lotTypeLock = new ItemStack(this.lotTypeId, 1, this.lotDurability);
        for (final Map.Entry<Integer, Integer> enchantment : this.lotEnchantments.entrySet()) {
            lotTypeLock.addUnsafeEnchantment(new EnchantmentWrapper((int)enchantment.getKey()), (int)enchantment.getValue());
        }
        for (final Map.Entry<Integer, Integer> enchantment : this.storedEnchantments.entrySet()) {
            items.addStoredEnchantment(lotTypeLock, enchantment.getKey(), enchantment.getValue(), true);
        }
        lotTypeLock.setAmount(this.sourceStackQuantity);
        
        items.setDisplayName(lotTypeLock, this.displayName);
        items.setBookAuthor(lotTypeLock, this.bookAuthor);
        items.setBookTitle(lotTypeLock, this.bookTitle);
        items.setBookPages(lotTypeLock, this.bookPages);
        items.setRepairCost(lotTypeLock, this.repairCost);
        items.setHeadOwner(lotTypeLock, this.headOwner);
        items.setFireworkPower(lotTypeLock, this.power);
        items.setFireworkEffects(lotTypeLock, this.effects);
        items.setLore(lotTypeLock, this.lore);
        return lotTypeLock;
    }
    
    private void setLotType(final ItemStack lotType) {
        final FileConfiguration tmpconfig = (FileConfiguration)new YamlConfiguration();
        tmpconfig.set("itemstack", (Object)lotType);
        this.itemSerialized = tmpconfig.saveToString();
        this.lotTypeId = lotType.getTypeId();
        this.lotDurability = lotType.getDurability();
        this.sourceStackQuantity = lotType.getAmount();
        this.lotEnchantments = new HashMap<Integer, Integer>();
        this.storedEnchantments = new HashMap<Integer, Integer>();
        Map<Enchantment, Integer> enchantmentList = (Map<Enchantment, Integer>)lotType.getEnchantments();
        for (final Map.Entry<Enchantment, Integer> enchantment : enchantmentList.entrySet()) {
            this.lotEnchantments.put(enchantment.getKey().getId(), enchantment.getValue());
        }
        enchantmentList = items.getStoredEnchantments(lotType);
        if (enchantmentList != null) {
            for (final Map.Entry<Enchantment, Integer> enchantment : enchantmentList.entrySet()) {
                this.storedEnchantments.put(enchantment.getKey().getId(), enchantment.getValue());
            }
        }
        this.displayName = items.getDisplayName(lotType);
        this.bookAuthor = items.getBookAuthor(lotType);
        this.bookTitle = items.getBookTitle(lotType);
        this.bookPages = items.getBookPages(lotType);
        this.repairCost = items.getRepairCost(lotType);
        this.headOwner = items.getHeadOwner(lotType);
        this.power = items.getFireworkPower(lotType);
        this.effects = items.getFireworkEffects(lotType);
        this.lore = items.getLore(lotType);
    }
    
    public String getOwner() {
        return this.ownerName;
    }
    
    public int getQuantity() {
        return this.quantity;
    }
}
