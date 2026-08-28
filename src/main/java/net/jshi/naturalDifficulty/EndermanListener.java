package net.jshi.naturalDifficulty;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EndermanListener implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();

    public EndermanListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEndermanSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Enderman enderman) {
            AttributeInstance rangeAttribute = enderman.getAttribute(Attribute.FOLLOW_RANGE);
            if (rangeAttribute != null) {
                rangeAttribute.setBaseValue(64.0);
            }

            UUID endermanId = enderman.getUniqueId();

            new BukkitRunnable() {
                @Override
                public void run() {
                    // 1. Exit & cancel if dead, invalid, or ALREADY AGGROED (e.g. hit by player)
                    if (enderman.isDead() || !enderman.isValid() || enderman.getTarget() != null) {
                        teleportCooldowns.remove(endermanId);
                        this.cancel();
                        return;
                    }

                    // Find nearest player within 64 blocks
                    Player targetPlayer = null;
                    double closestDistSq = 4096.0;

                    for (Player player : enderman.getWorld().getPlayers()) {
                        if (player.isDead() || !player.isValid()) continue;

                        double distSq = player.getLocation().distanceSquared(enderman.getLocation());
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            targetPlayer = player;
                        }
                    }

                    if (targetPlayer != null) {
                        Location playerEye = targetPlayer.getEyeLocation();
                        Location endermanEye = enderman.getEyeLocation();

                        Vector playerDir = playerEye.getDirection().normalize();
                        Vector toEnderman = endermanEye.toVector().subtract(playerEye.toVector()).normalize();

                        double dotProduct = playerDir.dot(toEnderman);

                        // 2. AGGRO CHECK: If on screen (~70%+ FOV cone) with line of sight
                        if (dotProduct > 0.70 && targetPlayer.hasLineOfSight(enderman)) {
                            enderman.setTarget(targetPlayer);

                            // Stop passive task immediately so standard attack pathfinding takes over
                            teleportCooldowns.remove(endermanId);
                            this.cancel();
                            return;
                        }

                        // 3. PASSIVE BEHAVIOR: Rotate to stare directly at the player
                        Vector toPlayer = playerEye.toVector().subtract(endermanEye.toVector());
                        Location currentLoc = enderman.getLocation();
                        currentLoc.setDirection(toPlayer);

                        // 4. PASSIVE CROSSHAIR TELEPORTATION (Runs every 1.5s while passive)
                        long currentTime = System.currentTimeMillis();
                        long lastTp = teleportCooldowns.getOrDefault(endermanId, 0L);

                        if (currentTime - lastTp >= 1500) {
                            RayTraceResult ray = targetPlayer.getWorld().rayTraceBlocks(playerEye, playerDir, 32.0);
                            Location tpTarget;

                            if (ray != null && ray.getHitBlock() != null) {
                                tpTarget = ray.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
                            } else {
                                tpTarget = playerEye.clone().add(playerDir.multiply(6.0));
                            }

                            Vector lookAtPlayer = playerEye.toVector().subtract(tpTarget.toVector());
                            tpTarget.setDirection(lookAtPlayer);

                            enderman.getWorld().playSound(enderman.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                            enderman.teleport(tpTarget);
                            enderman.getWorld().playSound(tpTarget, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

                            teleportCooldowns.put(endermanId, currentTime);
                        } else {
                            // Turn head/body in place between jumps
                            enderman.teleport(currentLoc);
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }
}