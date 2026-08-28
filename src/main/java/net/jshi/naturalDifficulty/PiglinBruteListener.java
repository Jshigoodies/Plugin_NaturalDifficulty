package net.jshi.naturalDifficulty;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public class PiglinBruteListener implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey hordeKey;
    private final NamespacedKey scaleKey;

    public PiglinBruteListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.hordeKey = new NamespacedKey(plugin, "horde_piglin_brute");
        this.scaleKey = new NamespacedKey(plugin, "piglin_brute_scale");
    }

    @EventHandler
    public void onPiglinBruteSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof PiglinBrute original)) return;

        // Prevent infinite recursion loops when spawning extra piglin brutes
        if (original.getPersistentDataContainer().has(hordeKey, PersistentDataType.BYTE)) {
            applyScaleAndAttributes(original);
            return;
        }

        // Mark the original Piglin Brute
        original.getPersistentDataContainer().set(hordeKey, PersistentDataType.BYTE, (byte) 1);
        applyScaleAndAttributes(original);

        // Spawn 19 additional Piglin Brutes (20 total)
        for (int i = 0; i < 19; i++) {
            original.getWorld().spawn(original.getLocation(), PiglinBrute.class, extra -> {
                extra.getPersistentDataContainer().set(hordeKey, PersistentDataType.BYTE, (byte) 1);
                applyScaleAndAttributes(extra);
            });
        }
    }

    private void applyScaleAndAttributes(PiglinBrute brute) {
        double scale;
        if (!brute.getPersistentDataContainer().has(scaleKey, PersistentDataType.DOUBLE)) {
            // Random size multiplier between 1.8x and 2.5x
            scale = ThreadLocalRandom.current().nextDouble(1.0, 2.5);
            brute.getPersistentDataContainer().set(scaleKey, PersistentDataType.DOUBLE, scale);
        } else {
            Double stored = brute.getPersistentDataContainer().get(scaleKey, PersistentDataType.DOUBLE);
            scale = (stored != null) ? stored : 2.0;
        }

        // Visual & Hitbox Scaling
        AttributeInstance scaleAttribute = getAttribute(brute, Attribute.SCALE, Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }

        // Max Health Scaling (Base vanilla = 50 HP / 25 hearts)
        AttributeInstance healthAttribute = getAttribute(brute, Attribute.MAX_HEALTH, Attribute.MAX_HEALTH);
        if (healthAttribute != null) {
            double scaledHealth = Math.round(50.0 * Math.pow(scale, 1.5)); // 1.8x = ~120 HP, 2.5x = ~198 HP
            healthAttribute.setBaseValue(scaledHealth);
            brute.setHealth(scaledHealth);
        }

        // Melee Attack Damage Scaling (Base vanilla = 12 HP before golden axe)
        AttributeInstance damageAttribute = getAttribute(brute, Attribute.ATTACK_DAMAGE, Attribute.ATTACK_DAMAGE);
        if (damageAttribute != null) {
            damageAttribute.setBaseValue(Math.round(12.0 * scale));
        }

        // Knockback Resistance (Base = 0.0, max cap = 1.0)
        AttributeInstance kbAttribute = getAttribute(brute, Attribute.KNOCKBACK_RESISTANCE, Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttribute != null) {
            double kbResistance = Math.min(1.0, (scale - 1.0) * 0.6); // Scales from 0.48 to 0.90 resistance
            kbAttribute.setBaseValue(kbResistance);
        }
    }

    private AttributeInstance getAttribute(PiglinBrute entity, Attribute primary, Attribute fallback) {
        AttributeInstance instance = entity.getAttribute(primary);
        return (instance != null) ? instance : entity.getAttribute(fallback);
    }
}