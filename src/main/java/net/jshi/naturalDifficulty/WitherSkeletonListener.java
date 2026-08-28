package net.jshi.naturalDifficulty;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WitherSkeletonListener implements Listener {
    private final JavaPlugin plugin;

    public WitherSkeletonListener(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWitherSkeletonSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton witherSkeleton)) return;

        applyAttributes(witherSkeleton);
    }

    private void applyAttributes(WitherSkeleton witherSkeleton) {
        // Boosted Movement Speed (Vanilla base = ~0.25 -> 0.45 for rapid sprint speed)
        AttributeInstance speedAttribute = witherSkeleton.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(1.0);
        }

        AttributeInstance healthAttribute = witherSkeleton.getAttribute(Attribute.MAX_HEALTH);
        if(healthAttribute != null)
        {
            healthAttribute.setBaseValue(60.0);
            witherSkeleton.setHealth(60.0);
        }

        // Expanded Detection / Follow Range (Vanilla base = 16 blocks -> 64 blocks)
        AttributeInstance followRange = witherSkeleton.getAttribute(Attribute.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(64.0);
        }
    }

}
