package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SkeletonListener implements Listener {
    private final JavaPlugin plugin;

    private final Set<UUID> activeSkeletons = new HashSet<>();

    public SkeletonListener(JavaPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onSkseltonSpawn(EntitySpawnEvent event) {
        if(event.getEntity() instanceof Skeleton skeleton) {
            AttributeInstance healthAttribute = skeleton.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttribute != null) {
                double newMaxHealth = 30.0; // 20 hearts
                healthAttribute.setBaseValue(newMaxHealth);
                skeleton.setHealth(newMaxHealth); // Fill health bar to new max
            }

            // 2. Increase detection/follow range (Default is 16.0 blocks)
            AttributeInstance followAttribute = skeleton.getAttribute(Attribute.FOLLOW_RANGE);
            if (followAttribute != null) {
                followAttribute.setBaseValue(25.0); // Spottable/trackable up to 64 blocks
            }
        }
    }

    @EventHandler
    public void onSkeletonTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Skeleton skeleton)) return;

        LivingEntity target = event.getTarget();
        if (target == null) return;

        UUID skeletonId = skeleton.getUniqueId();

        if (activeSkeletons.contains(skeletonId)) {
            return;
        }

        activeSkeletons.add(skeletonId);

        // Continuous loop that fires arrows at fixed tick intervals
        new BukkitRunnable() {
            @Override
            public void run() {
                // Cancel the task if the skeleton dies, loses target, or target dies
                if (skeleton.isDead() || !skeleton.isValid()
                        || skeleton.getTarget() == null || skeleton.getTarget().isDead()) {
                    activeSkeletons.remove(skeletonId);
                    this.cancel();
                    return;
                }

                if(skeleton.hasLineOfSight(skeleton.getTarget())) {
                    LivingEntity currentTarget = skeleton.getTarget();

                    Vector direction = currentTarget.getEyeLocation().toVector()
                            .subtract(skeleton.getEyeLocation().toVector())
                            .normalize();
                    // Spawn the arrow with custom velocity
                    Arrow arrow = skeleton.launchProjectile(Arrow.class, direction.multiply(3.0));
                    arrow.setShooter(skeleton);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onSkeletonDamage(EntityDamageByEntityEvent event) {
        // Prevent skeletons from taking damage from other skeletons or their arrows
        if (event.getEntity() instanceof Skeleton) {
            if (event.getDamager() instanceof Skeleton) {
                event.setCancelled(true);
            } else if (event.getDamager() instanceof Arrow arrow) {
                if (arrow.getShooter() instanceof Skeleton) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onVanillaShoot(EntityShootBowEvent event) {
        // Cancel vanilla bow shots so our custom timer fully controls the fire rate
        if (event.getEntity() instanceof Skeleton) {
            event.setCancelled(true);
        }
    }
}
