package net.jshi.naturalDifficulty;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ZombieListener implements Listener {

    private final JavaPlugin plugin;
    private final Set<UUID> activeZombies = new HashSet<>();

    public ZombieListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onZombieSpawn(EntitySpawnEvent event)
    {
        if(event.getEntity() instanceof Zombie zombie)
        {
            // 1. Set Max Health
            AttributeInstance healthAttribute = zombie.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttribute != null) {
                double newMaxHealth = 60.0;
                healthAttribute.setBaseValue(newMaxHealth);
                zombie.setHealth(newMaxHealth);
            }

            // 2. Set Follow Range
            AttributeInstance rangeAttribute = zombie.getAttribute(Attribute.FOLLOW_RANGE);
            if (rangeAttribute != null) {
                rangeAttribute.setBaseValue(64.0);
            }
        }
    }

    @EventHandler
    public void onZombieTarget(EntityTargetLivingEntityEvent event)
    {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!(event.getTarget() instanceof Player)) return;

        UUID zombieId = zombie.getUniqueId();
        if (activeZombies.contains(zombieId)) return;

        activeZombies.add(zombieId);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (zombie.isDead() || !zombie.isValid()
                        || zombie.getTarget() == null || zombie.getTarget().isDead()) {
                    activeZombies.remove(zombieId);
                    this.cancel();
                    return;
                }

                LivingEntity target = zombie.getTarget();
                Location zombieLoc = zombie.getLocation();
                Location targetLoc = target.getLocation();

                double dx = targetLoc.getX() - zombieLoc.getX();
                double dz = targetLoc.getZ() - zombieLoc.getZ();
                double yDiff = targetLoc.getY() - zombieLoc.getY();
                double xzDistanceSq = (dx * dx) + (dz * dz);

                // Horizontal direction vector toward player
                Vector dir = new Vector(dx, 0, dz);
                if (dir.lengthSquared() > 0) {
                    dir.normalize();
                }

                Location stepLoc = zombieLoc.clone().add(dir);

                // 1. BLOCK BREAKING: Smash obstacles blocking movement directly in front
                if (xzDistanceSq > 0.64 && xzDistanceSq <= 144.0) {
                    Block feetObstacle = stepLoc.getBlock();
                    Block headObstacle = stepLoc.clone().add(0, 1, 0).getBlock();

                    boolean brokeBlock = false;
                    if (canBreak(feetObstacle)) {
                        feetObstacle.breakNaturally();
                        brokeBlock = true;
                    }
                    if (canBreak(headObstacle)) {
                        headObstacle.breakNaturally();
                        brokeBlock = true;
                    }

                    if (brokeBlock) {
                        zombieLoc.getWorld().playSound(stepLoc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.8f, 1.0f);
                        return; // Yield tick to let block clear before continuing movement
                    }
                }

                // 1. DIAGONAL UPWARD SCAFFOLDING (Player is higher)
                if (yDiff >= 1.0 && xzDistanceSq > 0.64 && xzDistanceSq <= 144.0) {
                    Block targetBlock = stepLoc.getBlock();
                    Block headSpace = stepLoc.clone().add(0, 1, 0).getBlock();

                    if (targetBlock.isPassable() && headSpace.isPassable()) {
                        targetBlock.setType(Material.DIRT);
                        zombieLoc.getWorld().playSound(stepLoc, Sound.BLOCK_GRAVEL_PLACE, 0.8f, 1.2f);

                        Location tpLoc = stepLoc.add(0, 1, 0);
                        tpLoc.setDirection(zombieLoc.getDirection());
                        zombie.teleport(tpLoc);
                    }
                }
                // 2. DIAGONAL DOWNWARD BRIDGING (Player is lower)
                else if (yDiff <= -1.0 && xzDistanceSq > 0.64 && xzDistanceSq <= 144.0) {
                    Block floorUnderStep = stepLoc.clone().subtract(0, 2, 0).getBlock();
                    Block feetSpace = stepLoc.clone().subtract(0, 1, 0).getBlock();
                    Block headSpace = stepLoc.getBlock();

                    // Ensure air exists for the zombie to step down into
                    if (feetSpace.isPassable() && headSpace.isPassable()) {
                        if (floorUnderStep.isPassable()) {
                            floorUnderStep.setType(Material.DIRT);
                            zombieLoc.getWorld().playSound(stepLoc, Sound.BLOCK_GRAVEL_PLACE, 0.8f, 1.2f);
                        }

                        // Teleport down to step seamlessly
                        Location tpLoc = stepLoc.subtract(0, 1, 0);
                        tpLoc.setDirection(zombieLoc.getDirection());
                        zombie.teleport(tpLoc);
                    }
                }
                // 3. VERTICAL SCAFFOLDING (Player directly above)
                else if (yDiff >= 1.5 && xzDistanceSq <= 0.64) {
                    Block headSpace = zombieLoc.clone().add(0, 2, 0).getBlock();
                    if (headSpace.isPassable()) {
                        zombieLoc.getBlock().setType(Material.DIRT);
                        zombieLoc.getWorld().playSound(zombieLoc, Sound.BLOCK_GRAVEL_PLACE, 0.8f, 1.2f);
                        zombie.teleport(zombieLoc.add(0, 1, 0));
                    }
                }
                // 4. FLAT BRIDGING (Gap at same height)
                else if (xzDistanceSq > 1.44 && xzDistanceSq <= 144.0) {
                    Block floorAhead = stepLoc.subtract(0, 1, 0).getBlock();
                    if (floorAhead.isPassable()) {
                        floorAhead.setType(Material.DIRT);
                        zombieLoc.getWorld().playSound(stepLoc, Sound.BLOCK_GRAVEL_PLACE, 0.8f, 1.2f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 6L);
    }

    // Ensures bedrock, command blocks, and air are protected from destruction
    private boolean canBreak(Block block) {
        if (block.isPassable()) return false;
        Material type = block.getType();
        return type != Material.BEDROCK
                && type != Material.BARRIER
                && type != Material.COMMAND_BLOCK
                && type != Material.CHAIN_COMMAND_BLOCK
                && type != Material.REPEATING_COMMAND_BLOCK
                && type != Material.STRUCTURE_BLOCK;
    }
}
