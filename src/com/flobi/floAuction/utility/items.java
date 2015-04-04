package com.flobi.floAuction.utility;

import org.bukkit.inventory.*;
import org.bukkit.enchantments.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.entity.*;
import org.bukkit.*;

import com.flobi.floAuction.*;
import com.flobi.WhatIsIt.*;

import net.milkbowl.vault.item.*;

import java.util.*;

public class items
{
    private static Map<Integer, String> enchantmentNames;
    private static Map<Integer, String> enchantmentLevels;
    
    static {
        items.enchantmentNames = null;
        items.enchantmentLevels = null;
    }
    
    private static int firstPartial(final ItemStack item, final ItemStack[] inventory) {
        if (item == null) {
            return -1;
        }
        for (int i = 0; i < inventory.length; ++i) {
            final ItemStack cItem = inventory[i];
            if (cItem != null && cItem.getAmount() < cItem.getMaxStackSize() && isSameItem(item, cItem)) {
                return i;
            }
        }
        return -1;
    }
    
    private static int firstEmpty(final ItemStack[] inventory) {
        for (int i = 0; i < inventory.length; ++i) {
            if (inventory[i] == null) {
                return i;
            }
        }
        return -1;
    }
    
    public static void saferItemGive(final PlayerInventory playerInventory, final ItemStack item) {
        while (true) {
            final int firstPartial = firstPartial(item, playerInventory.getContents());
            if (firstPartial == -1) {
                final int firstFree = firstEmpty(playerInventory.getContents());
                if (firstFree == -1) {
                    break;
                }
                playerInventory.setItem(firstFree, item);
                break;
            }
            else {
                final ItemStack partialItem = playerInventory.getItem(firstPartial);
                final int amount = item.getAmount();
                final int partialAmount = partialItem.getAmount();
                final int maxAmount = partialItem.getMaxStackSize();
                if (amount + partialAmount <= maxAmount) {
                    partialItem.setAmount(amount + partialAmount);
                    break;
                }
                partialItem.setAmount(maxAmount);
                item.setAmount(amount + partialAmount - maxAmount);
            }
        }
    }
    
