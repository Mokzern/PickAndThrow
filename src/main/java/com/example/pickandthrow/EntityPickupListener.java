package com.example.pickandthrow;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityPickupListener implements Listener {
    
    private final PickAndThrowPlugin plugin;
    // Store entities being carried by players
    private final Map<UUID, Entity> carriedEntities = new HashMap<>();
    // Store charge start time for players
    private final Map<UUID, Long> chargeStartTime = new HashMap<>();
    // Store BossBar for each player
    private final Map<UUID, BossBar> chargeBossBars = new HashMap<>();
    // Store pickup time for cooldown check
    private final Map<UUID, Long> pickupTime = new HashMap<>();
    
    public EntityPickupListener(PickAndThrowPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Only handle main hand interaction
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check permission (no message if no permission)
        if (!player.hasPermission("pickandthrow.use")) {
            return;
        }
        
        Entity target = event.getRightClicked();
        UUID playerUUID = player.getUniqueId();
        
        // Check if player is holding an allowed item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!plugin.getItemManager().isHoldingAllowedItem(heldItem)) {
            return;
        }
        
        // Check if target is a living entity
        if (!(target instanceof LivingEntity)) {
            return;
        }
        
        // Check entity filter (whitelist/blacklist)
        if (!isEntityAllowed(target)) {
            return;
        }
        
        // If player is already carrying an entity
        if (carriedEntities.containsKey(playerUUID)) {
            // Check if target is in the stack (prevent clicking entity on head)
            Entity baseEntity = carriedEntities.get(playerUUID);
            if (isEntityInStack(baseEntity, target)) {
                // If clicked entity is in stack, use raycast to find real target
                Entity realTarget = findEntityBehindStack(player);
                if (realTarget != null && realTarget instanceof LivingEntity) {
                    stackEntity(player, realTarget);
                }
                event.setCancelled(true);
                return;
            }
            
            // Otherwise stack on top
            stackEntity(player, target);
            event.setCancelled(true);
            return;
        }
        
        // Otherwise pick up entity
        pickupEntity(player, target);
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle main hand interaction
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        // Only handle left click actions
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check permission (no message if no permission)
        if (!player.hasPermission("pickandthrow.use")) {
            return;
        }
        
        UUID playerUUID = player.getUniqueId();
        
        // Only process if player is carrying an entity
        if (!carriedEntities.containsKey(playerUUID)) {
            return;
        }
        
        // Check cooldown (prevent double-trigger)
        if (!canThrow(playerUUID)) {
            return;
        }
        
        // Check if player is holding an allowed item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!plugin.getItemManager().isHoldingAllowedItem(heldItem)) {
            return;
        }
        
        // If in charge mode, throw with current charge level
        if ("charge".equalsIgnoreCase(plugin.getThrowPowerMode())) {
            throwWithCharge(player);
        } else {
            // Fixed mode: throw with fixed velocity
            throwEntity(player, 1.0);
        }
        
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        // Check permission (no message if no permission)
        if (!player.hasPermission("pickandthrow.use")) {
            return;
        }
        
        UUID playerUUID = player.getUniqueId();
        
        // Only handle if using charge mode
        if (!"charge".equalsIgnoreCase(plugin.getThrowPowerMode())) {
            return;
        }
        
        // Only handle if player is carrying entities
        if (!carriedEntities.containsKey(playerUUID)) {
            return;
        }
        
        // Check if player is holding an allowed item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!plugin.getItemManager().isHoldingAllowedItem(heldItem)) {
            return;
        }
        
        if (event.isSneaking()) {
            // Player starts sneaking - start charging
            startCharging(player);
        } else {
            // Player stops sneaking - stop charging (but don't throw)
            // Player needs to left-click to throw
            stopCharging(player);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if damager is a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        
        // Check permission (no message if no permission)
        if (!player.hasPermission("pickandthrow.use")) {
            return;
        }
        
        UUID playerUUID = player.getUniqueId();
        
        // Only process if player is carrying an entity
        if (!carriedEntities.containsKey(playerUUID)) {
            return;
        }
        
        // Check cooldown (prevent double-trigger)
        if (!canThrow(playerUUID)) {
            return;
        }
        
        // Check if player is holding an allowed item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!plugin.getItemManager().isHoldingAllowedItem(heldItem)) {
            return;
        }
        
        // If in charge mode, throw with current charge level
        if ("charge".equalsIgnoreCase(plugin.getThrowPowerMode())) {
            throwWithCharge(player);
        } else {
            // Fixed mode: throw with fixed velocity
            throwEntity(player, 1.0);
        }
        
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up data when player quits
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        if (carriedEntities.containsKey(playerUUID)) {
            Entity entity = carriedEntities.get(playerUUID);
            if (entity != null && entity.isValid()) {
                // Remove all passengers in the stack
                removeAllPassengers(entity);
                entity.leaveVehicle();
            }
            carriedEntities.remove(playerUUID);
        }
        
        // Clean up charge data
        cleanupCharge(player);
        
        // Clean up pickup time
        pickupTime.remove(playerUUID);
    }
    
    /**
     * Recursively remove all passengers
     */
    private void removeAllPassengers(Entity entity) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        
        for (Entity passenger : entity.getPassengers()) {
            removeAllPassengers(passenger);
            passenger.leaveVehicle();
        }
    }
    
    /**
     * Check if entity is allowed based on whitelist/blacklist
     */
    private boolean isEntityAllowed(Entity entity) {
        String filterMode = plugin.getEntityFilterMode();
        java.util.List<String> filterList = plugin.getEntityFilterList();
        
        // If list is empty, behavior depends on mode
        if (filterList.isEmpty()) {
            // Whitelist + empty = allow none
            // Blacklist + empty = allow all
            return !"whitelist".equalsIgnoreCase(filterMode);
        }
        
        String entityType = entity.getType().name();
        boolean inList = filterList.contains(entityType);
        
        if ("whitelist".equalsIgnoreCase(filterMode)) {
            return inList; // Only allow if in list
        } else {
            // Blacklist mode (default)
            return !inList; // Only allow if NOT in list
        }
    }
    
    /**
     * Check if player can throw (cooldown check)
     */
    private boolean canThrow(UUID playerUUID) {
        long cooldown = plugin.getConfig().getLong("throw-cooldown", 300);
        if (cooldown <= 0) {
            return true; // No cooldown
        }
        
        Long lastPickup = pickupTime.get(playerUUID);
        if (lastPickup == null) {
            return true;
        }
        
        long elapsed = System.currentTimeMillis() - lastPickup;
        return elapsed >= cooldown;
    }
    
    /**
     * Pick up an entity
     */
    private void pickupEntity(Player player, Entity entity) {
        // Check if entity is already picked up by another player
        if (carriedEntities.containsValue(entity)) {
            return;
        }
        
        // Check quantity limit
        int maxEntities = plugin.getMaxEntities();
        if (maxEntities > 0) {
            // Currently 0, will become 1 after pickup
            if (1 > maxEntities) {
                return;
            }
        }
        
        // Make entity ride on player's head
        player.addPassenger(entity);
        carriedEntities.put(player.getUniqueId(), entity);
        
        // Record pickup time for cooldown
        pickupTime.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Check if entity is already in the stack
     */
    private boolean isEntityInStack(Entity stackBase, Entity target) {
        if (stackBase == null || target == null) {
            return false;
        }
        
        // Check if same entity
        if (stackBase.equals(target)) {
            return true;
        }
        
        // Recursively check all passengers
        for (Entity passenger : stackBase.getPassengers()) {
            if (isEntityInStack(passenger, target)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Count entities in the stack
     */
    private int countEntitiesInStack(Entity stackBase) {
        if (stackBase == null) {
            return 0;
        }
        
        int count = 1; // Current entity counts as 1
        
        // Recursively count all passengers
        for (Entity passenger : stackBase.getPassengers()) {
            count += countEntitiesInStack(passenger);
        }
        
        return count;
    }
    
    /**
     * Use raycast to find the entity player really wants to click (exclude entities in stack)
     */
    private Entity findEntityBehindStack(Player player) {
        UUID playerUUID = player.getUniqueId();
        Entity baseEntity = carriedEntities.get(playerUUID);
        
        if (baseEntity == null) {
            return null;
        }
        
        // Use world raycast to find entities in player's line of sight
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            5.0, // 5 block range
            0.5, // Entity hitbox accuracy
            entity -> {
                // Filter: must be living entity, not the player, and not in stack
                return entity instanceof LivingEntity 
                    && !entity.equals(player)
                    && !isEntityInStack(baseEntity, entity);
            }
        );
        
        if (result != null && result.getHitEntity() != null) {
            return result.getHitEntity();
        }
        
        return null;
    }
    
    /**
     * Get the topmost entity in the stack
     */
    private Entity getTopEntity(Player player) {
        UUID playerUUID = player.getUniqueId();
        Entity current = carriedEntities.get(playerUUID);
        
        if (current == null) {
            return null;
        }
        
        // Keep going up until no more passengers
        while (!current.getPassengers().isEmpty()) {
            current = current.getPassengers().get(0);
        }
        
        return current;
    }
    
    /**
     * Start charging throw power
     */
    private void startCharging(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Record charge start time
        chargeStartTime.put(playerUUID, System.currentTimeMillis());
        
        String displayType = plugin.getChargeDisplayType();
        
        if ("bossbar".equalsIgnoreCase(displayType)) {
            // Create and show BossBar with initial color from config
            String title = plugin.getLanguageManager().getMessage("charge-bar-title");
            String initialColorName = plugin.getBossBarColor(0.0); // Get color for 0% progress
            BarColor initialColor;
            try {
                initialColor = BarColor.valueOf(initialColorName.toUpperCase());
            } catch (IllegalArgumentException e) {
                initialColor = BarColor.RED; // Fallback
            }
            
            BossBar bossBar = Bukkit.createBossBar(title, initialColor, BarStyle.SOLID);
            bossBar.setProgress(0.0);
            bossBar.addPlayer(player);
            chargeBossBars.put(playerUUID, bossBar);
        }
        
        // Start repeating task to update charge display
        updateChargeBar(player);
    }
    
    /**
     * Update charge bar progress
     */
    private void updateChargeBar(Player player) {
        UUID playerUUID = player.getUniqueId();
        String displayType = plugin.getChargeDisplayType();
        
        Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                if (!chargeStartTime.containsKey(playerUUID) || !player.isOnline()) {
                    return;
                }
                
                Long storedValue = chargeStartTime.get(playerUUID);
                // If value is negative, charge was locked (stopped)
                if (storedValue < 0) {
                    return;
                }
                
                long startTime = storedValue;
                double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
                double maxChargeTime = plugin.getMaxChargeTime();
                double rawProgress = elapsedSeconds / maxChargeTime;
                
                // Handle charge loop mode
                double progress;
                if (plugin.isChargeLoop()) {
                    // Loop: 0% -> 100% -> 0% -> 100% ...
                    progress = rawProgress % 2.0; // 0 to 2
                    if (progress > 1.0) {
                        progress = 2.0 - progress; // 1 to 0 (reverse)
                    }
                } else {
                    // Normal: cap at 100%
                    progress = Math.min(rawProgress, 1.0);
                }
                
                if ("bossbar".equalsIgnoreCase(displayType)) {
                    // Update BossBar
                    BossBar bossBar = chargeBossBars.get(playerUUID);
                    if (bossBar != null) {
                        bossBar.setProgress(progress);
                        
                        // Change color based on progress from config
                        String colorName = plugin.getBossBarColor(progress);
                        try {
                            BarColor color = BarColor.valueOf(colorName.toUpperCase());
                            bossBar.setColor(color);
                        } catch (IllegalArgumentException e) {
                            // Fallback to default colors
                            if (progress < 0.33) {
                                bossBar.setColor(BarColor.RED);
                            } else if (progress < 0.66) {
                                bossBar.setColor(BarColor.YELLOW);
                            } else {
                                bossBar.setColor(BarColor.GREEN);
                            }
                        }
                    }
                } else {
                    // Update ActionBar - Rainbow bars
                    int totalBars = 30;
                    int filledBars = (int) (progress * totalBars);
                    
                    // Rainbow colors
                    ChatColor[] rainbowColors = {
                        ChatColor.RED,
                        ChatColor.GOLD,
                        ChatColor.YELLOW,
                        ChatColor.GREEN,
                        ChatColor.AQUA,
                        ChatColor.BLUE,
                        ChatColor.LIGHT_PURPLE
                    };
                    
                    StringBuilder bar = new StringBuilder();
                    
                    for (int i = 0; i < totalBars; i++) {
                        if (i < filledBars) {
                            // Rainbow color based on position
                            int colorIndex = (i * rainbowColors.length) / totalBars;
                            bar.append(rainbowColors[colorIndex]).append("|");
                        } else {
                            // Gray for unfilled
                            bar.append(ChatColor.DARK_GRAY).append("|");
                        }
                    }
                    
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar.toString()));
                }
            }
        };
        
        // Use compatible scheduler
        SchedulerUtil.runPlayerTaskTimer(plugin, player, updateTask, 1L, 1L);
    }
    
    /**
     * Stop charging (keep charge level for later throw)
     */
    private void stopCharging(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        if (!chargeStartTime.containsKey(playerUUID)) {
            return;
        }
        
        // Calculate and store the final charge level
        long startTime = chargeStartTime.get(playerUUID);
        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        double maxChargeTime = plugin.getMaxChargeTime();
        double chargePercent = Math.min(elapsedSeconds / maxChargeTime, 1.0);
        
        double minMultiplier = plugin.getMinVelocityMultiplier();
        double maxMultiplier = plugin.getMaxVelocityMultiplier();
        double multiplier = minMultiplier + (maxMultiplier - minMultiplier) * chargePercent;
        
        // Store the charge level temporarily
        chargeStartTime.put(playerUUID, -((long)(multiplier * 1000))); // Store as negative to indicate it's locked
        
        String displayType = plugin.getChargeDisplayType();
        
        if ("bossbar".equalsIgnoreCase(displayType)) {
            // Hide BossBar but keep it
            BossBar bossBar = chargeBossBars.get(playerUUID);
            if (bossBar != null) {
                bossBar.setVisible(false);
            }
        } else {
            // Clear ActionBar
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        }
    }
    
    /**
     * Throw with charged power
     */
    private void throwWithCharge(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        double multiplier;
        
        if (chargeStartTime.containsKey(playerUUID)) {
            Long storedValue = chargeStartTime.get(playerUUID);
            
            if (storedValue < 0) {
                // Charge level was locked, use stored value
                multiplier = Math.abs(storedValue) / 1000.0;
            } else {
                // Still charging, calculate current level
                long startTime = storedValue;
                double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
                double maxChargeTime = plugin.getMaxChargeTime();
                double chargePercent = Math.min(elapsedSeconds / maxChargeTime, 1.0);
                
                double minMultiplier = plugin.getMinVelocityMultiplier();
                double maxMultiplier = plugin.getMaxVelocityMultiplier();
                multiplier = minMultiplier + (maxMultiplier - minMultiplier) * chargePercent;
            }
        } else {
            // No charge, use minimum multiplier
            multiplier = plugin.getMinVelocityMultiplier();
        }
        
        // Clean up charge data
        cleanupCharge(player);
        
        // Throw with calculated multiplier
        throwEntity(player, multiplier);
    }
    
    /**
     * Clean up charge data for player
     */
    private void cleanupCharge(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        chargeStartTime.remove(playerUUID);
        
        BossBar bossBar = chargeBossBars.remove(playerUUID);
        if (bossBar != null) {
            bossBar.removeAll();
        }
        
        // Clear ActionBar
        if (player.isOnline()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        }
    }
    
    /**
     * Throw entity with velocity multiplier
     */
    private void throwEntity(Player player, double velocityMultiplier) {
        UUID playerUUID = player.getUniqueId();
        Entity baseEntity = carriedEntities.get(playerUUID);
        
        if (baseEntity == null || !baseEntity.isValid()) {
            carriedEntities.remove(playerUUID);
            return;
        }
        
        String throwMode = plugin.getThrowMode();
        String powerMode = plugin.getThrowPowerMode();
        
        // Get base velocity
        double baseVelocity;
        if ("charge".equalsIgnoreCase(powerMode)) {
            baseVelocity = plugin.getBaseHorizontalVelocity();
        } else {
            baseVelocity = plugin.getFixedHorizontalVelocity();
        }
        
        // Calculate direction and velocity - follow player's exact look direction
        Vector direction = player.getLocation().getDirection().normalize();
        double speed = baseVelocity * velocityMultiplier;
        Vector velocity = direction.multiply(speed);
        
        if ("all".equalsIgnoreCase(throwMode)) {
            // Throw all entities at once
            throwAllEntities(player, baseEntity, velocity);
            carriedEntities.remove(playerUUID);
        } else {
            // Throw one at a time (default)
            Entity topEntity = getTopEntity(player);
            
            if (topEntity != null && topEntity.isValid()) {
                // Remove topmost entity from ride
                topEntity.leaveVehicle();
                
                // Use compatible scheduler for the entity
                final Vector finalVelocity = velocity;
                SchedulerUtil.runEntityTask(plugin, topEntity, () -> {
                    topEntity.setVelocity(finalVelocity);
                });
                
                // If threw the first entity on player's head, clear record
                if (topEntity == baseEntity) {
                    carriedEntities.remove(playerUUID);
                    pickupTime.remove(playerUUID);
                }
            }
        }
    }
    
    /**
     * Stack entity (place new entity on top)
     */
    private void stackEntity(Player player, Entity newEntity) {
        UUID playerUUID = player.getUniqueId();
        Entity baseEntity = carriedEntities.get(playerUUID);
        
        // Check if new entity is already in stack (prevent entity riding itself)
        if (isEntityInStack(baseEntity, newEntity)) {
            return;
        }
        
        // Check if entity is already picked up by another player
        if (carriedEntities.containsValue(newEntity)) {
            return;
        }
        
        // Check quantity limit
        int maxEntities = plugin.getMaxEntities();
        if (maxEntities > 0) {
            int currentCount = countEntitiesInStack(baseEntity);
            if (currentCount >= maxEntities) {
                // Reached limit, cannot stack more
                return;
            }
        }
        
        // Find the topmost entity
        Entity topEntity = getTopEntity(player);
        
        if (topEntity != null && topEntity.isValid()) {
            // Make new entity ride on top of the topmost entity
            topEntity.addPassenger(newEntity);
        }
        
        // Update pickup time when stacking (reset cooldown)
        pickupTime.put(playerUUID, System.currentTimeMillis());
    }
    
    /**
     * Throw all entities in the stack
     */
    private void throwAllEntities(Player player, Entity baseEntity, Vector velocity) {
        if (baseEntity == null || !baseEntity.isValid()) {
            return;
        }
        
        // Remove base entity from player
        baseEntity.leaveVehicle();
        
        // Recursively throw all passengers
        throwEntityAndPassengers(baseEntity, velocity);
    }
    
    /**
     * Recursively throw entity and all its passengers
     */
    private void throwEntityAndPassengers(Entity entity, Vector velocity) {
        if (entity == null || !entity.isValid()) {
            return;
        }
        
        // Get passengers before removing them
        java.util.List<Entity> passengers = new java.util.ArrayList<>(entity.getPassengers());
        
        // Remove all passengers
        for (Entity passenger : passengers) {
            passenger.leaveVehicle();
        }
        
        // Throw current entity
        final Vector finalVelocity = velocity.clone();
        SchedulerUtil.runEntityTask(plugin, entity, () -> {
            entity.setVelocity(finalVelocity);
        });
        
        // Recursively throw all passengers
        for (Entity passenger : passengers) {
            throwEntityAndPassengers(passenger, velocity);
        }
    }
}

