package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.util.concurrent.ThreadLocalRandom;

public class PiglinBruteListener implements Listener {

    @EventHandler
    public void onPiglinBruteSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof PiglinBrute brute) {
            processPiglinBrute(brute);
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof PiglinBrute brute) {
                processPiglinBrute(brute);
            }
        }
    }

    private void processPiglinBrute(PiglinBrute original) {
        // Prevents duplicate horde spawning when Bastion chunks reload or entities are created by horde loop
        if (original.getScoreboardTags().contains("horde_piglin_brute")) {
            return;
        }
        original.addScoreboardTag("horde_piglin_brute");

        applyScaleAndAttributes(original);

        // Spawn 19 additional Piglin Brutes (20 total)
        for (int i = 0; i < 19; i++) {
            original.getWorld().spawn(original.getLocation(), PiglinBrute.class, extra -> {
                extra.addScoreboardTag("horde_piglin_brute");
                applyScaleAndAttributes(extra);
            });
        }
    }

    private void applyScaleAndAttributes(PiglinBrute brute) {
        double scale = ThreadLocalRandom.current().nextDouble(1.0, 2.5);

        AttributeInstance scaleAttribute = brute.getAttribute(Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }

        AttributeInstance healthAttribute = brute.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute != null) {
            double scaledHealth = Math.round(50.0 * Math.pow(scale, 1.5));
            healthAttribute.setBaseValue(scaledHealth);
            brute.setHealth(scaledHealth);
        }

        AttributeInstance damageAttribute = brute.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttribute != null) {
            damageAttribute.setBaseValue(Math.round(12.0 * scale));
        }

        AttributeInstance kbAttribute = brute.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttribute != null) {
            double kbResistance = Math.max(0.0, Math.min(1.0, (scale - 1.0) * 0.6));
            kbAttribute.setBaseValue(kbResistance);
        }
    }
}