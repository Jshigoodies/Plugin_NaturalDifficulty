package net.jshi.naturalDifficulty;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public class FarmAnimalListener implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey scaleKey;

    public FarmAnimalListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scaleKey = new NamespacedKey(plugin, "animal_scale");
    }

    @EventHandler
    public void onAnimalSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Cow || event.getEntity() instanceof Pig || event.getEntity() instanceof Chicken)) {
            return;
        }

        LivingEntity animal = (LivingEntity) event.getEntity();
        double scale;

        if (!animal.getPersistentDataContainer().has(scaleKey, PersistentDataType.DOUBLE)) {
            // Generates scale between 1.5x and 3.0x size
            scale = ThreadLocalRandom.current().nextDouble(1.5, 3.0);
            animal.getPersistentDataContainer().set(scaleKey, PersistentDataType.DOUBLE, scale);
        } else {
            // Retrieve existing scale if re-spawning/loading chunks
            Double storedScale = animal.getPersistentDataContainer().get(scaleKey, PersistentDataType.DOUBLE);
            scale = (storedScale != null) ? storedScale : 1.0;
        }

        applyScaleAndHealth(animal, scale);
    }

    @EventHandler
    public void onAnimalDeath(EntityDeathEvent event) {
        LivingEntity animal = event.getEntity();
        if (!(animal instanceof Cow || animal instanceof Pig || animal instanceof Chicken)) {
            return;
        }

        Double scale = animal.getPersistentDataContainer().get(scaleKey, PersistentDataType.DOUBLE);
        if (scale == null) return;

        // Volumetric (Cubic) drop multiplier
        double dropMultiplier = Math.pow(scale, 3);

        for (ItemStack drop : event.getDrops()) {
            int originalAmount = drop.getAmount();
            int newAmount = (int) Math.round(originalAmount * dropMultiplier);
            drop.setAmount(Math.max(1, newAmount));
        }

        event.setDroppedExp((int) Math.round(event.getDroppedExp() * dropMultiplier));
    }

    private void applyScaleAndHealth(LivingEntity entity, double scale) {
        // Apply Visual & Hitbox Scale
        AttributeInstance scaleAttribute = entity.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }

        // Apply Health Multiplier (Scales quadratically: scale^2)
        AttributeInstance healthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute == null) {
            healthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        }

        if (healthAttribute != null) {
            double baseDefaultHealth = 10.0; // Standard vanilla cow/pig base health
            if (entity instanceof Chicken) {
                baseDefaultHealth = 4.0;
            }

            double scaledMaxHealth = Math.round(baseDefaultHealth * Math.pow(scale, 2));
            healthAttribute.setBaseValue(scaledMaxHealth);
            entity.setHealth(scaledMaxHealth); // Fill health bar to max on spawn
        }
    }
}