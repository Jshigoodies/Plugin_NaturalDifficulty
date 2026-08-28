package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.util.Vector;

public class CreeperListener implements Listener {

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if(event.getEntity() instanceof Creeper creeper) {
            //charge creeper
            creeper.setPowered(true);

            // increase speed
            AttributeInstance speedAttribute = creeper.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                // Default creeper speed is 0.25
                // Setting it to 0.40 - 0.50 makes them significantly faster
                speedAttribute.setBaseValue(1.0);
            }

            // Increase follow/detection range (Default is 16.0 blocks)
            AttributeInstance followAttribute = creeper.getAttribute(Attribute.FOLLOW_RANGE);
            if (followAttribute != null) {
                followAttribute.setBaseValue(64.0); // Spawns will track players from 64 blocks away
            }

            // Increase step height (Default is 0.6)
            AttributeInstance stepAttribute = creeper.getAttribute(Attribute.STEP_HEIGHT);
            if (stepAttribute != null) {
                stepAttribute.setBaseValue(2.0); // Allows walking over 1-block steps smoothly without jumping
            }

            // Instantly target the nearest player on spawn
            Player nearestPlayer = findNearestPlayer(creeper, 64.0);
            if (nearestPlayer != null) {
                creeper.setTarget(nearestPlayer);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if(event.getEntity() instanceof Creeper creeper) {
            //loses target which does too often
            if (event.getTarget() == null) {
                Player nearestPlayer = findNearestPlayer(creeper, 64.0);
                if (nearestPlayer != null) {
                    event.setTarget(nearestPlayer);
                }
            }
        }
    }

    // Helper method to find the closest survival player within search radius
    private Player findNearestPlayer(Creeper creeper, double radius) {
        Player nearest = null;
        double nearestDistanceSquared = radius * radius;

        for (Player player : creeper.getWorld().getPlayers()) {
            // Ignore creative, spectator, or dead players
            if (player.getGameMode().name().equals("CREATIVE")
                    || player.getGameMode().name().equals("SPECTATOR")
                    || player.isDead()) {
                continue;
            }

            double distanceSquared = creeper.getLocation().distanceSquared(player.getLocation());
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = player;
            }
        }
        return nearest;
    }

    @EventHandler
    public void onCreeperExplodeDamage(EntityDamageByEntityEvent event) {
        if(event.getDamager() instanceof Creeper creeper && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            Entity victim = event.getEntity();

            Vector knockbackDir = victim.getLocation().toVector().subtract(creeper.getLocation().toVector());

            if (knockbackDir.lengthSquared() > 0) {
                knockbackDir.normalize();
            }
            else {
                knockbackDir = new Vector(0, 1, 0); // Default upward push if standing in the exact same spot
            }

            // 1. Multiply horizontal push (3.0x = strong knockback launching players backward)
            knockbackDir.multiply(3.0);

            // 2. Add guaranteed vertical lift so entities launch up into the air
            knockbackDir.setY(Math.max(knockbackDir.getY() + 0.6, 1.2));

            // Apply custom velocity
            victim.setVelocity(knockbackDir);
        }
    }

}
