package com.example.pickandthrow;

import org.bukkit.plugin.java.JavaPlugin;

public class PickAndThrowPlugin extends JavaPlugin {

    private ItemManager itemManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Save default language files
        saveResource("zh-CN.yml", false);
        saveResource("en-UK.yml", false);
        
        // Initialize managers
        languageManager = new LanguageManager(this);
        itemManager = new ItemManager(this);
        
        // Register event listener
        getServer().getPluginManager().registerEvents(new EntityPickupListener(this), this);
        
        // Register command
        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("pickandthrow").setExecutor(commandHandler);
        getCommand("pickandthrow").setTabCompleter(commandHandler);
        
        getLogger().info("PickAndThrow Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PickAndThrow Plugin disabled!");
    }
    
    /**
     * Get the maximum number of entities that can be picked up
     * @return Maximum number, -1 means unlimited
     */
    public int getMaxEntities() {
        return getConfig().getInt("max-entities", 5);
    }
    
    /**
     * Get throw mode: "one" or "all"
     */
    public String getThrowMode() {
        return getConfig().getString("throw-mode", "one");
    }
    
    /**
     * Get throw power mode: "fixed" or "charge"
     */
    public String getThrowPowerMode() {
        return getConfig().getString("throw-power-mode", "fixed");
    }
    
    /**
     * Get fixed throw horizontal velocity
     */
    public double getFixedHorizontalVelocity() {
        return getConfig().getDouble("throw-fixed.horizontal-velocity", 2.0);
    }
    
    /**
     * Get fixed throw vertical velocity
     */
    public double getFixedVerticalVelocity() {
        return getConfig().getDouble("throw-fixed.vertical-velocity", 0.8);
    }
    
    /**
     * Get max charge time in seconds
     */
    public double getMaxChargeTime() {
        return getConfig().getDouble("throw-charge.max-charge-time", 3.0);
    }
    
    /**
     * Get min velocity multiplier
     */
    public double getMinVelocityMultiplier() {
        return getConfig().getDouble("throw-charge.min-velocity-multiplier", 0.5);
    }
    
    /**
     * Get max velocity multiplier
     */
    public double getMaxVelocityMultiplier() {
        return getConfig().getDouble("throw-charge.max-velocity-multiplier", 3.0);
    }
    
    /**
     * Get base horizontal velocity for charge mode
     */
    public double getBaseHorizontalVelocity() {
        return getConfig().getDouble("throw-charge.base-horizontal-velocity", 2.0);
    }
    
    /**
     * Get base vertical velocity for charge mode
     */
    public double getBaseVerticalVelocity() {
        return getConfig().getDouble("throw-charge.base-vertical-velocity", 0.8);
    }
    
    /**
     * Get item manager
     */
    public ItemManager getItemManager() {
        return itemManager;
    }
    
    /**
     * Get language manager
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    /**
     * Get charge display type
     */
    public String getChargeDisplayType() {
        return getConfig().getString("charge-display-type", "bossbar");
    }
    
    /**
     * Get BossBar color for charge level
     */
    public String getBossBarColor(double progress) {
        if (progress < 0.33) {
            return getConfig().getString("bossbar-colors.low", "RED");
        } else if (progress < 0.66) {
            return getConfig().getString("bossbar-colors.medium", "YELLOW");
        } else {
            return getConfig().getString("bossbar-colors.high", "GREEN");
        }
    }
    
    /**
     * Get charge loop mode
     */
    public boolean isChargeLoop() {
        return getConfig().getBoolean("charge-loop", false);
    }
    
    /**
     * Get entity filter mode
     */
    public String getEntityFilterMode() {
        return getConfig().getString("entity-filter-mode", "blacklist");
    }
    
    /**
     * Get entity filter list
     */
    public java.util.List<String> getEntityFilterList() {
        return getConfig().getStringList("entity-filter-list");
    }
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (languageManager != null) {
            languageManager.loadLanguage();
        }
        if (itemManager != null) {
            itemManager.loadItem();
        }
    }
}

