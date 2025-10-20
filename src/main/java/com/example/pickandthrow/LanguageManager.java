package com.example.pickandthrow;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LanguageManager {
    
    private final PickAndThrowPlugin plugin;
    private FileConfiguration langConfig;
    private String currentLanguage;
    
    public LanguageManager(PickAndThrowPlugin plugin) {
        this.plugin = plugin;
        loadLanguage();
    }
    
    /**
     * Load language file based on config
     */
    public void loadLanguage() {
        String lang = plugin.getConfig().getString("language", "zh-CN");
        String fileName = lang + ".yml";
        
        File langFile = new File(plugin.getDataFolder(), fileName);
        
        // If specified language file doesn't exist, try to create it from resources
        if (!langFile.exists()) {
            try {
                plugin.saveResource(fileName, false);
            } catch (Exception e) {
                // If resource doesn't exist, fallback to en-UK
                plugin.getLogger().warning("Language file " + fileName + " not found, using default en-UK.yml");
                lang = "en-UK";
                fileName = "en-UK.yml";
                langFile = new File(plugin.getDataFolder(), fileName);
                
                if (!langFile.exists()) {
                    plugin.saveResource(fileName, false);
                }
            }
        }
        
        currentLanguage = lang;
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }
    
    /**
     * Get message from language file
     */
    public String getMessage(String path) {
        String message = langConfig.getString(path, "Missing message: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Get message with placeholder replacement
     */
    public String getMessage(String path, String placeholder, String value) {
        String message = getMessage(path);
        return message.replace("{" + placeholder + "}", value);
    }
    
    /**
     * Get current language code
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
