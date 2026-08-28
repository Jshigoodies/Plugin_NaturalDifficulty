package net.jshi.naturalDifficulty;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public class PiglinListener implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey hordeKey;
    private final NamespacedKey scaleKey;

    public PiglinListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.hordeKey = new NamespacedKey(plugin, "horde_piglin");
        this.scaleKey = new NamespacedKey(plugin, "piglin_scale");
    }

    @EventHandler
    public void onPiglinSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Piglin original)) return;

        // Prevent infinite recursion loops when spawning extra piglins
        if (original.getPersistentDataContainer().has(hordeKey, PersistentDataType.BYTE)) {
            applyScaleAndAttributes(original);
            return;
        }

        // Mark the original Piglin
        original.getPersistentDataContainer().set(hordeKey, PersistentDataType.BYTE, (byte) 1);
        applyScaleAndAttributes(original);

        // Spawn 19 additional Piglins (20 total)
        for (int i = 0; i < 19; i++) {
            original.getWorld().spawn(original.getLocation(), Piglin.class, extra -> {
                extra.getPersistentDataContainer().set(hordeKey, PersistentDataType.BYTE, (byte) 1);
                applyScaleAndAttributes(extra);
            });
        }
    }

    private void applyScaleAndAttributes(Piglin piglin) {
        double scale;
        if (!piglin.getPersistentDataContainer().has(scaleKey, PersistentDataType.DOUBLE)) {
            // Random size multiplier between 1.0x and 2.5x
            scale = ThreadLocalRandom.current().nextDouble(1.0, 2.5);
            piglin.getPersistentDataContainer().set(scaleKey, PersistentDataType.DOUBLE, scale);
        } else {
            Double stored = piglin.getPersistentDataContainer().get(scaleKey, PersistentDataType.DOUBLE);
            scale = (stored != null) ? stored : 2.0;
        }

        // Visual & Hitbox Scaling
        AttributeInstance scaleAttribute = getAttribute(piglin, Attribute.SCALE, Attribute.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(scale);
        }

        // Max Health Scaling (Base vanilla = 16 HP / 8 hearts)
        AttributeInstance healthAttribute = getAttribute(piglin, Attribute.MAX_HEALTH, Attribute.MAX_HEALTH);
        if (healthAttribute != null) {
            double scaledHealth = Math.round(16.0 * Math.pow(scale, 1.5)); // 1.0x = 16 HP, 2.5x = ~63 HP
            healthAttribute.setBaseValue(scaledHealth);
            piglin.setHealth(scaledHealth);
        }

        // Melee Attack Damage Scaling (Base vanilla = 5 HP)
        AttributeInstance damageAttribute = getAttribute(piglin, Attribute.ATTACK_DAMAGE, Attribute.ATTACK_DAMAGE);
        if (damageAttribute != null) {
            damageAttribute.setBaseValue(Math.round(5.0 * scale));
        }

        // Knockback Resistance (Base = 0.0, max cap = 1.0)
        AttributeInstance kbAttribute = getAttribute(piglin, Attribute.KNOCKBACK_RESISTANCE, Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttribute != null) {
            double kbResistance = Math.max(0.0, Math.min(1.0, (scale - 1.0) * 0.6)); // 0.0 to 0.90 resistance
            kbAttribute.setBaseValue(kbResistance);
        }
    }

    private AttributeInstance getAttribute(Piglin entity, Attribute primary, Attribute fallback) {
        AttributeInstance instance = entity.getAttribute(primary);
        return (instance != null) ? instance : entity.getAttribute(fallback);
    }
}