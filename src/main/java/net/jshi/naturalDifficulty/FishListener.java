package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Fish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class FishListener implements Listener {
    @EventHandler
    public void onFishSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Fish fish) {
            double randomScale = ThreadLocalRandom.current().nextDouble(5.0, 12.0);

            AttributeInstance scaleAttribute = fish.getAttribute(Attribute.SCALE);
            if (scaleAttribute != null) {
                scaleAttribute.setBaseValue(randomScale);
            }

            // Boost max health so giant fish are tankier (Default is 3.0)
            AttributeInstance healthAttribute = fish.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttribute != null) {
                double newMaxHealth = 3.0 * randomScale;
                healthAttribute.setBaseValue(newMaxHealth);
                fish.setHealth(newMaxHealth);
            }
        }
    }

    @EventHandler
    public void onFishDeath(EntityDeathEvent event) {
        if(event.getEntity() instanceof Fish fish)
        {
            AttributeInstance scaleAttribute = fish.getAttribute(Attribute.SCALE);
            if (scaleAttribute != null) {
                double scale = scaleAttribute.getValue();

                // Scale up vanilla drops if fish is larger than normal
                if (scale > 1.0) {
                    // Scale 5.0 = 15x drops; Scale 12.0 = 36x drops
                    int dropMultiplier = (int) Math.round(scale * 3.0);

                    for (ItemStack drop : event.getDrops()) {
                        int totalAmount = drop.getAmount() * dropMultiplier;
                        drop.setAmount(Math.min(64, totalAmount)); // Caps single stack size to 64
                    }
                }
            }
        }

    }
}
