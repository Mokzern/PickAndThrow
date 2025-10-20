package com.example.pickandthrow;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ItemManager {
    
    private final PickAndThrowPlugin plugin;
    private ItemStack allowedItem;
    
    public ItemManager(PickAndThrowPlugin plugin) {
        this.plugin = plugin;
        loadItem();
    }
    
    /**
     * Load item from config
     */
    public void loadItem() {
        allowedItem = null;
        
        if (plugin.getConfig().contains("pickup-item")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) plugin.getConfig().get("pickup-item");
            if (itemMap != null) {
                allowedItem = ItemStack.deserialize(itemMap);
            }
        }
    }
    
    /**
     * Save item to config
     */
    public void saveItem() {
        if (allowedItem == null) {
            plugin.getConfig().set("pickup-item", null);
        } else {
            plugin.getConfig().set("pickup-item", allowedItem.serialize());
        }
        
        plugin.saveConfig();
    }
    
    /**
     * Set the allowed item
     */
    public void setItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            allowedItem = null;
        } else {
            allowedItem = item.clone();
        }
        saveItem();
    }
    
    /**
     * Get the allowed item
     */
    public ItemStack getAllowedItem() {
        return allowedItem == null ? null : allowedItem.clone();
    }
    
    /**
     * Check if player is holding an allowed item
     */
    public boolean isHoldingAllowedItem(ItemStack item) {
        // If no custom item is set, only allow empty hand
        if (allowedItem == null) {
            return item == null || item.getType() == Material.AIR;
        }
        
        // Check if holding item matches the allowed item (including NBT)
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        return allowedItem.isSimilar(item);
    }
    
    /**
     * Check if custom item is set
     */
    public boolean hasCustomItem() {
        return allowedItem != null;
    }
}
