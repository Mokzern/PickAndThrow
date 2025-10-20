package com.example.baoshuai;

import org.bukkit.Material;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityPickupListener implements Listener {
    
    private final BaoShuaiPlugin plugin;
    // 存储玩家正在举起的实体
    private final Map<UUID, Entity> carriedEntities = new HashMap<>();
    
    public EntityPickupListener(BaoShuaiPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // 只处理主手交互
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        Entity target = event.getRightClicked();
        UUID playerUUID = player.getUniqueId();
        
        // 检查玩家是否空手
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }
        
        // 检查目标是否为生物或玩家
        if (!(target instanceof LivingEntity)) {
            return;
        }
        
        // 如果玩家已经举着一个实体
        if (carriedEntities.containsKey(playerUUID)) {
            // 检查目标是否是玩家头上堆叠中的实体（防止右键到自己头上的实体）
            Entity baseEntity = carriedEntities.get(playerUUID);
            if (isEntityInStack(baseEntity, target)) {
                // 如果右键的是堆叠中的实体，尝试用射线检测找到真正想右键的实体
                Entity realTarget = findEntityBehindStack(player);
                if (realTarget != null && realTarget instanceof LivingEntity) {
                    stackEntity(player, realTarget);
                }
                event.setCancelled(true);
                return;
            }
            
            // 否则叠加到最顶端
            stackEntity(player, target);
            event.setCancelled(true);
            return;
        }
        
        // 否则举起实体
        pickupEntity(player, target);
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只处理主手交互
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        // 只处理左键动作
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        // 只有在玩家举着实体时才处理
        if (!carriedEntities.containsKey(playerUUID)) {
            return;
        }
        
        // 检查是否空手
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }
        
        // 抛出实体
        throwEntity(player);
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 检查是否是玩家攻击
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        UUID playerUUID = player.getUniqueId();
        
        // 只有在玩家举着实体时才处理
        if (!carriedEntities.containsKey(playerUUID)) {
            return;
        }
        
        // 检查是否空手
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }
        
        // 抛出实体
        throwEntity(player);
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 玩家退出时，清理数据
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        if (carriedEntities.containsKey(playerUUID)) {
            Entity entity = carriedEntities.get(playerUUID);
            if (entity != null && entity.isValid()) {
                // 移除整个堆叠的骑乘关系
                removeAllPassengers(entity);
                entity.leaveVehicle();
            }
            carriedEntities.remove(playerUUID);
        }
    }
    
    /**
     * 递归移除所有passenger
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
     * 举起实体
     */
    private void pickupEntity(Player player, Entity entity) {
        // 检查实体是否已经被其他玩家举起
        if (carriedEntities.containsValue(entity)) {
            return;
        }
        
        // 检查数量限制
        int maxEntities = plugin.getMaxEntities();
        if (maxEntities > 0) {
            // 当前是0个，举起后会变成1个
            if (1 > maxEntities) {
                return;
            }
        }
        
        // 让实体骑乘在玩家头上
        player.addPassenger(entity);
        carriedEntities.put(player.getUniqueId(), entity);
    }
    
    /**
     * 叠加实体（把新实体放到最顶端）
     */
    private void stackEntity(Player player, Entity newEntity) {
        UUID playerUUID = player.getUniqueId();
        Entity baseEntity = carriedEntities.get(playerUUID);
        
        // 检查新实体是否已经在堆叠中（防止实体骑自己）
        if (isEntityInStack(baseEntity, newEntity)) {
            return;
        }
        
        // 检查实体是否已经被其他玩家举起
        if (carriedEntities.containsValue(newEntity)) {
            return;
        }
        
        // 检查数量限制
        int maxEntities = plugin.getMaxEntities();
        if (maxEntities > 0) {
            int currentCount = countEntitiesInStack(baseEntity);
            if (currentCount >= maxEntities) {
                // 已达到上限，不能再叠加
                return;
            }
        }
        
        // 找到当前最顶端的实体
        Entity topEntity = getTopEntity(player);
        
        if (topEntity != null && topEntity.isValid()) {
            // 让新实体骑在最顶端实体头上
            topEntity.addPassenger(newEntity);
        }
    }
    
    /**
     * 检查实体是否已经在堆叠中
     */
    private boolean isEntityInStack(Entity stackBase, Entity target) {
        if (stackBase == null || target == null) {
            return false;
        }
        
        // 检查是否是同一个实体
        if (stackBase.equals(target)) {
            return true;
        }
        
        // 递归检查所有passenger
        for (Entity passenger : stackBase.getPassengers()) {
            if (isEntityInStack(passenger, target)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 计算堆叠中的实体数量
     */
    private int countEntitiesInStack(Entity stackBase) {
        if (stackBase == null) {
            return 0;
        }
        
        int count = 1; // 当前实体算1个
        
        // 递归计算所有passenger
        for (Entity passenger : stackBase.getPassengers()) {
            count += countEntitiesInStack(passenger);
        }
        
        return count;
    }
    
    /**
     * 使用射线检测找到玩家真正想右键的实体（排除堆叠中的实体）
     */
    private Entity findEntityBehindStack(Player player) {
        UUID playerUUID = player.getUniqueId();
        Entity baseEntity = carriedEntities.get(playerUUID);
        
        if (baseEntity == null) {
            return null;
        }
        
        // 使用世界的射线检测找到玩家视线方向上的所有实体
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            5.0, // 5格范围
            0.5, // 实体碰撞箱精度
            entity -> {
                // 过滤条件：必须是生物，且不是玩家自己，且不在堆叠中
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
     * 获取堆叠中最顶端的实体
     */
    private Entity getTopEntity(Player player) {
        UUID playerUUID = player.getUniqueId();
        Entity current = carriedEntities.get(playerUUID);
        
        if (current == null) {
            return null;
        }
        
        // 一直往上找，直到没有passenger为止
        while (!current.getPassengers().isEmpty()) {
            current = current.getPassengers().get(0);
        }
        
        return current;
    }
    
    /**
     * 抛出实体（抛出最顶端的实体）
     */
    private void throwEntity(Player player) {
        UUID playerUUID = player.getUniqueId();
        Entity baseEntity = carriedEntities.get(playerUUID);
        
        if (baseEntity == null || !baseEntity.isValid()) {
            carriedEntities.remove(playerUUID);
            return;
        }
        
        // 获取最顶端的实体
        Entity topEntity = getTopEntity(player);
        
        if (topEntity == null || !topEntity.isValid()) {
            return;
        }
        
        // 移除最顶端实体的骑乘关系
        topEntity.leaveVehicle();
        
        // 计算抛出方向和力度（从配置文件读取）
        Vector direction = player.getLocation().getDirection();
        double horizontalVelocity = plugin.getThrowHorizontalVelocity();
        double verticalVelocity = plugin.getThrowVerticalVelocity();
        Vector velocity = direction.multiply(horizontalVelocity).setY(verticalVelocity);
        
        // 使用Folia的调度器在实体所在区域执行
        topEntity.getScheduler().run(plugin, (task) -> {
            topEntity.setVelocity(velocity);
        }, null);
        
        // 如果抛出的是玩家头上的第一个实体，清除记录
        if (topEntity == baseEntity) {
            carriedEntities.remove(playerUUID);
        }
    }
}
