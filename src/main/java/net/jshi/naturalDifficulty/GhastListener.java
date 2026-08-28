package net.jshi.naturalDifficulty;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GhastListener implements Listener {
    private final JavaPlugin plugin;

    public GhastListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGhastSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Ghast ghast)) return;

        AttributeInstance followRange = ghast.getAttribute(Attribute.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(64.0);
        }

        startRapidFireTask(ghast);
    }

    private void startRapidFireTask(Ghast ghast) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (ghast.isDead() || !ghast.isValid()) {
                    this.cancel();
                    return;
                }

                Player target = null;
                if (ghast.getTarget() instanceof Player p && !p.isDead() && p.isValid()) {
                    target = p;
                } else {
                    double closestDistSq = 4096.0;
                    for (Player p : ghast.getWorld().getPlayers()) {
                        if (p.isDead() || !p.isValid()) continue;
                        double distSq = p.getLocation().distanceSquared(ghast.getLocation());
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            target = p;
                        }
                    }
                    if (target != null) {
                        ghast.setTarget(target);
                    }
                }

                if (target != null && ghast.hasLineOfSight(target)) {
                    // Spawn at actual Ghast eye height
                    Location spawnLoc = ghast.getEyeLocation();

                    // Target torso center (0.8 blocks above ground) instead of eye level
                    Location targetLoc = target.getLocation().add(0, 0.8, 0);

                    Vector direction = targetLoc.toVector().subtract(spawnLoc.toVector()).normalize();

                    LargeFireball fireball = ghast.launchProjectile(LargeFireball.class, direction.multiply(1.3));
                    fireball.setDirection(direction); // Sync acceleration vector to prevent drift
                    fireball.setYield(1);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}