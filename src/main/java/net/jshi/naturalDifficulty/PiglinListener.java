package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.util.concurrent.ThreadLocalRandom;

public class PiglinListener implements Listener {

    @EventHandler
    public void onPiglinSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Piglin piglin) {
            processPiglin(piglin);
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Piglin piglin) {
                processPiglin(piglin);
            }
        }
    }

    private void processPiglin(Piglin original) {
        // Prevents duplicate horde spawning when chunks reload or entities are created by horde loop
        if (original.getScoreboardTags().contains("horde_piglin")) {
            return;
        }
        original.addScoreboardTag("horde_piglin");

        applyScaleAndAttributes(original);

        // Spawn 19 additional Piglins (20 total)
        for (int i = 0; i < 19; i++) {
            original.getWorld().spawn(original.getLocation(), Piglin.class, extra -> {
                extra.addScoreboardTag("horde_piglin");
                applyScaleAndAttributes(extra);
            });
        }
    }

    private void applyScaleAndAttributes(Piglin piglin) {
        double scale = ThreadLocalRandom.current().nextDouble(1.0, 2.5);

        AttributeInstance scaleAttribute = piglin.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }

        AttributeInstance healthAttribute = piglin.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute != null) {
            double scaledHealth = Math.round(16.0 * Math.pow(scale, 1.5));
            healthAttribute.setBaseValue(scaledHealth);
            piglin.setHealth(scaledHealth);
        }

        AttributeInstance damageAttribute = piglin.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttribute != null) {
            damageAttribute.setBaseValue(Math.round(5.0 * scale));
        }

        AttributeInstance kbAttribute = piglin.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttribute != null) {
            double kbResistance = Math.max(0.0, Math.min(1.0, (scale - 1.0) * 0.6));
            kbAttribute.setBaseValue(kbResistance);
        }
    }
}