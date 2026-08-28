package net.jshi.naturalDifficulty;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BlazeListener implements Listener {
    private final JavaPlugin plugin;

    public BlazeListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlazeSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Blaze blaze)) return;

        // Increase targeting range so orbit starts from further away
        AttributeInstance followRange = blaze.getAttribute(Attribute.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(32.0);
        }

        startOrbitAndAttackTask(blaze);
    }

    @EventHandler
    public void onVanillaBlazeShoot(ProjectileLaunchEvent event) {
        // Block vanilla small fireballs so only our big fireballs spawn
        if (event.getEntity() instanceof SmallFireball && event.getEntity().getShooter() instanceof Blaze) {
            event.setCancelled(true);
        }
    }

    private void startOrbitAndAttackTask(Blaze blaze) {
        new BukkitRunnable() {
            private double angle = 0.0;
            private int shootTimer = 0;

            @Override
            public void run() {
                if (blaze.isDead() || !blaze.isValid()) {
                    this.cancel();
                    return;
                }

                // Acquire target player
                Player target = null;
                if (blaze.getTarget() instanceof Player p && !p.isDead() && p.isValid()) {
                    target = p;
                } else {
                    double closestDistSq = 1024.0; // 32 blocks
                    for (Player p : blaze.getWorld().getPlayers()) {
                        if (p.isDead() || !p.isValid()) continue;
                        double distSq = p.getLocation().distanceSquared(blaze.getLocation());
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            target = p;
                        }
                    }
                    if (target != null) {
                        blaze.setTarget(target);
                    }
                }

                if (target == null) return;

                // --- 1. Circle Orbit Movement ---
                Location targetLoc = target.getLocation();
                double orbitRadius = 40.0; // Orbit distance in blocks
                double orbitHeight = 20.0; // Flight altitude relative to player feet

                angle += 0.07; // Angular speed
                if (angle > Math.PI * 2) {
                    angle -= Math.PI * 2;
                }

                double targetX = targetLoc.getX() + orbitRadius * Math.cos(angle);
                double targetZ = targetLoc.getZ() + orbitRadius * Math.sin(angle);
                double targetY = targetLoc.getY() + orbitHeight;

                Vector desiredVector = new Vector(targetX, targetY, targetZ).subtract(blaze.getLocation().toVector());
                if (desiredVector.lengthSquared() > 0.05) {
                    blaze.setVelocity(desiredVector.normalize().multiply(0.35));
                }

                // --- 2. Big Fireball Attack ---
                shootTimer++;
                if (shootTimer >= 20 && blaze.hasLineOfSight(target)) { // Shoots every 1.0 second
                    shootTimer = 0;

                    Location spawnLoc = blaze.getEyeLocation();
                    Location targetAim = target.getLocation().add(0, 1.0, 0); // Chest height
                    Vector direction = targetAim.toVector().subtract(spawnLoc.toVector()).normalize();

                    LargeFireball fireball = blaze.launchProjectile(LargeFireball.class, direction.multiply(1.2));
                    fireball.setDirection(direction);

                    // Controls both explosion yield and visible fireball size in vanilla engine
                    fireball.setYield(4);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private AttributeInstance getAttribute(Blaze blaze, Attribute primary, Attribute fallback) {
        AttributeInstance instance = blaze.getAttribute(primary);
        return (instance != null) ? instance : blaze.getAttribute(fallback);
    }
}