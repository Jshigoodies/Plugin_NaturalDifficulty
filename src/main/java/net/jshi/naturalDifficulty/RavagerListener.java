package net.jshi.naturalDifficulty;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Ravager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class RavagerListener implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey scaleKey;

    public RavagerListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scaleKey = new NamespacedKey(plugin, "ravager_scale");
    }

    @EventHandler
    public void onRavagerSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Ravager ravager)) return;

        double scale;
        if (!ravager.getPersistentDataContainer().has(scaleKey, PersistentDataType.DOUBLE)) {
            // Random size multiplier between 1.8x and 2.5x size
            scale = ThreadLocalRandom.current().nextDouble(5.0, 6.5);
            ravager.getPersistentDataContainer().set(scaleKey, PersistentDataType.DOUBLE, scale);
        } else {
            Double stored = ravager.getPersistentDataContainer().get(scaleKey, PersistentDataType.DOUBLE);
            scale = (stored != null) ? stored : 2.0;
        }

        applyScaleAndHealth(ravager, scale);
        startBlockBreakerTask(ravager, scale);
    }

    @EventHandler
    public void onRavagerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Ravager ravager) {
            Location impactPoint = event.getEntity().getLocation();

            // Creates a block-breaking explosion on attack target
            ravager.getWorld().createExplosion(impactPoint, 3.5F, true, true);
        }
    }

    private void startBlockBreakerTask(Ravager ravager, double scale) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (ravager.isDead() || !ravager.isValid()) {
                    this.cancel();
                    return;
                }

                // Dynamically expand destruction area based on scaled hitbox size
                int radius = (int) Math.ceil(scale * 1.2);
                int height = (int) Math.ceil(scale * 2.2);

                Location loc = ravager.getLocation();
                Vector forward = loc.getDirection().setY(0).normalize();

                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        for (int y = 0; y <= height; y++) {
                            Block block = loc.clone().add(x, y, z).getBlock();

                            if (isBreakable(block.getType())) {
                                // Protect ground directly underneath feet so it doesn't dig itself into a hole
                                if (y == 0) {
                                    Vector offset = block.getLocation().toVector().subtract(loc.toVector()).setY(0);
                                    if (offset.lengthSquared() > 0 && offset.normalize().dot(forward) < 0.2) {
                                        continue;
                                    }
                                }

                                block.breakNaturally();
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 3L); // Ticks every 0.15 seconds
    }

    private boolean isBreakable(Material material) {
        if (material.isAir()) return false;
        // Whitelist safe utility blocks
        if (material == Material.BEDROCK || material == Material.BARRIER || material == Material.END_PORTAL_FRAME) return false;
        if (material == Material.COMMAND_BLOCK || material == Material.CHAIN_COMMAND_BLOCK || material == Material.REPEATING_COMMAND_BLOCK) return false;
        return true;
    }

    private void applyScaleAndHealth(Ravager ravager, double scale) {
        AttributeInstance scaleAttribute = ravager.getAttribute(Attribute.SCALE);
        if (scaleAttribute == null) {
            scaleAttribute = ravager.getAttribute(Attribute.SCALE);
        }
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }

        AttributeInstance healthAttribute = ravager.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute == null) {
            healthAttribute = ravager.getAttribute(Attribute.MAX_HEALTH);
        }

        if (healthAttribute != null) {
            double scaledMaxHealth = Math.round(100.0 * scale * 1.5);
            healthAttribute.setBaseValue(scaledMaxHealth);
            ravager.setHealth(scaledMaxHealth);
        }
    }
}