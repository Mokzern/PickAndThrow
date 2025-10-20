package com.example.pickandthrow;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Utility class for handling scheduler compatibility between Folia and regular Bukkit servers
 */
public class SchedulerUtil {
    
    private static Boolean isFolia = null;
    
    /**
     * Check if the server is running Folia
     */
    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }
    
    /**
     * Run a task for an entity with compatibility
     */
    public static void runEntityTask(Plugin plugin, Entity entity, Runnable task) {
        if (isFolia()) {
            // Use Folia's entity scheduler
            entity.getScheduler().run(plugin, (scheduledTask) -> task.run(), null);
        } else {
            // Use regular Bukkit scheduler
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * Run a repeating task for a player with compatibility
     */
    public static void runPlayerTaskTimer(Plugin plugin, Player player, Runnable task, long delay, long period) {
        if (isFolia()) {
            // Use Folia's player scheduler
            player.getScheduler().runAtFixedRate(plugin, (scheduledTask) -> {
                task.run();
            }, null, delay, period);
        } else {
            // Use regular Bukkit scheduler
            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }
    
    /**
     * Run a task with compatibility
     */
    public static void runTask(Plugin plugin, Runnable task) {
        if (isFolia()) {
            // Use Folia's global region scheduler
            Bukkit.getGlobalRegionScheduler().run(plugin, (scheduledTask) -> task.run());
        } else {
            // Use regular Bukkit scheduler
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}

