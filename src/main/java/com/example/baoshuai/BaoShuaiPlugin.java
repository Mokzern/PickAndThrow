package com.example.baoshuai;

import org.bukkit.plugin.java.JavaPlugin;

public class BaoShuaiPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // 保存默认配置文件
        saveDefaultConfig();
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new EntityPickupListener(this), this);
        
        int maxEntities = getConfig().getInt("max-entities", 5);
        if (maxEntities == -1) {
            getLogger().info("抱摔插件已启用！无限制叠加模式");
        } else {
            getLogger().info("抱摔插件已启用！最大叠加数量: " + maxEntities);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("抱摔插件已禁用！");
    }
    
    /**
     * 获取最大可举起的生物数量
     * @return 最大数量，-1表示无限制
     */
    public int getMaxEntities() {
        return getConfig().getInt("max-entities", 5);
    }
    
    /**
     * 获取抛出水平速度
     */
    public double getThrowHorizontalVelocity() {
        return getConfig().getDouble("throw.horizontal-velocity", 2.0);
    }
    
    /**
     * 获取抛出垂直速度
     */
    public double getThrowVerticalVelocity() {
        return getConfig().getDouble("throw.vertical-velocity", 0.8);
    }
}