    public static String[] getLore(final ItemStack item) {
        if (item == null) {
            return null;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        final List<String> pageList = (List<String>)itemMeta.getLore();
        if (pageList == null) {
            return null;
        }
        final String[] pages = new String[pageList.size()];
        for (int i = 0; i < pageList.size(); ++i) {
            pages[i] = pageList.get(i);
        }
        return pages;
    }
    
    public static void setLore(final ItemStack item, final String[] pages) {
        if (item == null || pages == null) {
            return;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        final List<String> pageList = new ArrayList<String>();
        for (int i = 0; i < pages.length; ++i) {
            pageList.add(pages[i]);
        }
        itemMeta.setLore(pageList);
        item.setItemMeta(itemMeta);
    }
    
    public static Map<Enchantment, Integer> getStoredEnchantments(final ItemStack item) {
        if (item == null) {
            return null;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        if (itemMeta instanceof EnchantmentStorageMeta) {
            return (Map<Enchantment, Integer>)((EnchantmentStorageMeta)itemMeta).getStoredEnchants();
        }
        return null;
    }
    
    public static void addStoredEnchantment(final ItemStack item, final Integer enchantment, final Integer level, final boolean ignoreLevelRestriction) {
        if (item == null) {
            return;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        if (itemMeta instanceof EnchantmentStorageMeta) {
            final EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta)itemMeta;
            storageMeta.addStoredEnchant((Enchantment)new EnchantmentWrapper((int)enchantment), (int)level, ignoreLevelRestriction);
            item.setItemMeta((ItemMeta)storageMeta);
        }
    }
    
    public static Integer getFireworkPower(final ItemStack item) {
        if (item == null) {
            return null;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        if (itemMeta instanceof FireworkMeta) {
            return ((FireworkMeta)itemMeta).getPower();
        }
        return null;
    }
    
    public static void setFireworkPower(final ItemStack item, final Integer power) {
        if (item == null) {
            return;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        if (itemMeta instanceof FireworkMeta) {
            final FireworkMeta fireworkMeta = (FireworkMeta)itemMeta;
            fireworkMeta.setPower((int)power);
            item.setItemMeta((ItemMeta)fireworkMeta);
        }
    }
    
    public static FireworkEffect[] getFireworkEffects(final ItemStack item) {
        if (item == null) {
            return null;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        if (itemMeta instanceof FireworkMeta) {
            final List<FireworkEffect> effectList = (List<FireworkEffect>)((FireworkMeta)itemMeta).getEffects();
            final FireworkEffect[] effects = new FireworkEffect[effectList.size()];
            for (int i = 0; i < effectList.size(); ++i) {
                effects[i] = effectList.get(i);
            }
            return effects;
        }
        if (itemMeta instanceof FireworkEffectMeta) {
            final FireworkEffect[] effects2 = { ((FireworkEffectMeta)itemMeta).getEffect() };
            return effects2;
        }
        return null;
    }
    
    public static void setFireworkEffects(final ItemStack item, final FireworkEffect[] effects) {
        if (item == null || effects == null) {
            return;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        if (itemMeta instanceof FireworkMeta) {
            final FireworkMeta fireworkMeta = (FireworkMeta)itemMeta;
            fireworkMeta.addEffects(effects);
            item.setItemMeta((ItemMeta)fireworkMeta);
        }
        else if (itemMeta instanceof FireworkEffectMeta && effects.length > 0) {
            final FireworkEffectMeta fireworkEffectMeta = (FireworkEffectMeta)itemMeta;
            fireworkEffectMeta.setEffect(effects[0]);
            item.setItemMeta((ItemMeta)fireworkEffectMeta);
        }
    }
    
    public static String getHeadOwner(final ItemStack item) {
        if (item == null) {
            return null;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        if (itemMeta instanceof SkullMeta) {
            return ((SkullMeta)itemMeta).getOwner();
        }
        return null;
    }
    
    public static void setHeadOwner(final ItemStack item, final String headName) {
        if (item == null) {
            return;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        if (itemMeta instanceof SkullMeta) {
            final SkullMeta skullMeta = (SkullMeta)itemMeta;
            skullMeta.setOwner(headName);
            item.setItemMeta((ItemMeta)skullMeta);
        }
    }
    
    public static Integer getRepairCost(final ItemStack item) {
        if (item == null) {
            return null;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        if (itemMeta instanceof Repairable) {
            return ((Repairable)itemMeta).getRepairCost();
        }
        return null;
    }
    
    public static void setRepairCost(final ItemStack item, final Integer repairCost) {
        if (item == null) {
            return;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        if (itemMeta instanceof Repairable) {
            final Repairable repairable = (Repairable)itemMeta;
            repairable.setRepairCost((int)repairCost);
            item.setItemMeta((ItemMeta)repairable);
        }
    }
    
    public static String getDisplayName(final ItemStack item) {
        if (item == null) {
            return null;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        return itemMeta.getDisplayName();
    }
    
    public static void setDisplayName(final ItemStack item, final String name) {
        if (item == null) {
            return;
        }
        if (name == null) {
            return;
        }
        final ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        itemMeta.setDisplayName(name);
        item.setItemMeta(itemMeta);
    }
    
    public static String getBookAuthor(final ItemStack book) {
        if (book == null) {
            return null;
        }
        final ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        if (itemMeta instanceof BookMeta) {
            return ((BookMeta)itemMeta).getAuthor();
        }
        return null;
    }
    
    public static void setBookAuthor(final ItemStack book, final String author) {
        if (book == null) {
            return;
        }
        final ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        if (itemMeta instanceof BookMeta) {
            final BookMeta bookMeta = (BookMeta)itemMeta;
            bookMeta.setAuthor(author);
            book.setItemMeta((ItemMeta)bookMeta);
        }
    }
    
    public static String getBookTitle(final ItemStack book) {
        if (book == null) {
            return null;
        }
        final ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        if (itemMeta instanceof BookMeta) {
            return ((BookMeta)itemMeta).getTitle();
        }
        return null;
    }
    
    public static void setBookTitle(final ItemStack book, final String title) {
        if (book == null) {
            return;
        }
        final ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        if (itemMeta instanceof BookMeta) {
            final BookMeta bookMeta = (BookMeta)itemMeta;
            bookMeta.setTitle(title);
            book.setItemMeta((ItemMeta)bookMeta);
        }
    }
    
    public static String[] getBookPages(final ItemStack book) {
        if (book == null) {
            return null;
        }
        final ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        if (itemMeta instanceof BookMeta) {
            final List<String> pageList = (List<String>)((BookMeta)itemMeta).getPages();
            final String[] pages = new String[pageList.size()];
            for (int i = 0; i < pageList.size(); ++i) {
                pages[i] = pageList.get(i);
            }
            return pages;
        }
        return null;
    }
    
    public static void setBookPages(final ItemStack book, final String[] pages) {
        if (book == null || pages == null) {
            return;
        }
        final ItemMeta itemMeta = book.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        if (itemMeta instanceof BookMeta) {
            final BookMeta bookMeta = (BookMeta)itemMeta;
            bookMeta.setPages(pages);
            book.setItemMeta((ItemMeta)bookMeta);
        }
    }
    
    @SuppressWarnings("deprecation")
	public static boolean isSameItem(final ItemStack item, final String searchString) {
        if (searchString.matches("\\d+;\\d+")) {
            final String[] params = searchString.split(";");
            final int typeId = Integer.parseInt(params[0]);
            final short subTypeId = Short.parseShort(params[1]);
            if (item.getTypeId() == typeId && item.getDurability() == subTypeId) {
                return true;
            }
        }
        else if (searchString.matches("\\d+")) {
            final int typeId2 = Integer.parseInt(searchString);
            if (item.getTypeId() == typeId2) {
                return true;
            }
        }
        return false;
    }
    
    @SuppressWarnings("deprecation")
	public static boolean isSameItem(final ItemStack item1, final ItemStack item2) {
        if (item1 == null) {
            return false;
        }
        if (item2 == null) {
            return false;
        }
        if (item1.getTypeId() != item2.getTypeId()) {
            return false;
        }
        if (item1.getData().getData() != item2.getData().getData()) {
            return false;
        }
        if (item1.getDurability() != item2.getDurability()) {
            return false;
        }
        if (!item1.getEnchantments().equals(item2.getEnchantments())) {
            return false;
        }
        if (!isSame(getHeadOwner(item1), getHeadOwner(item2))) {
            return false;
        }
        if (!isSame(getRepairCost(item1), getRepairCost(item2))) {
            return false;
        }
        if (!isSame(getDisplayName(item1), getDisplayName(item2))) {
            return false;
        }
        if (!isSame(getFireworkPower(item1), getFireworkPower(item2))) {
            return false;
        }
        if (!isSame(getFireworkEffects(item1), getFireworkEffects(item2))) {
            return false;
        }
        if (!isSame(getStoredEnchantments(item1), getStoredEnchantments(item2))) {
            return false;
        }
        if (!isSame(getLore(item1), getLore(item2))) {
            return false;
        }
        if (!isSame(getBookAuthor(item1), getBookAuthor(item2))) {
            return false;
        }
        if (!isSame(getBookTitle(item1), getBookTitle(item2))) {
            return false;
        }
        final String[] pages1 = getBookPages(item1);
        final String[] pages2 = getBookPages(item2);
        if (pages1 == null ^ pages2 == null) {
            return false;
        }
        if (pages1 != null) {
            if (pages1.length != pages2.length) {
                return false;
            }
            for (int i = 0; i < pages1.length; ++i) {
                if (!pages1[i].equals(pages2[i])) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private static boolean isSame(final String[] strings1, final String[] strings2) {
        if (strings1 == null && strings2 == null) {
            return true;
        }
        if (strings1 == null || strings2 == null) {
            return false;
        }
        if (strings1.length != strings2.length) {
            return false;
        }
        for (int i = 0; i < strings1.length; ++i) {
            if (!isSame(strings1[i], strings2[i])) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean isSame(final Map<Enchantment, Integer> storedEnchantments1, final Map<Enchantment, Integer> storedEnchantments2) {
        return (storedEnchantments1 == null && storedEnchantments2 == null) || (storedEnchantments1 != null && storedEnchantments2 != null && storedEnchantments1.equals(storedEnchantments2));
    }
    
    private static boolean isSame(final String str1, final String str2) {
        return (str1 == null && str2 == null) || (str1 != null && str2 != null && str1.equals(str2));
    }
    
    private static boolean isSame(final Integer int1, final Integer int2) {
        return (int1 == null && int2 == null) || (int1 != null && int2 != null && int1.equals(int2));
    }
    
    private static boolean isSame(final FireworkEffect[] effects1, final FireworkEffect[] effects2) {
        if (effects1 == null && effects2 == null) {
            return true;
        }
        if (effects1 == null || effects2 == null) {
            return false;
        }
        if (effects1.length != effects2.length) {
            return false;
        }
        for (int i = 0; i < effects1.length; ++i) {
            if (!isSame(Integer.valueOf(effects1[i].hashCode()), Integer.valueOf(effects2[i].hashCode()))) {
                return false;
            }
        }
        return true;
    }
    
    public static int getMaxStackSize(final ItemStack item) {
        if (item == null) {
            return 0;
        }
        final int maxStackSize = item.getType().getMaxStackSize();
        return maxStackSize;
    }
    
    @SuppressWarnings("deprecation")
	public static boolean isStackable(final int id) {
        return getMaxStackSize(new ItemStack(id)) > 1;
    }
    
    public static int getSpaceForItem(final Player player, final ItemStack item) {
        final int maxstack = getMaxStackSize(item);
        int space = 0;
        final ItemStack[] items = player.getInventory().getContents();
        ItemStack[] array;
        for (int length = (array = items).length, i = 0; i < length; ++i) {
            final ItemStack current = array[i];
            if (current == null) {
                space += maxstack;
            }
            else if (isSameItem(item, current)) {
                space += maxstack - current.getAmount();
            }
        }
        return space;
    }
    
    public static boolean hasSpace(final Player player, final int needed, final ItemStack item) {
        return getSpaceForItem(player, item) >= needed;
    }
    
    public static boolean hasSpace(final Player player, final ArrayList<ItemStack> lot) {
        int needed = 0;
        for (final ItemStack item : lot) {
            if (item != null) {
                needed += item.getAmount();
            }
        }
        return hasSpace(player, needed, lot.get(0));
    }
    
    public static boolean hasAmount(final String ownerName, final int amount, final ItemStack compareItem) {
        final int has = getAmount(ownerName, compareItem);
        return has >= amount;
    }
    
    public static int getAmount(final String ownerName, final ItemStack compareItem) {
        if (Bukkit.getPlayer(ownerName) == null) {
            return 0;
        }
        final PlayerInventory inventory = Bukkit.getPlayer(ownerName).getInventory();
        final ItemStack[] items = inventory.getContents();
        int has = 0;
        ItemStack[] array;
        for (int length = (array = items).length, i = 0; i < length; ++i) {
            final ItemStack item = array[i];
            if (isSameItem(compareItem, item)) {
                has += item.getAmount();
            }
        }
        return has;
    }
    
    public static void remove(final String playerName, int amount, final ItemStack compareItem) {
        final Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            final PlayerInventory inventory = player.getInventory();
            if (isSameItem(compareItem, player.getItemInHand())) {
                final int heldAmount = player.getItemInHand().getAmount();
                if (heldAmount <= amount) {
                    amount -= heldAmount;
                    inventory.clear(inventory.getHeldItemSlot());
                }
                else {
                    player.getItemInHand().setAmount(heldAmount - amount);
                    amount = 0;
                }
            }
            int counter = amount;
            int leftover = 0;
            for (int invIndex = 0; invIndex < inventory.getSize(); ++invIndex) {
                final ItemStack current = inventory.getItem(invIndex);
                if (current != null) {
                    if (current.getAmount() > 0) {
                        if (isSameItem(compareItem, current)) {
                            if (current.getAmount() > counter) {
                                leftover = current.getAmount() - counter;
                            }
                            if (leftover != 0) {
                                current.setAmount(leftover);
                                counter = 0;
                                break;
                            }
                            counter -= current.getAmount();
                            inventory.clear(invIndex);
                        }
                    }
                }
            }
        }
    }
    
    public static boolean isEnchantable(final ItemStack heldItem) {
        final ItemStack item = new ItemStack(heldItem.getType());
        Enchantment[] values;
        for (int length = (values = Enchantment.values()).length, i = 0; i < length; ++i) {
            final Enchantment ench = values[i];
            if (ench.canEnchantItem(item)) {
                return true;
            }
        }
        return false;
    }
    
    public static String getItemName(final ItemStack typeStack) {
        if (floAuction.useWhatIsIt) {
            return WhatIsIt.itemName(typeStack);
        }
        final ItemInfo itemInfo = Items.itemByStack(typeStack);
        if (itemInfo == null) {
            return typeStack.getType().name();
        }
        return itemInfo.getName();
    }
    
    @SuppressWarnings("deprecation")
	public static String getEnchantmentName(final Map.Entry<Enchantment, Integer> enchantment) {
        if (floAuction.useWhatIsIt) {
            return WhatIsIt.enchantmentName(enchantment);
        }
        final int enchantmentId = enchantment.getKey().getId();
        final int enchantmentLevel = enchantment.getValue();
        String enchantmentName = null;
        if (items.enchantmentNames == null) {
            (items.enchantmentNames = new HashMap<Integer, String>()).put(0, "Protection");
            items.enchantmentNames.put(1, "Fire Protection");
            items.enchantmentNames.put(2, "Feather Falling");
            items.enchantmentNames.put(3, "Blast Protection");
            items.enchantmentNames.put(4, "Projectile Protection");
            items.enchantmentNames.put(5, "Respiration");
            items.enchantmentNames.put(6, "Aqua Afinity");
            items.enchantmentNames.put(16, "Sharpness");
            items.enchantmentNames.put(17, "Smite");
            items.enchantmentNames.put(18, "Bane of Arthropods");
            items.enchantmentNames.put(19, "Knockback");
            items.enchantmentNames.put(20, "Fire Aspect");
            items.enchantmentNames.put(21, "Looting");
            items.enchantmentNames.put(32, "Efficiency");
            items.enchantmentNames.put(33, "Silk Touch");
            items.enchantmentNames.put(34, "Unbreaking");
            items.enchantmentNames.put(35, "Fortune");
            items.enchantmentNames.put(48, "Power");
            items.enchantmentNames.put(49, "Punch");
            items.enchantmentNames.put(50, "Flame");
            items.enchantmentNames.put(51, "Infinity");
        }
        if (items.enchantmentNames.get(enchantmentId) != null) {
            enchantmentName = String.valueOf(items.enchantmentNames.get(enchantmentId)) + " ";
        }
        else {
            enchantmentName = "UNKNOWN ";
        }
        if (items.enchantmentLevels == null) {
            (items.enchantmentLevels = new HashMap<Integer, String>()).put(0, "");
            items.enchantmentLevels.put(1, "I");
            items.enchantmentLevels.put(2, "II");
            items.enchantmentLevels.put(3, "III");
            items.enchantmentLevels.put(4, "IV");
            items.enchantmentLevels.put(5, "V");
        }
        if (items.enchantmentLevels.get(enchantmentLevel) != null) {
            enchantmentName = String.valueOf(items.enchantmentLevels.get(enchantmentLevel)) + " ";
        }
        else {
            enchantmentName = String.valueOf(enchantmentName) + enchantmentLevel;
        }
        return enchantmentName;
    }
}
