package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;

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

            // increase health
            AttributeInstance healthAttribute = creeper.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttribute != null) {
                double newMaxHealth = 40.0; // 40.0 = 20 hearts (double health)
                healthAttribute.setBaseValue(newMaxHealth);
                // Heal the creeper to full with the new max health
                creeper.setHealth(newMaxHealth);
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

}
