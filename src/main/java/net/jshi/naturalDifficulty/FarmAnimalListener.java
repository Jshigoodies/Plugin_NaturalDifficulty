package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class FarmAnimalListener implements Listener {

    @EventHandler
    public void onAnimalSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            processAnimal(living);
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity living) {
                processAnimal(living);
            }
        }
    }

    private void processAnimal(LivingEntity animal) {
        if (!(animal instanceof Cow || animal instanceof Pig || animal instanceof Chicken)) {
            return;
        }

        // Prevent scaling the same animal multiple times across chunk unloads/reloads
        if (animal.getScoreboardTags().contains("scaled_animal")) {
            return;
        }
        animal.addScoreboardTag("scaled_animal");

        // Random size multiplier between 1.5x and 3.0x
        double randomScale = ThreadLocalRandom.current().nextDouble(1.5, 3.0);

        AttributeInstance scaleAttribute = animal.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(randomScale);
        }

        AttributeInstance healthAttribute = animal.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute != null) {
            double baseDefaultHealth = (animal instanceof Chicken) ? 4.0 : 10.0;
            double newMaxHealth = Math.round(baseDefaultHealth * Math.pow(randomScale, 2));
            healthAttribute.setBaseValue(newMaxHealth);
            animal.setHealth(newMaxHealth);
        }
    }

    @EventHandler
    public void onAnimalDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Cow || event.getEntity() instanceof Pig || event.getEntity() instanceof Chicken) {
            LivingEntity animal = event.getEntity();

            AttributeInstance scaleAttribute = animal.getAttribute(Attribute.SCALE);
            if (scaleAttribute != null) {
                double scale = scaleAttribute.getValue();

                if (scale > 1.0) {
                    double dropMultiplier = Math.pow(scale, 3);

                    for (ItemStack drop : event.getDrops()) {
                        int totalAmount = (int) Math.round(drop.getAmount() * dropMultiplier);
                        drop.setAmount(Math.min(64, Math.max(1, totalAmount)));
                    }

                    event.setDroppedExp((int) Math.round(event.getDroppedExp() * dropMultiplier));
                }
            }
        }
    }
}